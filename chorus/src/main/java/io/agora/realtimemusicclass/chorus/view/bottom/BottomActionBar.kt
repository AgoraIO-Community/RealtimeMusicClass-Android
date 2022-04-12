package io.agora.realtimemusicclass.chorus.view.bottom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import io.agora.realtimemusicclass.chorus.R

class BottomActionBar : RelativeLayout {
    private var listener: BottomActionBarClickListener? = null

    constructor(context: Context): super(context) {
        initLayout(context)
    }

    constructor(context: Context, attr: AttributeSet): super(context, attr) {
        initLayout(context)
    }

    private fun initLayout(context: Context) {
        val layout = LayoutInflater.from(context).inflate(
            R.layout.bottom_action_bar, this, false)
        addView(layout, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        layout.findViewById<RelativeLayout>(R.id.chorus_bottom_bar_item_help)?.setOnClickListener {
            listener?.onActionClicked(BottomAction.Help, it.isActivated)
        }

        layout.findViewById<RelativeLayout>(R.id.chorus_bottom_bar_item_in_ear)?.setOnClickListener {
            listener?.onActionClicked(BottomAction.InEar, it.isActivated)
        }

        layout.findViewById<RelativeLayout>(R.id.chorus_bottom_bar_item_camera)?.setOnClickListener {
            listener?.onActionClicked(BottomAction.Camera, it.isActivated)
        }

        layout.findViewById<RelativeLayout>(R.id.chorus_bottom_bar_item_mic)?.setOnClickListener {
            listener?.onActionClicked(BottomAction.Mic, it.isActivated)
        }

        layout.findViewById<RelativeLayout>(R.id.chorus_bottom_bar_item_sound_chat)?.setOnClickListener {
            listener?.onActionClicked(BottomAction.Chat, it.isActivated)
        }
    }

    internal fun setListener(listener: BottomActionBarClickListener) {
        this.listener = listener
    }

    fun setEnabled(action: BottomAction, enabled: Boolean) {
        val id = findViewIdByAction(action)
        findViewById<RelativeLayout>(id)?.let { layout ->
            setActivatedRecursive(layout, enabled)
        }
    }

    private fun setActivatedRecursive(layout: ViewGroup, activated: Boolean) {
        layout.isActivated = activated
        for (index in 0 until layout.childCount) {
            layout.getChildAt(index)?.let { child ->
                if (child is ViewGroup) {
                    setActivatedRecursive(child, activated)
                } else {
                    child.isActivated = activated
                }
            }
        }
    }

    private fun findViewIdByAction(action: BottomAction): Int {
        return when (action) {
            BottomAction.Help -> R.id.chorus_bottom_bar_item_help
            BottomAction.InEar -> R.id.chorus_bottom_bar_item_in_ear
            BottomAction.Camera -> R.id.chorus_bottom_bar_item_camera
            BottomAction.Mic -> R.id.chorus_bottom_bar_item_mic
            BottomAction.Chat -> R.id.chorus_bottom_bar_item_sound_chat
        }
    }
}

enum class BottomAction {
    Help, InEar, Camera, Mic, Chat
}

internal interface BottomActionBarClickListener {
    /**
     * Called when a n action bar button is clicked
     * @param action the action that this button indicates
     * @param enabled the current state of this option when
     * the click event occurs
     */
    fun onActionClicked(action: BottomAction, enabled: Boolean)
}