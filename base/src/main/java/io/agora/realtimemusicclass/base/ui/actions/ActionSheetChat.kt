package io.agora.realtimemusicclass.base.ui.actions

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.realtimemusicclass.base.MusicClassApp
import io.agora.realtimemusicclass.base.R
import io.agora.realtimemusicclass.base.edu.core.RMCChatItem
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo

class ActionSheetChat(
    context: Context,
    app: MusicClassApp,
    type: ActionSheetType,
    listener: DefaultActionSheetListener? = null
) : ActionSheet(context, app, type, listener) {

    private val recycler: RecyclerView
    private var inputView: AppCompatEditText? = null
    private val adapter: ActionSheetChatMessageAdapter

    init {
        LayoutInflater.from(context).inflate(
            R.layout.bottom_action_chat, this
        )
        // message list
        recycler = findViewById(R.id.action_dialog_chat_message_list)
        recycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL, false
        )

        inputView = findViewById(R.id.action_dialog_chat_input_view)
        inputView?.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (v.text.isNotEmpty()) {
                    (listener as ActionSheetChatListener?)?.let { listener ->
                        val userName = listener.getLocalUserInfo().userName
                        // send message
                        listener.sendMessage(userName, v.text.toString())
                    }
                    // clear editor text
                    v.text = null
                    return@setOnEditorActionListener true
                }
            }
            return@setOnEditorActionListener false
        }

        adapter = ActionSheetChatMessageAdapter(context, listener as? ActionSheetChatListener)
        recycler.adapter = adapter
    }

    fun refreshMessageList(list: List<RMCChatItem>) {
        adapter.refreshMessageList(list)
        recycler.scrollToPosition(adapter.itemCount - 1)
    }
}

class ActionSheetChatMessageAdapter(
    private val context: Context,
    private val listener: ActionSheetChatListener? = null
) : RecyclerView.Adapter<ActionSheetChatMessageItemHolder>() {
    companion object {
        const val DIRECT_TXT_SEND: Int = 0
        const val DIRECT_TXT_REC: Int = 1
    }

    private var messageList = mutableListOf<RMCChatItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionSheetChatMessageItemHolder {
        return getViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: ActionSheetChatMessageItemHolder, position: Int) {
        if (messageList.isNotEmpty()) {
            val pos = holder.adapterPosition
            val item = messageList[pos]
            if (item.role.isTeacher()) {
                holder.role.visibility = View.VISIBLE
            }
            //holder.avatar.setImageBitmap()
            holder.name.text = item.userName
            holder.message.text = item.message
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun getItemViewType(position: Int): Int {
        return getItem(position).direct.value
    }

    private fun getItem(position: Int): RMCChatItem {
        return messageList[position]
    }

    private fun getViewHolder(parent: ViewGroup, viewType: Int): ActionSheetChatMessageItemHolder {
        return when (viewType) {
            DIRECT_TXT_SEND -> ActionSheetChatMessageItemHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.bottom_action_chat_send_message_item, parent, false),
            )
            DIRECT_TXT_REC -> ActionSheetChatMessageItemHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.bottom_action_chat_recv_message_item, parent, false),
            )
            else -> {
                ActionSheetChatMessageItemHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.bottom_action_chat_send_message_item, parent, false),
                )
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshMessageList(list: List<RMCChatItem>) {
        messageList.clear()
        messageList.addAll(list)
        notifyDataSetChanged()
    }
}

class ActionSheetChatMessageItemHolder(layout: View) : RecyclerView.ViewHolder(layout) {
    val avatar: AppCompatImageView = layout.findViewById(R.id.iv_avatar)
    val name: AppCompatTextView = layout.findViewById(R.id.tv_name)
    val message: AppCompatTextView = layout.findViewById(R.id.tv_content)
    val role: AppCompatTextView = layout.findViewById(R.id.tv_role)
}

interface ActionSheetChatListener : DefaultActionSheetListener {
    fun getLocalUserInfo(): RMCUserInfo
    fun sendMessage(name: String, message: String)
}