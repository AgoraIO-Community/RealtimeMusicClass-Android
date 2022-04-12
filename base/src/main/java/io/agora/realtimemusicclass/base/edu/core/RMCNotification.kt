package io.agora.realtimemusicclass.base.edu.core

import android.util.Log
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserControl
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.edu.core.sync.RMCNotificationEventListener
import io.agora.realtimemusicclass.base.edu.core.sync.RMCSyncMessageFactory
import io.agora.realtimemusicclass.base.edu.core.sync.RMCSyncRtmChannelMessageParser
import io.agora.rtm.*

class RMCNotification(private val core: RMCCore) {
    private val tag = "RmcNotification"
    private val factory = RMCSyncMessageFactory()
    private val parser = RMCSyncRtmChannelMessageParser()


    private var rtmChannel: RtmChannel? = null
    private var rtmClient: RtmClient? = null

    private val msgReceiveListener: RMCRtmEventListener = object : RMCRtmEventListener() {
        override fun onMessageReceived(p0: RtmMessage, p1: String) {
            Log.d(tag, "peer message received: ${p0.text}, from $p1")
            parser.parse(p0.text, p1)
        }

        override fun onMessageReceived(p0: RtmMessage, p1: RtmChannelMember) {
            Log.d(tag, "channel message received: ${p0.text}, from ${p1.userId}")
            parser.parse(p0.text, p1.userId)
        }
    }

    init {
        core.registerRtmEventListener(msgReceiveListener)
    }

    fun initRtmContext(rtmChannel: RtmChannel,
                       rtmClient: RtmClient) {
        this.rtmChannel = rtmChannel
        this.rtmClient = rtmClient
    }

    fun groupChat(message: String) {
        val rtmMsgText = factory.buildGroupChatMessage(message)
        sendChannelRtmMessage(rtmMsgText)
    }

    fun userJoin(userInfo: RMCUserInfo) {
        val rtmMsgText = factory.buildUserJoinMessage(userInfo)
        sendChannelRtmMessage(rtmMsgText)
    }

    fun userLeave(userInfo: RMCUserInfo) {
        val rtmMsgText = factory.buildUserLeaveMessage(userInfo)
        sendChannelRtmMessage(rtmMsgText)
    }

    fun userUpdate(userInfo: RMCUserInfo) {
        val rtmMsgText = factory.buildUserUpdateMessage(userInfo)
        sendChannelRtmMessage(rtmMsgText)
    }

    fun sendUserControlMessage(user: String, userControl: RMCUserControl) {
        val rtmMsgText = factory.buildUserMediaForbiddenMessage(userControl)
        sendPeerRtmMessage(user, rtmMsgText)
    }

    private fun sendChannelRtmMessage(text: String) {
        rtmClient?.createMessage(text)?.let { message ->
            val option = SendMessageOptions()
            option.enableOfflineMessaging = false

            rtmChannel?.sendMessage(message, option, object : ResultCallback<Void> {
                override fun onSuccess(p0: Void?) {
                    Log.d(tag, "channel ${rtmChannel!!.id}, message sent: $text")
                }

                override fun onFailure(p0: ErrorInfo?) {
                    Log.e(tag, "channel message send failed: $text, error : ${p0?.errorDescription}")
                }
            })
        }
    }

    private fun sendPeerRtmMessage(peerId: String, text: String) {
        rtmClient?.createMessage(text)?.let { message ->
            val option = SendMessageOptions()
            option.enableOfflineMessaging = false

            rtmClient?.sendMessageToPeer(peerId, message, option, object : ResultCallback<Void> {
                override fun onSuccess(p0: Void?) {
                    Log.d(tag, "peer message sent: $text")
                }

                override fun onFailure(p0: ErrorInfo?) {
                    Log.e(tag, "send to $peerId message send failed: $text, error : ${p0?.errorDescription}")
                }
            })
        }
    }

    internal fun addEventListener(listener: RMCNotificationEventListener) {
        parser.addListener(listener)
    }

    internal fun removeEventListener(listener: RMCNotificationEventListener) {
        parser.removeListener(listener)
    }

    fun recycle() {
        core.removeRtmEventListener(msgReceiveListener)
    }
}


