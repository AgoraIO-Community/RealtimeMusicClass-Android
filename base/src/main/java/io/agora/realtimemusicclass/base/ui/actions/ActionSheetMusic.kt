package io.agora.realtimemusicclass.base.ui.actions

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.realtimemusicclass.base.MusicClassApp
import io.agora.realtimemusicclass.base.R

@SuppressLint("ViewConstructor")
class ActionSheetMusic(context: Context,
                       app: MusicClassApp,
                       type: ActionSheetType,
                       listener: DefaultActionSheetListener? = null)
    : ActionSheet(context, app, type, listener) {

    private val recycler: RecyclerView
    private val adapter: ActionSheetMusicListAdapter

    init {
        LayoutInflater.from(context).inflate(
            R.layout.bottom_action_bg_music, this)
        recycler = findViewById(R.id.action_dialog_music_list_recycler)
        recycler.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL, false)
        adapter = ActionSheetMusicListAdapter(
            listener as? ActionSheetMusicListener)
        recycler.adapter = adapter
    }

    fun setSelectedMusic(id: String?) {
        adapter.setCurrentMusicId(id)
    }

    fun refreshList(list: List<MusicItem>) {
        adapter.refreshMusicList(list)
    }
}

class ActionSheetMusicListAdapter(
    private val listener: ActionSheetMusicListener? = null
) : RecyclerView.Adapter<ActionSheetMusicListItemHolder>() {
    private val musicList = mutableListOf<MusicItem>()

    private var curMusicId: String? = null

    @SuppressLint("InflateParams")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionSheetMusicListItemHolder {
        return ActionSheetMusicListItemHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.bottom_action_bg_music_list_item, parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ActionSheetMusicListItemHolder, position: Int) {
        val pos = holder.adapterPosition
        val item = musicList[pos]
        holder.name.text = item.name
        if (curMusicId == item.id) {
            holder.name.isActivated = true
            holder.icon.isVisible = true
        } else {
            holder.name.isActivated = false
            holder.icon.isVisible = false
        }

        holder.itemView.setOnClickListener {
            val i = musicList[holder.adapterPosition]
            if (i.id != curMusicId) {
                curMusicId = i.id
                listener?.onMusicSelected(i.id, i.name)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshMusicList(list: List<MusicItem>) {
        musicList.clear()
        musicList.addAll(list)
        notifyDataSetChanged()
    }

    fun getCurrentMusicId(): String? {
        return curMusicId
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCurrentMusicId(id: String?) {
        this.curMusicId = id
        notifyDataSetChanged()
    }
}

class ActionSheetMusicListItemHolder(layout: View) : RecyclerView.ViewHolder(layout) {
    val name: AppCompatTextView = layout.findViewById(R.id.bottom_action_music_list_item_name)
    val icon: AppCompatImageView = layout.findViewById(R.id.bottom_action_music_list_item_icon)
}

interface ActionSheetMusicListener : DefaultActionSheetListener {
    fun onMusicSelected(id: String, name: String)
}

data class MusicItem(
    val id: String,
    val name: String)