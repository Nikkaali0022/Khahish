package com.example.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Friend
import com.example.model.FriendRequest
import com.example.model.UserProfile
import com.example.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    currentUserId: String,
    currentUsername: String,
    contactsViewModel: ContactsViewModel,
    onStartChat: (friendId: String) -> Unit,
    onStartVoiceCall: (friendId: String, friendName: String) -> Unit,
    onStartVideoCall: (friendId: String, friendName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by contactsViewModel.searchQuery.collectAsState()
    val searchResults by contactsViewModel.searchResults.collectAsState()
    val friends by contactsViewModel.friends.collectAsState()
    val incomingRequests by contactsViewModel.incomingRequests.collectAsState()
    val outgoingRequests by contactsViewModel.outgoingRequests.collectAsState()
    val actionMessage by contactsViewModel.actionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            contactsViewModel.clearActionMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Contacts & Search",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { contactsViewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search users by @username...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { contactsViewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_search_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Results Section
                if (searchQuery.trim().length >= 2) {
                    item {
                        Text(
                            text = "Search Results (${searchResults.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (searchResults.isEmpty()) {
                        item {
                            Text(
                                text = "No users found with username '@$searchQuery'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(searchResults, key = { it.userId }) { user ->
                            val isFriend = friends.any { it.friendId == user.userId }
                            val hasOutgoing = outgoingRequests.any { it.receiverId == user.userId && it.status == "pending" }
                            UserSearchItem(
                                user = user,
                                isFriend = isFriend,
                                isPending = hasOutgoing,
                                onSendRequest = {
                                    contactsViewModel.sendFriendRequest(
                                        senderUsername = currentUsername,
                                        receiverId = user.userId,
                                        receiverUsername = user.username
                                    )
                                },
                                onMessageClick = { onStartChat(user.userId) }
                            )
                        }
                    }
                }

                // Incoming Requests Section
                if (incomingRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = "Friend Requests (${incomingRequests.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(incomingRequests, key = { it.requestId }) { req ->
                        IncomingRequestCard(
                            request = req,
                            onAccept = { contactsViewModel.respondToRequest(req, accept = true) },
                            onDecline = { contactsViewModel.respondToRequest(req, accept = false) }
                        )
                    }
                }

                // Friends List Section
                item {
                    Text(
                        text = "My Friends (${friends.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (friends.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your contact list is empty",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Use the search bar above to find friends by username and send a request.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(friends, key = { it.friendId }) { friend ->
                        FriendItemCard(
                            friend = friend,
                            onMessageClick = { onStartChat(friend.friendId) },
                            onVoiceCallClick = { onStartVoiceCall(friend.friendId, friend.friendDisplayName) },
                            onVideoCallClick = { onStartVideoCall(friend.friendId, friend.friendDisplayName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSearchItem(
    user: UserProfile,
    isFriend: Boolean,
    isPending: Boolean,
    onSendRequest: () -> Unit,
    onMessageClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName.ifEmpty { "@${user.username}" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                isFriend -> {
                    IconButton(onClick = onMessageClick) {
                        Icon(imageVector = Icons.Default.Message, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                isPending -> {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                else -> {
                    Button(
                        onClick = onSendRequest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("send_request_${user.username}")
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${request.senderUsername}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Sent you a friend request",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("accept_request_${request.requestId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .testTag("decline_request_${request.requestId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Decline",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendItemCard(
    friend: Friend,
    onMessageClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMessageClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.friendDisplayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "@${friend.friendUsername}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onMessageClick, modifier = Modifier.testTag("msg_friend_${friend.friendUsername}")) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Message",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onVoiceCallClick, modifier = Modifier.testTag("voice_friend_${friend.friendUsername}")) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onVideoCallClick, modifier = Modifier.testTag("video_friend_${friend.friendUsername}")) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
