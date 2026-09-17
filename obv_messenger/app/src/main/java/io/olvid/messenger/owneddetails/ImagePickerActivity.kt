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

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import io.olvid.messenger.App
import io.olvid.messenger.AppSingleton
import io.olvid.messenger.R
import io.olvid.messenger.databases.AppDatabase
import io.olvid.messenger.databases.dao.FyleMessageJoinWithStatusDao.FyleAndStatus
import io.olvid.messenger.designsystem.components.OlvidTopAppBar
import androidx.compose.foundation.layout.plus
import io.olvid.messenger.designsystem.theme.ProvideOlvidRipple
import io.olvid.messenger.lock_screen.LockableActivity

class ImagePickerActivity : LockableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                ContextCompat.getColor(this, R.color.blackOverlay)
            )
        )

        super.onCreate(savedInstanceState)

        val bytesOwnedIdentity = intent.getByteArrayExtra(BYTES_OWNED_IDENTITY_INTENT_EXTRA)
            ?: AppSingleton.getBytesCurrentIdentity()
        if (bytesOwnedIdentity == null) {
            finish()
            return
        }

        setContent {
            val images by remember {
                AppDatabase.getInstance().fyleMessageJoinWithStatusDao()
                    .getImageFylesForOwnedIdentity(bytesOwnedIdentity)
            }.observeAsState()

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = colorResource(R.color.almostWhite),
                contentColor = colorResource(R.color.almostBlack),
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    OlvidTopAppBar(
                        titleText = stringResource(R.string.menu_action_choose_image_from_olvid),
                        onBackPressed = { onBackPressedDispatcher.onBackPressed() }
                    )
                }
            ) { contentPadding ->
                if (images?.isEmpty() == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.label_gallery_no_content))
                    }
                } else {
                    ProvideOlvidRipple {
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Adaptive(minSize = 96.dp),
                            contentPadding = contentPadding + PaddingValues(2.dp)
                        ) {
                            items(
                                items = images.orEmpty(),
                                key = { it.fyle.id }
                            ) { fyleAndStatus ->
                                ImageCell(
                                    fyleAndStatus = fyleAndStatus,
                                    onClick = { returnSelectedImage(fyleAndStatus) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun returnSelectedImage(fyleAndStatus: FyleAndStatus) {
        fyleAndStatus.contentUriForExternalSharing?.let { uri ->
            setResult(RESULT_OK, Intent().apply { data = uri })
        }
        finish()
    }

    companion object {
        const val BYTES_OWNED_IDENTITY_INTENT_EXTRA: String = "bytes_owned_identity"
    }
}

@Composable
private fun ImageCell(
    fyleAndStatus: FyleAndStatus,
    onClick: () -> Unit,
) {
    val imageUri = remember(fyleAndStatus.fyle.id) {
        fyleAndStatus.deterministicContentUriForGallery
    }
    AsyncImage(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        model = imageUri,
        imageLoader = App.imageLoader,
        placeholder = ColorPainter(colorResource(R.color.grey)),
        contentScale = ContentScale.Crop,
        contentDescription = fyleAndStatus.fyleMessageJoinWithStatus.fileName
    )
}
