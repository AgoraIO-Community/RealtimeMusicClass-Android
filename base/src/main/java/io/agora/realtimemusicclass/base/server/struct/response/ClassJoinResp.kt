package io.agora.realtimemusicclass.base.server.struct.response

import io.agora.realtimemusicclass.base.server.struct.RMCServerMediaInfo

data class ClassJoinResp(
    val className: String,
    val userName: String,
    val role: Int,
    val avatar: String?,
    val gender: Int,
    val media: RMCServerMediaInfo?,
    val ext: Map<String, Any>?)