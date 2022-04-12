package io.agora.realtimemusicclass.chorus

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.agora.realtimemusicclass.base.edu.classroom.ClassManager
import io.agora.realtimemusicclass.base.edu.classroom.RMCCoreHelper
import io.agora.realtimemusicclass.base.edu.core.*
import io.agora.realtimemusicclass.base.edu.core.data.*
import io.agora.realtimemusicclass.base.ui.actions.*
import io.agora.realtimemusicclass.base.ui.activities.ActionActivity
import io.agora.realtimemusicclass.base.utils.ToastUtil
import io.agora.realtimemusicclass.chorus.databinding.ActivityChorusBinding
import io.agora.realtimemusicclass.chorus.helper.AudioParametersHelper
import io.agora.realtimemusicclass.chorus.helper.MusicSyncHelper
import io.agora.realtimemusicclass.chorus.view.audience.LobbyAudienceView
import io.agora.realtimemusicclass.chorus.view.audience.LobbyAudienceViewListener
import io.agora.realtimemusicclass.chorus.view.bottom.BottomAction
import io.agora.realtimemusicclass.chorus.view.bottom.BottomActionBarWrapper
import io.agora.realtimemusicclass.chorus.view.bottom.BottomActionListener
import io.agora.realtimemusicclass.chorus.view.broadcaster.BroadcasterManager
import io.agora.realtimemusicclass.chorus.view.broadcaster.OnSeatStateListener
import io.agora.realtimemusicclass.chorus.view.pager.CourseWarePager
import io.agora.realtimemusicclass.chorus.view.broadcaster.SeatOpDialog
import io.agora.realtimemusicclass.chorus.view.broadcaster.SeatOpDialogListener
import io.agora.rtc2.Constants

class ChorusActivity : ActionActivity() {
    private val tag = "ChorusActivity"

    private lateinit var binding: ActivityChorusBinding

    private lateinit var audienceView: LobbyAudienceView
    private lateinit var bottomBarWrapper: BottomActionBarWrapper
    private lateinit var broadcasterManager: BroadcasterManager
    private lateinit var courseWarePager: CourseWarePager

    private lateinit var className: String
    private lateinit var channelId: String
    private lateinit var localUserInfo: RMCUserInfo

    private lateinit var coreHelper: RMCCoreHelper
    private lateinit var seatHelper: SeatUpdateHelper
    var musicSyncHelper: MusicSyncHelper? = null
    var audioParametersHelper: AudioParametersHelper? = null

    private var pendingInEarEnabled: Boolean? = null
    private var seatDialog: SeatOpDialog? = null

    private val bottomBarActionListener = object : BottomActionListener {
        override fun onActionPerformed(action: BottomAction, enabled: Boolean) {
            when (action) {
                BottomAction.Help -> {
                    ToastUtil.toast(this@ChorusActivity, R.string.function_under_development)
                }
                BottomAction.Camera -> {
                    if (enabled) {
                        coreHelper.startLocalVideoCapture()
                    } else {
                        coreHelper.stopLocalVideoCapture()
                    }
                    seatHelper.updateMyOwnSeatIfExist()
                }
                BottomAction.Mic -> {
                    if (enabled) {
                        coreHelper.startLocalAudioRecording()
                    } else {
                        coreHelper.stopLocalAudioRecording()
                    }
                    seatHelper.updateMyOwnSeatIfExist()
                    updateEarMonitorIfNeed(enabled)
                }
                BottomAction.Chat -> {
                    ContextCompat.getMainExecutor(this@ChorusActivity).execute {
                        actionSheetUtil()?.showActionSheetDialog(
                            this@ChorusActivity,
                            ActionSheetType.Chat,
                            chatActionSheetCallback
                        )
                        val actionSheet = actionSheetUtil()?.getCurrentAction()
                        (actionSheet as? ActionSheetChat)?.refreshMessageList(rmcCore().chat().list())
                    }
                }
                BottomAction.InEar -> {
                    if (headphoneInEarEnabled()) {
                        rmcCore().audio().setInEarEnabled(enabled)
                    }
                }
            }
        }

        private fun updateEarMonitorIfNeed(enabled: Boolean) {
            ContextCompat.getMainExecutor(this@ChorusActivity).execute {
                if (enabled && rmcCore().audio().isInEarEnabled() && headphoneInEarEnabled()) {
                    // update ui
                    bottomBarWrapper.setInEarEnabled(true)
                    rmcCore().audio().setInEarEnabled(true)
                } else {
                    bottomBarWrapper.setInEarEnabled(false)
                    //rmcCore().audio().setInEarEnabled(false)
                }
            }
        }

        override fun onInEarEnabled(): Boolean {
            return headphoneInEarEnabled()
        }

        override fun onCameraEnabled(): Boolean {
            return rmcCore().video().isLocalVideoCaptureEnabled()
        }

        override fun onMicEnabled(): Boolean {
            return rmcCore().audio().isLocalAudioRecording()
        }

        override fun onError(message: String) {
            ToastUtil.toast(this@ChorusActivity, message)
        }
    }

