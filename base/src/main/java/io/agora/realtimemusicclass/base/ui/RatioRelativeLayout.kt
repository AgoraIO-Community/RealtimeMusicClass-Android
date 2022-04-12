package io.agora.realtimemusicclass.base.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import io.agora.realtimemusicclass.base.R

class RatioRelativeLayout : RelativeLayout {
    private var ratio: Float = 1f

    constructor(context: Context): super(context) {

    }

    constructor(context: Context, attr: AttributeSet): super(context, attr) {
        init(context, attr)
    }

    constructor(context: Context, attr: AttributeSet, selfStyle: Int): super(context, attr, selfStyle) {
        init(context, attr)
    }

    private fun init(context: Context, attr: AttributeSet) {
        val array = context.obtainStyledAttributes(attr, R.styleable.RatioRelativeLayout)
        ratio = array.getFloat(R.styleable.RatioRelativeLayout_ratio, 1f)
        array.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)

        val modifiedW: Int
        val modifiedH: Int
        val curRatio = w.toFloat() / h
        if (curRatio >= ratio) {
            modifiedH = h
            modifiedW = (modifiedH * ratio).toInt()
        } else {
            modifiedW = w
            modifiedH = (modifiedW / ratio).toInt()
        }

        val wSpec = MeasureSpec.makeMeasureSpec(modifiedW, MeasureSpec.EXACTLY)
        val hSpec = MeasureSpec.makeMeasureSpec(modifiedH, MeasureSpec.EXACTLY)
        setMeasuredDimension(modifiedW, modifiedH)
        super.onMeasure(wSpec, hSpec)
    }
}