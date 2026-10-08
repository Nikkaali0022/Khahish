package com.example.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.UserProfile
import com.example.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var username by remember(userProfile) { mutableStateOf(userProfile?.username ?: "") }
    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var bio by remember(userProfile) { mutableStateOf(userProfile?.bio ?: "") }
    var phoneNumber by remember(userProfile) { mutableStateOf(userProfile?.phoneNumber ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Avatar and Display Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = displayName.ifEmpty { "Khahish User" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "@${username.ifEmpty { "username" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "UID: ${userProfile?.userId?.take(8) ?: "..."}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Agora RTC Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Agora RTC Call Engine",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mode: Testing Mode (Zero Token Bypass)\nApp ID: 6a5791178d1b47f499fee84c8d7a0beb\nVoice & Video Calling: Enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Edit Fields
                Text(
                    text = "Edit Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().trim() },
                    label = { Text("Username (for search)") },
                    prefix = { Text("@") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_username_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Status") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bio_field"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_phone_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        isSaving = true
                        authViewModel.saveUserProfile(
                            username = username,
                            displayName = displayName,
                            bio = bio,
                            phoneNumber = phoneNumber,
                            onSuccess = {
                                isSaving = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Profile updated successfully")
                                }
                            },
                            onError = { err ->
                                isSaving = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Error: $err")
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile Changes")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedButton(
                    onClick = { authViewModel.signOut(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