    private val seatListener = object : OnSeatStateListener {
        override fun onUpdateSeatUser(seat: BroadcasterManager.Seat,
                                      user: RMCUserInfo): RMCUserInfo {
            return seatHelper.updateSeatUserState(seat, user)
        }

        override fun onSeatClicked(index: Int, userInfo: RMCUserInfo?) {
            userInfo?.let { user ->
                if (localUserInfo.isTeacher() &&
                    user.userName != localUserInfo.userName &&
                        rmcCore().user().userHasJoined(user) &&
                        rmcCore().user().userIsOnline(user)) {
                    seatDialog?.dismiss()
                    user.let { student ->
                        SeatOpDialog(this@ChorusActivity, stuOpDialogListener).let {
                            seatDialog = it
                            it.show(student)
                        }
                    }
                }
            }
        }

        override fun onSeatUserOnline(userInfo: RMCUserInfo): Boolean {
            return rmcCore().user().userIsOnline(userInfo)
        }
    }

    private val stuOpDialogListener = object : SeatOpDialogListener {
        override fun onDialogDismiss() {

        }

        override fun onVideoPublished(info: RMCUserInfo, published: Boolean) {
            coreHelper.notifyRemoteVideoPublished(info.userName, published)
        }

        override fun onAudioPublished(info: RMCUserInfo, published: Boolean) {
            coreHelper.notifyRemoteAudioPublished(info.userName, published)
        }
    }

    private val userCallback = object : RMCUserCallback() {
        override fun onUserUpdate(info: RMCUserInfo) {
            onUserInfoChange(info)
            refreshActionSheet()
        }

        override fun onUserJoin(info: RMCUserInfo) {
            onUserJoinOrLeave()
            onUserInfoChange(info)
            refreshActionSheet()
        }

        override fun onUserLeave(info: RMCUserInfo) {
            onUserJoinOrLeave()
            onUserLeft(info)
            refreshActionSheet()
        }

        private fun onUserJoinOrLeave() {
            ContextCompat.getMainExecutor(this@ChorusActivity).execute {
                audienceView.setCount(rmcCore().user().getUserInfoList().size)
            }
        }

        private fun onUserInfoChange(info: RMCUserInfo) {
            ContextCompat.getMainExecutor(this@ChorusActivity).execute {
                info.media?.let {
                    seatHelper.updateSeatUserStateIfExist(info)
                }
            }
        }

        private fun onUserLeft(info: RMCUserInfo) {
            ContextCompat.getMainExecutor(this@ChorusActivity).execute {
                seatHelper.removeSeatUser(info)
            }
        }

        private fun refreshActionSheet() {
            ContextCompat.getMainExecutor(this@ChorusActivity).execute {
                refreshUserListActionSheet()
                refreshVolumeListActionSheet()
            }
        }

        override fun onUserOnline(info: RMCUserInfo) {
            onUserInfoChange(info)
            refreshActionSheet()
        }

        override fun onUserOffline(info: RMCUserInfo) {
            if (rmcCore().user().userHasJoined(info)) {
                onUserInfoChange(info)
            } else {
                onUserLeft(info)
            }
            refreshActionSheet()
        }
    }

