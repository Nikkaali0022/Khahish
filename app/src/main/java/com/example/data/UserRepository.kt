package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Friend
import com.example.model.FriendRequest
import com.example.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository(private val db: FirebaseFirestore) {

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

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getProfile(userId: String): Result<UserProfile?> = runCatching {
        val path = "users/$userId"
        val doc = db.collection("users").document(userId).get().await()
        if (doc.exists()) doc.toObject(UserProfile::class.java) else null
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, "users/$userId")
    }

    suspend fun saveProfile(
        username: String,
        displayName: String,
        photoUrl: String? = null,
        phoneNumber: String? = null,
        bio: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existingDoc = docRef.get().await()

        val cleanUsername = username.trim().lowercase()
        val cleanDisplayName = displayName.trim()

        if (existingDoc.exists()) {
            val payload = mapOf(
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "photoUrl" to photoUrl,
                "phoneNumber" to phoneNumber,
                "bio" to bio,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.update(payload).await()
        } else {
            val payload = mapOf(
                "userId" to uid,
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "photoUrl" to photoUrl,
                "phoneNumber" to phoneNumber,
                "bio" to bio,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
        }
        Unit
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.WRITE, "users/${auth.currentUser?.uid}")
    }

    fun searchUsers(query: String): Flow<List<UserProfile>> = flow {
        val currentUid = requireUserId()
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val path = "users"
        val queryRef = db.collection("users")
            .whereGreaterThanOrEqualTo("username", cleanQuery)
            .whereLessThanOrEqualTo("username", cleanQuery + "\uf8ff")
            .limit(20)

        emitAll(
            queryRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java).filter { it.userId != currentUid }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeFriends(userId: String): Flow<List<Friend>> = flow {
        val path = "users/$userId/friends"
        emitAll(
            db.collection("users").document(userId).collection("friends")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Friend::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> = flow {
        val path = "friendRequests"
        emitAll(
            db.collection("friendRequests")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FriendRequest::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeOutgoingRequests(userId: String): Flow<List<FriendRequest>> = flow {
        val path = "friendRequests"
        emitAll(
            db.collection("friendRequests")
                .whereEqualTo("senderId", userId)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FriendRequest::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun sendFriendRequest(
        senderUsername: String,
        receiverId: String,
        receiverUsername: String
    ): Result<String> = runCatching {
        val senderId = requireUserId()
        if (senderId == receiverId) {
            error("Cannot send friend request to yourself")
        }
        val reqId = "req_${UUID.randomUUID()}"
        val docRef = db.collection("friendRequests").document(reqId)
        val payload = mapOf(
            "requestId" to reqId,
            "senderId" to senderId,
            "senderUsername" to senderUsername,
            "receiverId" to receiverId,
            "receiverUsername" to receiverUsername,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        reqId
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.CREATE, "friendRequests")
    }

    suspend fun respondToFriendRequest(
        request: FriendRequest,
        accept: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("friendRequests").document(request.requestId)
        val newStatus = if (accept) "accepted" else "declined"

        docRef.update(
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        if (accept) {
            // Add to both users' friends collections
            val senderProfile = db.collection("users").document(request.senderId).get().await()
                .toObject(UserProfile::class.java)
            val receiverProfile = db.collection("users").document(request.receiverId).get().await()
                .toObject(UserProfile::class.java)

            // Add receiver to sender's friends list
            db.collection("users").document(request.senderId)
                .collection("friends").document(request.receiverId)
                .set(
                    mapOf(
                        "friendId" to request.receiverId,
                        "friendUsername" to request.receiverUsername,
                        "friendDisplayName" to (receiverProfile?.displayName ?: request.receiverUsername),
                        "friendPhotoUrl" to receiverProfile?.photoUrl,
                        "addedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                ).await()

            // Add sender to receiver's friends list
            db.collection("users").document(request.receiverId)
                .collection("friends").document(request.senderId)
                .set(
                    mapOf(
                        "friendId" to request.senderId,
                        "friendUsername" to request.senderUsername,
                        "friendDisplayName" to (senderProfile?.displayName ?: request.senderUsername),
                        "friendPhotoUrl" to senderProfile?.photoUrl,
                        "addedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                ).await()
        }
        Unit
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.UPDATE, "friendRequests/${request.requestId}")
    }
}
