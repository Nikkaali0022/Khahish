package com.example.ui.call

import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CallSession
import com.example.viewmodel.CallViewModel

@Composable
fun CallScreen(
    callSession: CallSession,
    currentUserId: String,
    callViewModel: CallViewModel,
    onCallEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        callViewModel.endCall()
        onCallEnded()
    }

    val isJoined by callViewModel.agoraManager.isJoined.collectAsState()
    val remoteUid by callViewModel.agoraManager.remoteUid.collectAsState()
    val isMuted by callViewModel.agoraManager.isMuted.collectAsState()
    val isVideoEnabled by callViewModel.agoraManager.isVideoEnabled.collectAsState()
    val isSpeakerOn by callViewModel.agoraManager.isSpeakerOn.collectAsState()
    val connectionStatus by callViewModel.agoraManager.connectionStatus.collectAsState()

    val isVideoCall = callSession.callType == "video"
    val isCaller = callSession.callerId == currentUserId
    val partnerName = if (isCaller) callSession.receiverName else callSession.callerName

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Video feeds if Video Call and Remote User is connected
        if (isVideoCall) {
            if (remoteUid != null) {
                // Remote Full Screen Video
                AndroidView(
                    factory = { context ->
                        SurfaceView(context).apply {
                            callViewModel.setupRemoteVideo(this, remoteUid!!)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Local PIP Preview
            if (isVideoEnabled) {
                Card(
                    modifier = Modifier
                        .size(width = 110.dp, height = 160.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    AndroidView(
                        factory = { context ->
                            SurfaceView(context).apply {
                                setZOrderMediaOverlay(true)
                                callViewModel.setupLocalVideo(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Overlay with Partner Details & Avatar if audio or remote not ready
        if (!isVideoCall || remoteUid == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 90.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Pulsing outer ripple
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.25f))
                    )
                    // Avatar center
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = partnerName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isJoined) connectionStatus else "Agora Testing Mode: Ringing...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isVideoCall) "Agora HD Video Call (Token Bypassed)" else "Agora HD Voice Call (Token Bypassed)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF38BDF8)
                )
            }
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 20.dp, end = 20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Audio Button
                    IconButton(
                        onClick = { callViewModel.toggleMute() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isMuted) Color(0xFFEF4444) else Color(0xFF334155),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("toggle_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute"
                        )
                    }

                    // Speakerphone Button
                    IconButton(
                        onClick = { callViewModel.toggleSpeaker() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isSpeakerOn) Color(0xFF6366F1) else Color(0xFF334155),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("toggle_speaker_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker"
                        )
                    }

                    // Video Toggle Button (for video calls)
                    if (isVideoCall) {
                        IconButton(
                            onClick = { callViewModel.toggleVideo() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isVideoEnabled) Color(0xFF6366F1) else Color(0xFF334155),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("toggle_video_button")
                        ) {
                            Icon(
                                imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Video"
                            )
                        }

                        // Switch Camera Button
                        IconButton(
                            onClick = { callViewModel.switchCamera() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF334155),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("switch_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera"
                            )
                        }
                    }

                    // End Call Button
                    IconButton(
                        onClick = {
                            callViewModel.endCall()
                            onCallEnded()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(58.dp)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}
