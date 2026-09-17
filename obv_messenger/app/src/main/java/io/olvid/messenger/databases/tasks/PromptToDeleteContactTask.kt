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
package io.olvid.messenger.databases.tasks

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.Toast
import io.olvid.engine.Logger
import io.olvid.engine.engine.types.sync.ObvSyncAtom
import io.olvid.messenger.App
import io.olvid.messenger.AppSingleton
import io.olvid.messenger.R
import io.olvid.messenger.customClasses.SecureAlertDialogBuilder
import io.olvid.messenger.databases.AppDatabase.Companion.getInstance

class PromptToDeleteContactTask(private val context: Context, private val bytesOwnedIdentity: ByteArray, private val bytesContactIdentity: ByteArray, private val runOnDelete: Runnable?) : Runnable {
    override fun run() {
        val db = getInstance()
        val contact = db.contactDao().get(bytesOwnedIdentity, bytesContactIdentity) ?: return

        if (contact.oneToOne && contact.capabilityOneToOneContacts) {
            val builder = SecureAlertDialogBuilder(context, R.style.CustomAlertDialog)
                .setTitle(R.string.dialog_title_remove_contact)
                .setMessage(context.getString(R.string.dialog_message_remove_contact, contact.getCustomDisplayName()))
                .setPositiveButton(R.string.button_label_ok) { _, _ ->
                    try {
                        AppSingleton.getEngine().downgradeOneToOneContact(contact.bytesOwnedIdentity, contact.bytesContactIdentity)
                        // when voluntarily downgrading a contact, stop suggesting them and propagate to other devices
                        db.contactDao().updateStopSuggesting(contact.bytesOwnedIdentity, contact.bytesContactIdentity, true)
                        runCatching {
                            AppSingleton.getEngine().propagateAppSyncAtomToOtherDevicesIfNeeded(
                                contact.bytesOwnedIdentity,
                                ObvSyncAtom.createStopSuggestingContact(contact.bytesContactIdentity)
                            )
                        }
                        App.toast(R.string.toast_message_contact_removed, Toast.LENGTH_SHORT)
                    } catch (e: Exception) {
                        Logger.x(e)
                    }
                }
                .setNegativeButton(R.string.button_label_cancel, null)

            Handler(Looper.getMainLooper()).post { builder.create().show() }
        } else {
            val groupCount = db.contactGroupJoinDao().countContactGroups(bytesOwnedIdentity, bytesContactIdentity) + db.group2MemberDao().countContactGroups(bytesOwnedIdentity, bytesContactIdentity)
            if (groupCount == 0) {
                val builder = SecureAlertDialogBuilder(context, R.style.CustomAlertDialog)
                    .setTitle(R.string.dialog_title_delete_user)
                    .setPositiveButton(R.string.button_label_ok) { _, _ ->
                        try {
                            AppSingleton.getEngine().deleteContact(contact.bytesOwnedIdentity, contact.bytesContactIdentity)
                            App.toast(R.string.toast_message_user_deleted, Toast.LENGTH_SHORT)
                            runOnDelete?.run()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .setNegativeButton(R.string.button_label_cancel, null)

                val pendingGroupCount = db.groupDao().getBytesGroupOwnerAndUidOfJoinedGroupWithPendingMember(contact.bytesOwnedIdentity, contact.bytesContactIdentity).size + db.group2PendingMemberDao().countContactGroups(bytesOwnedIdentity, bytesContactIdentity)
                if (pendingGroupCount == 0) {
                    builder.setMessage(context.getString(R.string.dialog_message_delete_user, contact.getCustomDisplayName()))
                } else {
                    builder.setMessage(context.resources.getQuantityString(R.plurals.dialog_message_delete_user_with_pending_groups, pendingGroupCount, contact.getCustomDisplayName(), pendingGroupCount))
                }
                Handler(Looper.getMainLooper()).post { builder.create().show() }
            } else {
                val ssb = SpannableStringBuilder(context.getString(R.string.dialog_message_delete_user_impossible_start, contact.getCustomDisplayName()))
                val spannableString = SpannableString(context.resources.getQuantityString(R.plurals.dialog_message_delete_user_impossible_count, groupCount, groupCount))
                spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.append(spannableString)
                ssb.append(context.resources.getQuantityString(R.plurals.dialog_message_delete_user_impossible_end, groupCount))

                val builder = SecureAlertDialogBuilder(context, R.style.CustomAlertDialog)
                    .setTitle(R.string.dialog_title_delete_user_impossible)
                    .setMessage(ssb)
                    .setPositiveButton(R.string.button_label_ok, null)
                Handler(Looper.getMainLooper()).post { builder.create().show() }
            }
        }
    }
}

