package io.agora.realtimemusicclass.base.edu.core.sync

import io.agora.realtimemusicclass.base.edu.core.data.RMCMediaInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.utils.NumberUtil

data class RMCSyncMessage(
    val type: Int,
    val msg: String?
)

data class RMCSyncDataMessage<T>(
    val type: Int,
    val msg: T?
)

enum class RMCSyncCmd(val value: Int) {
    SingleChat(0),
    GroupChat(1),
    UserUpdate(2),
    UserJoin(3),
    UserLeave(4),
    UserCtrl(5),
}

data class RMCNotifyMediaInfo(
    var index: Int,
    var micDeviceState: Int,
    var audioStreamState: Int,
    var cameraDeviceState: Int,
    var videoStreamState: Int,
    var streamId: String) {

    companion object {
        fun from(mediaInfo: RMCMediaInfo): RMCNotifyMediaInfo {
            return RMCNotifyMediaInfo(
                mediaInfo.index,
                mediaInfo.micDeviceState,
                mediaInfo.audioStreamState,
                mediaInfo.cameraDeviceState,
                mediaInfo.videoStreamState,
                NumberUtil.stringFromInt(mediaInfo.streamId))
        }

        fun to(mediaInfo: RMCNotifyMediaInfo): RMCMediaInfo {
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

data class RMCNotifyUserInfo(
    var userName: String,
    var role: Int,
    var avatar: String?,
    var gender: Int,
    var media: RMCNotifyMediaInfo? = null,
    var ext: MutableMap<String, Any>? = null) {

    companion object {
        fun from(userInfo: RMCUserInfo): RMCNotifyUserInfo {
            return RMCNotifyUserInfo(
                userInfo.userName,
                userInfo.role,
                userInfo.avatar,
                userInfo.gender,
                userInfo.media?.let { RMCNotifyMediaInfo.from(it) },
                userInfo.ext)
        }

        fun to(userInfo: RMCNotifyUserInfo): RMCUserInfo {
            return RMCUserInfo(
                userInfo.userName,
                userInfo.role,
                userInfo.avatar,
                userInfo.gender,
                userInfo.media?.let { RMCNotifyMediaInfo.to(it) },
                userInfo.ext)
        }
    }
}