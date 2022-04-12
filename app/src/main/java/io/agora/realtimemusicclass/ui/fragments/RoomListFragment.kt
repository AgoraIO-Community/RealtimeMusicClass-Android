package io.agora.realtimemusicclass.ui.fragments

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.agora.realtimemusicclass.R
import io.agora.realtimemusicclass.base.edu.classroom.ClassManager
import io.agora.realtimemusicclass.base.edu.core.data.RMCCallback
import io.agora.realtimemusicclass.base.edu.core.data.RMCClassInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCError
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.ui.activities.BaseActivity
import io.agora.realtimemusicclass.base.ui.activities.BaseActivityLifeCycleCallback
import io.agora.realtimemusicclass.base.utils.ToastUtil
import io.agora.realtimemusicclass.ui.RMCClass
import io.agora.realtimemusicclass.ui.view.MainPageAdapter
import io.agora.realtimemusicclass.ui.view.OnConfirmButtonClickListener
import io.agora.realtimemusicclass.ui.view.PasswordDialog

class RoomListFragment(adapter: MainPageAdapter,
                       activity: BaseActivity,
                       @LayoutRes private val layout: Int) : AbsFragment(adapter, activity) {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View
    private lateinit var createButton: AppCompatTextView

    private var dialog: PasswordDialog? = null

    private var itemSpacing = 0

    private var roomListAdapter: RoomListAdapter = RoomListAdapter()
    private val roomListPuller = RoomListPuller(roomListAdapter)

    private var launchClassFlag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getBaseActivity().addLifecycleCallbacks(
            object : BaseActivityLifeCycleCallback() {
                override fun onStopped(owner: LifecycleOwner) {
                    super.onStopped(owner)

                    if (launchClassFlag) {
                        getAdapter().reset()
                        launchClassFlag = false
                    }
                }
            })
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(layout, null)
        initLayout(layout)
        return layout
    }

    private fun initLayout(view: View) {
        view.findViewById<AppCompatImageView>(R.id.fragment_room_list_back)?.setOnClickListener {
            getAdapter().gotoPrevPage()
        }

        createButton = view.findViewById(R.id.fragment_room_list_button)
        createButton.setOnClickListener {
            getAdapter().gotoNextPage()
        }

        itemSpacing = view.context.resources.getDimensionPixelSize(
            R.dimen.fragment_room_list_item_margin)

        recycler = view.findViewById(R.id.fragment_room_list_recycler)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = roomListAdapter
        recycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, itemPosition: Int, parent: RecyclerView) {
                outRect.bottom = itemSpacing
            }
        })

        emptyView = view.findViewById(R.id.fragment_room_list_empty_layout)
        emptyView.isVisible = roomListAdapter.itemCount <= 0

        initRoomListRefreshing(view)
    }

    private fun initRoomListRefreshing(layout: View) {
        swipeRefreshLayout = layout.findViewById(
            R.id.fragment_room_list_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            roomListPuller.resetRoomList()
        }

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!swipeRefreshLayout.isRefreshing) {
                        val lastItemPosition = recyclerView.getChildAdapterPosition(
                            recyclerView.getChildAt(recyclerView.childCount - 1))

                        if (lastItemPosition == roomListPuller.getRoomCount() - 1) {
                            roomListPuller.pullMoreRooms()
                        }
                    }
                }
            }
        })
    }

    private fun dismissDialog() {
        dialog?.dismiss()
        dialog = null
    }

    override fun onResume() {
        super.onResume()
        updateOnResumed()
    }

    private fun updateOnResumed() {
        createButton.isVisible = getAdapter().getSelectedRoleType() == RoleType.Teacher.value
        roomListPuller.resetRoomList()
    }

    override fun onPause() {
        recycler.isVisible = false
        emptyView.isVisible = true
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        dismissDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class RoomListAdapter : RecyclerView.Adapter<RoomListViewHolder>() {
        private val classInfoList = mutableListOf<RoomListInfoItem>()

        @SuppressLint("InflateParams")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomListViewHolder {
            return RoomListViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_room_list_item, parent, false))
        }

        override fun onBindViewHolder(holder: RoomListViewHolder, position: Int) {
            val index = holder.adapterPosition
            val info = classInfoList[index]
            holder.name.text = info.name
            holder.creator.text = info.creator
            holder.id.text = info.channelId
            holder.count.text = info.count.toString()

            holder.itemView.setOnClickListener {
                enterRoom(classInfoList[index])
            }
        }

        override fun getItemCount(): Int {
            return classInfoList.size
        }

        private fun enterRoom(classInfo: RoomListInfoItem) {
            if (classInfo.hasPwd) {
                this@RoomListFragment.getBaseActivity().let { activity ->
                    dialog = PasswordDialog(activity, classInfo,
                        object : OnConfirmButtonClickListener {
                            override fun onRoomEnter(classInfo: RoomListInfoItem, password: String) {
                                preCheck(classInfo, password)
                            }
                        })
                    dialog!!.show()
                }
            } else {
                preCheck(classInfo)
            }
        }

        @Synchronized
        @SuppressLint("NotifyDataSetChanged")
        fun clearList() {
            classInfoList.clear()
            notifyDataSetChanged()
        }

        @Synchronized
        fun addItem(info: RoomListInfoItem) {
            classInfoList.add(info)
            notifyItemInserted(classInfoList.size - 1)
        }

        @Synchronized
        fun addItemList(list: List<RoomListInfoItem>) {
            if (list.isEmpty()) {
                return
            }

            val originCount =  classInfoList.size
            list.forEach {
                classInfoList.add(it)
            }
            notifyItemRangeChanged(originCount, list.size)
        }

        @Synchronized
        fun reset(list: List<RoomListInfoItem>) {
            classInfoList.clear()
            addItemList(list)
        }
    }

    private fun preCheck(classInfo: RoomListInfoItem, password: String? = null) {
        if (password.isNullOrEmpty() && classInfo.hasPwd) {
            ToastUtil.toast(getBaseActivity(), R.string.fragment_room_list_toast_enter_empty_password)
            return
        }

        getBaseActivity().showLoading()
        ClassManager.preClassCheck(classInfo.name, password, RMCUserInfo(
            getAdapter().getLocalUserName(), getAdapter().getSelectedRoleType(),
            null, 0, null), object : RMCCallback<RMCUserInfo> {
                override fun onSuccess(res: RMCUserInfo?) {
                    res?.let {
                        RMCClass.launch(getBaseActivity(),
                            getAdapter().getSelectedRoomType(),
                            classInfo.name, classInfo.channelId, it)
                        getBaseActivity().dismissLoadingDialog()

                        ContextCompat.getMainExecutor(
                            this@RoomListFragment.getBaseActivity()).execute {
                                launchClassFlag = true
                            }
                    }
                }

                override fun onFailure(error: RMCError) {
                    ToastUtil.toast(this@RoomListFragment.getBaseActivity(),
                        "join class error: ${error.msg}")
                    getBaseActivity().dismissLoadingDialog()
                }
            })

        dismissDialog()
    }

    inner class RoomListViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val name: AppCompatTextView = layout.findViewById(R.id.fragment_room_list_item_text_area_room_name_value)
        val creator: AppCompatTextView = layout.findViewById(R.id.fragment_room_list_item_text_area_room_creator_value)
        val id: AppCompatTextView = layout.findViewById(R.id.fragment_room_list_item_text_area_room_id_value)
        val count: AppCompatTextView = layout.findViewById(R.id.fragment_room_list_item_count)
    }

    inner class RoomListPuller(private val adapter: RoomListAdapter) {
        private var currentPage = 1
        private var isPulling = false

        @Synchronized
        private fun isPulling(): Boolean {
            return isPulling
        }

        @Synchronized
        private fun setPulling(pulling: Boolean) {
            isPulling = pulling
        }

        private val updateCallback = object : ((List<RMCClassInfo>?) -> Unit) {
            @SuppressLint("NotifyDataSetChanged")
            override fun invoke(p1: List<RMCClassInfo>?) {
                if (localUserIsTeacher()) {
                    // Special case:
                    // Teacher will pull the entire list, so we must
                    // clear all existing list data
                    val list = mutableListOf<RoomListInfoItem>()
                    p1?.forEach {
                        list.add(RoomListInfoItem(it.className,
                            it.creator, it.channelID,
                            it.hasPasswd, it.count))
                    }
                    adapter.reset(list)
                    finishLoading()
                } else {
                    if (this@RoomListFragment::swipeRefreshLayout.isInitialized) {
                        swipeRefreshLayout.post {
                            var start = -1
                            if (!p1.isNullOrEmpty()) {
                                start = adapter.itemCount
                                currentPage++
                                p1.forEach {
                                    val item = RoomListInfoItem(it.className,
                                        it.creator, it.channelID,
                                        it.hasPasswd, it.count)
                                    adapter.addItem(item)
                                }
                            }

                            if (start != -1) {
                                adapter.notifyItemRangeChanged(start, adapter.itemCount - 1)
                            }
                            finishLoading()
                        }
                    }
                }
            }
        }

        private fun finishLoading() {
            val listEmpty = adapter.itemCount <= 0
            emptyView.isVisible = listEmpty
            recycler.isVisible = !listEmpty
            swipeRefreshLayout.isRefreshing = false
            setPulling(false)
        }

        fun getRoomCount(): Int {
            return adapter.itemCount
        }

        fun pullMoreRooms() {
            readRoomListByRole(localUserIsTeacher())
        }

        private fun localUserIsTeacher(): Boolean {
            return getAdapter().getSelectedRoleType() == RoleType.Teacher.value
        }

        private fun readRoomListByRole(isTeacher: Boolean) {
            if (isPulling()) {
                return
            }

            setPulling(true)
            val callback = RoomListPullCallback(updateCallback)
            if (isTeacher) {
                ClassManager.getClassListByCreator(getAdapter().getLocalUserName(), callback)
            } else {
                ClassManager.getClassList(currentPage, callback)
            }
        }

        fun resetRoomList() {
            currentPage = 1
            adapter.clearList()
            pullMoreRooms()
        }

        private inner class RoomListPullCallback(
            private val callback: ((List<RMCClassInfo>?) -> Unit)? = null)
            : RMCCallback<List<RMCClassInfo>> {

            override fun onSuccess(res: List<RMCClassInfo>?) {
                callback?.invoke(res)
            }

            override fun onFailure(error: RMCError) {
                ToastUtil.toast(getBaseActivity(), error.msg ?: "")
            }
        }
    }
}

data class RoomListInfoItem(
    val name: String,
    val creator: String,
    val channelId: String,
    val hasPwd: Boolean = false,
    val count: Int = 0
)