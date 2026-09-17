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
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import io.olvid.messenger.App
import io.olvid.messenger.customClasses.PreviewUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShareAttachment(
    val uri: Uri,
    val mime: String,
    val fileName: String,
    val sizeBytes: Long?,
    val lastModifiedMillis: Long? = null,
) {
    val kind: Kind
        get() = when {
            mime.startsWith("image/") -> Kind.IMAGE
            mime.startsWith("video/") -> Kind.VIDEO
            mime.startsWith("audio/") -> Kind.AUDIO
            else -> Kind.FILE
        }

    enum class Kind { IMAGE, VIDEO, AUDIO, FILE }
}

/**
 * Resolve display metadata for a content URI, mirroring [AddFyleToDraftFromUriTask] so the
 * share-sheet preview shows the same name + mime the file would have once attached.
 *
 * The [intentTypeHint] matters for in-app Olvid shares where [FyleContentProvider] serves
 * files keyed by SHA-256 (no extension): getType returns the right mime via the provider,
 * but a sender that only sets the intent type leaves us nothing else to go on.
 */
fun resolveShareAttachment(
    resolver: ContentResolver,
    uri: Uri,
    intentTypeHint: String? = null,
): ShareAttachment {
    var name: String? = null
    var size: Long? = null
    var lastModified: Long? = null
    runCatching {
        // Explicit projection: some restrictive providers (enterprise MDM, certain cloud
        // DocumentsProviders) reject null projections, and cloud providers can do extra work
        // to materialise unused columns. Cursor.getColumnIndex returns -1 for absent columns,
        // which the code below handles, so listing all four is safe regardless of provider.
        val projection = arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                    name = cursor.getString(nameIdx)
                }
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    size = cursor.getLong(sizeIdx)
                }
                val documentDateIdx =
                    cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val mediaStoreDateIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val rawDate = when {
                    documentDateIdx >= 0 && !cursor.isNull(documentDateIdx) -> cursor.getLong(
                        documentDateIdx
                    )

                    mediaStoreDateIdx >= 0 && !cursor.isNull(mediaStoreDateIdx) -> cursor.getLong(
                        mediaStoreDateIdx
                    )

                    else -> 0L
                }
                if (rawDate > 0L) {
                    // MediaStore is documented in seconds; DocumentsContract in millis. Distinguish
                    // by magnitude (anything before year 2001 in millis is implausible for a
                    // user-shared file; same number interpreted as seconds is ~year 32969 — safely
                    // not-a-second value). 1e12 millis ≈ 2001-09-09.
                    lastModified = if (rawDate < 1_000_000_000_000L) rawDate * 1000 else rawDate
                }
            }
        }
    }
    // intent.type is the Sharesheet routing hint for the whole share. When the sender attaches
    // both EXTRA_STREAM and EXTRA_TEXT (image + caption) and sets intent.type = "text/plain"
    // because the caption is what they consider primary, the hint describes the text, not the
    // URI — EXTRA_TEXT is already captured separately by sharedText. Skip text/* hints so a
    // provider that doesn't expose getType doesn't misclassify the image as a text file.
    val usableHint = intentTypeHint?.takeIf { !it.startsWith("text/") }
    val rawMime = resolver.getType(uri) ?: usableHint
    val mime = PreviewUtils.getNonNullMimeType(rawMime, name)
    val fileName = name ?: run {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        val timestamp =
            SimpleDateFormat(App.TIMESTAMP_FILE_NAME_FORMAT, Locale.ENGLISH).format(Date())
        if (extension != null) "$timestamp.$extension" else timestamp
    }
    return ShareAttachment(
        uri = uri,
        mime = mime,
        fileName = fileName,
        sizeBytes = size,
        lastModifiedMillis = lastModified,
    )
}
