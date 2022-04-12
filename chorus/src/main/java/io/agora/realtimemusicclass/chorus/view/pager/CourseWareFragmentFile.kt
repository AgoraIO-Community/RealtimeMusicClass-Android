package io.agora.realtimemusicclass.chorus.view.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.agora.realtimemusicclass.chorus.R
import io.agora.realtimemusicclass.base.ui.activities.BaseClassActivity

class CourseWareFragmentFile(private val activity: BaseClassActivity) : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.course_ware_fragment_file_layout, container, false)
    }
}