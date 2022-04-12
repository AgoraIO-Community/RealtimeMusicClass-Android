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
import io.agora.rtc2.Constants

@SuppressLint("ViewConstructor")
class ActionSheetVoiceEffect(
    context: Context,
    app: MusicClassApp,
    type: ActionSheetType,
    listener: DefaultActionSheetListener? = null
) : ActionSheet(context, app, type, listener) {

    private val recycler: RecyclerView
    private val adapter: ActionSheetVoiceEffectListAdapter

    init {
        LayoutInflater.from(context).inflate(
            R.layout.bottom_action_voice_effect, this
        )
        recycler = findViewById(R.id.action_dialog_voice_effect_list_recycler)
        recycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL, false
        )
        adapter = ActionSheetVoiceEffectListAdapter(
            context,
            listener as? ActionSheetVoiceEffectListener
        )
        recycler.adapter = adapter
    }

    fun setSelectedVoiceEffect(value: Int) {
        adapter.setSelectedVoiceEffectValue(value)
    }
}

class ActionSheetVoiceEffectListAdapter(
    context: Context,
    private val listener: ActionSheetVoiceEffectListener? = null
) : RecyclerView.Adapter<ActionSheetVoiceEffectItemHolder>() {

    private val voiceEffectList = arrayOf(
        VoiceEffectItem(
            Constants.VOICE_BEAUTIFIER_OFF,
            context.getString(R.string.voice_effect_default)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_VIGOROUS,
            context.getString(R.string.voice_effect_vigorous)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_DEEP,
            context.getString(R.string.voice_effect_deep)),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_MELLOW,
            context.getString(R.string.voice_effect_mellow)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_FALSETTO,
            context.getString(R.string.voice_effect_falsetto)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_FULL,
            context.getString(R.string.voice_effect_full)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_CLEAR,
            context.getString(R.string.voice_effect_clear)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_RESOUNDING,
            context.getString(R.string.voice_effect_resounding)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_RINGING,
            context.getString(R.string.voice_effect_ringing)
        ),
        // male reverb
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_RINGING + 1,
            context.getString(R.string.man_reverb_small_room)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_RINGING + 2,
            context.getString(R.string.man_reverb_large_room)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_RINGING + 3,
            context.getString(R.string.man_reverb_hall)
        ),
        // female reverb
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_RINGING + 4,
            context.getString(R.string.woman_reverb_small_room)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_RINGING + 5,
            context.getString(R.string.woman_reverb_large_room)
        ),
        VoiceEffectItem(
            Constants.TIMBRE_TRANSFORMATION_RINGING + 6,
            context.getString(R.string.woman_reverb_hall)
        ),
    )

    private var curVoiceEffectValue: Int = 0

    @SuppressLint("InflateParams")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActionSheetVoiceEffectItemHolder {
        return ActionSheetVoiceEffectItemHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.bottom_action_voice_effect_list_item, parent, false
            )
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ActionSheetVoiceEffectItemHolder, position: Int) {
        val pos = holder.adapterPosition
        val item = voiceEffectList[pos]
        holder.name.text = item.name
        if (curVoiceEffectValue == item.value) {
            holder.name.isActivated = true
            holder.icon.isVisible = true
        } else {
            holder.name.isActivated = false
            holder.icon.isVisible = false
        }

        holder.itemView.setOnClickListener {
            val i = voiceEffectList[holder.adapterPosition]
            if (i.value != curVoiceEffectValue) {
                curVoiceEffectValue = i.value
                listener?.onVoiceEffectSelected(i.value)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return voiceEffectList.size
    }

    fun getCurrentVoiceEffectValue(): Int {
        return curVoiceEffectValue
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedVoiceEffectValue(value: Int) {
        this.curVoiceEffectValue = value
        notifyDataSetChanged()
    }
}

class ActionSheetVoiceEffectItemHolder(layout: View) : RecyclerView.ViewHolder(layout) {
    val name: AppCompatTextView = layout.findViewById(R.id.bottom_action_voice_effect_item_name)
    val icon: AppCompatImageView =
        layout.findViewById(R.id.bottom_action_voice_effect_choose_item_icon)
}

interface ActionSheetVoiceEffectListener : DefaultActionSheetListener {
    fun onVoiceEffectSelected(value: Int)
}

data class VoiceEffectItem(
    val value: Int,
    val name: String
)