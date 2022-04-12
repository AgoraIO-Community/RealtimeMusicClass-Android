package io.agora.realtimemusicclass.chorus.view.pager

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import io.agora.realtimemusicclass.chorus.R

class CourseWareTabLayout : LinearLayout {
    private var listener: CourseWareTabItemClickListener? = null

    private lateinit var lyricItem: CourseWareTabItem
    private lateinit var courseItem: CourseWareTabItem

    constructor(context: Context): super(context) {
        initLayout(context)
    }

    constructor(context: Context, attr: AttributeSet): super(context, attr) {
        initLayout(context)
    }

    private fun initLayout(context: Context) {
        orientation = HORIZONTAL

        lyricItem = CourseWareTabItem(context)
        lyricItem.setText(R.string.course_ware_title_lyrics)
        bindListener(lyricItem, CourseWareTabType.Music)
        addView(lyricItem)

        courseItem = CourseWareTabItem(context)
        courseItem.setText(R.string.course_ware_title_file)
        bindListener(courseItem, CourseWareTabType.CourseWare)
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT)
        params.leftMargin = context.resources.getDimensionPixelSize(
            R.dimen.course_ware_tab_item_padding)
        addView(courseItem, params)
    }

    fun setTabSelected(type: CourseWareTabType, selected: Boolean) {
        val item = getTabItem(type)
        if (selected) {
            lyricItem.setItemSelected(item == lyricItem)
            courseItem.setItemSelected(item == courseItem)
        } else {
            item.setItemSelected(selected)
        }
    }

    fun setListener(listener: CourseWareTabItemClickListener) {
        this.listener = listener
    }

    private fun bindListener(item: CourseWareTabItem, type: CourseWareTabType) {
        item.setOnClickListener {
            lyricItem.setItemSelected(item == lyricItem)
            courseItem.setItemSelected(item == courseItem)
            listener?.onCourseWareTabItemClicked(type)
        }
    }

    fun getTabItem(type: CourseWareTabType): CourseWareTabItem {
        return when (type) {
            CourseWareTabType.Music -> lyricItem
            CourseWareTabType.CourseWare -> courseItem
        }
    }
}

class CourseWareTabItem(context: Context) : LinearLayout(context) {
    private val title: AppCompatTextView
    private val indicator: View

    private var indicatorColorSelected: Int = 0
    private var indicatorColorNormal: Int = 0
    private val indicatorRadius: Float

    init {
        orientation = VERTICAL
        title = AppCompatTextView(context)
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
            context.resources.getDimensionPixelSize(R.dimen.text_size_14).toFloat())
        addView(title, LayoutParams.WRAP_CONTENT,
            context.resources.getDimensionPixelSize(
                R.dimen.course_ware_tab_title_layout_height))

        indicator = View(context)
        val params = LayoutParams(
            context.resources.getDimensionPixelSize(
                R.dimen.course_ware_tab_indicator_width),
            context.resources.getDimensionPixelSize(
                R.dimen.course_ware_tab_indicator_height))
        params.gravity = Gravity.CENTER_HORIZONTAL
        params.topMargin = context.resources.getDimensionPixelSize(
            R.dimen.course_ware_tab_indicator_margin_top)
        addView(indicator, params)

        indicatorColorSelected = context.resources.getColor(
            R.color.course_ware_tab_title_selected)
        indicatorColorNormal = context.resources.getColor(
            R.color.course_ware_tab_title_normal)

        indicatorRadius = context.resources.getDimensionPixelSize(
            R.dimen.course_ware_tab_indicator_corner).toFloat()
        indicator.background = makeIndicator(indicatorColorSelected)

        setItemSelected(false)
    }

    fun setText(@StringRes resId: Int) {
        title.setText(resId)
    }

    fun setText(text: String) {
        title.text = text
    }

    fun setItemSelected(selected: Boolean) {
        post {
            if (selected) {
                title.setTextColor(indicatorColorSelected)
                indicator.visibility = VISIBLE
            } else {
                title.setTextColor(indicatorColorNormal)
                indicator.visibility = INVISIBLE
            }
        }
    }

    private fun makeIndicator(color: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(color)
        drawable.cornerRadii = floatArrayOf(
            indicatorRadius, indicatorRadius, indicatorRadius, indicatorRadius,
            indicatorRadius, indicatorRadius, indicatorRadius, indicatorRadius)
        return drawable
    }
}

interface CourseWareTabItemClickListener {
    fun onCourseWareTabItemClicked(type: CourseWareTabType)
}

enum class CourseWareTabType(val value: Int) {
    Music(0), CourseWare(1);

    companion object {
        private const val count = 2

        fun getTabPosition(type: CourseWareTabType): Int {
            return when (type) {
                Music -> 0
                CourseWare -> 1
            }
        }

        fun tabCount(): Int {
            return count
        }
    }
}