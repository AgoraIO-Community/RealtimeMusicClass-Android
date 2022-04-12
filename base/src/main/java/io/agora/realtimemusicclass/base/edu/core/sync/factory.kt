package io.agora.realtimemusicclass.base.edu.core.sync

import com.google.gson.Gson
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserControl
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo

class RMCSyncMessageFactory {
    private val gson = Gson()

    fun buildGroupChatMessage(message: String): String {
        val data = RMCSyncMessage(RMCSyncCmd.GroupChat.value, message)
        return gson.toJson(data)
    }

    fun buildUserJoinMessage(info: RMCUserInfo): String {
        val msg = gson.toJson(RMCNotifyUserInfo.from(info))
        val data = RMCSyncMessage(RMCSyncCmd.UserJoin.value, msg)
        return gson.toJson(data)
    }

    fun buildUserLeaveMessage(info: RMCUserInfo): String {
        val msg = gson.toJson(RMCNotifyUserInfo.from(info))
        val data = RMCSyncMessage(RMCSyncCmd.UserLeave.value, msg)
        return gson.toJson(data)
    }

    fun buildUserUpdateMessage(info: RMCUserInfo): String {
        val msg = gson.toJson(RMCNotifyUserInfo.from(info))
        val data = RMCSyncDataMessage(RMCSyncCmd.UserUpdate.value, msg)
        return gson.toJson(data)
    }

    fun buildUserMediaForbiddenMessage(control: RMCUserControl): String {
        val msg = gson.toJson(control)
        val data = RMCSyncDataMessage(RMCSyncCmd.UserCtrl.value, msg)
        return gson.toJson(data)
    }
}