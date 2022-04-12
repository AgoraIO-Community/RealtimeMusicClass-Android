package io.agora.realtimemusicclass.chorus.view.audience

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import io.agora.realtimemusicclass.chorus.R

class LobbyAudienceView : RelativeLayout {
    private lateinit var countText: AppCompatTextView
    private var listener: LobbyAudienceViewListener? = null

    constructor(context: Context): super(context) {
        initLayout(context)
    }

    constructor(context: Context, attr: AttributeSet): super(context, attr) {
        initLayout(context)
    }

    private fun initLayout(context: Context) {
        setBackgroundResource(R.drawable.chorus_lobby_audience_bg)

        val layout = LayoutInflater.from(context).inflate(
            R.layout.lobby_audience_layout, this, false)
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT)
        params.addRule(CENTER_IN_PARENT, TRUE)
        addView(layout, params)

        countText = layout.findViewById(R.id.chorus_lobby_audience_count)
        layout.setOnClickListener {
            listener?.onViewClicked()
        }
    }

    fun setCount(count: Int) {
        if (this::countText.isInitialized) {
            countText.post { countText.text = count.toString() }
        }
    }

    fun setListener(listener: LobbyAudienceViewListener) {
        this.listener = listener
    }
}

interface LobbyAudienceViewListener {
    fun onViewClicked()
}