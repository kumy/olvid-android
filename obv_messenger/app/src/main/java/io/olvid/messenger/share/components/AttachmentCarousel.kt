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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.olvid.messenger.App
import io.olvid.messenger.R
import io.olvid.messenger.discussion.linkpreview.OpenGraph
import io.olvid.messenger.share.ShareAttachment

@Composable
fun AttachmentCarousel(
    attachments: List<ShareAttachment>,
    focusedIndex: Int,
    onFocus: (Int) -> Unit,
    onRemove: (ShareAttachment) -> Unit,
    modifier: Modifier = Modifier,
    openGraph: OpenGraph?,
    onDismissOpenGraph: () -> Unit,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        itemsIndexed(items = attachments, key = { _, item -> item.uri }) { index, attachment ->
            CarouselTile(
                attachment = attachment,
                focused = index == focusedIndex,
                onClick = { if (index == focusedIndex) onRemove(attachment) else onFocus(index) },
            )
        }
        openGraph?.let {
            item(key = openGraph.url) {
                CarouselTile(
                    attachment = null,
                    focused = attachments.size == focusedIndex,
                    onClick = { if (attachments.size == focusedIndex) onDismissOpenGraph() else onFocus(attachments.size) },
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.CarouselTile(
    attachment: ShareAttachment?, // null attachment is an openGraph
    focused: Boolean,
    onClick: () -> Unit,
) {
    val hasThumbnail = attachment?.kind == ShareAttachment.Kind.IMAGE
            || attachment?.kind == ShareAttachment.Kind.VIDEO
    val tileModifier = when (attachment?.kind) {
        ShareAttachment.Kind.VIDEO -> Modifier.size(width = 64.dp, height = 48.dp)
        else -> Modifier.size(48.dp)
    }
    val borderAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = tileModifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                colorResource(if (hasThumbnail) R.color.lighterGrey else R.color.greyTint)
            )
            .border(
                width = 2.dp,
                color = colorResource(R.color.olvid_gradient_light).copy(alpha = borderAlpha),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .animateItem(),
        contentAlignment = Alignment.Center,
    ) {
        when (attachment?.kind) {
            ShareAttachment.Kind.IMAGE -> AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = attachment.uri,
                imageLoader = App.imageLoader,
                contentDescription = attachment.fileName,
                contentScale = ContentScale.Crop,
            )

            ShareAttachment.Kind.VIDEO -> {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = attachment.uri,
                    imageLoader = App.imageLoader,
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Crop,
                )
                AnimatedVisibility(
                    visible = !focused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp).background(color = colorResource(R.color.almostBlack), shape = CircleShape).padding(2.dp),
                        painter = painterResource(R.drawable.ic_play),
                        tint = colorResource(R.color.alwaysWhite),
                        contentDescription = null,
                    )
                }
            }

            ShareAttachment.Kind.AUDIO ->
                AnimatedVisibility(
                    visible = !focused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_audio),
                        tint = colorResource(R.color.alwaysWhite),
                        contentDescription = attachment.fileName,
                    )
                }

            ShareAttachment.Kind.FILE ->
                AnimatedVisibility(
                    visible = !focused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.attachment_file),
                        tint = colorResource(R.color.alwaysWhite),
                        contentDescription = attachment.fileName,
                    )
                }

            // openGraph
            null ->
                AnimatedVisibility(
                    visible = !focused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.mime_type_icon_link),
                        tint = colorResource(R.color.alwaysWhite),
                        contentDescription = null,
                    )
                }
        }
        AnimatedVisibility(
            visible = focused,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            // Dark scrim so the white trash icon stays legible over any underlying content
            // (image thumbnails as well as the icon background).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.blackOverlay)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_delete),
                    tint = colorResource(R.color.alwaysWhite),
                    contentDescription = stringResource(R.string.content_description_delete_attachment),
                )
            }
        }
    }
}
