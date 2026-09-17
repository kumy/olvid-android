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

package io.olvid.messenger.owneddetails

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.olvid.messenger.App
import io.olvid.messenger.AppSingleton
import io.olvid.messenger.R
import io.olvid.messenger.databases.AppDatabase
import io.olvid.messenger.databases.dao.FyleMessageJoinWithStatusDao.FyleAndStatus
import io.olvid.messenger.databases.entity.Discussion
import io.olvid.messenger.databases.tasks.SetDiscussionBackgroundImageTask
import io.olvid.messenger.databases.tasks.UpdateContactCustomDisplayNameAndPhotoTask
import io.olvid.messenger.databases.tasks.UpdateGroupCustomNameAndPhotoTask
import io.olvid.messenger.databases.tasks.UpdateGroupV2CustomNameAndPhotoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Transparent activity chaining a target chooser (my profile / custom photo for the image's
 * discussion), the SelectDetailsPhotoActivity crop step, and the photo update itself.
 */
class UseImageAsProfilePictureActivity : AppCompatActivity() {

    private var messageId: Long = -1

    private val cropForSelfLauncher = registerForActivityResult(StartActivityForResult()) {
        onCropResult(it, toSelf = true)
    }
    private val cropForCustomLauncher = registerForActivityResult(StartActivityForResult()) {
        onCropResult(it, toSelf = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourceUri = intent.data
        messageId = intent.getLongExtra(MESSAGE_ID_INTENT_EXTRA, -1)
        if (sourceUri == null) {
            finish()
            return
        }

        setContent {
            // null while loading
            var options by remember { mutableStateOf<TargetOptions?>(null) }

            LaunchedEffect(Unit) {
                val loaded = withContext(Dispatchers.IO) { loadTargetOptions(messageId) }
                if (loaded.discussionId == null && loaded.customTargetName == null) {
                    // only "my profile" is possible, skip the chooser
                    launchCrop(sourceUri, toSelf = true)
                } else {
                    options = loaded
                }
            }

            options?.let { opts ->
                TargetChooserDialog(
                    customTargetName = opts.customTargetName,
                    discussionTitle = opts.discussionTitle,
                    onChooseSelf = { launchCrop(sourceUri, toSelf = true) },
                    onChooseCustom = { launchCrop(sourceUri, toSelf = false) },
                    onChooseBackground = opts.discussionId?.let { discussionId ->
                        { applyDiscussionBackground(sourceUri, discussionId) }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun applyDiscussionBackground(sourceUri: Uri, discussionId: Long) {
        App.runThread {
            SetDiscussionBackgroundImageTask(sourceUri, discussionId).run()
            App.toast(R.string.toast_message_background_set, Toast.LENGTH_SHORT)
        }
        finish()
    }

    private fun launchCrop(sourceUri: Uri, toSelf: Boolean) {
        (if (toSelf) cropForSelfLauncher else cropForCustomLauncher).launch(
            Intent(null, sourceUri, App.getContext(), SelectDetailsPhotoActivity::class.java)
        )
    }

    private fun onCropResult(result: ActivityResult, toSelf: Boolean) {
        val absolutePhotoUrl = if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra(SelectDetailsPhotoActivity.CROPPED_JPEG_RETURN_INTENT_EXTRA)
        } else {
            null
        }
        if (absolutePhotoUrl != null) {
            applyPhoto(absolutePhotoUrl, toSelf)
        }
        finish()
    }

    private fun applyPhoto(absolutePhotoUrl: String, toSelf: Boolean) {
        val messageId = messageId
        App.runThread {
            if (toSelf) {
                val bytesOwnedIdentity = AppSingleton.getBytesCurrentIdentity() ?: return@runThread
                runCatching {
                    AppSingleton.getEngine()
                        .updateOwnedIdentityPhoto(bytesOwnedIdentity, absolutePhotoUrl)
                    AppSingleton.getEngine().publishLatestIdentityDetails(bytesOwnedIdentity)
                }.onFailure {
                    it.printStackTrace()
                    App.toast(R.string.toast_message_error_publishing_details, Toast.LENGTH_SHORT)
                    return@runThread
                }
            } else {
                val db = AppDatabase.getInstance()
                val discussion = discussionForMessage(messageId) ?: return@runThread
                when (discussion.discussionType) {
                    Discussion.TYPE_CONTACT -> {
                        val contact = db.contactDao().get(
                            discussion.bytesOwnedIdentity,
                            discussion.bytesDiscussionIdentifier
                        ) ?: return@runThread
                        UpdateContactCustomDisplayNameAndPhotoTask(
                            contact.bytesOwnedIdentity,
                            contact.bytesContactIdentity,
                            contact.customDisplayName,
                            absolutePhotoUrl,
                            contact.customNameHue,
                            contact.personalNote,
                            false
                        ).run()
                    }

                    Discussion.TYPE_GROUP -> {
                        val group = db.groupDao().get(
                            discussion.bytesOwnedIdentity,
                            discussion.bytesDiscussionIdentifier
                        ) ?: return@runThread
                        UpdateGroupCustomNameAndPhotoTask(
                            group.bytesOwnedIdentity,
                            group.bytesGroupOwnerAndUid,
                            group.customName,
                            absolutePhotoUrl,
                            group.personalNote,
                            false
                        ).run()
                    }

                    Discussion.TYPE_GROUP_V2 -> {
                        val group2 = db.group2Dao().get(
                            discussion.bytesOwnedIdentity,
                            discussion.bytesDiscussionIdentifier
                        ) ?: return@runThread
                        UpdateGroupV2CustomNameAndPhotoTask(
                            group2.bytesOwnedIdentity,
                            group2.bytesGroupIdentifier,
                            group2.customName,
                            absolutePhotoUrl,
                            group2.personalNote,
                            false
                        ).run()
                    }

                    else -> return@runThread
                }
            }
            App.toast(R.string.toast_message_photo_set, Toast.LENGTH_SHORT)
        }
    }

    /**
     * [customTargetName] is the display name to offer as the "custom photo" target for the
     * discussion the image belongs to, or null if that discussion cannot receive a custom photo
     * (locked/pre-discussion, or not a contact/group discussion). [discussionId] is non-null
     * whenever the image belongs to a discussion, enabling the background image target.
     */
    private data class TargetOptions(
        val discussionId: Long?,
        val discussionTitle: String?,
        val customTargetName: String?,
    )

    private fun loadTargetOptions(messageId: Long): TargetOptions {
        val discussion = discussionForMessage(messageId)
            ?: return TargetOptions(null, null, null)
        // for unnamed groups, the discussion title is the full members enumeration --> use the
        // truncated version instead
        val title = when (discussion.discussionType) {
            Discussion.TYPE_GROUP_V2 -> AppDatabase.getInstance().group2Dao()
                .get(discussion.bytesOwnedIdentity, discussion.bytesDiscussionIdentifier)
                ?.truncatedCustomName
                ?: discussion.title

            else -> discussion.title
        }?.takeUnless { it.isBlank() }
            ?: getString(
                if (discussion.discussionType == Discussion.TYPE_CONTACT)
                    R.string.text_unnamed_discussion
                else
                    R.string.text_unnamed_group
            )
        val customTargetName = if (discussion.isNormalOrReadOnly) {
            when (discussion.discussionType) {
                Discussion.TYPE_CONTACT,
                Discussion.TYPE_GROUP,
                Discussion.TYPE_GROUP_V2 -> title

                else -> null
            }
        } else {
            null
        }
        return TargetOptions(discussion.id, title, customTargetName)
    }

    private fun discussionForMessage(messageId: Long): Discussion? {
        if (messageId == -1L) {
            return null
        }
        val db = AppDatabase.getInstance()
        val message = db.messageDao().get(messageId) ?: return null
        return db.discussionDao().getById(message.discussionId)
    }

    companion object {
        private const val MESSAGE_ID_INTENT_EXTRA = "message_id"

        fun launch(context: Context, fyleAndStatus: FyleAndStatus) {
            val uri = fyleAndStatus.contentUriForExternalSharing ?: return
            context.startActivity(
                Intent(context, UseImageAsProfilePictureActivity::class.java).apply {
                    data = uri
                    putExtra(
                        MESSAGE_ID_INTENT_EXTRA,
                        fyleAndStatus.fyleMessageJoinWithStatus.messageId
                    )
                }
            )
        }
    }
}

val FyleAndStatus.canBeUsedAsProfilePicture: Boolean
    get() = fyle.isComplete
            && fyleMessageJoinWithStatus.nonNullMimeType.startsWith("image/")
            && fyleMessageJoinWithStatus.mimeType != "image/svg+xml"
