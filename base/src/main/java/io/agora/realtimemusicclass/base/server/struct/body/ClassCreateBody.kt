package io.agora.realtimemusicclass.base.server.struct.body

data class ClassCreateBody(
    val className: String,
    val creator: String,
    val password: String?)