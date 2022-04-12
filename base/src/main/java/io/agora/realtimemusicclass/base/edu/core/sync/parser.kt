package io.agora.realtimemusicclass.base.edu.core.sync

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserControl

class RMCSyncRtmChannelMessageParser {
    private val tag = "RmcSyncParser"

    private val listeners = mutableListOf<RMCNotificationEventListener>()

    internal fun addListener(listener: RMCNotificationEventListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    internal fun removeListener(listener: RMCNotificationEventListener) {
        listeners.remove(listener)
    }

    fun parse(rtmMsg: String, fromId: String) {
        return try {
            val msg = Gson().fromJson(rtmMsg, RMCSyncMessage::class.java)
            Log.d(tag, "sync message received: $msg")
            dispatchEvent(msg, fromId)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
    }

    private fun dispatchEvent(message: RMCSyncMessage, fromId: String) {
        when (message.type) {
            RMCSyncCmd.GroupChat.value -> callbackGroupChat(message, fromId)
            RMCSyncCmd.UserJoin.value -> callbackUserJoin(message)
            RMCSyncCmd.UserLeave.value -> callbackUserLeave(message)
            RMCSyncCmd.UserUpdate.value -> callbackUserUpdate(message)
            RMCSyncCmd.UserCtrl.value -> callbackUserCtrl(message)
        }
    }

    private fun callbackGroupChat(message: RMCSyncMessage, fromId: String) {
        listeners.forEach {
            it.onGroupChat(fromId, message.msg ?: "")
        }
    }

    private fun callbackUserJoin(message: RMCSyncMessage) {
        val info = try {
            Gson().fromJson(message.msg, RMCNotifyUserInfo::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }

        if (info != null) {
            listeners.forEach {
                it.onUserJoin(RMCNotifyUserInfo.to(info))
            }
        }
    }

    private fun callbackUserLeave(message: RMCSyncMessage) {
        val info = try {
            Gson().fromJson(message.msg, RMCNotifyUserInfo::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }

        if (info != null) {
            listeners.forEach {
                it.onUserLeave(RMCNotifyUserInfo.to(info))
            }
        }
    }

    private fun callbackUserUpdate(message: RMCSyncMessage) {
        val info = try {
            Gson().fromJson(message.msg, RMCNotifyUserInfo::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }

        if (info != null) {
            listeners.forEach {
                it.onUserUpdate(RMCNotifyUserInfo.to(info))
            }
        }
    }

    private fun callbackUserCtrl(message: RMCSyncMessage) {
        val ctrl = try {
            Gson().fromJson(message.msg, RMCUserControl::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }

        if (ctrl != null) {
            if (ctrl.ctrlType == "mic" && ctrl.action is Boolean) {
                listeners.forEach {
                    it.onAudioPublished(ctrl.action)
                }
            } else if (ctrl.ctrlType == "camera" && ctrl.action is Boolean) {
                listeners.forEach {
                    it.onVideoPublished(ctrl.action)
                }
            } else if (ctrl.ctrlType == "updateExt") {
                listeners.forEach {
                    it.onExtChanged(ctrl.action as String)
                }
            }
        }
    }
}