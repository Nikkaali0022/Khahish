package com.example.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CallSession

@Composable
fun IncomingCallDialog(
    callSession: CallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = callSession.callerName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (callSession.callType == "video") Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Incoming ${callSession.callType.replaceFirstChar { it.uppercase() }} Call",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Text(
                    text = "Agora RTC testing mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onDecline,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("decline_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Decline", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelMedium)
                    }

                    // Accept Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onAccept,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("accept_call_button")
                        ) {
                            Icon(
                                imageVector = if (callSession.callType == "video") Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Accept",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Accept", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
