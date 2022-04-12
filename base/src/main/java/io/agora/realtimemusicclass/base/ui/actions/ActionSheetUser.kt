package io.agora.realtimemusicclass.base.ui.actions

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.realtimemusicclass.base.MusicClassApp
import io.agora.realtimemusicclass.base.R
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole
import io.agora.realtimemusicclass.base.ui.views.RMCRadioGroup
import io.agora.realtimemusicclass.base.ui.views.RMCRadioGroupListener
import io.agora.realtimemusicclass.base.ui.views.RMCRadioGroupOption

@SuppressLint("ViewConstructor")
class ActionSheetUser(
    context: Context,
    app: MusicClassApp,
    type: ActionSheetType,
    listener: DefaultActionSheetListener? = null
) : ActionSheet(context, app, type, listener) {

    private val recycler: RecyclerView
    private val adapter: ActionSheetUserListAdapter
    private val aecTitle: AppCompatTextView
    private val aecModeList: RelativeLayout

    init {
        LayoutInflater.from(context).inflate(
            R.layout.bottom_action_user, this)

        recycler = findViewById(R.id.action_dialog_user_list_recycler)
        recycler.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL, false)
        adapter = ActionSheetUserListAdapter(
            context,
            listener as? ActionSheetUserListener)
        aecTitle = findViewById(R.id.action_dialog_aec_title)
        aecModeList = findViewById(R.id.action_dialog_user_aec_mode_list)
        recycler.adapter = adapter
    }

    private fun sortUserByRole(local: String, list: List<UserItem>): List<UserItem> {
        val hostList = mutableListOf<UserItem>()
        val audienceList = mutableListOf<UserItem>()
        val userList = mutableListOf<UserItem>()
        var findTeacher = false
        list.forEach {
            if (it.role == RMCUserRole.ROLE_TYPE_TEACHER) {
                findTeacher = true
                hostList.add(0, it)
            } else if (it.role == RMCUserRole.ROLE_TYPE_STUDENT) {
                if (it.name == local) {
                    if (findTeacher) {
                        // i'm student,find teacher put after teacher
                        hostList.add(1, it)
                    } else {
                        // i'm student,but no teacher is finded, put first
                        hostList.add(0, it)
                    }
                } else {
                    hostList.add(it)
                }
            } else {
                if (it.name == local) {
                    // i'm audience,put myself as audience first
                    audienceList.add(0, it)
                } else {
                    audienceList.add(it)
                }
            }
        }
        userList.addAll(hostList)
        userList.addAll(audienceList)
        return userList
    }

    fun refreshUserList(isShowAecSwitch: Boolean, local: String, userList: List<UserItem>) {
        aecTitle.isVisible = isShowAecSwitch
        aecModeList.isVisible = isShowAecSwitch
        val sortedList = sortUserByRole(local, userList)
        adapter.refreshUserList(isShowAecSwitch, local, sortedList)
    }
}

class ActionSheetUserListAdapter(
    private val context: Context,
    private val listener: ActionSheetUserListener? = null
) : RecyclerView.Adapter<ActionSheetUserItemHolder>() {

    private var isShowAecSwitch: Boolean = false
    private lateinit var local: String
    private val userList = mutableListOf<UserItem>()

    @SuppressLint("InflateParams")
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ActionSheetUserItemHolder {
        return ActionSheetUserItemHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.bottom_action_user_list_item, parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ActionSheetUserItemHolder, position: Int) {
        val pos = holder.adapterPosition
        val item = userList[pos]
        if (local == item.name) {
            holder.name.text = "(".plus(context.getString(R.string.myself)).plus(")").plus(item.name)
        } else {
            when (item.role) {
                RMCUserRole.ROLE_TYPE_TEACHER ->
                    holder.name.text = "(".plus(context.getString(R.string.role_name_teacher)).plus(")").plus(item.name)
                RMCUserRole.ROLE_TYPE_STUDENT ->
                    holder.name.text = "(".plus(context.getString(R.string.role_name_student)).plus(")").plus(item.name)
                RMCUserRole.ROLE_TYPE_AUDIENCE ->
                    holder.name.text = "(".plus(context.getString(R.string.role_name_audience)).plus(")").plus(item.name)
                else -> {
                    holder.name.text = "(".plus(context.getString(R.string.role_name_audience)).plus(")").plus(item.name)
                }
            }
        }

        holder.name.isEnabled = item.onLine

        if (isShowAecSwitch) {
            // show aec radio button by default,
            // but must be determined by actual user
            // when the user list completes the logic
            // holder.aecSwitch.isVisible = item.hasSwitch

            if (item.hasSwitch) {
                holder.aecSwitch.isVisible = true
                holder.aecSwitch.isEnabled = item.onLine
                val opt = when (item.mode) {
                    AECMode.NoEcho -> RMCRadioGroupOption.Left
                    AECMode.Standard -> RMCRadioGroupOption.Middle
                    AECMode.Fluent -> RMCRadioGroupOption.Right
                    else -> {
                        RMCRadioGroupOption.Middle
                    }
                }
                holder.aecSwitch.setRadioOption(opt)
                holder.aecSwitch.setRadioGroupListener(
                    object : RMCRadioGroupListener {
                        override fun onRadioButtonSelected(
                            view: View,
                            option: RMCRadioGroupOption
                        ) {
                            val i = userList[holder.adapterPosition]
                            val mode = when (option) {
                                RMCRadioGroupOption.Left -> AECMode.NoEcho
                                RMCRadioGroupOption.Middle -> AECMode.Standard
                                RMCRadioGroupOption.Right -> AECMode.Fluent
                            }
                            listener?.onAecModeChanged(mode, i.name)
                            notifyDataSetChanged()
                        }
                    })
            }
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshUserList(isShowAecSwitch: Boolean, local: String, list: List<UserItem>) {
        this.isShowAecSwitch = isShowAecSwitch
        this.local = local
        userList.clear()
        userList.addAll(list)
        notifyDataSetChanged()
    }
}

class ActionSheetUserItemHolder(layout: View)
    : RecyclerView.ViewHolder(layout) {
    val name: AppCompatTextView = layout.findViewById(R.id.bottom_action_user_list_item_name)
    val aecSwitch: RMCRadioGroup = layout.findViewById(R.id.bottom_action_user_list_aec)
}

interface ActionSheetUserListener : DefaultActionSheetListener {
    fun onAecModeChanged(mode: AECMode, name: String)
}

data class UserItem(
    var mode: AECMode,
    var role: RMCUserRole,
    var onLine: Boolean,
    var hasSwitch: Boolean,
    var name: String
)

enum class AECMode(val value: Int) {
    None(-1),
    NoEcho(1),
    Standard(3),
    Fluent(5);

    companion object {
        fun fromValue(value: Int): AECMode {
            return when (value) {
                NoEcho.value -> NoEcho
                Standard.value -> Standard
                Fluent.value -> Fluent
                else -> Standard
            }
        }
    }
}