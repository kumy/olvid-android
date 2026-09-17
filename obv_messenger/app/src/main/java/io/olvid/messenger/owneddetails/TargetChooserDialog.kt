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

package io.olvid.messenger.owneddetails

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.olvid.messenger.R
import io.olvid.messenger.designsystem.components.BaseDialogContent
import io.olvid.messenger.designsystem.components.DialogSecure
import io.olvid.messenger.designsystem.components.OlvidTextButton

@Composable
fun TargetChooserDialog(
    customTargetName: String?,
    discussionTitle: String?,
    onChooseSelf: () -> Unit,
    onChooseCustom: () -> Unit,
    onChooseBackground: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    DialogSecure(onDismissRequest = onCancel) {
        TargetChooserDialogContent(
            customTargetName = customTargetName,
            discussionTitle = discussionTitle,
            onChooseSelf = onChooseSelf,
            onChooseCustom = onChooseCustom,
            onChooseBackground = onChooseBackground,
            onCancel = onCancel
        )
    }
}

@Composable
private fun TargetChooserDialogContent(
    customTargetName: String?,
    discussionTitle: String?,
    onChooseSelf: () -> Unit,
    onChooseCustom: () -> Unit,
    onChooseBackground: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    BaseDialogContent(
        title = stringResource(R.string.menu_action_use_image_as),
        content = {
            Spacer(Modifier.height(8.dp))
            OlvidTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.label_use_photo_for_my_profile),
                allowTwoLines = true,
                large = true,
                onClick = onChooseSelf
            )
            customTargetName?.let { name ->
                OlvidTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_use_photo_for_target, name),
                    allowTwoLines = true,
                    large = true,
                    onClick = onChooseCustom
                )
            }
            onChooseBackground?.let { onClick ->
                OlvidTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = discussionTitle?.let { title ->
                        stringResource(R.string.label_use_photo_for_discussion_background, title)
                    } ?: stringResource(R.string.pref_discussion_background_image_title),
                    allowTwoLines = true,
                    large = true,
                    onClick = onClick
                )
            }
        },
        actions = {
            Spacer(Modifier.weight(1f))
            OlvidTextButton(
                text = stringResource(R.string.button_label_cancel),
                contentColor = colorResource(R.color.greyTint),
                onClick = onCancel
            )
        }
    )
}

@PreviewLightDark
@Composable
private fun TargetChooserDialogContentPreview() {
    TargetChooserDialogContent(
        customTargetName = "Alice",
        discussionTitle = "Alice",
        onChooseSelf = {},
        onChooseCustom = {},
        onChooseBackground = {},
        onCancel = {}
    )
}
