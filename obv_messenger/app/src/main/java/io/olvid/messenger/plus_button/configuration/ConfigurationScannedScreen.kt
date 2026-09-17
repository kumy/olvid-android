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

package io.olvid.messenger.plus_button.configuration

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.olvid.engine.Logger
import io.olvid.engine.datatypes.ObvBase64
import io.olvid.messenger.AppSingleton
import io.olvid.messenger.R
import io.olvid.messenger.activities.ObvLinkActivity
import io.olvid.messenger.customClasses.ConfigurationPojo
import io.olvid.messenger.designsystem.theme.OlvidTypography
import io.olvid.messenger.openid.KeycloakAuthenticator
import io.olvid.messenger.plus_button.PlusButtonViewModel

sealed class Screen {
    object ConfigurationScan : Screen()
    object KeycloakBind : Screen()
}

@Composable
fun ConfigurationScannedScreen(
    modifier: Modifier = Modifier,
    plusButtonViewModel: PlusButtonViewModel,
    onCancel: () -> Unit
) {
    val activity = LocalActivity.current
    val authenticator = remember { KeycloakAuthenticator(activity as ComponentActivity) }

    when (plusButtonViewModel.currentConfigurationScreen) {
        is Screen.ConfigurationScan -> {
            ConfigurationScanContent(
                modifier = modifier,
                plusButtonViewModel = plusButtonViewModel,
                onCancel = onCancel,
                onNavigateToKeycloakBind = { plusButtonViewModel.currentConfigurationScreen = Screen.KeycloakBind },
                authenticator = authenticator
            )
        }

        is Screen.KeycloakBind -> {
            KeycloakBindScreen(
                viewModel = plusButtonViewModel,
                onBindSuccess = onCancel,
                onBack = onCancel
            )
        }
    }
}

@Composable
fun ConfigurationScanContent(
    modifier: Modifier = Modifier,
    plusButtonViewModel: PlusButtonViewModel,
    onCancel: () -> Unit,
    onNavigateToKeycloakBind: () -> Unit,
    authenticator: KeycloakAuthenticator
) {

    LaunchedEffect(plusButtonViewModel.scannedUri) {
        val uri = plusButtonViewModel.scannedUri
        if (uri == null) {
            onCancel()
            return@LaunchedEffect
        }

        val matcher = ObvLinkActivity.CONFIGURATION_PATTERN.matcher(uri)
        if (matcher.find()) {
            try {
                plusButtonViewModel.configurationPojo = AppSingleton.getJsonObjectMapper().readValue(
                    ObvBase64.decode(matcher.group(2)!!),
                    ConfigurationPojo::class.java
                )
            } catch (e: Exception) {
                Logger.x(e)
                onCancel()
            }
        } else {
            onCancel()
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        plusButtonViewModel.configurationPojo?.let { configuration ->
            when {
                configuration.server != null && configuration.apikey != null -> {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.activity_title_license_activation),
                        style = OlvidTypography.h2,
                        color = colorResource(R.color.almostBlack)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LicenseActivationContent(plusButtonViewModel, configuration, onCancel)
                }

                configuration.settings != null -> {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.activity_title_settings_update),
                        style = OlvidTypography.h2,
                        color = colorResource(R.color.almostBlack)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsUpdateContent(settingsPojo = configuration.settings, onUpdate = {
                        configuration.settings?.toBackupPojo()?.restore()
                        onCancel()
                    }, onCancel = onCancel)
                }

                configuration.keycloak != null -> {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.activity_title_identity_provider),
                        style = OlvidTypography.h2,
                        color = colorResource(R.color.almostBlack)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    KeycloakContent(
                        viewModel = plusButtonViewModel,
                        keycloakPojo = configuration.keycloak,
                        keycloakMagic = configuration.magic,
                        onCancel = onCancel,
                        onNavigateToKeycloakBind = onNavigateToKeycloakBind,
                        authenticator = authenticator
                    )
                }

                else -> {
                    // Invalid configuration, go back
                    LaunchedEffect(Unit) { onCancel() }
                }
            }
        }
        Spacer(
            modifier = Modifier
                .height(WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding())
        )
    }
}