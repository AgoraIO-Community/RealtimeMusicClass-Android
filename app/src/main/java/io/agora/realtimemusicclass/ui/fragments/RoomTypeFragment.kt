package io.agora.realtimemusicclass.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatTextView
import io.agora.realtimemusicclass.R
import io.agora.realtimemusicclass.base.edu.core.data.RMCSceneType
import io.agora.realtimemusicclass.base.ui.activities.BaseActivity
import io.agora.realtimemusicclass.base.utils.ToastUtil
import io.agora.realtimemusicclass.ui.view.MainPageAdapter
import io.agora.realtimemusicclass.ui.view.RoomTypeItemView

class RoomTypeFragment(adapter: MainPageAdapter,
                       activity: BaseActivity,
                       @LayoutRes private val layout: Int) : AbsFragment(adapter, activity) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(layout, null)
        initFragment(layout)
        return layout
    }

    private fun initFragment(layout: View) {
        layout.findViewById<RoomTypeItemView>(R.id.fragment_room_type_item_chorus)?.let { item ->
            item.findViewById<AppCompatTextView>(R.id.fragment_room_type_item_button_enter)?.let {
                it.setOnClickListener {
                    onRoomEntered(RMCSceneType.Chorus.value)
                }
            }
            item.findViewById<AppCompatTextView>(R.id.fragment_room_type_item_button_more)?.let {
                it.setOnClickListener {
                    onMoreClicked(RMCSceneType.Chorus.value)
                }
            }
        }

        layout.findViewById<RoomTypeItemView>(R.id.fragment_room_type_item_instruments)?.let { item ->
            item.findViewById<AppCompatTextView>(R.id.fragment_room_type_item_button_enter)?.let {
                it.setOnClickListener {
                    onRoomEntered(RMCSceneType.Instruments.value)
                }
            }
            item.findViewById<AppCompatTextView>(R.id.fragment_room_type_item_button_more)?.let {
                it.setOnClickListener {
                    onMoreClicked(RMCSceneType.Instruments.value)
                }
            }
        }

        layout.findViewById<RoomTypeItemView>(R.id.fragment_room_type_item_piano)?.let { item ->
            item.findViewById<AppCompatTextView>(R.id.fragment_room_type_item_button_enter)?.let {
                it.setOnClickListener {
                    onRoomEntered(RMCSceneType.Piano.value)
                }
            }
            item.findViewById<AppCompatTextView>(R.id.fragment_room_type_item_button_more)?.let {
                it.setOnClickListener {
                    onMoreClicked(RMCSceneType.Piano.value)
                }
            }
        }
    }

    private fun onRoomEntered(type: Int) {
        when (type) {
            RMCSceneType.Chorus.value -> {
                getAdapter().let { adapter ->
                    adapter.setSelectedRoomType(type)
                    adapter.gotoNextPage()
                }
            }
            RMCSceneType.Instruments.value,
            RMCSceneType.Piano.value -> {
                functionDeveloped()
            }
        }

    }

    private fun onMoreClicked(type: Int) {
        functionDeveloped()
    }

    private fun functionDeveloped() {
        ToastUtil.toast(getBaseActivity(), R.string.function_under_development)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}