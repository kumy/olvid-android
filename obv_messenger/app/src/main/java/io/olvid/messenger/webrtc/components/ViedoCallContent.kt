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

package io.olvid.messenger.webrtc.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.olvid.messenger.R
import io.olvid.messenger.webrtc.WebrtcCallService
import io.olvid.messenger.webrtc.WebrtcCallService.CallParticipantPojo
import io.olvid.messenger.webrtc.WebrtcPeerConnectionHolder.Companion.localScreenTrack
import io.olvid.messenger.webrtc.WebrtcPeerConnectionHolder.Companion.localVideoTrack

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun VideoCallContent(
    participants: List<CallParticipantPojo>,
    webrtcCallService: WebrtcCallService,
    peekHeight: Dp,
    onCallAction: (CallAction) -> Unit,
    isPip: Boolean = false,
    pipAspectCallback: ((Context, Int, Int) -> Unit)? = null
) {
    val selectedCamera by webrtcCallService.selectedCameraLiveData.observeAsState()
    val speakingColor = colorResource(id = R.color.olvid_gradient_light)
    val notSpeakingColor = Color(0xFF29282D)
    val borderColorOwned by animateColorAsState(
        if ((webrtcCallService.getAudioLevel(webrtcCallService.bytesOwnedIdentity)
                ?: 0.0) > 0.1
        ) speakingColor else notSpeakingColor,
        label = "borderColorOwned",
        animationSpec = tween(durationMillis = 1000, easing = EaseOutExpo)
    )

    if (participants.size == 1) {
        // 1to1
        val remoteVideoTrack =
            webrtcCallService.getCallParticipant(participants.firstOrNull()?.bytesContactIdentity)?.peerConnectionHolder?.remoteVideoTrack
        val remoteScreenTrack =
            webrtcCallService.getCallParticipant(participants.firstOrNull()?.bytesContactIdentity)?.peerConnectionHolder?.remoteScreenTrack
        if (webrtcCallService.bytesOwnedIdentity == null || webrtcCallService.selectedParticipant.contentEquals(
                webrtcCallService.bytesOwnedIdentity
            )
                .not()
        ) {
            CallParticipant(
                callParticipant = participants.firstOrNull(),
                videoTrack = remoteVideoTrack,
                screenTrack = remoteScreenTrack,
                peekHeight = peekHeight,
                modifier = Modifier.fillMaxSize(),
                zoomable = true,
                audioLevel = webrtcCallService.getAudioLevel(participants.firstOrNull()?.bytesContactIdentity),
                isPip = isPip,
                pipAspectCallback = pipAspectCallback
            )
            AnimatedVisibility(!isPip) {
                // small preview of (usually) your own video at the top left
                Card(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start))
                        .padding(start = 10.dp, top = 10.dp)
                        .clickable {
                            webrtcCallService.selectedParticipant =
                                webrtcCallService.bytesOwnedIdentity
                        },
                    border = BorderStroke(
                        width = 2.dp,
                        color = borderColorOwned
                    ),
                    colors = CardDefaults.cardColors(
                      containerColor = colorResource(R.color.newDialogBackground),
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    CallParticipant(
                        modifier = Modifier.sizeIn(maxWidth = 120.dp, maxHeight = 120.dp),
                        bytesOwnedIdentity = webrtcCallService.bytesOwnedIdentity,
                        mirror = selectedCamera?.mirror == true,
                        videoTrack = localVideoTrack.takeIf { webrtcCallService.cameraEnabled },
                        audioLevel = webrtcCallService.getAudioLevel(webrtcCallService.bytesOwnedIdentity)
                    )
                }
            }
        } else {
            if (webrtcCallService.screenShareActive) {
                ScreenSharingNoticeScreen { webrtcCallService.toggleScreenShare() }
            } else {
                CallParticipant(
                    bytesOwnedIdentity = webrtcCallService.bytesOwnedIdentity,
                    mirror = selectedCamera?.mirror == true,
                    videoTrack = localVideoTrack.takeIf { webrtcCallService.cameraEnabled },
                    screenTrack = localScreenTrack.takeIf { webrtcCallService.screenShareActive },
                    peekHeight = peekHeight,
                    zoomable = true,
                    modifier = Modifier.fillMaxSize(),
                    audioLevel = webrtcCallService.getAudioLevel(webrtcCallService.bytesOwnedIdentity),
                    isPip = isPip,
                    fitVideo = true
                )
            }
            AnimatedVisibility(!isPip) {
                Card(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start))
                        .padding(start = 10.dp, top = 10.dp)
                        .clickable {
                            webrtcCallService.selectedParticipant =
                                participants.firstOrNull()?.bytesContactIdentity!!
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.newDialogBackground),
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    CallParticipant(
                        modifier = Modifier.sizeIn(maxWidth = 120.dp, maxHeight = 120.dp),
                        callParticipant = participants.firstOrNull(),
                        videoTrack = remoteVideoTrack,
                        screenTrack = remoteScreenTrack,
                        audioLevel = webrtcCallService.getAudioLevel(participants.firstOrNull()?.bytesContactIdentity),
                        pipAspectCallback = pipAspectCallback
                    )
                }
            }
        }
    } else if (participants.size == 2) {
        val borderColorFirst by animateColorAsState(
            if ((webrtcCallService.getAudioLevel(participants.first().bytesContactIdentity)
                    ?: 0.0) > 0.1
            ) speakingColor else notSpeakingColor,
            label = "borderColor",
            animationSpec = tween(durationMillis = 1000, easing = EaseOutExpo)
        )
        val borderColorSecond by animateColorAsState(
            if ((webrtcCallService.getAudioLevel(participants[1].bytesContactIdentity)
                    ?: 0.0) > 0.1
            ) speakingColor else notSpeakingColor,
            label = "borderColor",
            animationSpec = tween(durationMillis = 1000, easing = EaseOutExpo)
        )
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            pipAspectCallback?.invoke(context, 9, 16)
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPip) 0.dp else peekHeight)
        ) {
            val boxWithConstraintsScope = this
            FlowRow(
                modifier = Modifier
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.SpaceBetween,
                maxItemsInEachRow = if (maxWidth > maxHeight) 2 else 1
            ) {
                Card(
                    modifier = Modifier
                        .then(
                            if (boxWithConstraintsScope.maxWidth > boxWithConstraintsScope.maxHeight)
                                Modifier
                                    .fillMaxHeight()
                                    .width(boxWithConstraintsScope.maxWidth / 2 - 6.dp)
                            else
                                Modifier
                                    .fillMaxWidth()
                                    .height(boxWithConstraintsScope.maxHeight / 2 - 6.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.newDialogBackground),
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = borderColorFirst,
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    val remoteVideoTrack =
                        webrtcCallService.getCallParticipant(participants.first().bytesContactIdentity)?.peerConnectionHolder?.remoteVideoTrack
                    val remoteScreenTrack =
                        webrtcCallService.getCallParticipant(participants.first().bytesContactIdentity)?.peerConnectionHolder?.remoteScreenTrack
                    CallParticipant(
                        videoTrack = remoteVideoTrack,
                        screenTrack = remoteScreenTrack,
                        callParticipant = participants.first(),
                        zoomable = true,
                        audioLevel = webrtcCallService.getAudioLevel(participants.first().bytesContactIdentity),
                        isPip = isPip
                    )
                }
                Card(
                    modifier = Modifier
                        .then(
                            if (boxWithConstraintsScope.maxWidth > boxWithConstraintsScope.maxHeight)
                                Modifier
                                    .fillMaxHeight()
                                    .width(boxWithConstraintsScope.maxWidth / 2 - 6.dp)
                            else
                                Modifier
                                    .fillMaxWidth()
                                    .height(boxWithConstraintsScope.maxHeight / 2 - 6.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.newDialogBackground),
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = borderColorSecond,
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    val remoteVideoTrack =
                        webrtcCallService.getCallParticipant(participants[1].bytesContactIdentity)?.peerConnectionHolder?.remoteVideoTrack
                    val remoteScreenTrack =
                        webrtcCallService.getCallParticipant(participants[1].bytesContactIdentity)?.peerConnectionHolder?.remoteScreenTrack
                    CallParticipant(
                        videoTrack = remoteVideoTrack,
                        screenTrack = remoteScreenTrack,
                        callParticipant = participants[1],
                        zoomable = true,
                        audioLevel = webrtcCallService.getAudioLevel(participants[1].bytesContactIdentity),
                        isPip = isPip
                    )
                }
            }
        }
        AnimatedVisibility(!isPip) {
            Card(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start))
                    .padding(start = 10.dp, top = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.newDialogBackground),
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = borderColorOwned,
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                CallParticipant(
                    modifier = Modifier.sizeIn(maxWidth = 120.dp, maxHeight = 120.dp),
                    bytesOwnedIdentity = webrtcCallService.bytesOwnedIdentity,
                    mirror = selectedCamera?.mirror == true,
                    videoTrack = localVideoTrack.takeIf { webrtcCallService.cameraEnabled },
                    audioLevel = webrtcCallService.getAudioLevel(webrtcCallService.bytesOwnedIdentity)
                )
            }
        }
    } else if (participants.size > 2) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            pipAspectCallback?.invoke(context, 9, 16)
        }
        AudioCallContent(
            participants = participants,
            webrtcCallService = webrtcCallService,
            onCallAction = onCallAction,
            isPip = isPip
        )
        //MultiVideoCallContent(participants = participants, webrtcCallService = webrtcCallService, peekHeight = peekHeight)
    }
}