    private val audioListener = object : RMCAudioCallback() {
        override fun onAudioVolumesChanged(volumes: List<RMCAudioVolume>) {
            seatHelper.updateVolumeIndication(volumes)
        }
    }

    private val userCommandCallback = object : RMCRemoteCommandCallback {
        override fun onLocalAudioShouldPublish(published: Boolean) {
            coreHelper.muteLocalAudioStream(!published, object : RMCCallback<Boolean> {
                override fun onSuccess(res: Boolean?) {
                    seatHelper.updateMyOwnSeatIfExist()
                }

                override fun onFailure(error: RMCError) {

                }
            })
        }

        override fun onLocalVideoShouldPublish(published: Boolean) {
            coreHelper.muteLocalVideoStream(!published, object : RMCCallback<Boolean> {
                override fun onSuccess(res: Boolean?) {
                    seatHelper.updateMyOwnSeatIfExist()
                }

                override fun onFailure(error: RMCError) {

                }
            })
        }
    }

    private val chatCallback = object : RMCChatCallback() {
        override fun onGroupChat(fromId: String, message: String) {
            var role:RMCUserRole = RMCUserRole.ROLE_TYPE_UNKNOWN
            rmcCore().user().getUserInfoList().forEach {
                if (it.userName == fromId) {
                    role = RMCUserRole.fromValue(it.role)
                }
            }
            rmcCore().chat().addChat(fromId, role, message)
            ContextCompat.getMainExecutor(this@ChorusActivity).execute {
                val actionSheet = actionSheetUtil()?.getCurrentAction()
                if (actionSheet is ActionSheetChat) {
                    actionSheet.refreshMessageList(rmcCore().chat().list())
                }
            }
        }
    }

    private val chatActionSheetCallback = object : ActionSheetChatListener {
        override fun getLocalUserInfo(): RMCUserInfo {
            return localUserInfo
        }

        override fun sendMessage(name: String, message: String) {
            rmcCore().chat().sendChat(name, localUserInfo.role(), message)
            val actionSheet = actionSheetUtil()?.getCurrentAction()
            if (actionSheet is ActionSheetChat) {
                actionSheet.refreshMessageList(rmcCore().chat().list())
            }
        }
    }

    private val userActionSheetCallback = object : ActionSheetUserListener {
        override fun onAecModeChanged(mode: AECMode, name: String) {
            // only host user can adjust aec mode
            audioParametersHelper?.changeUserAECMode(mode, name)
        }
    }

    override fun onIncomingData() {
        className = intent.getStringExtra(ClassManager.KEY_CLASS_NAME) ?: ""
        channelId = intent.getStringExtra(ClassManager.KEY_CHANNEL_ID) ?: ""
        val json = intent.getStringExtra(ClassManager.KEY_USER_INFO)
        localUserInfo = try {
            Gson().fromJson(json, RMCUserInfo::class.java)
        } catch (e: JsonSyntaxException) {
            RMCUserInfo.defaultUserInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChorusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermissions()
        showLoading()
    }

    override fun onPermissionGranted() {
        if (localUserInfo.role() == RMCUserRole.ROLE_TYPE_UNKNOWN) {
            ToastUtil.toast(this, "Local user is not found, please " +
                    "make sure you have obtained local user information")
            return
        }

        coreHelper = RMCCoreHelper(className, rmcCore())
        initLayout()
        join()
    }

    private fun initLayout() {
        binding.chorusTitleLayoutName.text = className

        binding.chorusTitleLayoutBackIcon.setOnClickListener {
            exitRoom()
        }

        audienceView = binding.chorusLobbyAudienceIconLayout
        audienceView.setCount(0)
        audienceView.setListener(object : LobbyAudienceViewListener {
            override fun onViewClicked() {
                actionSheetUtil()?.showActionSheetDialog(
                    this@ChorusActivity,
                    ActionSheetType.User,
                    userActionSheetCallback)
                refreshUserListActionSheet()
            }
        })

        val bottomAction = binding.chorusBottomActionBar
        bottomBarWrapper = BottomActionBarWrapper(bottomAction,
            localUserInfo.role(), bottomBarActionListener)
        pendingInEarEnabled?.let { enabled ->
            bottomBarWrapper.setInEarEnabled(enabled)
        }

        broadcasterManager = BroadcasterManager(
            binding.chorusBroadcasterSeatLayout, seatListener)
        seatHelper = SeatUpdateHelper(rmcCore(), broadcasterManager)

        courseWarePager = CourseWarePager(this, binding.chorusCourseWareLayout)
    }

    private fun join() {
        rmcCore().user().addCallback(userCallback)
        rmcCore().user().remoteCommandListener = userCommandCallback
        rmcCore().audio().setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_CHORUS)
        rmcCore().audio().registerCallback(audioListener)

        val params = RMCSceneParams.getSceneParams(RMCSceneType.Chorus)
        rmcCore().join(null, className, channelId, localUserInfo, params,
            object : RMCCallback<Int> {
                override fun onSuccess(res: Int?) {
                    res?.let { initAfterJoined(localUserInfo, it) }
                    callClassJoined(rmcCore())
                }

                override fun onFailure(error: RMCError) {
                    ContextCompat.getMainExecutor(this@ChorusActivity).execute {
                        dismissLoadingDialog()
                        ToastUtil.toast(this@ChorusActivity,
                            "Join class failed, ${error.msg}")
                    }
                }
            })
    }

