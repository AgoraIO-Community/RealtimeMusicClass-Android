package io.agora.realtimemusicclass.chorus.view.pager

import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.agora.realtimemusicclass.base.ui.activities.BaseClassActivity
import io.agora.realtimemusicclass.chorus.databinding.CourseWarePagerLayoutBinding

class CourseWarePager(activity: BaseClassActivity,
                      layout: RelativeLayout) {
    private val pagerBinding: CourseWarePagerLayoutBinding =
        CourseWarePagerLayoutBinding.inflate(LayoutInflater.from(layout.context))

    init {
        layout.addView(pagerBinding.root)

        val adapter = CourseWareFragmentAdapter(activity, pagerBinding.courseWarePager)
        hackRemovePagerOverScrollEffect(pagerBinding.courseWarePager)
        pagerBinding.courseWarePager.adapter = adapter
        adapter.setListener(object : CourseWarePagerListener {
            override fun onPageSelected(position: Int, type: CourseWareTabType) {
                pagerBinding.courseWarePagerTabLayout.setTabSelected(type, true)
            }
        })

        pagerBinding.courseWarePagerTabLayout.setTabSelected(CourseWareTabType.Music, true)
        pagerBinding.courseWarePagerTabLayout.setListener(object : CourseWareTabItemClickListener {
            override fun onCourseWareTabItemClicked(type: CourseWareTabType) {
                adapter.setSelectedTabType(type, true)
            }
        })
    }

    // No official solution but this workaround
    private fun hackRemovePagerOverScrollEffect(pager: ViewPager2) {
        pager.children.forEach { child ->
            (child as? RecyclerView)?.let {
                it.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                return@forEach
            }
        }
    }
}