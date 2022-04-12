package io.agora.realtimemusicclass.base.edu.core.data;

data class RMCClassInfo(
    var className: String,
    var channelID: String,
    var creator: String,
    var hasPasswd : Boolean,
    var count: Int,
    var ext: MutableMap<String, Any>? = null)