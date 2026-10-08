package com.example.model

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "username" to username,
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "phoneNumber" to phoneNumber,
        "bio" to bio,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}

data class Friend(
    val friendId: String = "",
    val friendUsername: String = "",
    val friendDisplayName: String = "",
    val friendPhotoUrl: String? = null,
    val addedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "friendId" to friendId,
        "friendUsername" to friendUsername,
        "friendDisplayName" to friendDisplayName,
        "friendPhotoUrl" to friendPhotoUrl,
        "addedAt" to addedAt
    ).filterValues { it != null }
}

data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val receiverId: String = "",
    val receiverUsername: String = "",
    val status: String = "pending", // "pending", "accepted", "declined"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "requestId" to requestId,
        "senderId" to senderId,
        "senderUsername" to senderUsername,
        "receiverId" to receiverId,
        "receiverUsername" to receiverUsername,
        "status" to status,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}

data class ChatRoom(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageTimestamp: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "chatId" to chatId,
        "participants" to participants,
        "lastMessage" to lastMessage,
        "lastMessageSenderId" to lastMessageSenderId,
        "lastMessageTimestamp" to lastMessageTimestamp,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val isRead: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "messageId" to messageId,
        "chatId" to chatId,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "createdAt" to createdAt,
        "isRead" to isRead
    ).filterValues { it != null }
}

data class CallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val channelId: String = "",
    val callType: String = "voice", // "voice" or "video"
    val status: String = "ringing", // "ringing", "accepted", "rejected", "ended"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "callId" to callId,
        "callerId" to callerId,
        "callerName" to callerName,
        "receiverId" to receiverId,
        "receiverName" to receiverName,
        "channelId" to channelId,
        "callType" to callType,
        "status" to status,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}
