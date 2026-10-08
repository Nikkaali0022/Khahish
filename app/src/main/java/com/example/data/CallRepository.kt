package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CallSession
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CallRepository(private val db: FirebaseFirestore) {

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

    suspend fun startCall(
        callerName: String,
        receiverId: String,
        receiverName: String,
        callType: String
    ): Result<CallSession> = runCatching {
        val callerId = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val channelId = callId

        val callSession = CallSession(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            receiverId = receiverId,
            receiverName = receiverName,
            channelId = channelId,
            callType = callType,
            status = "ringing"
        )

        val payload = mapOf(
            "callId" to callId,
            "callerId" to callerId,
            "callerName" to callerName,
            "receiverId" to receiverId,
            "receiverName" to receiverName,
            "channelId" to channelId,
            "callType" to callType,
            "status" to "ringing",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("calls").document(callId).set(payload).await()
        callSession
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.CREATE, "calls")
    }

    fun observeIncomingCalls(userId: String): Flow<CallSession?> = flow {
        val path = "calls"
        emitAll(
            db.collection("calls")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "ringing")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CallSession::class.java).firstOrNull()
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeCall(callId: String): Flow<CallSession?> = flow {
        val path = "calls/$callId"
        emitAll(
            db.collection("calls").document(callId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) snapshot.toObject(CallSession::class.java) else null
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun acceptCall(callId: String): Result<Unit> = runCatching {
        db.collection("calls").document(callId).update(
            mapOf(
                "status" to "accepted",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.UPDATE, "calls/$callId")
    }

    suspend fun rejectCall(callId: String): Result<Unit> = runCatching {
        db.collection("calls").document(callId).update(
            mapOf(
                "status" to "rejected",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.UPDATE, "calls/$callId")
    }

    suspend fun endCall(callId: String): Result<Unit> = runCatching {
        db.collection("calls").document(callId).update(
            mapOf(
                "status" to "ended",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.UPDATE, "calls/$callId")
    }
}
