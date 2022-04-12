package io.agora.realtimemusicclass.ui.fragments

import androidx.fragment.app.Fragment
import io.agora.realtimemusicclass.base.ui.activities.BaseActivity
import io.agora.realtimemusicclass.ui.view.MainPageAdapter

abstract class AbsFragment(private val adapter: MainPageAdapter,
                           private val activity: BaseActivity
) : Fragment() {
    fun getAdapter(): MainPageAdapter {
        return adapter
    }

    fun getBaseActivity(): BaseActivity {
        return activity
    }
}