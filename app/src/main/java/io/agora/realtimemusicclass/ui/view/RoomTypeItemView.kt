package io.agora.realtimemusicclass.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import io.agora.realtimemusicclass.R
import io.agora.realtimemusicclass.databinding.FragmentRoomTypeItemBinding

class RoomTypeItemView : FrameLayout {
    private lateinit var binding: FragmentRoomTypeItemBinding

    constructor(context: Context): super(context) {
        buildView(context)
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        buildView(context)
        setAttributes(context, attrs)
    }

    private fun buildView(context: Context) {
        binding = FragmentRoomTypeItemBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)
    }

    private fun setAttributes(context: Context, attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoomTypeItemView)
        val radius = typedArray.getDimensionPixelSize(R.styleable.RoomTypeItemView_radius, 0)
        val icon = typedArray.getDrawable(R.styleable.RoomTypeItemView_icon)
        val startColor = typedArray.getColor(R.styleable.RoomTypeItemView_startColor, Color.WHITE)
        val endColor = typedArray.getColor(R.styleable.RoomTypeItemView_endColor, Color.WHITE)
        val title = typedArray.getString(R.styleable.RoomTypeItemView_title)
        val subTitle = typedArray.getString(R.styleable.RoomTypeItemView_subtitle)
        val border = typedArray.getColor(R.styleable.RoomTypeItemView_borderColor, Color.WHITE)
        val thick = typedArray.getDimensionPixelSize(R.styleable.RoomTypeItemView_borderThick, 0)
        typedArray.recycle()

        setColoredBackground(startColor, endColor, radius.toFloat())
        setButtonBackground(border, thick, radius.toFloat())
        binding.fragmentRoomTypeItemIcon.setImageDrawable(icon)
        binding.fragmentRoomTypeItemTitle.text = title
        binding.fragmentRoomTypeItemSubtitle.text = subTitle
    }

    private fun setColoredBackground(startColor: Int, endColor: Int, radius: Float = 0f) {
        val gradient = GradientDrawable()
        gradient.shape = GradientDrawable.RECTANGLE
        gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
        gradient.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        gradient.colors = intArrayOf(startColor, endColor)
        gradient.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        binding.fragmentRoomTypeItemContent.background = gradient
    }

    private fun setButtonBackground(border: Int, thick: Int, radius: Float) {
        val gradient = GradientDrawable()
        gradient.shape = GradientDrawable.RECTANGLE
        gradient.setStroke(thick, border)
        gradient.setColor(Color.WHITE)
        gradient.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius)
        binding.fragmentRoomTypeItemButtonLayout.background = gradient
    }
}