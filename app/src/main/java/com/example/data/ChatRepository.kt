package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ChatRoom
import com.example.model.Message
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserChats(userId: String): Flow<List<ChatRoom>> = flow {
        val path = "chats"
        emitAll(
            db.collection("chats")
                .whereArrayContains("participants", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ChatRoom::class.java).sortedByDescending {
                        it.lastMessageTimestamp ?: it.createdAt
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getOrCreateChat(otherUserId: String): Result<String> = runCatching {
        val currentUid = requireUserId()
        val participants = listOf(currentUid, otherUserId).sorted()
        val chatId = "chat_${participants[0]}_${participants[1]}"
        val chatRef = db.collection("chats").document(chatId)

        val existingChats = db.collection("chats")
            .whereArrayContains("participants", currentUid)
            .get().await()
            .toObjects(ChatRoom::class.java)

        val alreadyExists = existingChats.any { it.chatId == chatId }
        if (!alreadyExists) {
            val payload = mapOf(
                "chatId" to chatId,
                "participants" to participants,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            chatRef.set(payload).await()
        }
        chatId
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.WRITE, "chats")
    }

    fun observeMessages(chatId: String): Flow<List<Message>> = flow {
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Message::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderName: String
    ): Result<String> = runCatching {
        val senderId = requireUserId()
        val messageId = "msg_${UUID.randomUUID()}"
        val messageRef = db.collection("chats").document(chatId).collection("messages").document(messageId)

        val msgPayload = mapOf(
            "messageId" to messageId,
            "chatId" to chatId,
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text.trim(),
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        messageRef.set(msgPayload).await()

        // Update chat room last message
        val chatRef = db.collection("chats").document(chatId)
        chatRef.update(
            mapOf(
                "lastMessage" to text.trim(),
                "lastMessageSenderId" to senderId,
                "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        messageId
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.CREATE, "chats/$chatId/messages")
    }

    suspend fun markMessageAsRead(chatId: String, messageId: String): Result<Unit> = runCatching {
        val ref = db.collection("chats").document(chatId).collection("messages").document(messageId)
        ref.update("isRead", true).await()
        Unit
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
    }
}
