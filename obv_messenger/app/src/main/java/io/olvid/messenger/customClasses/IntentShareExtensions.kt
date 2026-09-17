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

package io.olvid.messenger.customClasses

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri

/**
 * Attach an outbound share URI plus the metadata the system Sharesheet needs to render a
 * thumbnail and grant the chosen target read access. Equivalent to setting EXTRA_STREAM +
 * type + ClipData + FLAG_GRANT_READ_URI_PERMISSION; without those, Android falls back to a
 * generic file icon and the target may fail with SecurityException on API 31+.
 */
fun Intent.attachShareUri(uri: Uri, mime: String): Intent {
    putExtra(Intent.EXTRA_STREAM, uri)
    type = mime
    clipData = ClipData(ClipDescription(null, arrayOf(mime)), ClipData.Item(uri))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return this
}

/**
 * Multi-uri variant used by ACTION_SEND_MULTIPLE. The ClipData carries one Item per uri,
 * the description's mime list mirrors them so receivers can read per-item types via
 * ClipData.getDescription().
 */
fun Intent.attachShareUris(uris: List<Pair<Uri, String>>, intentType: String): Intent {
    val uriList = ArrayList<Uri>(uris.size)
    val mimes = Array(uris.size) { uris[it].second }
    val description = ClipDescription(null, mimes)
    val clip = ClipData(description, ClipData.Item(uris[0].first))
    uriList.add(uris[0].first)
    for (i in 1 until uris.size) {
        clip.addItem(ClipData.Item(uris[i].first))
        uriList.add(uris[i].first)
    }
    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
    type = intentType
    clipData = clip
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return this
}
