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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.olvid.messenger.App
import io.olvid.messenger.R
import io.olvid.messenger.customClasses.formatMarkdownToAnnotatedString
import io.olvid.messenger.designsystem.components.CustomDialogContent
import io.olvid.messenger.designsystem.components.DialogSecure
import io.olvid.messenger.designsystem.components.OlvidActionButton
import io.olvid.messenger.designsystem.components.OlvidOutlinedActionButton
import io.olvid.messenger.designsystem.components.OlvidOutlinedSecondaryButton
import io.olvid.messenger.designsystem.components.OlvidPasswordInput
import io.olvid.messenger.designsystem.theme.OlvidTypography


/**
 * The two dialogs shown before creating a zip export: first a password choice dialog
 * (generate a random password, type one, or continue without), then, when needed, the
 * password input dialog. Calls [onPasswordChosen] (with null for an unprotected export)
 * once the user validates, or [onDismiss] if they close the dialogs.
 */
@Composable
fun ZipExportPasswordDialogs(
    onPasswordChosen: (password: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var generatedPassword by rememberSaveable { mutableStateOf("") }
    var showZipPasswordInputDialog by rememberSaveable { mutableStateOf(false) }
    val isRandomZipPassword = generatedPassword.isNotEmpty()

    if (!showZipPasswordInputDialog) {
        PasswordDialogScaffold(onDismiss = onDismiss) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(R.string.dialog_title_protect_export),
                style = OlvidTypography.h6,
                color = colorResource(R.color.almostBlack),
            )
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(R.string.dialog_message_protect_export),
                style = OlvidTypography.body1,
                color = colorResource(R.color.greyTint),
                textAlign = TextAlign.Center
            )

            OlvidActionButton(
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_shield,
                text = stringResource(R.string.button_label_generate_password),
                allowTwoLines = true,
            ) {
                generatedPassword = generateRandomPassword()
                showZipPasswordInputDialog = true
            }
            OlvidOutlinedSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_question_shield,
                text = stringResource(R.string.button_label_choose_password),
                allowTwoLines = true,
            ) {
                generatedPassword = ""
                showZipPasswordInputDialog = true
            }
            OlvidOutlinedActionButton(
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_no_shield,
                outlinedColor = colorResource(R.color.red),
                contentColor = colorResource(R.color.red),
                text = stringResource(R.string.button_label_continue_without_password),
                allowTwoLines = true,
            ) {
                onPasswordChosen(null)
            }
        }
    } else {
        PasswordDialogScaffold(onDismiss = onDismiss) {
            val password = rememberSaveable { mutableStateOf(generatedPassword) }

            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(
                    if (isRandomZipPassword)
                        R.string.dialog_title_generated_password
                    else
                        R.string.dialog_title_choose_password
                ),
                style = OlvidTypography.h6,
                color = colorResource(R.color.almostBlack),
            )

            if (isRandomZipPassword) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(R.string.dialog_message_generated_password).formatMarkdownToAnnotatedString(),
                    style = OlvidTypography.body2,
                    color = colorResource(R.color.greyTint),
                    textAlign = TextAlign.Center
                )
            }

            OlvidPasswordInput(
                modifier = Modifier.padding(bottom = 8.dp),
                password = password,
                initiallyShowPassword = isRandomZipPassword,
                readOnly = isRandomZipPassword,
            )

            if (isRandomZipPassword) {
                OlvidOutlinedSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.ic_swipe_copy,
                    text = stringResource(R.string.button_label_copy_to_clipboard),
                    allowTwoLines = true,
                ) {
                    runCatching {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                        val clip = ClipData.newPlainText("", password.value)
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip)
                            App.toast(
                                R.string.toast_message_clipboard_copied,
                                Toast.LENGTH_SHORT
                            )
                        }
                    }
                }
            }

            OlvidActionButton(
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_folder,
                text = stringResource(R.string.button_label_create_zip_file),
                allowTwoLines = true,
                enabled = password.value.isNotEmpty()
            ) {
                onPasswordChosen(password.value.ifEmpty { null })
            }
        }
    }
}


// the dialog chrome shared by both password dialogs: secure dialog, scrollable
// centered column with the key icon header, and a top-end close button
@Composable
private fun PasswordDialogScaffold(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DialogSecure(
        onDismissRequest = onDismiss,
    ) {
        CustomDialogContent {
            Box {
                Column(
                    modifier = Modifier
                        .verticalScroll(state = rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier
                            .size(64.dp)
                            .background(colorResource(R.color.green), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        painter = painterResource(R.drawable.ic_backup_key),
                        contentDescription = null,
                        tint = colorResource(R.color.almostWhite),
                    )

                    content()
                }

                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp),
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = colorResource(R.color.almostBlack)
                    ),
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.button_label_cancel),
                    )
                }
            }
        }
    }
}


private fun generateRandomPassword(): String {
    @Suppress("SpellCheckingInspection")
    val characters = "ABCDEFGHJKLMNPQRTUVWXYZabcdefghijkmnopqrstuvwxyz2346789".toCharArray()

    return String(
        CharArray(6) { characters.random() } +
                '-' +
                CharArray(6) { characters.random() } +
                '-' +
                CharArray(6) { characters.random() }
    )
}
