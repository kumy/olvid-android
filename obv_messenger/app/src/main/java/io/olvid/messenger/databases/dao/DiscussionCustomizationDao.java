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

package io.olvid.messenger.databases.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.olvid.messenger.databases.entity.DiscussionCustomization;

@Dao
public interface DiscussionCustomizationDao {
    @Insert
    long insert(@NonNull DiscussionCustomization discussionCustomization);

    @Update
    void update(@NonNull DiscussionCustomization discussionCustomization);

    @Delete
    void delete(@NonNull DiscussionCustomization discussionCustomization);

    @Query("SELECT * FROM " + DiscussionCustomization.TABLE_NAME + " WHERE " + DiscussionCustomization.DISCUSSION_ID + " = :discussionId;")
    @Nullable DiscussionCustomization get(long discussionId);

    @Query("SELECT * FROM " + DiscussionCustomization.TABLE_NAME + " WHERE " + DiscussionCustomization.DISCUSSION_ID + " = :discussionId;")
    LiveData<DiscussionCustomization> getLiveData(long discussionId);

    @Query("SELECT " + DiscussionCustomization.BACKGROUND_IMAGE_URL + " FROM " + DiscussionCustomization.TABLE_NAME +
            " WHERE " + DiscussionCustomization.BACKGROUND_IMAGE_URL + " IS NOT NULL")
    List<String> getAllBackgroundImageFilePaths();

    // set start timestamp when transitioning into mute; preserve original start when extending an
    // active mute. Mirrors OwnedIdentityDao.updateMuteNotifications. The start is used to recap
    // missed notifications when the discussion mute ends.
    @Query("UPDATE " + DiscussionCustomization.TABLE_NAME +
            " SET " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS + " = 1, " +
            DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_TIMESTAMP + " = :prefMuteNotificationsTimestamp, " +
            DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_EXCEPT_MENTIONED + " = :prefMuteNotificationsExceptMentioned, " +
            DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_START_TIMESTAMP +
            " = CASE WHEN " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_START_TIMESTAMP + " IS NULL THEN :nowTimestamp" +
            "        ELSE " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_START_TIMESTAMP +
            " END " +
            " WHERE " + DiscussionCustomization.DISCUSSION_ID + " = :discussionId")
    void updateMuteNotifications(long discussionId, @Nullable Long prefMuteNotificationsTimestamp, boolean prefMuteNotificationsExceptMentioned, long nowTimestamp);

    // Clear the discussion mute (flag, end timestamp and start timestamp) atomically. Used on
    // manual unmute and on mute expiry.
    @Query("UPDATE " + DiscussionCustomization.TABLE_NAME +
            " SET " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS + " = 0, " +
            DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_TIMESTAMP + " = NULL, " +
            DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_START_TIMESTAMP + " = NULL " +
            " WHERE " + DiscussionCustomization.DISCUSSION_ID + " = :discussionId")
    void clearMuteNotifications(long discussionId);

    @Query("SELECT * FROM " + DiscussionCustomization.TABLE_NAME +
            " WHERE " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS + " = 1" +
            " AND " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_TIMESTAMP + " IS NOT NULL")
    @NonNull
    List<DiscussionCustomization> getAllWithFiniteMute();

    @Query("SELECT MIN(" + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_TIMESTAMP + ") FROM " + DiscussionCustomization.TABLE_NAME +
            " WHERE " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS + " = 1" +
            " AND " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_TIMESTAMP + " IS NOT NULL" +
            " AND " + DiscussionCustomization.PREF_MUTE_NOTIFICATIONS_TIMESTAMP + " > :nowTimestamp")
    @Nullable
    Long getNextMuteExpirationAfter(long nowTimestamp);
}
