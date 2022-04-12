package io.agora.realtimemusicclass.base.server.struct.response

data class ClassBriefInfoResp(
    val className: String,
    val channelID: String,
    val creator: String,
    val hasPasswd: Boolean,
    val count: Int)