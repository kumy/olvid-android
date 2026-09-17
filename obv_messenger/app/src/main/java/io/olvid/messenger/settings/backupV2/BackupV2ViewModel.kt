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

package io.olvid.messenger.settings.backupV2

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.olvid.engine.Logger
import io.olvid.engine.engine.types.ObvBytesKey
import io.olvid.engine.engine.types.ObvDeviceBackupForRestore
import io.olvid.engine.engine.types.ObvDeviceList
import io.olvid.engine.engine.types.ObvProfileBackupsForRestore
import io.olvid.engine.engine.types.sync.ObvProfileBackupSnapshot
import io.olvid.messenger.App
import io.olvid.messenger.AppSingleton
import io.olvid.messenger.BuildConfig
import io.olvid.messenger.customClasses.DeviceBackupProfile
import io.olvid.messenger.customClasses.KeycloakInfo
import io.olvid.messenger.customClasses.ProfileBackupSnapshot
import io.olvid.messenger.databases.AppDatabase
import io.olvid.messenger.databases.entity.backups.AppDeviceSnapshot
import io.olvid.messenger.settings.backupV2.composables.ProfilePictureLabelAndKey
import java.util.concurrent.atomic.AtomicInteger


class BackupV2ViewModel: ViewModel() {
    val showManageBackupsDialog = mutableStateOf(false)
    val showResetConfirmationDialog = mutableStateOf(false)
    val showBackupFailed = mutableStateOf(false)
    val backupNowState = mutableStateOf(BackupNowState.NONE)

    val showingBackupsForOtherKey = mutableStateOf(false)
    val deviceBackupSeed = mutableStateOf<String?>(null)
    val backupSeedError = mutableStateOf(false)
    val selectedProfileBackup = mutableStateOf<DeviceBackupProfile?>(null)
    val selectedProfileDeviceList = mutableStateOf<ObvDeviceList?>(null)
    val fetchingDeviceBackup = mutableStateOf(false)
    val fetchingProfileBackups = mutableStateOf(false)
    val profileBackupsTruncated = mutableStateOf(false)
    val fetchError = mutableStateOf(FetchError.NONE)

    val deviceBackup: MutableState<List<DeviceBackupProfile>?> = mutableStateOf(null)
    val profileBackups: MutableState<List<ProfileBackupSnapshot>?> = mutableStateOf(null)

    val credentialManagerAvailable = mutableStateOf<Boolean?>(null)
    val disableSeedGeneration = mutableStateOf(false)

    private val fetchCount = AtomicInteger(0)

    fun resetYourBackups() {
        showManageBackupsDialog.value = false
        showResetConfirmationDialog.value = false
        showBackupFailed.value = false
        backupNowState.value = BackupNowState.NONE

        showingBackupsForOtherKey.value = false
        deviceBackupSeed.value = null
        backupSeedError.value = false
        selectedProfileBackup.value = null
        selectedProfileDeviceList.value = null
        fetchingDeviceBackup.value = false
        fetchingProfileBackups.value = false
        profileBackupsTruncated.value = false
        fetchError.value = FetchError.NONE

        deviceBackup.value = null
        profileBackups.value = null

        disableSeedGeneration.value = false
    }


