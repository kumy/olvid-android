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

package io.olvid.messenger.fragments.dialog

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.olvid.messenger.App
import io.olvid.messenger.customClasses.StringUtils
import io.olvid.messenger.databases.entity.Contact
import io.olvid.messenger.databases.entity.Discussion
import io.olvid.messenger.databases.entity.Group
import io.olvid.messenger.databases.entity.Group2
import io.olvid.messenger.databases.tasks.UpdateContactCustomDisplayNameAndPhotoTask
import io.olvid.messenger.databases.tasks.UpdateGroupCustomNameAndPhotoTask
import io.olvid.messenger.databases.tasks.UpdateGroupV2CustomNameAndPhotoTask
import io.olvid.messenger.databases.tasks.UpdateLockedDiscussionTitleAndPhotoTask

/**
 * Kotlin port of the legacy EditNameAndPhotoDialogFragment.EditNameAndPhotoViewModel. The null/empty
 * diff conventions in [commit] are load-bearing for the engine update tasks and are preserved verbatim.
 */
class EditNameAndPhotoViewModel : ViewModel() {
    enum class Type {
        GROUP_V2,
        GROUP,
        CONTACT,
        LOCKED_DISCUSSION,
    }

    class InitialViewContent(
        val bytesInitial: ByteArray,
        val type: Type?,
        val initial: String?,
        val absolutePhotoUrl: String?,
    )

    var type: Type? = null
        private set
    var contact: Contact? = null
        private set
    var group: Group? = null
        private set
    var group2: Group2? = null
        private set
    var discussion: Discussion? = null
        private set

    private var nickname: String? = null
    private var initialBytes: ByteArray? = null
    var absolutePhotoUrl: String? = null
        private set
    var takePictureUri: Uri? = null
    private var customNameHue: Int? = null
    private var personalNote: String? = null

    private val _initialViewLiveData = MutableLiveData<InitialViewContent?>(null)
    val initialViewLiveData: LiveData<InitialViewContent?> = _initialViewLiveData
    private val _valid = MutableLiveData(false)
    val valid: LiveData<Boolean> = _valid
    private val _customNameHueLiveData = MutableLiveData<Int?>(null)
    val customNameHueLiveData: LiveData<Int?> = _customNameHueLiveData

    fun setContact(contact: Contact) {
        type = Type.CONTACT
        this.contact = contact
        group = null
        group2 = null
        discussion = null

        nickname = contact.getCustomDisplayName()
        initialBytes = contact.bytesContactIdentity
        absolutePhotoUrl = App.absolutePathFromRelative(contact.getCustomPhotoUrl())
        setCustomNameHue(contact.customNameHue)
        personalNote = contact.personalNote

        checkValid()
        updateInitialViewLiveData()
    }

    fun setGroup(group: Group) {
        type = Type.GROUP
        contact = null
        this.group = group
        group2 = null
        discussion = null

        nickname = group.getCustomName()
        initialBytes = group.bytesGroupOwnerAndUid
        absolutePhotoUrl = App.absolutePathFromRelative(group.getCustomPhotoUrl())
        setCustomNameHue(null)
        personalNote = group.personalNote

        checkValid()
        updateInitialViewLiveData()
    }

    fun setGroupV2(group2: Group2) {
        type = Type.GROUP_V2
        contact = null
        group = null
        this.group2 = group2
        discussion = null

        nickname = group2.customName ?: group2.name
        initialBytes = group2.bytesGroupIdentifier
        absolutePhotoUrl = App.absolutePathFromRelative(group2.getCustomPhotoUrl())
        setCustomNameHue(null)
        personalNote = group2.personalNote

        checkValid()
        updateInitialViewLiveData()
    }

    fun setLockedDiscussion(discussion: Discussion) {
        type = Type.LOCKED_DISCUSSION
        contact = null
        group = null
        group2 = null
        this.discussion = discussion

        nickname = discussion.title
        initialBytes = ByteArray(0)
        absolutePhotoUrl = App.absolutePathFromRelative(discussion.photoUrl)
        setCustomNameHue(null)
        personalNote = null

        checkValid()
        updateInitialViewLiveData()
    }

    fun checkValid() {
        _valid.postValue(type == Type.GROUP_V2 || !nickname.isNullOrBlank())
    }

    fun setNickname(nickname: String?) {
        // check if initial changed
        val shouldUpdateInitialViewLiveData =
            StringUtils.getInitial(nickname) != StringUtils.getInitial(this.nickname)

        this.nickname = nickname
        checkValid()
        if (shouldUpdateInitialViewLiveData) {
            updateInitialViewLiveData()
        }
    }

    fun setAbsolutePhotoUrl(absolutePhotoUrl: String?) {
        this.absolutePhotoUrl = absolutePhotoUrl
        updateInitialViewLiveData()
    }

