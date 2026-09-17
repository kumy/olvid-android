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

import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.olvid.messenger.App
import io.olvid.messenger.R
import io.olvid.messenger.customClasses.StringUtils
import io.olvid.messenger.designsystem.theme.OlvidTypography
import io.olvid.messenger.discussion.gallery.getDrawableResourceForMimeType
import io.olvid.messenger.discussion.linkpreview.LinkPreviewContent
import io.olvid.messenger.discussion.linkpreview.OpenGraph
import io.olvid.messenger.share.ShareAttachment
import io.olvid.messenger.share.renderUriPdfFirstPage
import kotlinx.coroutines.launch

@Composable
fun ShareContentPreview(
    attachments: List<ShareAttachment>,
    openGraph: OpenGraph?,
    modifier: Modifier = Modifier,
    onRemoveAttachment: (ShareAttachment) -> Unit = {},
    onDismissOpenGraph: () -> Unit = {},
) {
    when {
        attachments.isEmpty() -> UrlPreview(modifier, openGraph, onDismissOpenGraph)

        attachments.size == 1 && openGraph == null -> SingleAttachmentPreview(attachments.first(), modifier)

        else -> MultipleAttachmentsPreview(
            modifier = modifier,
            attachments = attachments,
            openGraph = openGraph,
            onDismissOpenGraph = onDismissOpenGraph,
            onRemove = onRemoveAttachment,
        )
    }
}

@Composable
private fun UrlPreview(
    modifier: Modifier = Modifier,
    openGraph: OpenGraph?,
    onDismissOpenGraph: () -> Unit = {},
) {
    // Plain shared text is NOT previewed here: it is seeded into the compose bar (draftText) and
    // editable there, so rendering it again in the preview area would just duplicate it. Only a
    // resolved link preview gets a card — that is genuine added value the compose bar can't show.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (openGraph?.isEmpty() == false) {
            // Reuses the same LinkPreviewContent the discussion list renders, so what the user
            // sees here matches what recipients will see once sent. The X overlay matches the
            // dismissal affordance from the compose-bar LinkPreviewPicker.
            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                LinkPreviewContent(
                    openGraph = openGraph,
                    highlighter = null,
                    blockClicks = true,
                )
                Icon(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClick = onDismissOpenGraph
                        )
                        .background(colorResource(R.color.almostWhite))
                        .border(width = 1.dp, color = colorResource(R.color.attachmentBorder), shape = CircleShape)
                        .padding(4.dp),
                    painter = painterResource(R.drawable.ic_close),
                    tint = colorResource(R.color.almostBlack),
                    contentDescription = stringResource(R.string.button_label_cancel),
                )
            }
        }
    }
}

@Composable
private fun SingleAttachmentPreview(attachment: ShareAttachment, modifier: Modifier = Modifier) {
    when (attachment.kind) {
        ShareAttachment.Kind.IMAGE -> ImageAttachmentPreview(attachment, modifier)
        ShareAttachment.Kind.VIDEO -> VideoAttachmentPreview(attachment, modifier)
        ShareAttachment.Kind.AUDIO, ShareAttachment.Kind.FILE ->
            FileAttachmentPreview(attachment, modifier)
    }
}

@Composable
private fun ImageAttachmentPreview(attachment: ShareAttachment, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp)),
            model = attachment.uri,
            imageLoader = App.imageLoader,
            contentDescription = attachment.fileName,
            contentScale = ContentScale.FillWidth,
        )
    }
}

@Composable
private fun VideoAttachmentPreview(attachment: ShareAttachment, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorResource(R.color.almostBlack)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                model = attachment.uri,
                imageLoader = App.imageLoader,
                contentDescription = attachment.fileName,
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.blackOverlay)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(40.dp),
                    painter = painterResource(R.drawable.ic_play),
                    tint = colorResource(R.color.alwaysWhite),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun FileAttachmentPreview(attachment: ShareAttachment, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pdfBitmap = rememberPdfFirstPageBitmap(attachment)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorResource(R.color.lighterGrey))
                .border(1.dp, color = colorResource(R.color.lightGrey), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (pdfBitmap != null) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = pdfBitmap,
                    imageLoader = App.imageLoader,
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Fit,
                )
            } else {
                Image(
                    modifier = Modifier.size(56.dp),
                    painter = painterResource(attachment.mime.getDrawableResourceForMimeType()),
                    contentDescription = null,
                )
            }
        }
        Text(
            text = attachment.fileName,
            style = OlvidTypography.body1.copy(
                color = colorResource(R.color.almostBlack),
                fontWeight = FontWeight.Medium,
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        attachment.lastModifiedMillis?.let { millis ->
            Text(
                text = StringUtils.getDateString(context, millis),
                style = OlvidTypography.body2.copy(color = colorResource(R.color.greyTint)),
                textAlign = TextAlign.Center,
            )
        }
        attachment.sizeBytes?.let { size ->
            Text(
                text = Formatter.formatShortFileSize(context, size),
                style = OlvidTypography.body2.copy(color = colorResource(R.color.greyTint)),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun rememberPdfFirstPageBitmap(attachment: ShareAttachment): android.graphics.Bitmap? {
    if (attachment.mime != "application/pdf") return null
    val context = LocalContext.current
    var bitmap by remember(attachment.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(attachment.uri) {
        bitmap = renderUriPdfFirstPage(context.contentResolver, attachment.uri, sizePx = 512)
    }
    return bitmap
}

@Composable
private fun MultipleAttachmentsPreview(
    modifier: Modifier = Modifier,
    attachments: List<ShareAttachment>,
    openGraph: OpenGraph?,
    onDismissOpenGraph: () -> Unit,
    onRemove: (ShareAttachment) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { attachments.size + if (openGraph == null) 0 else 1})
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
        ) { pageIndex ->
            if (pageIndex > attachments.lastIndex) {
                UrlPreview(modifier = Modifier.fillMaxSize(), openGraph = openGraph, onDismissOpenGraph = onDismissOpenGraph)
            } else {
                attachments.getOrNull(pageIndex)?.let {
                    SingleAttachmentPreview(it, Modifier.fillMaxSize())
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AttachmentCarousel(
            modifier = Modifier.fillMaxWidth(),
            attachments = attachments,
            focusedIndex = pagerState.currentPage,
            onFocus = { scope.launch { pagerState.animateScrollToPage(it) } },
            onRemove = onRemove,
            openGraph = openGraph,
            onDismissOpenGraph = onDismissOpenGraph,
        )
    }
}
