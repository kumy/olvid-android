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

package io.olvid.messenger.designsystem.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.olvid.messenger.R

data class OlvidLogoDefaults(val iconSize: Dp, val rounding: Dp)
enum class OlvidLogoSize(val defaults: OlvidLogoDefaults) {
    SMALL(OlvidLogoDefaults(iconSize = 16.dp, rounding = 4.dp)),
    MEDIUM(OlvidLogoDefaults(iconSize = 48.dp, rounding = 12.dp)),
    LARGE(OlvidLogoDefaults(iconSize = 64.dp, rounding = 16.dp)),
    EXTRA_LARGE(OlvidLogoDefaults(iconSize = 80.dp, rounding = 20.dp))
}

@Composable
fun OlvidLogo(
    size: OlvidLogoSize = OlvidLogoSize.MEDIUM
) {
    Box(
        modifier = Modifier
            .size(size.defaults.iconSize)
            .background(
                shape = RoundedCornerShape(size.defaults.rounding),
                brush = Brush.verticalGradient(
                    listOf(
                        colorResource(id = R.color.olvid_gradient_light),
                        colorResource(id = R.color.olvid_gradient_dark)
                    )
                )
            )
    ) {
        Image(
            modifier = Modifier
                .align(Alignment.Center)
                .size(size.defaults.iconSize),
            painter = painterResource(id = R.drawable.icon_olvid_no_padding),
            contentDescription = "Olvid"
        )
    }
}

@Preview
@Composable
private fun OlvidLogoPreview() {
    OlvidLogo(size = OlvidLogoSize.LARGE)
}