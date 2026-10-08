package com.example.ui.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPhoneAuthDialog by remember { mutableStateOf(false) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.surface
        )
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header & Branding
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Khahish Logo",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Khahish",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Connect deeply. Message in real-time, talk freely with Agora HD audio & video.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Features Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Default.Chat,
                            title = "Real-Time 1-on-1 Chat",
                            subtitle = "Instant messaging powered by Firebase Firestore"
                        )
                        FeatureRow(
                            icon = Icons.Default.Search,
                            title = "Search & Friend Requests",
                            subtitle = "Find users by unique username and add contacts"
                        )
                        FeatureRow(
                            icon = Icons.Default.Videocam,
                            title = "Agora Voice & Video Calling",
                            subtitle = "Zero-token testing mode for crystal clear calls"
                        )
                    }
                }

                // Auth Action Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google Sign-In Button (Mandatory Interactive Invariant)
                    Button(
                        onClick = { authViewModel.signInWithGoogle(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Phone Auth Information Button
                    OutlinedButton(
                        onClick = { showPhoneAuthDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("phone_auth_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Phone Auth / Setup Details",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Text(
                        text = "Zero-setup Agora Testing Mode enabled (App ID: 6a5791...)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showPhoneAuthDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPhoneAuthDialog = false },
            title = { Text("Authentication Architecture") },
            text = {
                Text(
                    "Google Sign-In is active as the provisioned primary authenticator with Credential Manager.\n\n" +
                    "Phone Auth is ready to connect with your Firebase SMS provider in the Firebase Console under Authentication > Sign-in method > Phone."
                )
            },
            confirmButton = {
                Button(onClick = { showPhoneAuthDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
