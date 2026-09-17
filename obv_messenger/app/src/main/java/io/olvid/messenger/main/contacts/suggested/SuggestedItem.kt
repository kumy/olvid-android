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

package io.olvid.messenger.main.contacts.suggested

import androidx.compose.runtime.Immutable
import io.olvid.engine.Logger
import io.olvid.engine.engine.types.ObvDialog.Category
import io.olvid.messenger.App
import io.olvid.messenger.AppSingleton
import io.olvid.messenger.databases.entity.Contact
import io.olvid.messenger.databases.entity.Invitation

/** Accept or refuse a received invitation through its dialog; a no-op for other categories. */
fun respondToInvitation(invitation: Invitation?, accept: Boolean) {
    val dialog = invitation?.associatedDialog?.takeIf { it.category.id == Category.ACCEPT_ONE_TO_ONE_INVITATION_DIALOG_CATEGORY } ?: return
    App.runThread {
        runCatching {
            dialog.setResponseToAcceptOneToOneInvitation(accept)
            AppSingleton.getEngine().respondToDialog(dialog)
        }.onFailure { Logger.x(it) }
    }
}

fun abortInvitation(invitation: Invitation?) {
    val dialog = invitation?.associatedDialog?.takeIf { it.category.id == Category.ONE_TO_ONE_INVITATION_SENT_DIALOG_CATEGORY } ?: return
    App.runThread {
        runCatching {
            dialog.setAbortOneToOneInvitationSent(true)
            AppSingleton.getEngine().respondToDialog(dialog)
        }.onFailure { Logger.x(it) }
    }
}

// the oneToOneInvitation is only here to sort the contacts when first opening the bottom sheet
// a liveData is used to dynamically observe the current invitation state
@Immutable
data class SuggestedContactItem(val contact: Contact, val receivedOneToOneInvitation: Boolean) {
    /** stable Compose key / identity for the stack and the list */
    val key: String get() = contact.bytesContactIdentity.contentToString()
}
