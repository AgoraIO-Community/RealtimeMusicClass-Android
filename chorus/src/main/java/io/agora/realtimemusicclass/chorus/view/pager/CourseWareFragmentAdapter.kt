package io.agora.realtimemusicclass.chorus.view.pager

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.agora.realtimemusicclass.chorus.R
import io.agora.realtimemusicclass.base.ui.activities.BaseClassActivity

class CourseWareFragmentAdapter(private val activity: BaseClassActivity,
                                private val pager: ViewPager2) : FragmentStateAdapter(activity) {
    private var selectedTabType: CourseWareTabType = CourseWareTabType.Music

    private var listener: CourseWarePagerListener? = null

    init {
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                listener?.onPageSelected(position, getFragmentTypeByPosition(position))
            }
        })
    }

    fun setListener(listener: CourseWarePagerListener) {
        this.listener = listener
    }

    override fun getItemCount(): Int {
        R.layout.course_ware_fragment_file_layout
        return CourseWareTabType.tabCount()
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            CourseWareTabType.Music.value -> CourseWareFragmentLyrics(activity)
            CourseWareTabType.CourseWare.value -> CourseWareFragmentFile(activity)
            else -> CourseWareFragmentLyrics(activity)
        }
    }

    private fun getFragmentTypeByPosition(position: Int): CourseWareTabType {
        return when (position) {
            CourseWareTabType.Music.value -> CourseWareTabType.Music
            CourseWareTabType.CourseWare.value -> CourseWareTabType.CourseWare
            else -> CourseWareTabType.Music
        }
    }

    private fun getFragmentPosition(type: CourseWareTabType): Int {
        return when (type) {
            CourseWareTabType.Music -> CourseWareTabType.Music.value
            CourseWareTabType.CourseWare -> CourseWareTabType.CourseWare.value
        }
    }

    @Synchronized
    fun setSelectedTabType(type: CourseWareTabType, smoothly: Boolean = true) {
        selectedTabType = type
        pager.setCurrentItem(getFragmentPosition(type), smoothly)
    }

    fun getSelectedRoomType(): CourseWareTabType {
        return selectedTabType
    }
}

interface CourseWarePagerListener {
    fun onPageSelected(position: Int, type: CourseWareTabType)
}