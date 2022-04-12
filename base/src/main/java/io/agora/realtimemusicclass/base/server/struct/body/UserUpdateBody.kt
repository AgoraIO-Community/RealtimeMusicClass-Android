package io.agora.realtimemusicclass.base.server.struct.body

data class UserUpdateBody(
    val avatar: String?,
    val gender: Int?,
    val videoDeviceState: Int?,
    val micDeviceState: Int?,
    val videoStreamState: Int?,
    val audioStreamState: Int?,
    val streamId: String?,
    val ext: Map<String, Any>?)