    fun fetchDeviceBackup(otherKey: Boolean) {
        val fetchId = fetchCount.addAndGet(1)
        App.runThread {
            deviceBackup.value = null
            fetchError.value = FetchError.NONE
            try {
                fetchingDeviceBackup.value = true
                deviceBackupSeed.value?.let {
                    val obvDeviceBackupForRestore: ObvDeviceBackupForRestore? = AppSingleton.getEngine().fetchDeviceBackup(BuildConfig.SERVER_NAME, it)
                    when(obvDeviceBackupForRestore?.status) {
                        ObvDeviceBackupForRestore.Status.SUCCESS -> Unit
                        ObvDeviceBackupForRestore.Status.NETWORK_ERROR -> {
                            fetchError.value = FetchError.NETWORK
                            return@let
                        }
                        ObvDeviceBackupForRestore.Status.PERMANENT_ERROR -> {
                            fetchError.value = FetchError.PERMANENT
                            return@let
                        }
                        ObvDeviceBackupForRestore.Status.ERROR, null -> {
                            fetchError.value = FetchError.RETRIABLE
                            return@let
                        }
                    }
                    val nickNames = (obvDeviceBackupForRestore.appDeviceBackupSnapshot as? AppDeviceSnapshot)?.owned_identities

                    var notHiddenOwnedIdentities = AppDatabase.getInstance().ownedIdentityDao().allNotHidden.map { it.bytesOwnedIdentity }
                    AppSingleton.getBytesCurrentIdentity()?.let { currentBytesOwnedIdentity ->
                        if (notHiddenOwnedIdentities.none { it.contentEquals(currentBytesOwnedIdentity) }) {
                            notHiddenOwnedIdentities = notHiddenOwnedIdentities + currentBytesOwnedIdentity
                        }
                    }

                    if (fetchId == fetchCount.get()) {
                        fetchError.value = FetchError.NONE
                        deviceBackup.value = obvDeviceBackupForRestore.profiles?.mapNotNull { obvDeviceBackupProfile ->
                            val profile = obvDeviceBackupProfile ?: return@mapNotNull null
                            val bytesProfileIdentity = profile.bytesProfileIdentity ?: return@mapNotNull null
                            val profileBackupSeed = profile.profileBackupSeed ?: return@mapNotNull null
                            val identityDetailsWithPhoto = profile.identityDetails ?: return@mapNotNull null
                            val identityDetails = identityDetailsWithPhoto.identityDetails ?: return@mapNotNull null
                            if (otherKey
                                || notHiddenOwnedIdentities.any { notHiddenBytesIdentity -> notHiddenBytesIdentity.contentEquals(bytesProfileIdentity) }) {
                                DeviceBackupProfile(
                                    bytesProfileIdentity = bytesProfileIdentity,
                                    nickName = nickNames?.get(ObvBytesKey(bytesProfileIdentity))?.custom_name,
                                    identityDetails = identityDetails,
                                    keycloakManaged = profile.keycloakManaged,
                                    photo = identityDetailsWithPhoto.photoUrl?.let {
                                        App.absolutePathFromRelative(it)
                                    } ?: identityDetailsWithPhoto.photoServerLabel?.let { label ->
                                        identityDetailsWithPhoto.photoServerKey?.let { key ->
                                            ProfilePictureLabelAndKey(identity = bytesProfileIdentity, photoLabel = label, photoKey = key)
                                        }
                                    },
                                    profileAlreadyPresent = notHiddenOwnedIdentities.any { notHiddenBytesIdentity -> notHiddenBytesIdentity.contentEquals(bytesProfileIdentity) },
                                    profileBackupSeed = profileBackupSeed
                                )
                            } else {
                                null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.x(e)
                if (fetchId == fetchCount.get()) {
                    deviceBackup.value = null
                }
            } finally {
                if (fetchId == fetchCount.get()) {
                    fetchingDeviceBackup.value = false
                }
            }
        }
    }

    fun fetchProfileBackups() {
        val fetchId = fetchCount.addAndGet(1)
        App.runThread {
            profileBackups.value = null
            fetchError.value = FetchError.NONE
            try {
                fetchingProfileBackups.value = true
                selectedProfileBackup.value?.let { selectedProfileBackup ->
                    val obvProfileBackupsForRestore: ObvProfileBackupsForRestore? =
                        AppSingleton.getEngine().fetchProfileBackups(selectedProfileBackup.bytesProfileIdentity, selectedProfileBackup.profileBackupSeed)
                    when (obvProfileBackupsForRestore?.status) {
                        ObvProfileBackupsForRestore.Status.SUCCESS,
                        ObvProfileBackupsForRestore.Status.TRUNCATED -> Unit

                        ObvProfileBackupsForRestore.Status.NETWORK_ERROR -> {
                            fetchError.value = FetchError.NETWORK
                            return@runThread
                        }

                        ObvProfileBackupsForRestore.Status.PERMANENT_ERROR -> {
                            fetchError.value = FetchError.PERMANENT
                            return@runThread
                        }

                        ObvProfileBackupsForRestore.Status.ERROR, null -> {
                            fetchError.value = FetchError.RETRIABLE
                            return@runThread
                        }
                    }

                    selectedProfileDeviceList.value = obvProfileBackupsForRestore.deviceList

                    if (fetchId == fetchCount.get()) {
                        fetchError.value = FetchError.NONE
                        profileBackups.value =
                            obvProfileBackupsForRestore.snapshots?.mapNotNull { obvProfileBackupForRestore ->
                                val backup = obvProfileBackupForRestore ?: return@mapNotNull null
                                val threadId = backup.bytesBackupThreadId ?: return@mapNotNull null
                                val snapshot = backup.snapshot ?: return@mapNotNull null
                                val keycloakStatus = backup.keycloakStatus ?: return@mapNotNull null
                                val serverUrl = backup.keycloakServerUrl
                                val authMethods = backup.supportedAuthenticationMethods
                                ProfileBackupSnapshot(
                                    threadId = threadId,
                                    version = backup.version,
                                    timestamp = backup.timestamp,
                                    thisDevice = backup.fromThisDevice,
                                    deviceName = backup.additionalInfo?.get(ObvProfileBackupSnapshot.INFO_DEVICE_NAME),
                                    platform = backup.additionalInfo?.get(ObvProfileBackupSnapshot.INFO_PLATFORM),
                                    contactCount = backup.contactCount,
                                    groupCount = backup.groupCount,
                                    keycloakStatus = keycloakStatus,
                                    keycloakInfo = if (serverUrl != null && authMethods != null) {
                                        KeycloakInfo(serverUrl, authMethods.filterNotNull())
                                    } else {
                                        null
                                    },
                                    snapshot = snapshot,
                                )
                            }

                        profileBackupsTruncated.value = obvProfileBackupsForRestore.status == ObvProfileBackupsForRestore.Status.TRUNCATED
                    }
                }
            } catch (e: Exception) {
                Logger.x(e)
                if (fetchId == fetchCount.get()) {
                    profileBackups.value = null
                }
            } finally {
                if (fetchId == fetchCount.get()) {
                    fetchingProfileBackups.value = false
                }
            }
        }

    }

    fun cancelCurrentFetch(clearResults: Boolean) {
        fetchCount.addAndGet(1)
        fetchingDeviceBackup.value = false
        if (clearResults) {
            deviceBackup.value = null
        }
    }

    fun deleteProfileSnapshot(profileBackupSnapshot: ProfileBackupSnapshot) {
        selectedProfileBackup.value?.let { selectedProfileBackup ->
            fetchingProfileBackups.value = true
            App.runThread {
                val deleted = AppSingleton.getEngine().deleteProfileBackupSnapshot(selectedProfileBackup.bytesProfileIdentity, selectedProfileBackup.profileBackupSeed, profileBackupSnapshot.threadId, profileBackupSnapshot.version)
                fetchingProfileBackups.value = false
                if (deleted) {
                    profileBackups.value?.let { list ->
                        profileBackups.value = list.filter {
                            !it.threadId.contentEquals(profileBackupSnapshot.threadId) || it.version != profileBackupSnapshot.version
                        }
                    }
                }
            }
        }
    }

    fun validateDeviceBackupSeed() : Boolean {
        deviceBackupSeed.value?.let {
            if (it.replace("[^A-Za-z0-9]".toRegex(), "").length == 32) {
                backupSeedError.value = false
                return true
            }
        }
        backupSeedError.value = true
        return false
    }

    enum class FetchError {
        NONE,
        NETWORK,
        RETRIABLE,
        PERMANENT,
    }

    enum class BackupNowState {
        NONE,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
    }
}