package io.agora.realtimemusicclass.base.edu.core

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import io.agora.realtimemusicclass.base.edu.core.data.RMCCallback
import io.agora.realtimemusicclass.base.edu.core.data.RMCError
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.edu.core.sync.RMCNotificationEventListener
import io.agora.realtimemusicclass.base.ui.activities.BaseActivityLifeCycleCallback
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.CameraCapturerConfiguration.CAMERA_DIRECTION
import io.agora.rtc2.video.CameraCapturerConfiguration.CaptureFormat
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE
import io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE
import io.agora.rtm.RtmClient
import java.util.concurrent.atomic.AtomicBoolean

class RMCCore(context: Context, appId: String,
              listener: RMCCoreStateListener? = null)
    : BaseActivityLifeCycleCallback() {

    private val tag = "RMCCore"
    private val coreCreateSuccess = AtomicBoolean(false)

    private lateinit var rtcEngine: RtcEngine
    private lateinit var rtmClient: RtmClient

    private lateinit var roomManager: RMCRoomManager
    private lateinit var videoManager: RMCVideoManager
    private lateinit var audioManager: RMCAudioManager
    private lateinit var playerManager: RMCPlayerManager
    private lateinit var dataManager: RMCStreamDataManager
    private lateinit var chatManager: RMCChatManager
    private lateinit var userManager: RMCUserManager
    private lateinit var rmcService: RMCService
    private lateinit var rmcEngine: RMCEngine

    private lateinit var mediaStateHolder: RMCDeviceStateHolder

    var rmcEngineEventManager: RMCEngineListenerManager? = null

    private val userIllegalStateListener = object : RMCUserIllegalStateListener {
        override fun onRemoteDeviceJoined() {
            Log.w(tag, "remote device login detected, recycle current core instance")
            coreCreateSuccess.set(false)
            roomManager.leaveWhenAborted()
            recycle()
            listener?.onRMCCoreAbort()
        }
    }

    init {
        initRmcEngine(context, appId, listener)
        initCore()
    }

    fun service(): RMCService {
        return rmcService
    }

    fun engine(): RMCEngine {
        return rmcEngine
    }

    fun notification(): RMCNotification? {
        return engine().notifier()
    }

    private fun initRmcEngine(context: Context, appId: String,
                              listener: RMCCoreStateListener? = null) {
        try {
            rmcEngineEventManager = RMCEngineListenerManager()
            rtmClient = RtmClient.createInstance(context, appId, rmcEngineEventManager)
            val config = RtcEngineConfig()
            config.mEventHandler = rmcEngineEventManager
            config.mContext = context.applicationContext
            config.mAppId = appId
            rtcEngine = RtcEngine.create(config)
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            setupVideoConfig()
            coreCreateSuccess.compareAndSet(false, true)
            listener?.onRMCCoreCreateSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            coreCreateSuccess.set(false)
            listener?.onRMCCoreCreateFail(-1, e.message)
        }
    }

    private fun setupVideoConfig() {
        rtcEngine.enableVideo()
        val videoDimen = VideoEncoderConfiguration.VD_160x120
        val format = CaptureFormat(videoDimen.width, videoDimen.height, 15)
        rtcEngine.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                videoDimen, FRAME_RATE.FRAME_RATE_FPS_7,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT))
        rtcEngine.setCameraCapturerConfiguration(
            CameraCapturerConfiguration(CAMERA_DIRECTION.CAMERA_FRONT, format)
        )
    }

    private fun hasCreated(): Boolean {
        return coreCreateSuccess.get()
    }

    private fun initCore() {
        rmcService = RMCService()
        rmcEngine = RMCEngine(this, rtcEngine, rtmClient)
        roomManager = RMCRoomManager(this)
        videoManager = RMCVideoManager(this)
        audioManager = RMCAudioManager(this)
        playerManager = RMCPlayerManager(this, 1)
        dataManager = RMCStreamDataManager(this)
        chatManager = RMCChatManager(this, 20)
        userManager = RMCUserManager(this, userIllegalStateListener)

        mediaStateHolder = RMCDeviceStateHolder(this)
    }

    fun registerRtcEventListener(listener: RMCRtcEventListener?) {
        if (hasCreated()) {
            rmcEngineEventManager!!.registerRtcEventListener(listener!!)
        } else {
            Log.w(tag, "Illegal rmc core state")
        }
    }

    fun removeRtcEventListener(listener: RMCRtcEventListener?) {
        if (hasCreated()) {
            rmcEngineEventManager!!.removeRtcEventListener(listener!!)
        } else {
            Log.w(tag, "Illegal rmc core state")
        }
    }

    fun registerRtmEventListener(listener: RMCRtmEventListener?) {
        if (hasCreated()) {
            rmcEngineEventManager!!.registerRtmEventListener(listener!!)
        } else {
            Log.w(tag, "Illegal rmc core state")
        }
    }

    fun removeRtmEventListener(listener: RMCRtmEventListener?) {
        if (hasCreated()) {
            rmcEngineEventManager!!.removeRtmEventListener(listener!!)
        } else {
            Log.w(tag, "Illegal rmc core state")
        }
    }

    internal fun registerNotificationEventListener(listener: RMCNotificationEventListener?) {
        if (hasCreated() && notification() != null) {
            notification()!!.addEventListener(listener!!)
        } else {
            Log.w(tag, "Illegal rmc core state")
        }
    }

    internal fun removeNotificationEventListener(listener: RMCNotificationEventListener?) {
        if (hasCreated() && notification() != null) {
            notification()!!.removeEventListener(listener!!)
        } else {
            Log.w(tag, "Illegal rmc core state")
        }
    }

    fun room(): RMCRoomManager {
        return roomManager
    }

    fun audio(): RMCAudioManager {
        return audioManager
    }

    fun video(): RMCVideoManager {
        return videoManager
    }

    fun player(): RMCPlayerManager {
        return playerManager
    }

    fun data(): RMCStreamDataManager {
        return dataManager
    }

    fun chat(): RMCChatManager {
        return chatManager
    }

    fun user(): RMCUserManager {
        return userManager
    }

    fun join(token: String?, className: String, channelId: String,
             info: RMCUserInfo, params: String? = null, callback: RMCCallback<Int>? = null) {
        if (hasCreated()) {
            // First to obtain the entire user list of class
            user().refreshUserInfoList(className, info, null, object : RMCCallback<List<RMCUserInfo>> {
                override fun onSuccess(res: List<RMCUserInfo>?) {
                    room().join(token, className, channelId, info, params, callback)
                }

                override fun onFailure(error: RMCError) {
                    callback?.onFailure(error)
                }
            })
        } else {
            Log.w(tag, "Illegal rmc core state")
        }
    }

    fun leave(className: String, info: RMCUserInfo,
              callback: RMCCallback<String>? = null,
              handler: Handler? = null) {
        if (hasCreated()) {
            room().leave(className, info, callback, handler)
        } else {
            Log.w(tag, "Illegal rmc core state")
        }
    }

    override fun onStarted(owner: LifecycleOwner) {
        mediaStateHolder.dispatch()
    }

    override fun onStopped(owner: LifecycleOwner) {
        mediaStateHolder.collect()

        if (hasCreated()) {
            audio().setLocalRecordingEnabled(false)
            video().enableLocalVideoCapturing(false)
        }
    }

    fun recycle() {
        rmcEngine.recycle()
        dataManager.recycle()
        userManager.recycle()
        audioManager.recycle()
        chatManager.recycle()
        notification()?.recycle()
    }
}

interface RMCCoreStateListener {
    fun onRMCCoreCreateSuccess()
    fun onRMCCoreCreateFail(code: Int, msg: String?)
    fun onRMCCoreAbort()
}

internal class RMCDeviceStateHolder(private val core: RMCCore) {
    private var micRecordingStarted: Boolean = core.audio().isLocalAudioRecording()
    private var videoCaptureStarted: Boolean = core.video().isLocalVideoCaptureEnabled()

    fun collect() {
        micRecordingStarted = core.audio().isLocalAudioRecording()
        videoCaptureStarted = core.video().isLocalVideoCaptureEnabled()
    }

    fun dispatch() {
        core.audio().setLocalRecordingEnabled(micRecordingStarted)
        core.video().enableLocalVideoCapturing(videoCaptureStarted)
    }
}