package io.agora.realtimemusicclass.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

class SquareRelativeLayout : RelativeLayout {
    constructor(context: Context): super(context)

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val s = w.coerceAtMost(h)
        val wSpec = MeasureSpec.makeMeasureSpec(s, MeasureSpec.EXACTLY)
        super.onMeasure(wSpec, wSpec)
    }
}