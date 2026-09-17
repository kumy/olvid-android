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

package io.olvid.messenger.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.olvid.engine.Logger
import io.olvid.messenger.App
import io.olvid.messenger.customClasses.BytesKey
import io.olvid.messenger.databases.AppDatabase
import io.olvid.messenger.databases.dao.PollVoteDao.PollVoteAndMessage
import io.olvid.messenger.databases.dao.ReactionDao.ReactionAndMessage
import io.olvid.messenger.databases.entity.Contact
import io.olvid.messenger.databases.entity.Discussion
import io.olvid.messenger.databases.entity.DiscussionCustomization
import io.olvid.messenger.databases.entity.Message
import io.olvid.messenger.databases.entity.OwnedIdentity
import io.olvid.messenger.notifications.AndroidNotificationManager
import java.util.Timer
import java.util.TimerTask
import kotlin.collections.mutableListOf

class MuteExpirationService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == MUTE_EXPIRED_ACTION) {
            App.runThread {
                dispatchAllExpiredMutes()
                scheduleNextExpiration()
            }
        }
    }

    companion object {
        const val MUTE_EXPIRED_ACTION: String = "mute_expired"

        const val PER_DISCUSSION_MESSAGE_NOTIFICATION_LIMIT = 5

        private var scheduledAlarmTimestamp: Long? = null
        private var expireTimer: Timer? = null
        private var expireTimerTask: TimerTask? = null

        // Walks every owned identity and discussion that still has a non-null mute timestamp and
        // tries to claim the recap dispatch atomically. Used by both the scheduled-alarm path and
        // the app-startup catch-up.
        @JvmStatic
        fun dispatchAllExpiredMutes() {
            val now = System.currentTimeMillis()
            val db = AppDatabase.getInstance()
            for (ownedIdentity in db.ownedIdentityDao().getAllWithFiniteMute()) {
                val endTimestamp = ownedIdentity.prefMuteNotificationsTimestamp ?: continue
                if (endTimestamp > now) continue
                claimAndEmitForProfile(ownedIdentity.bytesOwnedIdentity, endTimestamp, requireExpired = true)
            }
            for (discussionCustomization in db.discussionCustomizationDao().getAllWithFiniteMute()) {
                val endTimestamp = discussionCustomization.prefMuteNotificationsTimestamp ?: continue
                if (endTimestamp > now) continue
                claimAndEmitForDiscussion(discussionCustomization.discussionId, endTimestamp, requireExpired = true)
            }
        }

        // Persist a discussion mute in a single atomic UPDATE (flag + end timestamp + except
        // mentioned + start capture, so extending an active mute preserves its original start) and
        // schedule the expiry alarm. All interactive discussion-mute sites must go through here.
        // Returns the freshly persisted customization (for propagation to other devices).
        @JvmStatic
        fun muteDiscussion(discussionId: Long, muteExpirationTimestamp: Long?, muteExceptMentioned: Boolean): DiscussionCustomization? {
            val dao = AppDatabase.getInstance().discussionCustomizationDao()
            if (dao.get(discussionId) == null) {
                dao.insert(DiscussionCustomization(discussionId))
            }
            dao.updateMuteNotifications(discussionId, muteExpirationTimestamp, muteExceptMentioned, System.currentTimeMillis())
            scheduleNextExpiration()
            return dao.get(discussionId)
        }

        // Called when the user manually unmutes their profile. Always clears the mute (so older
        // mutes set before this feature existed still get cleared); only emits a recap when there
        // is a captured start window.
        @JvmStatic
        fun clearAndEmitForManualUnmute(bytesOwnedIdentity: ByteArray) {
            claimAndEmitForProfile(bytesOwnedIdentity, System.currentTimeMillis(), requireExpired = false, alwaysClear = true)
            scheduleNextExpiration()
        }

        // Called when the user manually unmutes a single discussion. Always clears the mute; only
        // emits a recap when there is a captured start window.
        @JvmStatic
        fun clearAndEmitForDiscussionManualUnmute(discussionId: Long) {
            claimAndEmitForDiscussion(discussionId, System.currentTimeMillis(), requireExpired = false, alwaysClear = true)
            scheduleNextExpiration()
        }

        // Atomically reads-and-clears the profile mute (start_timestamp + flag) in a single Room
        // transaction. Only one caller can observe a non-null start_timestamp; concurrent races
        // (e.g. alarm firing at the same moment as a manual unmute) collapse to a single recap.
        private fun claimAndEmitForProfile(bytesOwnedIdentity: ByteArray, endTimestamp: Long, requireExpired: Boolean, alwaysClear: Boolean = false) {
            val db = AppDatabase.getInstance()
            val now = System.currentTimeMillis()
            val claim: Pair<Long, OwnedIdentity>? = db.runInTransaction<Pair<Long, OwnedIdentity>?> {
                val fresh = db.ownedIdentityDao().get(bytesOwnedIdentity) ?: return@runInTransaction null
                val startTimestamp = fresh.prefMuteNotificationsStartTimestamp
                // for the alarm path, require the mute to actually have expired; otherwise we'd
                // wipe a still-active mute when this runs after the user extended it
                if (requireExpired) {
                    val end = fresh.prefMuteNotificationsTimestamp ?: return@runInTransaction null
                    if (end > now) return@runInTransaction null
                }
                // clear the mute when leaving it for good: explicitly (manual unmute, alwaysClear) or
                // because it has expired (requireExpired passed above). This also self-heals legacy mutes
                // set before this feature existed (start_timestamp == null): we clear the stale flag so
                // getAllWithFiniteMute() stops re-scanning them, and simply emit no recap (no captured window).
                val shouldClear = alwaysClear || requireExpired
                if (startTimestamp == null && !shouldClear) {
                    return@runInTransaction null
                }
                db.ownedIdentityDao().clearMuteNotifications(bytesOwnedIdentity)
                // reflect the cleared state on the snapshot we'll pass to displayReceivedMessageNotification
                fresh.prefMuteNotifications = false
                fresh.prefMuteNotificationsTimestamp = null
                fresh.prefMuteNotificationsStartTimestamp = null
                if (startTimestamp == null) null else (startTimestamp to fresh)
            }
            if (claim != null) {
                emitProfileCatchUp(claim.second, claim.first, endTimestamp)
            }
        }

        // Atomically reads-and-clears a discussion mute (start_timestamp + flag) in a single Room
        // transaction. Mirrors claimAndEmitForProfile.
        private fun claimAndEmitForDiscussion(discussionId: Long, endTimestamp: Long, requireExpired: Boolean, alwaysClear: Boolean = false) {
            val db = AppDatabase.getInstance()
            val now = System.currentTimeMillis()
            val claim: Long? = db.runInTransaction<Long?> {
                val fresh = db.discussionCustomizationDao().get(discussionId) ?: return@runInTransaction null
                val startTimestamp = fresh.prefMuteNotificationsStartTimestamp
                if (requireExpired) {
                    val end = fresh.prefMuteNotificationsTimestamp ?: return@runInTransaction null
                    if (end > now) return@runInTransaction null
                }
                val shouldClear = alwaysClear || requireExpired
                if (startTimestamp == null && !shouldClear) {
                    return@runInTransaction null
                }
                db.discussionCustomizationDao().clearMuteNotifications(discussionId)
                startTimestamp
            }
            if (claim != null) {
                emitDiscussionCatchUp(discussionId, claim, endTimestamp)
            }
        }

        // Profile-mute recap: gather every discussion that received messages/reactions/poll-votes
        // while the whole profile was muted and re-emit the missed notifications.
        private fun emitProfileCatchUp(ownedIdentity: OwnedIdentity, startTimestamp: Long, endTimestamp: Long) {
            if (startTimestamp >= endTimestamp) return
            val db = AppDatabase.getInstance()

            val messages = db.messageDao()
                .getInboundMessagesReceivedInWindow(ownedIdentity.bytesOwnedIdentity, startTimestamp, endTimestamp)

            val messageMap: MutableMap<Long, MutableList<Message>> = mutableMapOf()

            // first split messages by discussion
            for (message in messages) {
                // do not re-notify for messages where I am mentioned if mentioned messages were not muted
                if (message.mentioned && ownedIdentity.prefMuteNotificationsExceptMentioned) continue
                messageMap.getOrPut(message.discussionId) { mutableListOf() }
                    .add(message)
            }

            messageMap.forEach { (discussionId, messagesForDiscussion) ->
                val discussion = db.discussionDao().getById(discussionId) ?: return@forEach
                emitMissedMessagesForDiscussion(ownedIdentity, discussion, messagesForDiscussion)
            }

            // reactions and poll-votes received from contacts on my outbound messages while muted
            emitMissedReactions(
                ownedIdentity,
                db.reactionDao().getReactionsReceivedInWindow(ownedIdentity.bytesOwnedIdentity, startTimestamp, endTimestamp)
            )
            emitMissedPollVotes(
                ownedIdentity,
                db.pollVoteDao().getPollVotesReceivedInWindow(ownedIdentity.bytesOwnedIdentity, startTimestamp, endTimestamp)
            )
        }

        // Discussion-mute recap: gather the messages/reactions/poll-votes a single discussion missed
        // while it was muted and re-emit them. The profile may still be muted at its own level: in
        // that case the per-notification mute checks inside AndroidNotificationManager keep things
        // silent, so we don't need to special-case it here.
        private fun emitDiscussionCatchUp(discussionId: Long, startTimestamp: Long, endTimestamp: Long) {
            if (startTimestamp >= endTimestamp) return
            val db = AppDatabase.getInstance()
            val discussion = db.discussionDao().getById(discussionId) ?: return
            val ownedIdentity = db.ownedIdentityDao().get(discussion.bytesOwnedIdentity) ?: return

            val messages = db.messageDao()
                .getInboundMessagesReceivedInWindowForDiscussion(discussionId, startTimestamp, endTimestamp)
            emitMissedMessagesForDiscussion(ownedIdentity, discussion, messages)

            emitMissedReactions(
                ownedIdentity,
                db.reactionDao().getReactionsReceivedInWindowForDiscussion(discussionId, startTimestamp, endTimestamp)
            )
            emitMissedPollVotes(
                ownedIdentity,
                db.pollVoteDao().getPollVotesReceivedInWindowForDiscussion(ownedIdentity.bytesOwnedIdentity, discussionId, startTimestamp, endTimestamp)
            )
        }

        // Re-emit (at most PER_DISCUSSION_MESSAGE_NOTIFICATION_LIMIT) missed inbound messages for a
        // single discussion, honoring the discussion's own mute level and "except mentioned".
        private fun emitMissedMessagesForDiscussion(ownedIdentity: OwnedIdentity, discussion: Discussion, messages: List<Message>) {
            if (messages.isEmpty()) return
            val db = AppDatabase.getInstance()
            // skip discussions that are still muted at their own level — those weren't "missed
            // because of the (profile) mute that just ended", they would have been silenced regardless
            val discussionCustomization = db.discussionCustomizationDao().get(discussion.id)

            // if all notifications should be muted (even for mentioned messages), do not notify
            if (discussionCustomization != null && discussionCustomization.shouldMuteNotifications(true)) {
                return
            }

            // if only mentioned messages should be notified, filter the messages
            val filteredMessages = if (discussionCustomization != null && discussionCustomization.shouldMuteNotifications(false))
                messages.filter { it.mentioned }
            else
                messages

            // now take the last few received messages and show a notification
            filteredMessages.takeLast(PER_DISCUSSION_MESSAGE_NOTIFICATION_LIMIT).forEach { message ->
                val contact = db.contactDao().get(ownedIdentity.bytesOwnedIdentity, message.senderIdentifier)
                AndroidNotificationManager.displayReceivedMessageNotification(discussion, message, contact, ownedIdentity)
            }
        }

        // Re-emit missed reaction notifications. The displayReactionNotification call rechecks the
        // discussion- and profile-level mutes, so a still-muted target stays silent. Many reactions
        // share the same discussion/contact (all of them, in the discussion-mute path), so memoize
        // the lookups.
        private fun emitMissedReactions(ownedIdentity: OwnedIdentity, reactions: List<ReactionAndMessage>) {
            if (reactions.isEmpty()) return
            val db = AppDatabase.getInstance()
            val discussionCache = mutableMapOf<Long, Discussion?>()
            val contactCache = mutableMapOf<BytesKey, Contact?>()
            reactions.forEach { reactionAndMessage ->
                val reaction = reactionAndMessage.reaction
                val message = reactionAndMessage.message
                val bytesReactingContact = reaction.bytesIdentity ?: return@forEach
                val discussion = discussionCache.getOrPut(message.discussionId) { db.discussionDao().getById(message.discussionId) } ?: return@forEach
                val contact = contactCache.getOrPut(BytesKey(bytesReactingContact)) { db.contactDao().get(ownedIdentity.bytesOwnedIdentity, bytesReactingContact) } ?: return@forEach
                AndroidNotificationManager.displayReactionNotification(ownedIdentity, discussion, message, reaction.emoji, contact)
            }
        }

        // Re-emit missed poll-vote notifications.
        private fun emitMissedPollVotes(ownedIdentity: OwnedIdentity, pollVotes: List<PollVoteAndMessage>) {
            if (pollVotes.isEmpty()) return
            val db = AppDatabase.getInstance()
            val discussionCache = mutableMapOf<Long, Discussion?>()
            pollVotes.forEach { pollVoteAndMessage ->
                val pollVote = pollVoteAndMessage.pollVote
                val message = pollVoteAndMessage.message
                val discussion = discussionCache.getOrPut(message.discussionId) { db.discussionDao().getById(message.discussionId) } ?: return@forEach
                AndroidNotificationManager.displayPollVoteNotification(ownedIdentity, discussion, message, pollVote.voteUuid)
            }
        }

        @JvmStatic
        fun scheduleNextExpiration() {
            try {
                val now = System.currentTimeMillis()
                val db = AppDatabase.getInstance()
                val nextProfileExpiration = db.ownedIdentityDao().getNextMuteExpirationAfter(now)
                val nextDiscussionExpiration = db.discussionCustomizationDao().getNextMuteExpirationAfter(now)
                // the single alarm covers both kinds: schedule at the earliest of the two
                val nextExpirationTimestamp = listOfNotNull(nextProfileExpiration, nextDiscussionExpiration).minOrNull()
                if (scheduledAlarmTimestamp == nextExpirationTimestamp) {
                    return
                }
                scheduledAlarmTimestamp = nextExpirationTimestamp

                scheduledAlarmTimestamp?.let { timestamp ->
                    if (expireTimer == null) {
                        expireTimer = Timer("MuteExpirationServiceTimer")
                    }
                    expireTimerTask?.cancel()
                    val task = object : TimerTask() {
                        override fun run() {
                            dispatchAllExpiredMutes()
                            scheduleNextExpiration()
                        }
                    }
                    expireTimerTask = task
                    // +10ms so the comparison `timestamp <= now` is true when we fire
                    val delay = (timestamp - System.currentTimeMillis() + 10).coerceAtLeast(0)
                    expireTimer!!.schedule(task, delay)
                }

                val intent = Intent(MUTE_EXPIRED_ACTION, null, App.getContext(), MuteExpirationService::class.java)
                val pendingIntent = PendingIntent.getBroadcast(App.getContext(), 0, intent, PendingIntent.FLAG_MUTABLE)
                val alarmManager = App.getContext().getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

                alarmManager.cancel(pendingIntent)
                scheduledAlarmTimestamp?.let { ts ->
                    Logger.d("MuteExpirationService - Scheduling mute-end recap at $ts")
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, ts, pendingIntent)
                    } else {
                        Logger.e("Missing exact alarm permission - Using approximate alarm")
                        alarmManager.set(AlarmManager.RTC_WAKEUP, ts, pendingIntent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
