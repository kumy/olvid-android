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

package io.olvid.messenger.settings.history_transfer.composables

import android.content.res.Configuration
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.olvid.engine.engine.types.JsonIdentityDetails
import io.olvid.messenger.R
import io.olvid.messenger.customClasses.BytesKey
import io.olvid.messenger.customClasses.formatMarkdownToAnnotatedString
import io.olvid.messenger.databases.entity.OwnedIdentity
import io.olvid.messenger.designsystem.components.OlvidActionButton
import io.olvid.messenger.designsystem.components.OlvidCircularProgress
import io.olvid.messenger.designsystem.theme.OlvidTypography
import io.olvid.messenger.settings.history_transfer.ExportScope
import io.olvid.messenger.settings.history_transfer.HistoryTransferRoutes


fun NavGraphBuilder.exportZipScreen(
    onChooseZipFile: (password: String?) -> Unit,
    selectedOwnedIdentity: State<OwnedIdentity?>,
    exportScope: State<ExportScope>,
    discussionCountLiveData: LiveData<Int>,
    messageCountLiveData: LiveData<Int>,
    sha256sMapLiveData: LiveData<Map<BytesKey, Long>?>,
) {
    composable(
        HistoryTransferRoutes.EXPORT_ZIP_SCREEN,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        val displayName = remember {
            val details = selectedOwnedIdentity.value?.getIdentityDetails()

            return@remember selectedOwnedIdentity.value?.customDisplayName
                ?: details?.formatFirstAndLastName(
                    JsonIdentityDetails.FORMAT_STRING_FIRST_LAST,
                    false
                )
                ?: selectedOwnedIdentity.value?.displayName
                ?: ""
        }

        val context = LocalContext.current

        val discussionCount by discussionCountLiveData.observeAsState()
        val messageCount by messageCountLiveData.observeAsState()
        val sha256sMap by sha256sMapLiveData.observeAsState()

        var showPasswordDialogs by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.lightGrey))
                .verticalScroll(rememberScrollState())
                .padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorResource(R.color.almostWhite))
                    .padding(all = 16.dp),
                verticalArrangement = spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    modifier = Modifier
                        .size(80.dp)
                        .background(colorResource(R.color.orange), CircleShape)
                        .padding(12.dp),
                    painter = painterResource(R.drawable.ic_zip),
                    contentDescription = null,
                    tint = colorResource(R.color.almostWhite),
                )
                Text(
                    text = stringResource(R.string.history_transfer_via_zip_title_export),
                    style = OlvidTypography.h1.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = colorResource(R.color.almostBlack),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.history_transfer_export_zip_description, displayName).formatMarkdownToAnnotatedString(),
                    style = OlvidTypography.body1,
                    color = colorResource(R.color.almostBlack),
                    textAlign = TextAlign.Center,
                )

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorResource(R.color.mediumGrey)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp),
                            painter = painterResource(R.drawable.ic_bubbles),
                            tint = colorResource(R.color.olvid_gradient_light),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        discussionCount?.takeIf { it >= 0 }?.let { discussions ->
                            messageCount?.takeIf { it >= 0 }?.let { messages ->
                                Text(
                                    text = stringResource(R.string.label_discussions_and_messages_count, discussions, messages),
                                    style = OlvidTypography.body1,
                                    color = colorResource(R.color.almostBlack),
                                )
                            }
                        } ?: run {
                            OlvidCircularProgress(size = 16.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.label_computing),
                                style = OlvidTypography.body1,
                                color = colorResource(R.color.almostBlack),
                            )
                        }
                    }

                    if (exportScope.value == ExportScope.EVERYTHING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp),
                                painter = painterResource(R.drawable.ic_attach_file),
                                tint = colorResource(R.color.greyTint),
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            sha256sMap?.let { sha256s ->
                                Text(
                                    text = LocalResources.current.getQuantityString(R.plurals.label_files_count, sha256s.size, sha256s.size),
                                    style = OlvidTypography.body1,
                                    color = colorResource(R.color.almostBlack),
                                )
                            } ?: run {
                                OlvidCircularProgress(size = 16.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.label_computing),
                                    style = OlvidTypography.body1,
                                    color = colorResource(R.color.almostBlack),
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp),
                                painter = painterResource(R.drawable.ic_archive),
                                tint = colorResource(R.color.orange),
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            sha256sMap?.let { sha256s ->
                                val totalSize = remember(sha256s) {
                                    sha256s.values.sum()
                                }
                                Text(
                                    text = stringResource(R.string.label_total_size, Formatter.formatShortFileSize(context, totalSize)),
                                    style = OlvidTypography.body1,
                                    color = colorResource(R.color.almostBlack),
                                )
                            } ?: run {
                                OlvidCircularProgress(size = 16.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.label_computing),
                                    style = OlvidTypography.body1,
                                    color = colorResource(R.color.almostBlack),
                                )
                            }
                        }
                    }
                }

                OlvidActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.ic_folder,
                    text = stringResource(R.string.button_label_create_zip_file),
                    large = true,
                    allowTwoLines = true,
                    onClick = {
                        showPasswordDialogs = true
                    },
                )
            }


            if (showPasswordDialogs) {
                ZipExportPasswordDialogs(
                    onPasswordChosen = { password ->
                        showPasswordDialogs = false
                        onChooseZipFile(password)
                    },
                    onDismiss = {
                        showPasswordDialogs = false
                    },
                )
            }
        }
    }
}


@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    locale = "fr"
)
@Composable
private fun ExportZipScreenPreview() {
    NavHost(
        navController = rememberNavController(),
        startDestination = HistoryTransferRoutes.EXPORT_ZIP_SCREEN,
    ) {
        exportZipScreen(
            onChooseZipFile = {},
            selectedOwnedIdentity = mutableStateOf(
                OwnedIdentity(
                    ByteArray(2),
                    "Lisa Martin",
                    null,
                    0,
                    0,
                    null,
                    0,
                    null,
                    true,
                    true,
                    "Lisa 💗",
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    false,
                    true,
                    true,
                    true,
                ),
            ),
            exportScope = mutableStateOf(ExportScope.EVERYTHING),
            discussionCountLiveData = MutableLiveData(12),
            messageCountLiveData = MutableLiveData(736),
            sha256sMapLiveData = MutableLiveData(mapOf(
                BytesKey(ByteArray(0)) to 758_000L,
                BytesKey(ByteArray(1)) to 2_475_000L,
                BytesKey(ByteArray(2)) to 756L,
            )),
        )
    }
}