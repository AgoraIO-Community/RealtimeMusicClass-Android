package io.agora.realtimemusicclass.base.edu.core

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.agora.realtimemusicclass.base.edu.core.data.RMCCallback
import io.agora.realtimemusicclass.base.edu.core.data.RMCError
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtm.*
import java.util.concurrent.CountDownLatch

class RMCEngine(private val core: RMCCore,
                private val rtcEngine: RtcEngine,
                private val rtmClient: RtmClient) {
    private val tag = "RMCEngine"

    private var hasJoined: Boolean = false
    private var joinFailed: Boolean = false
    private var recycled: Boolean = false

    private val latchCount = 2
    private var joinCDLock: CountDownLatch? = null
    private var joinCallback: RMCCallback<Int>? = null
    private var rtmChannel: RtmChannel? = null

    @Volatile
    private var notification = RMCNotification(core)

    @Volatile
    private var rtcUid: Int = -1

    private val rtcListener = object : RMCRtcEventListener() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            rtcUid = uid
            doIfJoinSuccess()
        }
    }

    private val rtmListener = object : RMCRtmEventListener() {

    }

    init {
        core.registerRtcEventListener(rtcListener)
        core.registerRtmEventListener(rtmListener)
    }

    /**
     * @param token authentication token string
     * @param channel rtc and rtm channel id
     * @param userId user id to login to rtm client
     * @param streamId media stream id in the rtc channel, the default is 0
     * @param callback result of this join operation, returns the
     * stream id of current user if succeeded (may be not the same as passed
     * via streamId), or an error message if failed
     */
    fun join(token: String?, channel: String, userId: String, streamId: Int = 0,
             params: String?, callback: RMCCallback<Int>? = null) {

        synchronized(this) {
            if (joinCDLock != null || recycled || hasJoined) {
                callback?.onFailure(RMCError(RMCError.defaultError,
                    "Illegal engine state: you may be already joining or has joined " +
                            "a room, or the engine has been recycled."))
                return
            }

            rtmClient.login(token, userId, object : ResultCallback<Void> {
                override fun onSuccess(p0: Void?) {
                    parseParams(params)
                    joinChannel(token, channel, streamId, callback)
                }

                override fun onFailure(p0: ErrorInfo?) {
                    val error = buildRMCError(p0)
                    callback?.onFailure(error)
                }
            })
        }
    }

    private fun parseParams(params: String?) {
        val engineParams = EngineParamParser().parseString(params)
        engineParams.apply(rtcEngine, rtmClient)
    }

    private fun joinChannel(token: String?, channel: String,
                            streamId: Int, callback: RMCCallback<Int>? = null) {
        this.joinCallback = callback
        joinCDLock = CountDownLatch(latchCount)
        rtmChannel = rtmClient.createChannel(channel, core.rmcEngineEventManager)
        rtmChannel?.join(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                notification.initRtmContext(rtmChannel!!, rtmClient)
                doIfJoinSuccess()
            }

            override fun onFailure(p0: ErrorInfo?) {
                resetOnJoinFail()
                this@RMCEngine.joinCallback?.onFailure(buildRMCError(p0))
            }
        })

        rtcEngine.enableAudioVolumeIndication(1000, 3)

        val options = ChannelMediaOptions()
        options.autoSubscribeAudio = true
        options.autoSubscribeVideo = true
        options.publishAudioTrack = true
        options.publishCameraTrack = true
        options.publishCustomAudioTrack = false
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
        rtcEngine.joinChannel(token, channel, streamId, options).let { code ->
            if (code != Constants.ERR_OK) {
                resetOnJoinFail()
                callback?.onFailure(RMCError(code, RtcEngine.getErrorDescription(code)))
            }
        }
    }

    private fun buildRMCError(rtmError: ErrorInfo?) : RMCError {
        return if (rtmError != null) {
            RMCError(rtmError.errorCode, rtmError.errorDescription)
        } else {
            RMCError(RMCError.defaultError, "")
        }
    }

    @Synchronized
    /**
     * Only determined if both rtc and rtm perform join
     * channel and keep the two routines synchronized
     */
    private fun ifJoinCountDownFinished(): Boolean {
        joinCDLock?.countDown()
        return joinCDLock?.count ?: 0 == 0L
    }

    private fun doIfJoinSuccess() {
        if (ifJoinCountDownFinished()) {
            joinCDLock = null
            hasJoined = true
            this.joinCallback?.onSuccess(this.rtcUid)
        }
    }

    @Synchronized
    private fun resetOnJoinFail() {
        joinCDLock = null
        hasJoined = false
        joinFailed = false
    }

    fun leave(callback: RMCCallback<Boolean>? = null) {
        synchronized(this) {
            if (!hasJoined || recycled) {
                callback?.onFailure(
                    RMCError(RMCError.defaultError,
                    "already left, or the engine has been recycled")
                )
                return
            }
        }

        rtcEngine.leaveChannel()
        RtcEngine.destroy()
        rtmChannel?.leave(null)
        rtmChannel?.release()
        rtmClient.logout(null)
        rtmClient.release()
    }

    fun notifier(): RMCNotification? {
        return notification
    }

    fun applyUserRole(isBroadcaster: Boolean) {
        if (hasJoined) {
            setRtcClientRole(isBroadcaster)
        } else {
            Log.e(tag, "apply a user role only after joining the room")
        }
    }

    private fun setRtcClientRole(isBroadcaster: Boolean) {
        rtcEngine.setClientRole(if (isBroadcaster) {
            Constants.CLIENT_ROLE_BROADCASTER
        } else {
            Constants.CLIENT_ROLE_AUDIENCE
        })
    }

    fun rtcEngine(): RtcEngine {
        return rtcEngine
    }

    fun recycle() {
        synchronized(this) {
            joinCDLock = null
            hasJoined = false
            recycled = true
        }

        core.removeRtcEventListener(rtcListener)
        core.removeRtmEventListener(rtmListener)
    }
}

