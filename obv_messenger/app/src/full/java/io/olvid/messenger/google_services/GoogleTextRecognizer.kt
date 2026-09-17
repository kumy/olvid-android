/*
 *  Olvid for Android
 *  Copyright © 2019-2026 Olvid SAS
 *
 *  This file is part of Olvid for Android.
 *
 *  Olvid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  Olvid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with Olvid.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.olvid.messenger.google_services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.olvid.engine.Logger
import io.olvid.messenger.App
import io.olvid.messenger.customClasses.TextBlock
import io.olvid.messenger.customClasses.TextElement
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Text recognition runs strictly one image at a time on a dedicated thread.
 * A single [TextRecognizer] is shared by all recognitions and released once the
 * queue drains.
 *
 * Images larger than [MAX_DIMENSION] on their long side are decoded downscaled to
 * that size. Bounding boxes are scaled back to the original image coordinates before
 * being returned.
 */
class GoogleTextRecognizer {
    companion object {
        private val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "TextRecognizer")
        }
        private val pending = AtomicInteger(0)
        private var recognizer: TextRecognizer? = null

        private const val MAX_DIMENSION = 2048

        private class DecodedImage(val image: InputImage, val bitmap: Bitmap?, val scale: Float)

        private fun decode(uri: Uri): DecodedImage {
            val path = uri.path
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (path != null) {
                BitmapFactory.decodeFile(path, bounds)
            }
            val longSide = max(bounds.outWidth, bounds.outHeight)
            if (path == null || longSide <= MAX_DIMENSION) {
                return DecodedImage(InputImage.fromFilePath(App.getContext(), uri), null, 1f)
            }

            // largest power of two subsampling that keeps the long side >= MAX_DIMENSION,
            // then density scaling brings it exactly down to MAX_DIMENSION
            var sampleSize = 1
            while (longSide / (sampleSize * 2) >= MAX_DIMENSION) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inScaled = true
                inDensity = longSide / sampleSize
                inTargetDensity = MAX_DIMENSION
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(path, options)
                ?: return DecodedImage(InputImage.fromFilePath(App.getContext(), uri), null, 1f)
            val rotation = runCatching { ExifInterface(path).rotationDegrees }.getOrDefault(0)
            return DecodedImage(
                InputImage.fromBitmap(bitmap, rotation),
                bitmap,
                longSide.toFloat() / max(bitmap.width, bitmap.height)
            )
        }

        private fun Rect?.scaled(scale: Float): Rect? {
            if (this == null || scale == 1f) {
                return this
            }
            return Rect(
                (left * scale).roundToInt(),
                (top * scale).roundToInt(),
                (right * scale).roundToInt(),
                (bottom * scale).roundToInt()
            )
        }

        @JvmStatic
        fun recognizeTextFromImage(
            uri: Uri,
            onSuccess: (List<TextBlock>?) -> Unit
        ) {
            pending.incrementAndGet()
            executor.execute {
                runCatching {
                    val client = recognizer
                        ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                            .also { recognizer = it }
                    val decoded = decode(uri)
                    try {
                        Tasks.await(client.process(decoded.image)) to decoded.scale
                    } finally {
                        decoded.bitmap?.recycle()
                    }
                }.onSuccess { (text, scale) ->
                    val textBlocks = text.textBlocks.map { block ->
                        TextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox.scaled(scale),
                            elements = block.lines.flatMap { line ->
                                line.elements.map { element ->
                                    TextElement(
                                        element.text,
                                        element.boundingBox.scaled(scale)
                                    )
                                }
                            }
                        )
                    }
                    onSuccess(textBlocks)
                }.onFailure { e ->
                    Logger.e("Text recognition failed: $e")
                }
                if (pending.decrementAndGet() == 0) {
                    recognizer?.close()
                    recognizer = null
                }
            }
        }
    }
}
