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

package io.olvid.messenger.share

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import io.olvid.engine.Logger
import io.olvid.messenger.customClasses.PreviewUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * PdfRenderer is documented as not safe across overlapping instances on different threads
 * (see PdfBitmapConverter.kt:100-101). Serialize all share-preview PDF renders so a fast
 * carousel switch can't open two PdfRenderers concurrently.
 */
private val pdfRenderMutex = Mutex()

/**
 * Renders the first page of a PDF at the given short-edge size, served from a content URI.
 *
 * The actual render loop is shared with the in-app thumbnail path via
 * [PreviewUtils.renderPdfFirstPage]; this wrapper only adds what is share-specific: opening the
 * PDF from a content URI (no on-disk Fyle yet), serializing renders through [pdfRenderMutex], and
 * propagating coroutine cancellation.
 */
suspend fun renderUriPdfFirstPage(
    resolver: ContentResolver,
    uri: Uri,
    sizePx: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    pdfRenderMutex.withLock {
        try {
            resolver.openFileDescriptor(uri, "r")?.use { fd ->
                PreviewUtils.renderPdfFirstPage(fd, sizePx)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.d("renderUriPdfFirstPage failed: " + e.message)
            null
        }
    }
}
