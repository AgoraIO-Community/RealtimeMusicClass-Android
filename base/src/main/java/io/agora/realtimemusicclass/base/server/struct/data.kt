package io.agora.realtimemusicclass.base.server.struct

import io.agora.realtimemusicclass.base.edu.core.data.RMCMediaInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.server.network.ResponseBody
import io.agora.realtimemusicclass.base.utils.NumberUtil

class ServerResponseBody<T> : ResponseBody<String>() {
    var data: T? = null
}

interface EduCallback<T> {
    fun onSuccess(res: T?)

    fun onFailure(error: EduError)
}

data class EduError(
        val type: Int,
        val msg: String) {
    var httpError: Int? = 0

    constructor(type: Int, msg: String, httpError: Int?) : this(type, msg) {
        this.httpError = httpError
    }

    companion object {
        fun customMsgError(msg: String?): EduError {
            return EduError(-1, msg ?: "")
        }
    }
}

enum class ServerUserRole(val value: Int) {
    Owner(0),
    CoHost(1),
    Audience(2);

    companion object {
        fun toRoleString(role: ServerUserRole): String {
            return when (role) {
                Owner -> "owner"
                CoHost -> "cohost"
                Audience -> "audience"
            }
        }

        fun toRoleStringFromValue(value: Int): String {
            return when (value) {
                Owner.value -> "owner"
                CoHost.value -> "cohost"
                else -> "audience"
            }
        }
    }
}

data class RMCServerMediaInfo(
    var index: Int,
    var micDeviceState: Int,
    var audioStreamState: Int,
    var cameraDeviceState: Int,
    var videoStreamState: Int,
    var streamId: String) {

    companion object {
        fun from(mediaInfo: RMCMediaInfo): RMCServerMediaInfo {
            return RMCServerMediaInfo(
                mediaInfo.index,
                mediaInfo.micDeviceState,
                mediaInfo.audioStreamState,
                mediaInfo.cameraDeviceState,
                mediaInfo.videoStreamState,
                NumberUtil.stringFromInt(mediaInfo.streamId))
        }

        fun to(mediaInfo: RMCServerMediaInfo): RMCMediaInfo {
            return RMCMediaInfo(
                mediaInfo.index,
                mediaInfo.micDeviceState,
                mediaInfo.audioStreamState,
                mediaInfo.cameraDeviceState,
                mediaInfo.videoStreamState,
                NumberUtil.intFromString(mediaInfo.streamId))
        }
    }
}

data class RMCServerUserInfo(
    var userName: String,
    var role: Int,
    var avatar: String?,
    var gender: Int,
    var media: RMCServerMediaInfo? = null,
    var ext: MutableMap<String, Any>? = null) {

    companion object {
        fun from(userInfo: RMCUserInfo): RMCServerUserInfo {
            return RMCServerUserInfo(
                userInfo.userName,
                userInfo.role,
                userInfo.avatar,
                userInfo.gender,
                userInfo.media?.let { RMCServerMediaInfo.from(it) },
                userInfo.ext)
        }

        fun to(userInfo: RMCServerUserInfo): RMCUserInfo {
            return RMCUserInfo(
                userInfo.userName,
                userInfo.role,
                userInfo.avatar,
                userInfo.gender,
                userInfo.media?.let { RMCServerMediaInfo.to(it) },
                userInfo.ext)
        }
    }
}