class RMCEngineListenerManager : RtmClientListener, RtmChannelListener, IRtcEngineEventHandler() {
    private val rtcListeners = mutableListOf<RMCRtcEventListener>()
    private val highPriorityRtcListeners = mutableListOf<RMCRtcEventListenerWithHighPriority>()

    private val rtmListeners = mutableListOf<RMCRtmEventListener>()

    @Synchronized
    fun registerRtcEventListener(listener: RMCRtcEventListener) {
        if (listener is RMCRtcEventListenerWithHighPriority &&
            !highPriorityRtcListeners.contains(listener)) {
            highPriorityRtcListeners.add(listener)
        } else {
            if (!rtcListeners.contains(listener)) {
                rtcListeners.add(listener)
            }
        }
    }

    @Synchronized
    fun removeRtcEventListener(listener: RMCRtcEventListener) {
        highPriorityRtcListeners.remove(listener)
        rtcListeners.remove(listener)
    }

    @Synchronized
    fun registerRtmEventListener(listener: RMCRtmEventListener) {
        if (!rtmListeners.contains(listener)) {
            rtmListeners.add(listener)
        }
    }

    @Synchronized
    fun removeRtmEventListener(listener: RMCRtmEventListener) {
        rtmListeners.remove(listener)
    }

    // Rtc engine event callbacks
    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        highPriorityRtcListeners.forEach {
            it.onJoinChannelSuccess(channel, uid, elapsed)
        }

        rtcListeners.forEach {
            it.onJoinChannelSuccess(channel, uid, elapsed)
        }
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        highPriorityRtcListeners.forEach {
            it.onUserJoined(uid, elapsed)
        }

        rtcListeners.forEach {
            it.onUserJoined(uid, elapsed)
        }
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        highPriorityRtcListeners.forEach {
            it.onUserOffline(uid, reason)
        }