    /**
     * The things that must be done right after joining the class
     * successfully. This is the starting point of the entire class
     * data initialization.
     */
    private fun initAfterJoined(localUser: RMCUserInfo, uid: Int) {
        rmcCore().user().let { manager ->
            // Must use local cached user info of current user
            rmcCore().user().getUserInfo(localUser.userName)?.let {
                // We trust local cached ext, but not the one
                // passed from join callback
                val ext = it.ext
                it.set(localUserInfo)
                it.media?.streamId = uid
                it.ext = ext

                // Reset current local user uid for convenience
                localUser.media?.streamId = uid

                manager.setLocalUserInfo(it)
                initLocalMediaState(it)
                rmcCore().audio().setDefaultAecMode(it)

                // Notify server to update my actual rtc stream uid
                // as early as possible
                manager.notifyUserUpdate(className, it)
            }

            seatHelper.updateMyOwnSeatIfExist()

            manager.getUserInfoList().let {
                if (it.isNotEmpty()) {
                    broadcasterManager.updateBroadcasters(it)
                }

                audienceView.setCount(it.size)
            }
            audioParametersHelper = AudioParametersHelper(localUser.role(), this, rmcCore())
        }

        rmcCore().chat().addChatCallback(chatCallback)

        // Local device state may respond slightly later because
        // of networking latency, so we must refresh bottom
        // UI in the callbacks of core function request
        resetBottomBarUI()

        dismissLoadingDialog()
    }

    fun initMusicSyncHelper() {
        musicSyncHelper = MusicSyncHelper(localUserInfo, rmcCore())
    }

    fun destroyMusicSyncHelper() {
        musicSyncHelper!!.destroy()
    }

    private fun initLocalMediaState(localUser: RMCUserInfo) {
        if (!localUser.isTeacher() && !localUser.isStudent()) {
            return
        }

        coreHelper.syncLocalDevices(localUser)
    }

    private fun resetBottomBarUI() {
        bottomBarWrapper.setCameraEnabled(rmcCore().video().isLocalVideoCaptureEnabled())
        bottomBarWrapper.setMicEnabled(rmcCore().audio().isLocalAudioRecording())
        bottomBarWrapper.setInEarEnabled(rmcCore().audio().isInEarEnabled()
                && headphoneInEarEnabled())
    }

    private fun refreshUserListActionSheet() {
        val actionSheet = actionSheetUtil()?.getCurrentAction()
        (actionSheet as? ActionSheetUser)?.let {
            val isTeacher = rmcCore().user().localUser()!!.isTeacher()
            val localUserName = rmcCore().user().localUser()!!.userName
            it.refreshUserList(
                isTeacher,
                localUserName,
                audioParametersHelper?.userList()!!
            )
        }
    }

