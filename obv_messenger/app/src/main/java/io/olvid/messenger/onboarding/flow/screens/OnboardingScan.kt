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

package io.olvid.messenger.onboarding.flow.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.olvid.messenger.App
import io.olvid.messenger.R
import io.olvid.messenger.activities.ObvLinkActivity
import io.olvid.messenger.designsystem.components.OlvidDropdownMenu
import io.olvid.messenger.designsystem.components.OlvidDropdownMenuItem
import io.olvid.messenger.designsystem.components.OlvidTopAppBar
import io.olvid.messenger.onboarding.OnboardingViewModel
import io.olvid.messenger.onboarding.flow.OnboardingRoutes
import io.olvid.messenger.plus_button.scan.CameraPreview
import io.olvid.messenger.plus_button.scan.ScanningReticle

/**
 * Compose replacement for the legacy ScanFragment: a full-screen QR scanner (reusing the modern
 * [CameraPreview] + [ScanningReticle] from the plus button) used to read a configuration or keycloak
 * link, with an overflow menu to import from the clipboard, enter a manual configuration, or use a
 * keycloak server. Navigation callbacks are always invoked on the main thread.
 */
fun NavGraphBuilder.onboardingScan(
    onboardingViewModel: OnboardingViewModel,
    onConfigurationScanned: () -> Unit,
    onKeycloakScanned: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        OnboardingRoutes.ONBOARDING_SCAN,
        enterTransition = { slideIntoContainer(SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start) },
        popEnterTransition = { slideIntoContainer(SlideDirection.End) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End) }
    ) {
        val context = LocalContext.current
        var showMenu by remember { mutableStateOf(false) }
        var handled by remember { mutableStateOf(false) }
        var lastUnrecognized by remember { mutableStateOf<String?>(null) }
        var useFrontCamera by remember { mutableStateOf(false) }

        fun onMainThread(block: () -> Unit) = Handler(Looper.getMainLooper()).post(block)

        fun routeFromParsedConfiguration() {
            onboardingViewModel.isDeepLinked = true
            if (onboardingViewModel.keycloakServer != null) {
                onMainThread(onKeycloakScanned)
            } else {
                onMainThread(onConfigurationScanned)
            }
        }

        fun parseConfiguration(text: CharSequence): Boolean {
            val matcher = ObvLinkActivity.CONFIGURATION_PATTERN.matcher(text)
            return matcher.find() && onboardingViewModel.parseScannedConfigurationUri(
                matcher.group(
                    2
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                paused = handled,
                useFrontCamera = useFrontCamera,
                onQrCodeScanned = { text ->
                    // CameraPreview invokes this on its background analysis executor; marshal to the
                    // main thread before touching Compose state / navigating.
                    onMainThread {
                        if (!handled) {
                            if (parseConfiguration(text)) {
                                handled = true
                                routeFromParsedConfiguration()
                            } else if (text != lastUnrecognized) {
                                lastUnrecognized = text
                                App.toast(
                                    R.string.toast_message_unrecognized_url,
                                    Toast.LENGTH_SHORT
                                )
                            }
                        }
                    }
                },
            )

            Box(
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top))
            ) {
                val configuration = LocalConfiguration.current
                ScanningReticle(
                    brush = SolidColor(Color.White),
                    fraction = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) .7f else .9f,
                )
            }

            OlvidTopAppBar(
                modifier = Modifier.align(Alignment.TopCenter),
                titleText = stringResource(R.string.activity_title_scan_configuration),
                actions = {
                    IconButton(
                        modifier = Modifier.padding(8.dp),
                        onClick = { useFrontCamera = !useFrontCamera }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera_switch),
                            tint = colorResource(R.color.alwaysWhite),
                            contentDescription = stringResource(R.string.content_description_switch_camera)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_three_dots_always_white),
                                tint = colorResource(R.color.alwaysWhite),
                                contentDescription = null
                            )
                        }
                        OlvidDropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }) {
                            OlvidDropdownMenuItem(
                                text = stringResource(R.string.menu_action_import_activation_from_clipboard),
                                onClick = {
                                    showMenu = false
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val text =
                                        clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
                                            ?.getItemAt(0)?.text
                                    if (text != null && parseConfiguration(text)) {
                                        routeFromParsedConfiguration()
                                    } else {
                                        App.toast(
                                            R.string.toast_message_invalid_clipboard_data,
                                            Toast.LENGTH_SHORT
                                        )
                                    }
                                }
                            )
                            OlvidDropdownMenuItem(
                                text = stringResource(R.string.menu_action_manual_configuration),
                                onClick = {
                                    showMenu = false
                                    onConfigurationScanned()
                                }
                            )
                            OlvidDropdownMenuItem(
                                text = stringResource(R.string.menu_action_use_identity_provider),
                                onClick = {
                                    showMenu = false
                                    onKeycloakScanned()
                                }
                            )
                        }
                    }
                },
                alwaysDark = true,
                elevationShadow = false,
                onBackPressed = onBack
            )
        }
    }
}
