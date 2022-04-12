package io.agora.realtimemusicclass.ui.view

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.agora.realtimemusicclass.R
import io.agora.realtimemusicclass.base.edu.core.data.RMCSceneType
import io.agora.realtimemusicclass.base.ui.activities.BaseActivity
import io.agora.realtimemusicclass.ui.fragments.*

class MainPageAdapter(private val activity: BaseActivity,
                      private val pager: ViewPager2) : FragmentStateAdapter(activity) {
    private var selectedRoomType = RMCSceneType.Chorus.value
    private var selectedRole = RoleType.Teacher.value
    private var userName = ""

    companion object {
        const val FRAG_LAYOUT_RES_ROLE = R.layout.fragment_role_type
        const val FRAG_LAYOUT_RES_ROOM_TYPE = R.layout.fragment_room_type
        const val FRAG_LAYOUT_RES_ROOM_LIST = R.layout.fragment_room_list
        const val FRAG_LAYOUT_RES_ROOM_CREATE = R.layout.fragment_room_create
    }

    override fun getItemCount(): Int {
        return FragmentTypes.count()
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            FragmentTypes.Role.value -> RoleTypeFragment(this, activity, FRAG_LAYOUT_RES_ROLE)
            FragmentTypes.RoomList.value -> RoomListFragment(this, activity, FRAG_LAYOUT_RES_ROOM_LIST)
            FragmentTypes.RoomCreate.value -> RoomCreateFragment(this, activity, FRAG_LAYOUT_RES_ROOM_CREATE)
            else -> RoomTypeFragment(this, activity, FRAG_LAYOUT_RES_ROOM_TYPE)
        }
    }

    @Synchronized
    fun setSelectedRoomType(type: Int) {
        selectedRoomType = type
    }

    fun getSelectedRoomType(): Int {
        return selectedRoomType
    }

    @Synchronized
    fun setSelectedRole(role: Int) {
        selectedRole = role
    }

    fun getSelectedRoleType(): Int {
        return selectedRole
    }

    @Synchronized
    fun setLocalUserName(name: String) {
        userName = name
    }

    fun getLocalUserName(): String {
        return userName
    }

    fun gotoNextPage(smoothly: Boolean = true): Boolean {
        var result = false
        val current = pager.currentItem
        if (current < FragmentTypes.count() - 1) {
            pager.setCurrentItem(current.inc(), smoothly)
            result = true
        }
        return result
    }

    fun gotoPrevPage(smoothly: Boolean = true): Boolean {
        var result = false
        val current = pager.currentItem
        if (current > 0) {
            pager.setCurrentItem(current.dec(), smoothly)
            result = true
        }
        return result
    }

    fun reset() {
        pager.setCurrentItem(FragmentTypes.RoomType.value, false)
        setLocalUserName("")
        setSelectedRole(RoleType.Teacher.value)
        setSelectedRoomType(FragmentTypes.RoomType.value)
    }
}

enum class FragmentTypes(val value: Int) {
    RoomType(0),
    Role(1),
    RoomList(2),
    RoomCreate(3);

    companion object {
        private const val count = 4
        fun count(): Int {
            return count
        }
    }
}