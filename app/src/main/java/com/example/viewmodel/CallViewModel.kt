package com.example.viewmodel

import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agora.AgoraManager
import com.example.data.CallRepository
import com.example.model.CallSession
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CallViewModel(
    private val callRepository: CallRepository,
    val agoraManager: AgoraManager
) : ViewModel() {

    private val auth = Firebase.auth
    private val currentUserId = auth.currentUser?.uid ?: ""

    val incomingCall: StateFlow<CallSession?> = if (currentUserId.isNotEmpty()) {
        callRepository.observeIncomingCalls(currentUserId)
            .catch { emit(null) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    } else {
        MutableStateFlow(null)
    }

    private val _activeCall = MutableStateFlow<CallSession?>(null)
    val activeCall: StateFlow<CallSession?> = _activeCall.asStateFlow()

    private var activeCallObserverJob: Job? = null

    fun startCall(
        callerName: String,
        receiverId: String,
        receiverName: String,
        callType: String
    ) {
        viewModelScope.launch {
            val result = callRepository.startCall(
                callerName = callerName,
                receiverId = receiverId,
                receiverName = receiverName,
                callType = callType
            )
            result.onSuccess { session ->
                _activeCall.value = session
                // Join Agora channel in testing mode (token bypassed)
                agoraManager.joinChannel(
                    channelId = session.channelId,
                    isVideo = (callType == "video")
                )
                observeActiveCallSession(session.callId)
            }
        }
    }

    fun acceptIncomingCall(session: CallSession) {
        viewModelScope.launch {
            callRepository.acceptCall(session.callId)
            _activeCall.value = session.copy(status = "accepted")
            // Join Agora channel in testing mode (token bypassed)
            agoraManager.joinChannel(
                channelId = session.channelId,
                isVideo = (session.callType == "video")
            )
            observeActiveCallSession(session.callId)
        }
    }

    fun rejectIncomingCall(session: CallSession) {
        viewModelScope.launch {
            callRepository.rejectCall(session.callId)
        }
    }

    fun endCall() {
        val current = _activeCall.value
        if (current != null) {
            viewModelScope.launch {
                callRepository.endCall(current.callId)
            }
        }
        agoraManager.leaveChannel()
        activeCallObserverJob?.cancel()
        activeCallObserverJob = null
        _activeCall.value = null
    }

    private fun observeActiveCallSession(callId: String) {
        activeCallObserverJob?.cancel()
        activeCallObserverJob = viewModelScope.launch {
            callRepository.observeCall(callId).collect { session ->
                if (session == null || session.status == "rejected" || session.status == "ended") {
                    agoraManager.leaveChannel()
                    _activeCall.value = null
                    activeCallObserverJob?.cancel()
                } else {
                    _activeCall.value = session
                }
            }
        }
    }

    fun setupLocalVideo(surfaceView: SurfaceView) {
        agoraManager.setupLocalVideo(surfaceView)
    }

    fun setupRemoteVideo(surfaceView: SurfaceView, uid: Int) {
        agoraManager.setupRemoteVideo(surfaceView, uid)
    }

    fun toggleMute() = agoraManager.toggleMute()
    fun toggleVideo() = agoraManager.toggleVideo()
    fun toggleSpeaker() = agoraManager.toggleSpeaker()
    fun switchCamera() = agoraManager.switchCamera()

    override fun onCleared() {
        super.onCleared()
        agoraManager.destroy()
    }
}
