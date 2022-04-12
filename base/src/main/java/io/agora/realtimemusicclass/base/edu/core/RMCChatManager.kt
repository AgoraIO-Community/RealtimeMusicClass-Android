package io.agora.realtimemusicclass.base.edu.core

import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole
import io.agora.realtimemusicclass.base.edu.core.sync.RMCNotificationEventListener
import java.util.*

class RMCChatManager(private val core: RMCCore,
                     private val limit: Int = 20) {
    private val cache = LinkedList<RMCChatItem>()
    private val chatCallbacks: MutableList<RMCChatCallback> = mutableListOf()

    private val eventListener = object : RMCNotificationEventListener {
        override fun onGroupChat(fromId: String, message: String) {
            chatCallbacks.forEach {
                it.onGroupChat(fromId, message)
            }
        }

        override fun onUserUpdate(info: RMCUserInfo) {}

        override fun onUserJoin(info: RMCUserInfo) {}

        override fun onUserLeave(info: RMCUserInfo) {}

        override fun onAudioPublished(published: Boolean) {}

        override fun onVideoPublished(published: Boolean) {}

        override fun onExtChanged(changedExt: String) {}
    }

    init {
        core.registerNotificationEventListener(eventListener)
    }

    fun addChatCallback(callback: RMCChatCallback) {
        if (!chatCallbacks.contains(callback)) {
            chatCallbacks.add(callback)
        }
    }

    fun removeChatCallback(callback: RMCChatCallback) {
        chatCallbacks.remove(callback)
    }

    /**
     * Add a chat message to local cache, used
     * when receiving remote chat messages
     */
    fun addChat(name: String, role: RMCUserRole, message: String) {
        addChat(Direct.RECV, name, role, message)
    }

    /**
     * Add this message to local cache and send
     * this message to remote users, used
     * when sending local chat messages.
     */
    fun sendChat(name: String, role: RMCUserRole, message: String) {
        addChat(Direct.SEND, name, role, message)
        core.notification()?.groupChat(message)
    }

    @Synchronized
    private fun addChat(direct: Direct, name: String, role: RMCUserRole, message: String) {
        val item = RMCChatItem(direct, name, role, message)
        cache.add(item)

        if (cache.size > limit) {
            cache.pollFirst()
        }
    }

    fun list(): List<RMCChatItem> {
        return cache.toList()
    }

    fun size(): Int {
        return cache.size
    }

    @Synchronized
    fun recycle() {
        cache.clear()
        chatCallbacks.clear()
    }
}

abstract class RMCChatCallback {
    open fun onGroupChat(fromId: String, message: String) {

    }
}

data class RMCChatItem(
    val direct: Direct,
    val userName: String,
    val role: RMCUserRole,
    val message: String)

enum class Direct(val value: Int) {
    SEND(0),
    RECV(1),
}
