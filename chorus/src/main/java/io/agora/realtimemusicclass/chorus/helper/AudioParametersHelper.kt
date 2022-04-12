package io.agora.realtimemusicclass.chorus.helper

import android.content.Context
import io.agora.realtimemusicclass.chorus.R
import io.agora.realtimemusicclass.base.edu.core.RMCCore
import io.agora.realtimemusicclass.base.edu.core.RMCUserCallback
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserControl
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole
import io.agora.realtimemusicclass.base.ui.actions.AECMode
import io.agora.realtimemusicclass.base.ui.actions.UserItem
import io.agora.realtimemusicclass.base.ui.actions.VolumeItem

class AudioParametersHelper(
    role: RMCUserRole,
    private val context: Context,
    private val core: RMCCore
) {
    private val defaultVolume = 100
    private val commonMaxVolume = 100
    private val remoteUserMaxVolume = 400
    private val userList = mutableListOf<UserItem>()
    private val volumeList = mutableListOf<VolumeItem>()

    private val userCallback = object : RMCUserCallback() {
        override fun onUserUpdate(info: RMCUserInfo) {
            updateVolumeItem(info)
        }

        override fun onUserJoin(info: RMCUserInfo) {
            addUserItem(info)
            addVolumeItem(info)
        }

        @Synchronized
        private fun addUserItem(info: RMCUserInfo) {
            var find = false
            userList.forEach {
                if (it.name == info.userName) {
                    find = true
                }
            }

            if (!find) {
                var mode = AECMode.None
                var hasSwitch = false
                if (info.isTeacher() || info.isStudent()) {
                    mode = AECMode.Standard
                    hasSwitch = true
                }
                userList.add(
                    UserItem(
                        mode,
                        RMCUserRole.fromValue(info.role),
                        core.user().userIsOnline(info),
                        hasSwitch,
                        info.userName
                    )
                )
            }
        }

        @Synchronized
        private fun addVolumeItem(info: RMCUserInfo) {
            if (info.role == RMCUserRole.ROLE_TYPE_AUDIENCE.value ||
                    info.role == RMCUserRole.ROLE_TYPE_AUDIENCE.value) {
                return
            }

            var find = false
            volumeList.forEach {
                if (it.type == info.userName) {
                    find = true
                }
            }

            //  only teacher or student can adjust volume
            if (!find) {
                volumeList.add(VolumeItem(info.media?.streamId!!,
                    defaultVolume,
                    remoteUserMaxVolume,
                    !info.audioStreamMuted() && core.user().userIsOnline(info),
                    info.userName)
                )
            }
        }

        @Synchronized
        private fun updateVolumeItem(info: RMCUserInfo) {
            volumeList.forEach {
                if (it.type == info.userName) {
                    it.id = info.media?.streamId!!
                    it.enabled = !info.media?.audioStreamMuted()!! && core.user().userIsOnline(info)
                }
            }
        }

        override fun onUserLeave(info: RMCUserInfo) {
            removeItem(info)
        }

        @Synchronized
        private fun removeItem(info: RMCUserInfo) {
            userList.removeAll {
                it.name == info.userName
            }
            volumeList.removeAll {
                it.type == info.userName
            }
        }

        @Synchronized
        private fun onUserIfOnline(online: Boolean, info: RMCUserInfo) {
            userList.forEach {
                if (it.name == info.userName) {
                    it.onLine = online
                    return@forEach
                }
            }
            volumeList.forEach {
                if (it.type == info.userName) {
                    it.enabled = online
                    return@forEach
                }
            }
        }

        override fun onUserOnline(info: RMCUserInfo) {
            onUserIfOnline(true, info)
        }

        override fun onUserOffline(info: RMCUserInfo) {
            onUserIfOnline(false, info)
        }
    }

    init {
        core.user().addCallback(userCallback)
        buildDefaultVolumeList(role)
        buildDefaultUserList()
    }

    fun userList(): List<UserItem> {
        return userList
    }

    fun volumeList(): List<VolumeItem> {
        return volumeList
    }

    @Synchronized
    fun updateVolumeItem(volume: Int, name: String) {
        volumeList.forEach {
            if (it.type == name) {
                it.currentValue = volume
            }
        }
    }

    @Synchronized
    fun changeUserAECMode(mode: AECMode, name: String) {
        userList.forEach {
            if (it.name == name) {
                it.mode = mode
            }
        }

        core.user().localUser()?.let { local ->
            if (local.userName == name) {
                core.audio().setLocalAecMode(mode)
            } else {
                core.user().getUserInfo(name)?.let {
                    sendRemoteUserAECControl(it, mode)
                }
            }
        }
    }

    private fun sendRemoteUserAECControl(userInfo: RMCUserInfo, mode: AECMode) {
        core.notification()?.sendUserControlMessage(
            userInfo.userName, buildUserControlMessage(mode))
    }

    private fun buildUserControlMessage(mode: AECMode): RMCUserControl {
        return RMCUserControl("updateExt", "{\"aecMode\": ${mode.value}}")
    }

    @Synchronized
    private fun buildDefaultVolumeList(role: RMCUserRole) {
        if (role == RMCUserRole.ROLE_TYPE_TEACHER ||
            role == RMCUserRole.ROLE_TYPE_STUDENT
        ) {
            // local mic
            VolumeItem(0, defaultVolume, commonMaxVolume,true, context.getString(R.string.volume_mic))
                .let { volumeList.add(it) }
            // ear monitor
            VolumeItem(1, defaultVolume, commonMaxVolume,true, context.getString(R.string.volume_ear_monitor))
                .let { volumeList.add(it) }
        }
        // bgm
        VolumeItem(volumeList.size, defaultVolume, commonMaxVolume,true, context.getString(R.string.volume_bgm))
            .let { volumeList.add(it) }

        // default user
        core.user().getHostUserInfoList().forEach {
            val enable = !it.audioStreamMuted() && core.user().userIsOnline(it)
            volumeList.add(
                VolumeItem(
                    it.media?.streamId!!,
                    defaultVolume,
                    remoteUserMaxVolume,
                    enable,
                    it.userName
                )
            )
        }
    }

    @Synchronized
    private fun buildDefaultUserList() {
        core.user().getUserInfoList().forEach {
            var mode = AECMode.None
            var hasSwitch = false
            if (it.role == RMCUserRole.ROLE_TYPE_STUDENT.value ||
                it.role == RMCUserRole.ROLE_TYPE_TEACHER.value
            ) {
                mode = AECMode.Standard
                hasSwitch = true
            }

            userList.add(
                UserItem(
                    mode,
                    RMCUserRole.fromValue(it.role),
                    core.user().userIsOnline(it),
                    hasSwitch,
                    it.userName
                )
            )
        }
    }
}