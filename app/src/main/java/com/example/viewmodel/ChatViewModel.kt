package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.model.ChatRoom
import com.example.model.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val auth = Firebase.auth
    private val currentUserId = auth.currentUser?.uid ?: ""

    val userChats: StateFlow<List<ChatRoom>> = if (currentUserId.isNotEmpty()) {
        chatRepository.observeUserChats(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else {
        MutableStateFlow(emptyList())
    }

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    val currentChatMessages: StateFlow<List<Message>> = _activeChatId.flatMapLatest { chatId ->
        if (chatId != null) {
            chatRepository.observeMessages(chatId).catch { emit(emptyList()) }
        } else {
            emptyFlow()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectChat(chatId: String) {
        _activeChatId.value = chatId
    }

    fun clearActiveChat() {
        _activeChatId.value = null
    }

    fun openOrCreateChatWithUser(otherUserId: String, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.getOrCreateChat(otherUserId)
            result.onSuccess { chatId ->
                _activeChatId.value = chatId
                onOpened(chatId)
            }
        }
    }

    fun sendMessage(chatId: String, text: String, senderName: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                text = text,
                senderName = senderName
            )
        }
    }
}
