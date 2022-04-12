package io.agora.realtimemusicclass.base.edu.core

import com.google.gson.Gson
import io.agora.realtimemusicclass.base.edu.core.RMCServiceDataTransformer.toServerRole
import io.agora.realtimemusicclass.base.edu.core.data.*
import io.agora.realtimemusicclass.base.edu.core.sync.RMCNotificationEventListener
import io.agora.realtimemusicclass.base.ui.actions.AECMode
import io.agora.realtimemusicclass.base.utils.NumberUtil
import io.agora.rtm.RtmStatusCode

class RMCUserManager(private val core: RMCCore,
                     private val stateListener: RMCUserIllegalStateListener? = null) {
    private val tag = "RMCUserManager"
    private var localUser: RMCUserInfo? = null

    var remoteCommandListener: RMCRemoteCommandCallback? = null

    private val cache = UserCache()
    private val onlineMediaIdSet = mutableSetOf<Int>()
    private val callbacks: MutableList<RMCUserCallback> = mutableListOf()

    fun addCallback(callback: RMCUserCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    fun removeCallback(callback: RMCUserCallback) {
        callbacks.remove(callback)
    }

    @Synchronized
    fun setLocalUserInfo(userInfo: RMCUserInfo) {
        this.localUser?.let {
            cache.removeUser(it)
        }
        this.localUser = userInfo.copy()
        cache.addUser(userInfo, true)
    }

    @Synchronized
    fun localUser(): RMCUserInfo? {
        return localUser?.copy()
    }

    private fun isLocalUser(userInfo: RMCUserInfo): Boolean {
        return this.localUser?.userName == userInfo.userName
    }

    @Synchronized
    fun setLocalMicState(state: Int) {
        localUser?.media?.micDeviceState = state
    }

    @Synchronized
    fun setLocalCameraState(state: Int) {
        localUser?.media?.cameraDeviceState = state
    }

    @Synchronized
    fun setLocalAudioStreamState(state: Int) {
        localUser?.media?.audioStreamState = state
    }

    @Synchronized
    fun setLocalVideoStreamState(state: Int) {
        localUser?.media?.videoStreamState = state
    }

    @Synchronized
    fun setLocalUserExt(key: String, value: Any) {
        localUser?.let {
            if (it.ext == null) {
                it.ext = mutableMapOf()
            }

            if (it.ext?.containsKey(key) == true) {
                it.ext!!.remove(key)
            }

            it.ext!![key] = value
        }
    }

    @Synchronized
    fun getUserInfo(userName: String): RMCUserInfo? {
        return cache.getUserInfo(userName)
    }

    @Synchronized
    fun userIsOnline(userInfo: RMCUserInfo): Boolean {
        return onlineMediaIdSet.contains(userInfo.media?.streamId)
    }

    @Synchronized
    fun userHasJoined(userInfo: RMCUserInfo): Boolean {
        return cache.contains(userInfo)
    }

    private val eventListener: RMCNotificationEventListener = object : RMCNotificationEventListener {
        override fun onGroupChat(fromId: String, message: String) {

        }

        override fun onUserUpdate(info: RMCUserInfo) {
            if (info.userName == localUser?.userName) {
                localUser!!.set(info)
            }
            cache.updateUser(info)
            callbacks.forEach {
                it.onUserUpdate(info)
            }
        }

        override fun onUserJoin(info: RMCUserInfo) {
            cache.addUser(info, isLocalUser(info))
            callbacks.forEach {
                it.onUserJoin(info)
            }
        }

        override fun onUserLeave(info: RMCUserInfo) {
            cache.removeUser(info)
            callbacks.forEach {
                it.onUserLeave(info)
            }
        }

        override fun onAudioPublished(published: Boolean) {
            if (published) localUser?.media?.audioStreamState = RMCStreamState.Publish.value
            else localUser?.media?.audioStreamState = RMCStreamState.Mute.value
            remoteCommandListener?.onLocalAudioShouldPublish(published)
        }

        override fun onVideoPublished(published: Boolean) {
            if (published) localUser?.media?.videoStreamState = RMCStreamState.Publish.value
            else localUser?.media?.videoStreamState = RMCStreamState.Mute.value
            remoteCommandListener?.onLocalVideoShouldPublish(published)
        }

        override fun onExtChanged(changedExt: String) {
            (Gson().fromJson(changedExt, Any::class.java) as? Map<String, Any>)?.let { map ->
                getUserAecMode(map).let { mode ->
                    core.audio().setLocalAecMode(mode)
                    setLocalUserExt("aecMode", mode.value)
                    localUser()?.let {
                        notifyUserUpdate(core.room().className, it)
                    }
                }
            }
        }
    }

    fun getUserAecMode(userInfo: RMCUserInfo): AECMode {
        return userInfo.ext?.let {
            getUserAecMode(it)
        } ?: AECMode.Standard
    }

    private fun getUserAecMode(map: Map<String, Any>): AECMode {
        return (map["aecMode"] as? Double)?.toInt()?.let {
            AECMode.fromValue(it)
        } ?: AECMode.Standard
    }

    private val rtcListener = object : RMCRtcEventListenerWithHighPriority() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            synchronized(this@RMCUserManager) {
                // Add my real server-assigned stream uid to media id set
                onlineMediaIdSet.add(uid)

                // 0 always means local user, if join successfully,
                // add 0 to local online set as well
                onlineMediaIdSet.add(0)
            }

            localUser()?.let { localUser ->
                callbacks.forEach { callback ->
                    callback.onUserOnline(localUser)
                }
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            onlineMediaIdSet.add(uid)

            // Only report user online when user has joined
            cache.getUserFromMediaId(uid)?.let { user ->
                callbacks.forEach { callback ->
                    callback.onUserOnline(user)
                }
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            onlineMediaIdSet.remove(uid)

            // Only report user offline when the user has joined
            cache.getUserFromMediaId(uid)?.let { user ->
                callbacks.forEach { callback ->
                    callback.onUserOffline(user)
                }
            }
        }
    }

    private val rtmListener = object : RMCRtmEventListener() {
        override fun onConnectionStateChanged(p0: Int, p1: Int) {
            if (p0 == RtmStatusCode.ConnectionState.CONNECTION_STATE_ABORTED &&
                    p1 == RtmStatusCode.ConnectionChangeReason.CONNECTION_CHANGE_REASON_REMOTE_LOGIN) {
                stateListener?.onRemoteDeviceJoined()
            }
        }
    }

    init {
        core.registerNotificationEventListener(eventListener)
        core.registerRtcEventListener(rtcListener)
        core.registerRtmEventListener(rtmListener)
    }

    fun applyLocalUserRole(role: RMCUserRole) {
        core.engine().applyUserRole(
            role === RMCUserRole.ROLE_TYPE_TEACHER ||
                    role === RMCUserRole.ROLE_TYPE_STUDENT
        )
    }

    internal fun refreshUserInfoList(className: String,
                                     local: RMCUserInfo,
                                     role: RMCUserRole?,
                                     callback: RMCCallback<List<RMCUserInfo>>? = null) {
        val sRole = role?.let { toServerRole(role) }
        core.service().getClassUserInfoList(className, sRole,
            object : RMCCallback<List<RMCUserInfo>> {
                override fun onSuccess(res: List<RMCUserInfo>?) {
                    res?.let { cache.reset(it, local) }
                    callback?.onSuccess(res)
                }

                override fun onFailure(error: RMCError) {
                    callback?.onFailure(error)
                }
            })
    }

    fun getUserInfoFromMediaId(uid: Int): RMCUserInfo? {
        return if (uid == 0) {
            core.user().localUser
        } else {
            cache.getUserFromMediaId(uid)
        }
    }

    @Synchronized
    fun getUserInfoList(): List<RMCUserInfo> {
        return cache.list()
    }

    @Synchronized
    fun getHostUserInfoList(): List<RMCUserInfo> {
        val list = cache.list().toMutableList()
        list.removeAll {
            it.role == RMCUserRole.ROLE_TYPE_AUDIENCE.value ||
                    it.role == RMCUserRole.ROLE_TYPE_UNKNOWN.value ||
                    it.userName == localUser?.userName
        }
        return list
    }

    fun notifyUserUpdate(className: String?, info: RMCUserInfo, callback: RMCCallback<Void>? = null) {
        val mediaInfo = info.media
        val micState: Int? = mediaInfo?.micDeviceState
        val cameraState: Int? = mediaInfo?.cameraDeviceState
        val audioState: Int? = mediaInfo?.audioStreamState
        val videoState: Int? = mediaInfo?.videoStreamState
        val streamId: String? = mediaInfo?.streamId?.let { NumberUtil.stringFromInt(it) }

        val request = UserInfoUpdateRequest(
            className!!, info.userName,
            info.avatar, info.gender,
            cameraState, micState,
            videoState, audioState,
            streamId, info.ext)

        core.service().updateUserInfo(request, object : RMCCallback<String> {
            override fun onSuccess(res: String?) {
                callback?.onSuccess(null)
                core.notification()?.userUpdate(info)
            }

            override fun onFailure(error: RMCError) {
                callback?.onFailure(error)
            }
        })
    }

    fun publishRemoteUserMic(published: Boolean, peerId: String) {
        val control = RMCUserControl("mic", published)
        core.notification()?.sendUserControlMessage(peerId, control)
    }

    fun publishRemoteUserCamera(published: Boolean, peerId: String) {
        val control = RMCUserControl("camera", published)
        core.notification()?.sendUserControlMessage(peerId, control)
    }

    fun recycle() {
        callbacks.clear()
        onlineMediaIdSet.clear()
        core.removeNotificationEventListener(eventListener)
        core.removeRtcEventListener(rtcListener)
        core.removeRtmEventListener(rtmListener)
        cache.recycle()
    }
}

internal class UserCache {
    private val userInfoListCache = mutableListOf<RMCUserInfo>()
    private val userInfoMap = mutableMapOf<String, RMCUserInfo>()

    // Assume that a user will not obtain media states once
    // not assigned by server. This will probably be not the
    // case for future requirements.
    private val userMediaMap = mutableMapOf<Int, RMCUserInfo>()

    @Synchronized
    fun addUser(userInfo: RMCUserInfo, isLocal: Boolean = false) {
        if (!userInfoMap.containsKey(userInfo.userName)) {
            userInfoListCache.add(userInfo)
            userInfoMap[userInfo.userName] = userInfo

            userInfo.media?.let {
                if (!isLocal && it.streamId == 0) {
                    // Only local media can has a zero stream uid,
                    // and at meantime, local user will have both
                    // a zero media uid and an actual media uid
                    return@let
                }
                userMediaMap[it.streamId] = userInfo
            }
        }
    }

    @Synchronized
    fun removeUser(userInfo: RMCUserInfo) {
        userInfoListCache.remove(userInfo)
        userInfoMap.remove(userInfo.userName)

        // Here we ignore local user's 0 uid, for
        // the only case where we remove local user
        // is when the local user wants to leave
        userInfo.media?.let {
            userMediaMap.remove(it.index)
        }
    }

    @Synchronized
    fun updateUser(userInfo: RMCUserInfo) {
        userInfoMap[userInfo.userName]?.let { info ->
            info.media?.let { media ->
                if (media.streamId != 0) {
                    userMediaMap.remove(media.streamId)
                }
            }

            info.avatar = userInfo.avatar
            info.role = userInfo.role
            info.media = userInfo.media

            userInfo.media?.let { media ->
                if (media.streamId != 0) {
                    userMediaMap[media.streamId] = info
                }
            }
        }
    }

    @Synchronized
    fun contains(userInfo: RMCUserInfo): Boolean {
        return userInfoMap.containsKey(userInfo.userName)
    }

    @Synchronized
    fun getUserInfo(userName: String): RMCUserInfo? {
        return userInfoMap[userName]?.copy()
    }

    fun getUserFromMediaId(uid: Int): RMCUserInfo? {
        return userMediaMap[uid]
    }

    @Synchronized
    fun reset(list: List<RMCUserInfo>, local: RMCUserInfo? = null) {
        recycle()
        list.forEach {
            addUser(it, it.userName == local?.userName)
        }
    }

    fun list(): List<RMCUserInfo> {
        return userInfoListCache
    }

    @Synchronized
    fun recycle() {
        userInfoListCache.clear()
        userInfoMap.clear()
        userMediaMap.clear()
    }
}

abstract class RMCUserCallback {
    open fun onUserUpdate(info: RMCUserInfo) {

    }

    open fun onUserJoin(info: RMCUserInfo) {

    }

    open fun onUserLeave(info: RMCUserInfo) {

    }

    /**
     * Remote rtc user joins the rtc channel and
     * is ready to publish video and audio streams
     */
    open fun onUserOnline(info: RMCUserInfo) {

    }

    /**
     * Remote rtc user leaves the rtc channel but
     * does not leave the class.
     * In this case the user couldn't publish
     * video or audio streams
     */
    open fun onUserOffline(info: RMCUserInfo) {

    }
}

interface RMCUserIllegalStateListener {
    fun onRemoteDeviceJoined()
}

interface RMCRemoteCommandCallback {
    fun onLocalAudioShouldPublish(published: Boolean)

    fun onLocalVideoShouldPublish(published: Boolean)
}