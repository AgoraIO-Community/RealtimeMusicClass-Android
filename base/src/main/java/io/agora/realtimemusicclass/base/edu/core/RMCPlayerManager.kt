package io.agora.realtimemusicclass.base.edu.core

import io.agora.mediaplayer.IMediaPlayer
import io.agora.mediaplayer.IMediaPlayerObserver
import io.agora.mediaplayer.Constants.*
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole
import io.agora.rtc2.*

class RMCPlayerManager(private val core: RMCCore,
                       private val playerUid: Int = 1)
    : IRtcEngineEventHandler(), IMediaPlayerObserver {
    private val tag = "RMCPlayerManager"

    private var player: IMediaPlayer? = null
    private var connection: RtcConnection? = null
    private var listener: RMCPlayerListener? = null
    private var rtcEventListenerForPlayer: RMCRtcEventListenerWithHighPriority? =
        object : RMCRtcEventListenerWithHighPriority() {
            override fun onUserJoined(uid: Int, elapsed: Int) {
                if (core.user().localUser()!!.isTeacher() || core.user().localUser()!!.isStudent()) {
                    if (uid == playerUid) {
                        core.audio().muteRemoteAudio(true, playerUid)
                    }
                }
            }
        }

    fun join(channelName:String, role: RMCUserRole,
             listener: RMCPlayerListener) {
        this.listener = listener

        if (role == RMCUserRole.ROLE_TYPE_AUDIENCE ||
                role == RMCUserRole.ROLE_TYPE_UNKNOWN) {
            // just fake join success
            listener.onJoinChannelSuccess(channelName, 0, 0)
        } else if (role == RMCUserRole.ROLE_TYPE_TEACHER ||
                role == RMCUserRole.ROLE_TYPE_STUDENT) {
            (core.engine().rtcEngine() as? RtcEngineEx)?.let { engine ->
                player = engine.createMediaPlayer()
                player?.registerPlayerObserver(listener)
                if (role == RMCUserRole.ROLE_TYPE_TEACHER) {
                    createAndJoin(engine, channelName, listener)
                } else {
                    listener.onJoinChannelSuccess(channelName, 0, 0)
                }
            }
        }
        registerRtcEventListener()
    }

    private fun registerRtcEventListener() {
        core.registerRtcEventListener(rtcEventListenerForPlayer)
    }

    private fun removeRtcEventListener() {
        core.removeRtcEventListener(rtcEventListenerForPlayer)
    }

    @Synchronized
    private fun createAndJoin(engine: RtcEngineEx, channelName: String,
                              listener: RMCPlayerListener) {
        connection = RtcConnection(channelName, playerUid)
        val option = ChannelMediaOptions()
        option.enableAudioRecordingOrPlayout = false
        option.publishMediaPlayerAudioTrack = true
        option.publishMediaPlayerId = player!!.mediaPlayerId
        option.autoSubscribeAudio = false
        option.autoSubscribeVideo = false
        option.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
        option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        engine.joinChannelEx(null, connection, option, listener)
    }

    fun leave() {
        synchronized(this) {
            player?.destroy()
            connection?.let {
                (core.engine().rtcEngine() as? RtcEngineEx)?.let { engine ->
                    engine.leaveChannelEx(it)
                    connection = null
                }
            }
        }
        removeRtcEventListener()
    }

    fun open(path : String, startPos: Long) {
        player?.open(path, startPos)
    }

    fun play() {
        player?.play();
    }

    fun seek(pos: Long) {
        player?.seek(pos)
    }

    fun pause() {
        player?.pause()
    }

    fun stop() {
        player?.stop()
    }

    fun setLoopCount(var1:Int) {
        player?.setLoopCount(var1)
    }

    fun adjustPlayoutVolume(volume: Int) {
        player?.adjustPlayoutVolume(volume)
    }

    // no use
    fun adjustPublishSignalVolume(volume: Int) {
        player?.adjustPublishSignalVolume(volume)
    }

    fun destroy() {
        player?.destroy()
    }

    fun getStatus(): MediaPlayerState {
        return player!!.getState()
    }

    fun getPlayPosition(): Long {
        return player!!.getPlayPosition()
    }

    fun getDuration(): Long {
        return player!!.duration
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        this.listener?.onJoinChannelSuccess(channel, uid, elapsed)
    }

    override fun onPlayerStateChanged(p0: MediaPlayerState, p1: MediaPlayerError) {
        this.listener?.onPlayerStateChanged(p0, p1)
    }

    override fun onPositionChanged(p0: Long) {
        this.listener?.onPositionChanged(p0)
    }

    override fun onPlayerEvent(p0: MediaPlayerEvent) {
        this.listener?.onPlayerEvent(p0)
    }

    override fun onMetaData(p0: MediaPlayerMetadataType, p1: ByteArray) {
        this.listener?.onMetaData(p0, p1)
    }

    override fun onPlayBufferUpdated(p0: Long) {
        this.listener?.onPlayBufferUpdated(p0)
    }

    override fun onCompleted() {
        this.listener?.onCompleted()
    }
}

open class RMCPlayerListener : IMediaPlayerObserver, IRtcEngineEventHandler() {
    override fun onPlayerStateChanged(state: MediaPlayerState,
                                      error: MediaPlayerError) {

    }

    override fun onPositionChanged(position: Long) {

    }

    override fun onPlayerEvent(event: MediaPlayerEvent) {

    }

    override fun onMetaData(type: MediaPlayerMetadataType,
                            data: ByteArray?) {

    }

    override fun onPlayBufferUpdated(p0: Long) {

    }

    override fun onCompleted() {

    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {

    }
}