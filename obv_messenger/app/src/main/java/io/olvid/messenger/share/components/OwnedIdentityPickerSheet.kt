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

package io.olvid.messenger.share.components

import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import io.olvid.messenger.R
import io.olvid.messenger.customClasses.OpenHiddenProfileDialog
import io.olvid.messenger.databases.entity.OwnedIdentity
import io.olvid.messenger.designsystem.components.OlvidDragHandle
import io.olvid.messenger.designsystem.components.OwnedIdentityCard
import io.olvid.messenger.designsystem.theme.OlvidTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnedIdentityPickerSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onIdentityChosen: (ByteArray) -> Unit,
    identities: MutableList<OwnedIdentity>?,
) {
    // Hidden profiles are intentionally NOT listed (that would reveal their existence). As in the
    // main-app switcher (OwnIdentitySelectorPopupWindow long-pressing "Add profile"), a long-press
    // on the sheet title opens the password dialog: typing a hidden profile's password unlocks it
    // and selects it as the share sender, routed through the same onIdentityChosen path as a normal
    // pick. No visible hint, so the affordance doesn't betray whether any hidden profile exists.
    val activity = LocalActivity.current as? FragmentActivity
    val compact = LocalWindowInfo.current.containerSize.height < 540 * LocalDensity.current.density

    ModalBottomSheet(
        modifier = Modifier
            .padding(top = if (compact) 0.dp else 48.dp),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            if (compact)
                Box {}
            else
                OlvidDragHandle()
        },
        containerColor = colorResource(R.color.almostWhite),
        contentColor = colorResource(R.color.almostBlack),
        contentWindowInsets = { WindowInsets() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(activity) {
                            detectTapGestures(onLongPress = {
                                activity?.let { openHiddenProfileDialog(it, onIdentityChosen) }
                            })
                        },
                    text = stringResource(R.string.button_label_switch_profile),
                    style = OlvidTypography.h2.copy(color = colorResource(R.color.almostBlack)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(8.dp)
                        .size(24.dp),
                    painter = painterResource(R.drawable.ic_close),
                    tint = colorResource(R.color.almostBlack),
                    contentDescription = stringResource(R.string.content_description_close_button),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = identities.orEmpty(),
                    key = { it.bytesOwnedIdentity.toList() },
                ) { identity ->
                    OwnedIdentityCard(
                        identity = identity,
                        onClick = { onIdentityChosen(identity.bytesOwnedIdentity) },
                    )
                }
            }
        }
    }
}

/**
 * Opens the canonical hidden-profile password dialog ([OpenHiddenProfileDialog]). On a matching
 * password the unlocked identity is handed to [onIdentityChosen] — the same path a normal pick
 * takes.
 */
internal fun openHiddenProfileDialog(
    activity: FragmentActivity,
    onIdentityChosen: (ByteArray) -> Unit,
) {
    object : OpenHiddenProfileDialog(activity) {
        override fun onHiddenIdentityPasswordEntered(
            dialog: AlertDialog,
            byteOwnedIdentity: ByteArray
        ) {
            dialog.dismiss()
            onIdentityChosen(byteOwnedIdentity)
        }
    }
}
