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

package io.olvid.messenger.onboarding.flow

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.olvid.messenger.App
import io.olvid.messenger.R
import kotlin.time.Duration.Companion.hours

/**
 * Persists an invitation or mutual-scan deep link opened while no profile existed yet, so it can
 * be processed once onboarding completes. Saved by [OnboardingFlowActivity], consumed by
 * MainActivity as soon as an owned identity is available.
 */
object PendingInvitationLink {
    private const val PREF_KEY_PENDING_INVITATION_LINK = "pref_key_pending_invitation_link"
    private const val PREF_KEY_PENDING_INVITATION_LINK_TIMESTAMP =
        "pref_key_pending_invitation_link_timestamp"

    private val VALIDITY_DURATION_MILLIS = 24.hours.inWholeMilliseconds

    private val prefs: SharedPreferences
        get() = App.getContext().getSharedPreferences(
            App.getContext().getString(R.string.preference_filename_app),
            Context.MODE_PRIVATE
        )

    fun save(uri: String) {
        prefs.edit {
            putString(PREF_KEY_PENDING_INVITATION_LINK, uri)
            putLong(PREF_KEY_PENDING_INVITATION_LINK_TIMESTAMP, System.currentTimeMillis())
        }
    }

    /** Returns the pending link if one was saved recently enough; always clears what was stored. */
    fun consume(): String? {
        val link = prefs.getString(PREF_KEY_PENDING_INVITATION_LINK, null) ?: return null
        val timestamp = prefs.getLong(PREF_KEY_PENDING_INVITATION_LINK_TIMESTAMP, 0L)
        prefs.edit {
            remove(PREF_KEY_PENDING_INVITATION_LINK)
            remove(PREF_KEY_PENDING_INVITATION_LINK_TIMESTAMP)
        }
        return link.takeIf { System.currentTimeMillis() - timestamp < VALIDITY_DURATION_MILLIS }
    }
}
