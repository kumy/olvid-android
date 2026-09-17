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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.olvid.engine.engine.types.JsonIdentityDetails
import io.olvid.messenger.R
import io.olvid.messenger.customClasses.formatMarkdownToAnnotatedString
import io.olvid.messenger.databases.entity.OwnedIdentity
import io.olvid.messenger.designsystem.components.OlvidCircularProgress
import io.olvid.messenger.designsystem.components.OwnedIdentityCard
import io.olvid.messenger.designsystem.theme.OlvidTypography
import io.olvid.messenger.settings.history_transfer.HistoryTransferRoutes


fun NavGraphBuilder.pickProfileScreen(
    onProfileSelected: (OwnedIdentity) -> Unit,
    importMode: State<Boolean>,
    ownedIdentityList: State<List<OwnedIdentity>?>,
) {
    composable(
        HistoryTransferRoutes.PICK_PROFILE_SCREEN,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.lightGrey))
                .verticalScroll(rememberScrollState())
                .padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.history_transfer_pick_profile_title),
                style = OlvidTypography.h1.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = colorResource(R.color.almostBlack),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(
                    if (importMode.value)
                        R.string.history_transfer_pick_profile_text_import
                    else
                        R.string.history_transfer_pick_profile_text_export
                ).formatMarkdownToAnnotatedString(),
                style = OlvidTypography.body1,
                color = colorResource(R.color.almostBlack),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            ownedIdentityList.value?.takeIf { it.isNotEmpty() }?.let {
                it.forEach { ownedIdentity ->
                    val ownedDetails = remember(ownedIdentity.identityDetails) {
                        ownedIdentity.getIdentityDetails()
                    }
                    val customTitle = ownedIdentity.customDisplayName
                        ?: ownedDetails?.formatFirstAndLastName(
                            JsonIdentityDetails.FORMAT_STRING_FIRST_LAST,
                            false,
                        )
                        ?: ownedIdentity.displayName
                    val customSubtitle = if (ownedIdentity.customDisplayName == null) {
                        ownedDetails?.formatPositionAndCompany(
                            JsonIdentityDetails.FORMAT_STRING_FIRST_LAST_POSITION_COMPANY,
                        )
                    } else {
                        ownedDetails?.formatDisplayName(
                            JsonIdentityDetails.FORMAT_STRING_FIRST_LAST_POSITION_COMPANY,
                            false,
                        ) ?: ownedIdentity.displayName
                    }
                    OwnedIdentityCard(
                        identity = ownedIdentity,
                        onClick = { onProfileSelected.invoke(ownedIdentity) },
                        backgroundColor = colorResource(R.color.almostWhite),
                        title = customTitle,
                        subtitle = customSubtitle,
                        avatarCornerRadius = 12.dp,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            } ?: run {
                OlvidCircularProgress()
            }
        }
    }
}

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    locale = "fr"
)
@Composable
private fun PickProfileScreenPreview() {
    NavHost(
        navController = rememberNavController(),
        startDestination = HistoryTransferRoutes.PICK_PROFILE_SCREEN,
    ) {
        pickProfileScreen(
            onProfileSelected = {},
            importMode = mutableStateOf(true),
            ownedIdentityList = mutableStateOf(
                listOf(
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
                        true
                    ),

                    OwnedIdentity(
                        ByteArray(2),
                        "Marie Boulier",
                        null,
                        0,
                        0,
                        null,
                        0,
                        null,
                        false,
                        true,
                        null,
                        null,
                        null,
                        false,
                        false,
                        null,
                        null,
                        false,
                        true,
                        true,
                        true
                    )
                )
            )
        )
    }
}