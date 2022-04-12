package io.agora.realtimemusicclass.base.edu.core.sync

import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo

internal interface RMCNotificationEventListener {
    fun onGroupChat(fromId: String, message: String)

    fun onUserUpdate(info: RMCUserInfo)

    fun onUserJoin(info: RMCUserInfo)

    fun onUserLeave(info: RMCUserInfo)

    fun onAudioPublished(published: Boolean)

    fun onVideoPublished(published: Boolean)

    fun onExtChanged(changedExt: String)
}