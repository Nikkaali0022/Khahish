package com.example.agora

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgoraManager(private val context: Context) {

    companion object {
        private const val TAG = "AgoraManager"
        // Agora App ID configured in Testing Mode (Bypasses token verification)
        const val AGORA_APP_ID = "6a5791178d1b47f499fee84c8d7a0beb"
    }

    private var rtcEngine: RtcEngine? = null

    private val _isJoined = MutableStateFlow(false)
    val isJoined: StateFlow<Boolean> = _isJoined.asStateFlow()

    private val _remoteUid = MutableStateFlow<Int?>(null)
    val remoteUid: StateFlow<Int?> = _remoteUid.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Connecting...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Joined channel: $channel with uid: $uid in testing mode (token bypassed)")
            _isJoined.value = true
            _connectionStatus.value = "Connected"
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "Remote user joined: $uid")
            _remoteUid.value = uid
            _connectionStatus.value = "Connected to remote user"
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "Remote user left: $uid reason: $reason")
            if (_remoteUid.value == uid) {
                _remoteUid.value = null
                _connectionStatus.value = "Remote user disconnected"
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            Log.d(TAG, "Left Agora channel")
            _isJoined.value = false
            _remoteUid.value = null
            _connectionStatus.value = "Call Ended"
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora error: $err")
            _connectionStatus.value = "Connection Error ($err)"
        }
    }

    fun initEngine(): Boolean {
        if (rtcEngine != null) return true
        return try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = AGORA_APP_ID
                mEventHandler = rtcEventHandler
                mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            }
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.enableAudio()
            rtcEngine?.setEnableSpeakerphone(true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora RTC engine: ${e.message}", e)
            _connectionStatus.value = "Failed to initialize engine"
            false
        }
    }

    /**
     * Joins Agora channel in Testing Mode.
     * Tokens are completely bypassed by passing "" (empty string) as the token argument.
     */
    fun joinChannel(channelId: String, isVideo: Boolean) {
        initEngine()
        val engine = rtcEngine ?: return

        _isVideoEnabled.value = isVideo
        _isMuted.value = false
        _isSpeakerOn.value = true
        _connectionStatus.value = "Joining room $channelId..."

        if (isVideo) {
            engine.enableVideo()
            engine.startPreview()
        } else {
            engine.disableVideo()
        }

        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            publishMicrophoneTrack = true
            publishCameraTrack = isVideo
            autoSubscribeAudio = true
            autoSubscribeVideo = isVideo
        }

        // AGORA TESTING MODE: Token bypassed using empty string ""
        val result = engine.joinChannel("", channelId, 0, options)
        Log.d(TAG, "joinChannel result: $result for channel: $channelId (token bypassed)")
    }

    fun setupLocalVideo(surfaceView: SurfaceView) {
        rtcEngine?.let { engine ->
            engine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            engine.startPreview()
        }
    }

    fun setupRemoteVideo(surfaceView: SurfaceView, uid: Int) {
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        rtcEngine?.muteLocalAudioStream(newMute)
        _isMuted.value = newMute
    }

    fun toggleVideo() {
        val newVideo = !_isVideoEnabled.value
        rtcEngine?.muteLocalVideoStream(!newVideo)
        if (newVideo) {
            rtcEngine?.enableVideo()
            rtcEngine?.startPreview()
        } else {
            rtcEngine?.disableVideo()
            rtcEngine?.stopPreview()
        }
        _isVideoEnabled.value = newVideo
    }

    fun toggleSpeaker() {
        val newSpeaker = !_isSpeakerOn.value
        rtcEngine?.setEnableSpeakerphone(newSpeaker)
        _isSpeakerOn.value = newSpeaker
    }

    fun switchCamera() {
        rtcEngine?.switchCamera()
    }

    fun leaveChannel() {
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        _isJoined.value = false
        _remoteUid.value = null
    }

    fun destroy() {
        leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}