        rtcListeners.forEach {
            it.onUserOffline(uid, reason)
        }
    }

    override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>,
                                         totalVolume: Int) {
        rtcListeners.forEach {
            it.onAudioVolumeIndication(speakers, totalVolume)
        }
    }

    // Rtm client listener implementations
    override fun onConnectionStateChanged(p0: Int, p1: Int) {
        rtmListeners.forEach {
            it.onConnectionStateChanged(p0, p1)
        }
    }

    override fun onMessageReceived(p0: RtmMessage, p1: String) {
        rtmListeners.forEach {
            it.onMessageReceived(p0, p1)
        }
    }

    override fun onImageMessageReceivedFromPeer(p0: RtmImageMessage?, p1: String?) {
        rtmListeners.forEach {
            it.onImageMessageReceivedFromPeer(p0, p1)
        }
    }

    override fun onFileMessageReceivedFromPeer(p0: RtmFileMessage?, p1: String?) {
        rtmListeners.forEach {
            it.onFileMessageReceivedFromPeer(p0, p1)
        }
    }

    override fun onMediaUploadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {
        rtmListeners.forEach {
            it.onMediaUploadingProgress(p0, p1)
        }
    }

    override fun onMediaDownloadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {
        rtmListeners.forEach {
            it.onMediaDownloadingProgress(p0, p1)
        }
    }

    override fun onTokenExpired() {
        rtmListeners.forEach {
            it.onTokenExpired()
        }
    }

    override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {
        rtmListeners.forEach {
            it.onPeersOnlineStatusChanged(p0)
        }
    }

    // Rtm channel listener implementations
    override fun onMemberCountUpdated(p0: Int) {
        rtmListeners.forEach {
            it.onMemberCountUpdated(p0)
        }
    }

    override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {
        rtmListeners.forEach {
            it.onAttributesUpdated(p0)
        }
    }

    override fun onMessageReceived(p0: RtmMessage, p1: RtmChannelMember) {
        rtmListeners.forEach {
            it.onMessageReceived(p0, p1)
        }
    }

    override fun onImageMessageReceived(p0: RtmImageMessage?, p1: RtmChannelMember?) {
        rtmListeners.forEach {
            it.onImageMessageReceived(p0, p1)
        }
    }

    override fun onFileMessageReceived(p0: RtmFileMessage?, p1: RtmChannelMember?) {
        rtmListeners.forEach {
            it.onFileMessageReceived(p0, p1)
        }
    }

    override fun onMemberJoined(p0: RtmChannelMember?) {
        rtmListeners.forEach {
            it.onMemberJoined(p0)
        }
    }

    override fun onMemberLeft(p0: RtmChannelMember?) {
        rtmListeners.forEach {
            it.onMemberLeft(p0)
        }
    }

    override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
        rtcListeners.forEach{
            it.onStreamMessage(uid, streamId, data)
        }
    }

    override fun onStreamMessageError(
        uid: Int,
        streamId: Int,
        error: Int,
        missed: Int,
        cached: Int
    ) {
        Log.e("RMCStreamDatamanager", "error")
    }
}

open class RMCRtcEventListener : IRtcEngineEventHandler()

internal open class RMCRtcEventListenerWithHighPriority : RMCRtcEventListener()

open class RMCRtmEventListener : RtmClientListener, RtmChannelListener {
    // Rtm client listener implementations
    override fun onConnectionStateChanged(p0: Int, p1: Int) {

    }

    override fun onMessageReceived(p0: RtmMessage, p1: String) {

    }

    override fun onImageMessageReceivedFromPeer(p0: RtmImageMessage?, p1: String?) {

    }

    override fun onFileMessageReceivedFromPeer(p0: RtmFileMessage?, p1: String?) {

    }

    override fun onMediaUploadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {

    }

    override fun onMediaDownloadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {

    }

    override fun onTokenExpired() {

    }

    override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {

    }

    // Rtm channel listener implementations
    override fun onMemberCountUpdated(p0: Int) {

    }

    override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {

    }

    override fun onMessageReceived(p0: RtmMessage, p1: RtmChannelMember) {

    }

    override fun onImageMessageReceived(p0: RtmImageMessage?, p1: RtmChannelMember?) {

    }

    override fun onFileMessageReceived(p0: RtmFileMessage?, p1: RtmChannelMember?) {

    }

    override fun onMemberJoined(p0: RtmChannelMember?) {

    }

    override fun onMemberLeft(p0: RtmChannelMember?) {

    }
}

internal class EngineParamParser {
    fun parseString(paramString: String?): EngineParams {
        return try {
            Gson().fromJson(paramString, EngineParams::class.java)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            EngineParams(null, null)
        }
    }
}

internal class EngineParams(private var rtc: ArrayList<String>?,
                            private var rtm: ArrayList<String>?) {
    fun apply(rtcEngine: RtcEngine?, rtmClient: RtmClient?) {
        if (rtc != null) {
            for (param in rtc!!) {
                Log.d(TAG, "rtc engine params applied: $param")
                rtcEngine!!.setParameters(param)
            }
        }
        if (rtm != null) {
            for (param in rtm!!) {
                Log.d(TAG, "rtm engine params applied: $param")
                rtmClient!!.setParameters(param)
            }
        }
    }

    companion object {
        private const val TAG = "RmcEngineParams"
    }
}