    fun refreshVolumeListActionSheet() {
        val actionSheet = actionSheetUtil()?.getCurrentAction()
        (actionSheet as? ActionSheetConsole)?.refreshVolumeList(audioParametersHelper?.volumeList()!!)
    }

    override fun onHeadsetWithMicPlugged(plugged: Boolean) {
        // Note that the invocation of this callback may be
        // earlier than the bottom bar is initialized,
        // especially when asking for the system permission.
        if (this::bottomBarWrapper.isInitialized) {
            bottomBarWrapper.setInEarEnabled(plugged && rmcCore().audio().isInEarEnabled())
        } else {
            pendingInEarEnabled = plugged
        }
        // todo:notify action sheet ?
    }

    override fun onPermissionFail() {
        dismissLoadingDialog()
    }

    override fun finish() {
        callClassLeft(rmcCore())
        rmcCore().leave(className, localUserInfo,
            object : RMCCallback<String> {
                override fun onSuccess(res: String?) {
                    // ignored
                }

                override fun onFailure(error: RMCError) {
                    // ignored
                }
            }, worker())

        super.finish()
    }

    private fun exitRoom() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        rmcCore().recycle()
    }
}

class SeatUpdateHelper(private val core: RMCCore,
                       private val broadcasterManager: BroadcasterManager) {
    private val tag = "SeatUpdateHelper"

    fun updateSeatUserState(seat: BroadcasterManager.Seat,
                            userInfo: RMCUserInfo): RMCUserInfo {
        Log.d(tag, "seat update called, $userInfo")
        seat.user?.let { curUser ->
            if (curUser.userName != userInfo.userName) {
                // Under current design, the case that overwriting
                // a current user by another will never happen
                return@let
            }

            seat.setSeatUser(userInfo)
            // Only take care of refreshing ui, thus no
            // state synchronization is called to server,
            // and only video rendering is taken into account
            if (userInfo.cameraShouldOpen()) {
                if (isLocalUser(userInfo)) {
                    core.video().renderLocalVideo(seat.textureView)
                } else {
                    userInfo.media?.let {
                        if (it.streamId != 0) {
                            core.video().renderRemoteVideo(seat.textureView, it.streamId)
                        }
                    }
                }
            }
        } ?: run {
            seat.setSeatUser(userInfo)
            if (userInfo.cameraShouldOpen()) {
                if (isLocalUser(userInfo)) {
                    core.video().renderLocalVideo(seat.textureView)
                } else {
                    userInfo.media?.let {
                        if (it.streamId != 0) {
                            core.video().renderRemoteVideo(seat.textureView, it.streamId)
                        }
                    }
                }
            }
        }
        return userInfo
    }

    fun updateVolumeIndication(volumes: List<RMCAudioVolume>) {
        broadcasterManager.playVolumeAnim(volumes)
    }

    fun updateSeatUserStateIfExist(userInfo: RMCUserInfo) {
        broadcasterManager.findUserSeat(userInfo)?.let {
            updateSeatUserState(it, userInfo)
        }
    }

    fun removeSeatUser(seat: BroadcasterManager.Seat) {
        val user = seat.user
        seat.setSeatUser(null)

        if (user?.media != null) {
            if (isLocalUser(user)) {
                core.video().clearLocalVideo()
            } else {
                core.video().clearRemoteVideo(user.media!!.streamId)
            }
        }
    }

    fun removeSeatUser(userInfo: RMCUserInfo) {
        broadcasterManager.findUserSeat(userInfo)?.let {
            removeSeatUser(it)
        }
    }

    private fun isLocalUser(userInfo: RMCUserInfo): Boolean {
        return core.user().localUser()?.userName == userInfo.userName
    }

    fun findAndUpdateUser(userInfo: RMCUserInfo) {
        broadcasterManager.updateBroadcasters(listOf(userInfo))
    }

    // Find my seat, and update seat state if exists
    fun updateMyOwnSeatIfExist() {
        core.user().localUser()?.let {
            findAndUpdateUser(it)
        }
    }
}