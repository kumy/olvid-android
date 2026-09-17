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

package io.olvid.messenger.main.contacts.suggested

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.olvid.messenger.R
import io.olvid.messenger.designsystem.cutoutHorizontalPadding
import io.olvid.messenger.designsystem.systemBarsHorizontalPadding
import io.olvid.messenger.designsystem.theme.OlvidTypography

/**
 * Entry banner at the top of the Contacts tab. Styled after [io.olvid.messenger.main.tips
 * .TipItem]'s TipBubble (rounded, lighterGrey, gradient border) with the Figma layout: icon ·
 * title + subtitle · chevron, whole row clickable.
 */
@Composable
fun SuggestedContactsBanner(
    modifier: Modifier = Modifier,
    redBadgeContactCount: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .cutoutHorizontalPadding()
            .systemBarsHorizontalPadding()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorResource(R.color.lighterGrey))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        colorResource(R.color.green),
                        colorResource(R.color.olvid_gradient_light),
                    ),
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_suggested_contacts),
            tint = colorResource(R.color.green ),
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.heightIn(min = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f, false),
                    text = stringResource(R.string.label_suggested_contacts),
                    maxLines = 1,
                    style = OlvidTypography.body1.copy(fontWeight = FontWeight.Medium),
                    color = colorResource(R.color.almostBlack),
                )
                if (redBadgeContactCount != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        modifier = Modifier
                            .background(
                                color = colorResource(id = R.color.red),
                                shape = CircleShape
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        text = "$redBadgeContactCount",
                        style = OlvidTypography.body2,
                        color = colorResource(id = R.color.alwaysWhite)
                    )
                }
            }
            Text(
                text = stringResource(R.string.label_suggested_contacts_subtitle),
                maxLines = 1,
                style = OlvidTypography.body2,
                color = colorResource(R.color.greyTint),
            )
        }
        Icon(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape),
            painter = painterResource(R.drawable.ic_chevron_right),
            tint = colorResource(R.color.greyTint),
            contentDescription = null,
        )
    }
}

/**
 * Variant banner shown at the top of the OTHERS ("Autres utilisateurs") tab: a [R.color
 * .dialogBackground] card with an explanatory paragraph, a divider, and a blue "Invite these users
 * in a single gesture" action row that opens the same swipe sheet. Matches the Figma OTHERS-tab
 * layout (node 174:1176) rather than the compact gradient-border banner used on the Contacts tab.
 */
@Composable
fun SuggestedContactsOthersBanner(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cutoutHorizontalPadding()
            .systemBarsHorizontalPadding()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorResource(R.color.lighterGrey)),
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
            text = stringResource(R.string.explanation_suggested_contacts_others_tab),
            style = OlvidTypography.body1,
            color = colorResource(R.color.greyTint),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = colorResource(R.color.lightGrey)
        )
        Row(
            modifier = Modifier.fillMaxWidth() .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
                .padding(top = 12.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_suggested_contacts),
                tint = colorResource(R.color.olvid_gradient_light),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.label_suggested_contacts_invite_in_one_gesture),
                style = OlvidTypography.body1,
                color = colorResource(R.color.olvid_gradient_light),
            )
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_chevron_right),
                tint = colorResource(R.color.olvid_gradient_light),
                contentDescription = null,
            )
        }
    }
}


@Preview
@Composable
private fun SuggestedContactsOthersBannerPreview() {
    Column {
        SuggestedContactsBanner(redBadgeContactCount = 12) {}
        SuggestedContactsBanner() {}
        SuggestedContactsOthersBanner {}
    }
}