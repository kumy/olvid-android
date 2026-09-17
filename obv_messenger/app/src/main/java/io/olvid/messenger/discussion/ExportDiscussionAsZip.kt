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

package io.olvid.messenger.discussion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.olvid.messenger.databases.entity.Discussion
import io.olvid.messenger.history_transfer.HistoryTransferActivity
import io.olvid.messenger.history_transfer.TransferService
import io.olvid.messenger.history_transfer.types.TransferScope
import io.olvid.messenger.history_transfer.types.TransferTransportType
import io.olvid.messenger.settings.history_transfer.composables.ZipExportPasswordDialogs
import io.olvid.messenger.settings.history_transfer.toExportFileName


/**
 * Full flow to export a single [discussion] (messages and attachments) as a zip file:
 * password choice dialogs, zip file location picker, then the export itself with its
 * progress shown in [HistoryTransferActivity]. Call [onFinished] to remove it from the
 * composition once the flow completes or is dismissed.
 */
@Composable
fun ExportDiscussionAsZip(
    discussion: Discussion,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var zipPassword: String? by rememberSaveable { mutableStateOf(null) }
    var pickingZipFile by rememberSaveable { mutableStateOf(false) }

    val createZipFileLauncher = rememberLauncherForActivityResult(
        CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            TransferService.initiateHistoryTransferAndShowProgress(
                context = context,
                transferTransportType = TransferTransportType.ZipFileExport(
                    bytesOwnedIdentity = discussion.bytesOwnedIdentity,
                    zipWritableFileUri = it,
                    password = zipPassword
                ),
                transferScope = TransferScope.Discussions(
                    discussionIds = listOf(discussion.id),
                    messagesOnly = false
                )
            )
        }
        onFinished()
    }

    if (!pickingZipFile) {
        ZipExportPasswordDialogs(
            onPasswordChosen = { password ->
                zipPassword = password
                pickingZipFile = true
                createZipFileLauncher.launch(System.currentTimeMillis().toExportFileName(discussionName = discussion.title))
            },
            onDismiss = onFinished,
        )
    }
}
