package io.agora.realtimemusicclass.base.edu.core

import io.agora.realtimemusicclass.base.edu.core.data.RMCMediaInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole
import io.agora.realtimemusicclass.base.server.struct.RMCServerMediaInfo
import io.agora.realtimemusicclass.base.server.struct.response.UserInfoResp
import io.agora.realtimemusicclass.base.utils.NumberUtil

data class UserInfoUpdateRequest(
    val className: String,
    val userName: String,
    val avatar: String?,
    val gender: Int?,
    val videoDeviceState: Int?,
    val micDeviceState: Int?,
    val videoStreamState: Int?,
    val audioStreamState: Int?,
    val streamId: String?,
    val ext: Map<String, Any>?)

object RMCServiceDataTransformer {
    fun toRMCMediaInfo(info: RMCServerMediaInfo?): RMCMediaInfo? {
        return info?.let {
            val rmcInfo = RMCMediaInfo(
                it.index,
                it.micDeviceState,
                it.audioStreamState,
                it.cameraDeviceState,
                it.videoStreamState,
                NumberUtil.intFromString(it.streamId))
            rmcInfo
        }
    }

    fun toRMCUserInfo(info: UserInfoResp?): RMCUserInfo? {
        return info?.let {
            return RMCUserInfo(
                info.userName,
                info.role,
                info.avatar,
                info.gender,
                toRMCMediaInfo(info.media),
                info.ext?.toMutableMap())
        }
    }

    fun toServerRole(role: RMCUserRole): String {
        return when (role) {
            RMCUserRole.ROLE_TYPE_TEACHER -> "owner"
            RMCUserRole.ROLE_TYPE_STUDENT -> "cohost"
            else -> "audience"
        }
    }
}