package io.agora.realtimemusicclass.base.ui.actions

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.realtimemusicclass.base.MusicClassApp
import io.agora.realtimemusicclass.base.R

class ActionSheetConsole(
    context: Context,
    app: MusicClassApp,
    type: ActionSheetType,
    listener: DefaultActionSheetListener? = null
) : ActionSheet(context, app, type, listener) {

    private val recycler: RecyclerView
    private val adapter: ActionSheetConsoleListAdapter

    init {
        LayoutInflater.from(context).inflate(
            R.layout.bottom_action_console, this
        )
        recycler = findViewById(R.id.action_dialog_console_recycler)
        recycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL, false
        )
        adapter = ActionSheetConsoleListAdapter(
            listener as? ActionSheetConsoleListener
        )
        recycler.adapter = adapter
    }

    fun refreshVolumeList(volumeList: List<VolumeItem>) {
        adapter.refreshVolumeList(volumeList)
    }
}

class ActionSheetConsoleListAdapter(
    private val listener: ActionSheetConsoleListener? = null
) : RecyclerView.Adapter<ActionSheetConsoleItemHolder>() {

    private val volumeTypeList = mutableListOf<VolumeItem>()

    @SuppressLint("InflateParams")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActionSheetConsoleItemHolder {
        return ActionSheetConsoleItemHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.bottom_action_console_list_item, parent, false
            )
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ActionSheetConsoleItemHolder, position: Int) {
        val pos = holder.adapterPosition
        val item = volumeTypeList[pos]
        holder.name.text = item.type
        holder.name.isEnabled = item.enabled

        // if user mic is forbidden or ear monitor, disable seekbar
        holder.volumeBar.isEnabled = item.enabled
        holder.volumeBar.progress = item.currentValue
        holder.volumeBar.max = item.maxVaule
        holder.volumeBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar, progress: Int, fromUser: Boolean
                ) {
                    item.currentValue = progress
                    holder.currentVolume.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    listener?.onVolumeChanged(item.id, item.currentValue, item.type)
                }
            }
        )

        holder.currentVolume.text = item.currentValue.toString()
    }

    override fun getItemCount(): Int {
        return volumeTypeList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshVolumeList(volumeList: List<VolumeItem>) {
        volumeTypeList.clear()
        volumeTypeList.addAll(volumeList)
        notifyDataSetChanged()
    }
}

class ActionSheetConsoleItemHolder(layout: View) : RecyclerView.ViewHolder(layout) {
    val name: AppCompatTextView = layout.findViewById(R.id.bottom_action_console_list_item_name)
    val volumeBar: SeekBar =
        layout.findViewById(R.id.bottom_action_console_list_item_volume_progress_bar)
    val currentVolume: AppCompatTextView =
        layout.findViewById(R.id.bottom_action_console_list_item_volume_value)
}

interface ActionSheetConsoleListener : DefaultActionSheetListener {
    fun onVolumeChanged(id: Int, value: Int, type: String)
}

data class VolumeItem(
    var id:Int,
    var currentValue: Int,
    var maxVaule: Int,
    var enabled: Boolean,
    val type: String
)