package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserRepository
import com.example.model.Friend
import com.example.model.FriendRequest
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val auth = Firebase.auth
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val searchResults: StateFlow<List<UserProfile>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.trim().length >= 2) {
                userRepository.searchUsers(query).catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val friends: StateFlow<List<Friend>> = if (currentUserId.isNotEmpty()) {
        userRepository.observeFriends(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else {
        MutableStateFlow(emptyList())
    }

    val incomingRequests: StateFlow<List<FriendRequest>> = if (currentUserId.isNotEmpty()) {
        userRepository.observeIncomingRequests(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else {
        MutableStateFlow(emptyList())
    }

    val outgoingRequests: StateFlow<List<FriendRequest>> = if (currentUserId.isNotEmpty()) {
        userRepository.observeOutgoingRequests(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else {
        MutableStateFlow(emptyList())
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun sendFriendRequest(
        senderUsername: String,
        receiverId: String,
        receiverUsername: String
    ) {
        viewModelScope.launch {
            val result = userRepository.sendFriendRequest(
                senderUsername = senderUsername,
                receiverId = receiverId,
                receiverUsername = receiverUsername
            )
            result.onSuccess {
                _actionMessage.value = "Friend request sent to @$receiverUsername"
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to send request"
            }
        }
    }

    fun respondToRequest(request: FriendRequest, accept: Boolean) {
        viewModelScope.launch {
            val result = userRepository.respondToFriendRequest(request, accept)
            result.onSuccess {
                _actionMessage.value = if (accept) {
                    "Accepted friend request from @${request.senderUsername}"
                } else {
                    "Declined friend request"
                }
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to respond"
            }
        }
    }
}
