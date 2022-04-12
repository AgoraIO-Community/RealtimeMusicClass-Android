package io.agora.realtimemusicclass.base.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import io.agora.realtimemusicclass.base.databinding.RecRadioGroupLayoutBinding

class RMCRadioGroup : RelativeLayout {
    private lateinit var binding: RecRadioGroupLayoutBinding
    private lateinit var thumb: AppCompatImageView

    private lateinit var receiverLeft: AppCompatImageView
    private lateinit var receiverMiddle: AppCompatImageView
    private lateinit var receiverRight: AppCompatImageView

    private var listener: RMCRadioGroupListener? = null

    fun setRadioGroupListener(listener: RMCRadioGroupListener) {
        this.listener = listener
    }

    constructor(context: Context): super(context) {
        initView(context)
    }

    constructor(context: Context, attr: AttributeSet): super(context, attr) {
        initView(context)
    }

    private fun initView(context: Context) {
        binding = RecRadioGroupLayoutBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)
        receiverLeft = binding.radioGroupLayoutThumbClickLeft
        receiverMiddle = binding.radioGroupLayoutThumbClickMiddle
        receiverRight = binding.radioGroupLayoutThumbClickRight
        thumb = binding.radioGroupLayoutThumb

        receiverLeft.setOnClickListener {
            setThumbPosition(RMCRadioGroupOption.Left)
            listener?.onRadioButtonSelected(it, RMCRadioGroupOption.Left)
        }

        receiverMiddle.setOnClickListener {
            setThumbPosition(RMCRadioGroupOption.Middle)
            listener?.onRadioButtonSelected(it, RMCRadioGroupOption.Middle)
        }

        receiverRight.setOnClickListener {
            setThumbPosition(RMCRadioGroupOption.Right)
            listener?.onRadioButtonSelected(it, RMCRadioGroupOption.Right)
        }
    }

    fun setRadioOption(option: RMCRadioGroupOption) {
        setThumbPosition(option)
    }

    private fun setThumbPosition(option: RMCRadioGroupOption) {
        val params = thumb.layoutParams as LayoutParams
        params.removeRule(ALIGN_PARENT_START)
        params.removeRule(CENTER_IN_PARENT)
        params.removeRule(ALIGN_PARENT_END)

        params.addRule(when (option) {
            RMCRadioGroupOption.Left -> {
                ALIGN_PARENT_START
            }
            RMCRadioGroupOption.Middle -> {
                CENTER_IN_PARENT
            }
            RMCRadioGroupOption.Right -> {
                ALIGN_PARENT_END
            }
        }, TRUE)
        thumb.layoutParams = params
    }
}

interface RMCRadioGroupListener {
    fun onRadioButtonSelected(view: View, option: RMCRadioGroupOption)
}

enum class RMCRadioGroupOption {
    Left, Middle, Right
}