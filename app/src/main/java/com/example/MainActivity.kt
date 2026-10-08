package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.agora.AgoraManager
import com.example.data.CallRepository
import com.example.data.ChatRepository
import com.example.data.UserRepository
import com.example.model.CallSession
import com.example.ui.auth.AuthScreen
import com.example.ui.call.CallScreen
import com.example.ui.call.IncomingCallDialog
import com.example.ui.chat.ChatDetailScreen
import com.example.ui.chat.ChatListScreen
import com.example.ui.contacts.ContactsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CallViewModel
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.ContactsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Repositories initialized with named Firestore database ID
        val userRepository = UserRepository(applicationContext)
        val chatRepository = ChatRepository(applicationContext)
        val callRepository = CallRepository(applicationContext)
        val agoraManager = AgoraManager(applicationContext)

        val authViewModel = AuthViewModel(userRepository)

        setContent {
            MyApplicationTheme {
                KhahishApp(
                    authViewModel = authViewModel,
                    userRepository = userRepository,
                    chatRepository = chatRepository,
                    callRepository = callRepository,
                    agoraManager = agoraManager
                )
            }
        }
    }
}

@Composable
fun KhahishApp(
    authViewModel: AuthViewModel,
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    callRepository: CallRepository,
    agoraManager: AgoraManager
) {
    val context = LocalContext.current
    val authState by authViewModel.uiState.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    // Request Audio & Camera permissions for Agora RTC
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions response
    }

    LaunchedEffect(Unit) {
        authViewModel.attemptAutoSignIn(context)
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        )
    }

    val currentUser = Firebase.auth.currentUser

    if (currentUser == null) {
        AuthScreen(authViewModel = authViewModel)
    } else {
        // Authenticated Session: Instantiate feature ViewModels
        val chatViewModel = remember(currentUser.uid) { ChatViewModel(chatRepository) }
        val contactsViewModel = remember(currentUser.uid) { ContactsViewModel(userRepository) }
        val callViewModel = remember(currentUser.uid) { CallViewModel(callRepository, agoraManager) }

        val incomingCall by callViewModel.incomingCall.collectAsState()
        val activeCall by callViewModel.activeCall.collectAsState()

        var selectedTab by remember { mutableIntStateOf(0) }
        var activeChatId by remember { mutableStateOf<String?>(null) }

        // Active Full-Screen Call
        if (activeCall != null) {
            CallScreen(
                callSession = activeCall!!,
                currentUserId = currentUser.uid,
                callViewModel = callViewModel,
                onCallEnded = { /* Handled in CallViewModel */ }
            )
        } else {
            // Incoming Call Alert Dialog
            if (incomingCall != null) {
                IncomingCallDialog(
                    callSession = incomingCall!!,
                    onAccept = { callViewModel.acceptIncomingCall(incomingCall!!) },
                    onDecline = { callViewModel.rejectIncomingCall(incomingCall!!) }
                )
            }

            if (activeChatId != null) {
                ChatDetailScreen(
                    chatId = activeChatId!!,
                    currentUserId = currentUser.uid,
                    currentUserName = userProfile?.displayName ?: currentUser.displayName ?: "User",
                    chatViewModel = chatViewModel,
                    onBack = { activeChatId = null },
                    onStartVoiceCall = { partnerId, partnerName ->
                        callViewModel.startCall(
                            callerName = userProfile?.displayName ?: "User",
                            receiverId = partnerId,
                            receiverName = partnerName,
                            callType = "voice"
                        )
                    },
                    onStartVideoCall = { partnerId, partnerName ->
                        callViewModel.startCall(
                            callerName = userProfile?.displayName ?: "User",
                            receiverId = partnerId,
                            receiverName = partnerName,
                            callType = "video"
                        )
                    }
                )
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                                label = { Text("Chats") },
                                modifier = Modifier.testTag("nav_tab_chats")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.People, contentDescription = "Contacts") },
                                label = { Text("Contacts") },
                                modifier = Modifier.testTag("nav_tab_contacts")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") },
                                modifier = Modifier.testTag("nav_tab_profile")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> ChatListScreen(
                            chatViewModel = chatViewModel,
                            currentUserId = currentUser.uid,
                            onOpenChat = { chatId -> activeChatId = chatId },
                            onNewChatClicked = { selectedTab = 1 },
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> ContactsScreen(
                            currentUserId = currentUser.uid,
                            currentUsername = userProfile?.username ?: "",
                            contactsViewModel = contactsViewModel,
                            onStartChat = { friendId ->
                                chatViewModel.openOrCreateChatWithUser(friendId) { chatId ->
                                    activeChatId = chatId
                                }
                            },
                            onStartVoiceCall = { friendId, friendName ->
                                callViewModel.startCall(
                                    callerName = userProfile?.displayName ?: "User",
                                    receiverId = friendId,
                                    receiverName = friendName,
                                    callType = "voice"
                                )
                            },
                            onStartVideoCall = { friendId, friendName ->
                                callViewModel.startCall(
                                    callerName = userProfile?.displayName ?: "User",
                                    receiverId = friendId,
                                    receiverName = friendName,
                                    callType = "video"
                                )
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        2 -> ProfileScreen(
                            userProfile = userProfile,
                            authViewModel = authViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
