package io.agora.realtimemusicclass.base.server.struct.body

data class ClassJoinBody(
    val name: String,
    val role: Int,
    val avatar: String?,
    val password: String?)