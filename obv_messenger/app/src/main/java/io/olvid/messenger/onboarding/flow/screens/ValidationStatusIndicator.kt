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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.olvid.messenger.R
import io.olvid.messenger.onboarding.OnboardingViewModel.ValidatedStatus

/** Shared 24dp server/identity-provider validation status indicator (spinner / green check / red cross). */
@Composable
fun ValidationStatusIndicator(status: ValidatedStatus?) {
    Crossfade(targetState = status) { status ->
        when (status) {
            ValidatedStatus.CHECKING -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = colorResource(R.color.olvid_gradient_light),
            )

            ValidatedStatus.VALID -> Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_ok_green),
                tint = Color.Unspecified,
                contentDescription = null,
            )

            ValidatedStatus.INVALID -> Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_remove),
                tint = Color.Unspecified,
                contentDescription = null,
            )

            else -> Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