    fun reset() {
        when (type) {
            Type.GROUP -> {
                nickname = group?.name
                absolutePhotoUrl = App.absolutePathFromRelative(group?.photoUrl)
            }
            Type.GROUP_V2 -> {
                nickname = group2?.name
                absolutePhotoUrl = App.absolutePathFromRelative(group2?.photoUrl)
            }
            Type.CONTACT -> {
                nickname = contact?.displayName
                absolutePhotoUrl = App.absolutePathFromRelative(contact?.photoUrl)
            }
            Type.LOCKED_DISCUSSION -> {
                nickname = discussion?.title
                absolutePhotoUrl = App.absolutePathFromRelative(discussion?.photoUrl)
            }
            null -> {}
        }
        checkValid()
        setCustomNameHue(null)
        updateInitialViewLiveData()
    }

    fun getNickname(): String? =
        nickname?.trim()?.takeUnless { it.isEmpty() }

    fun setCustomNameHue(customNameHue: Int?) {
        this.customNameHue = customNameHue
        _customNameHueLiveData.postValue(customNameHue)
    }

    fun getCustomNameHue(): Int? = customNameHue

    fun getPersonalNote(): String? =
        personalNote?.trim()?.takeUnless { it.isEmpty() }

    fun setPersonalNote(personalNote: String?) {
        this.personalNote = personalNote
    }

    private fun updateInitialViewLiveData() {
        _initialViewLiveData.postValue(
            InitialViewContent(
                initialBytes ?: ByteArray(0),
                type,
                StringUtils.getInitial(getNickname()),
                absolutePhotoUrl
            )
        )
    }

    fun clearData() {
        contact = null
        group = null
        nickname = null
        initialBytes = null
        absolutePhotoUrl = null
        takePictureUri = null
        personalNote = null
        _initialViewLiveData.postValue(null)
        _valid.postValue(false)
    }

    /** Dispatches the appropriate engine update task. Mirrors the legacy updateAndDismiss() switch. */
    fun commit() {
        when (type) {
            Type.GROUP -> {
                val group = group ?: return
                val customName: String? = if (group.name == getNickname()) {
                    // nickname was reset
                    null
                } else {
                    getNickname()
                }

                val absoluteCustomPhotoUrl: String? = when {
                    App.absolutePathFromRelative(group.photoUrl) == absolutePhotoUrl ->
                        // photo was reset
                        null
                    absolutePhotoUrl == null && group.photoUrl != null ->
                        // photo was removed
                        ""
                    else ->
                        // new photo or still the same custom photo
                        absolutePhotoUrl
                }

                App.runThread(
                    UpdateGroupCustomNameAndPhotoTask(
                        group.bytesOwnedIdentity,
                        group.bytesGroupOwnerAndUid,
                        customName,
                        absoluteCustomPhotoUrl,
                        getPersonalNote(),
                        false
                    )
                )
            }

            Type.GROUP_V2 -> {
                val group = group2 ?: return
                val customName: String? = if (group.name == getNickname()) {
                    // nickname was reset
                    null
                } else {
                    getNickname() ?: ""
                }

                val absoluteCustomPhotoUrl: String? = when {
                    App.absolutePathFromRelative(group.photoUrl) == absolutePhotoUrl ->
                        null
                    absolutePhotoUrl == null && group.photoUrl != null ->
                        ""
                    else ->
                        absolutePhotoUrl
                }

                App.runThread(
                    UpdateGroupV2CustomNameAndPhotoTask(
                        group.bytesOwnedIdentity,
                        group.bytesGroupIdentifier,
                        customName,
                        absoluteCustomPhotoUrl,
                        getPersonalNote(),
                        false
                    )
                )
            }

            Type.CONTACT -> {
                val contact = contact ?: return
                val customName: String? = if (contact.displayName == getNickname()) {
                    // nickname was reset
                    null
                } else {
                    getNickname()
                }

                val absoluteCustomPhotoUrl: String? = when {
                    App.absolutePathFromRelative(contact.photoUrl) == absolutePhotoUrl ->
                        null
                    absolutePhotoUrl == null && contact.photoUrl != null ->
                        ""
                    else ->
                        absolutePhotoUrl
                }

                App.runThread(
                    UpdateContactCustomDisplayNameAndPhotoTask(
                        contact.bytesOwnedIdentity,
                        contact.bytesContactIdentity,
                        customName,
                        absoluteCustomPhotoUrl,
                        getCustomNameHue(),
                        getPersonalNote(),
                        false
                    )
                )
            }

            Type.LOCKED_DISCUSSION -> {
                val discussion = discussion ?: return
                val customName = getNickname()
                val absoluteCustomPhotoUrl: String? = if (
                    App.absolutePathFromRelative(discussion.photoUrl) == absolutePhotoUrl
                ) {
                    // photo did not change
                    ""
                } else {
                    // new photo (can be null if it was removed)
                    absolutePhotoUrl
                }
                App.runThread(
                    UpdateLockedDiscussionTitleAndPhotoTask(
                        discussion.id,
                        customName,
                        absoluteCustomPhotoUrl
                    )
                )
            }

            null -> {}
        }
    }
}
