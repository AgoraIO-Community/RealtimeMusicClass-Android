package io.agora.realtimemusicclass.base.edu.classroom

import io.agora.realtimemusicclass.base.edu.core.data.RMCClassInfo
import io.agora.realtimemusicclass.base.server.struct.response.ClassBriefInfoResp

data class ClassCreateRequest(
    val className: String,
    val creator: String,
    val password: String?)

object ClassManagerDataConvertor {
    fun toRMCRoomInfo(resp: ClassBriefInfoResp?): RMCClassInfo? {
        return resp?.let {
            val info =
                RMCClassInfo(
                    it.className,
                    it.channelID,
                    it.creator,
                    it.hasPasswd,
                    it.count
                )
            info
        }
    }
}