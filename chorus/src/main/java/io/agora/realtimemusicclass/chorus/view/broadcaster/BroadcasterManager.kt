package io.agora.realtimemusicclass.chorus.view.broadcaster

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Outline
import android.graphics.drawable.AnimationDrawable
import android.view.*
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import io.agora.realtimemusicclass.chorus.R
import io.agora.realtimemusicclass.base.edu.core.RMCAudioVolume
import io.agora.realtimemusicclass.base.edu.core.data.RMCMediaInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole

class BroadcasterManager(private val layout: ViewGroup,
                         private val listener: OnSeatStateListener) {
    private val tag = "BroadcasterManage"
    private lateinit var seats: MutableMap<Int, Seat>

    companion object {
        const val MAX_COUNT = 6
    }

    init {
        LayoutInflater.from(layout.context).inflate(
            getLayoutRes(layout.context.resources), layout)
        initSeatInfo(layout)
    }

    private fun getLayoutRes(resources: Resources): Int {
        val largeScreen = resources.configuration.screenLayout.and(
            Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
        return if (largeScreen) {
            R.layout.broadcaster_seat_layout_tablet
        } else {
            R.layout.broadcaster_seat_layout
        }
    }

    private fun initSeatInfo(layout: ViewGroup) {
        seats = mutableMapOf()
        layout.findViewById<RelativeLayout>(R.id.broadcaster_seat_0)?.let {
            val index = 0
            seats[index] = Seat(index, it)
            it.setOnClickListener {
                listener.onSeatClicked(index, seats[index]?.user)
            }
        }

        layout.findViewById<RelativeLayout>(R.id.broadcaster_seat_1)?.let {
            val index = 1
            seats[index] = Seat(index, it)
            it.setOnClickListener {
                listener.onSeatClicked(index, seats[index]?.user)
            }
        }

        layout.findViewById<RelativeLayout>(R.id.broadcaster_seat_2)?.let {
            val index = 2
            seats[index] = Seat(index, it)
            it.setOnClickListener {
                listener.onSeatClicked(index, seats[index]?.user)
            }
        }

        layout.findViewById<RelativeLayout>(R.id.broadcaster_seat_3)?.let {
            val index = 3
            seats[index] = Seat(index, it)
            it.setOnClickListener {
                listener.onSeatClicked(index, seats[index]?.user)
            }
        }

        layout.findViewById<RelativeLayout>(R.id.broadcaster_seat_4)?.let {
            val index = 4
            seats[index] = Seat(index, it)
            it.setOnClickListener {
                listener.onSeatClicked(index, seats[index]?.user)
            }
        }

        layout.findViewById<RelativeLayout>(R.id.broadcaster_seat_5)?.let {
            val index = 5
            seats[index] = Seat(index, it)
            it.setOnClickListener {
                listener.onSeatClicked(index, seats[index]?.user)
            }
        }
    }

    fun updateBroadcasters(userList: List<RMCUserInfo>) {
        val seatMap = mutableMapOf<Int, RMCUserInfo>()
        userList.forEach { user ->
            user.media?.let { media ->
                seatMap[media.index] = user
            }
        }

        seatMap.forEach { entry ->
            seats[entry.key]?.let { seat ->
                seat.user = listener.onUpdateSeatUser(seat, entry.value)
            }
        }
    }

    fun playVolumeAnim(volumes: List<RMCAudioVolume>) {
        volumes.forEach { item ->
            if (item.volume > 0) {
                findUserSeat(item.user)?.playVolumeAnim()
            }
        }
    }

    fun findUserSeat(userInfo: RMCUserInfo): Seat? {
        return userInfo.media?.let {
            seats[it.index]
        }
    }

    inner class Seat(val seatNo: Int, val layout: View) {
        private val offlineLayout: RelativeLayout = layout.findViewById(R.id.broadcaster_item_offline_layout)
        private val emptyLayout: RelativeLayout = layout.findViewById(R.id.broadcaster_item_empty_layout)
        private val videoLayout: RelativeLayout = layout.findViewById(R.id.broadcaster_item_content_layout)
        val videoContainer: FrameLayout = layout.findViewById(R.id.broadcaster_item_video_container)
        val textureView: TextureView = layout.findViewById(R.id.broadcaster_item_video_view)
        private val roleText: AppCompatTextView = layout.findViewById(R.id.broadcaster_recycler_item_role_name)
        val micImage: AppCompatImageView = layout.findViewById(R.id.broadcaster_recycler_item_mic_icon)
        val nameText: AppCompatTextView = layout.findViewById(R.id.broadcaster_recycler_item_user_name)

        var user: RMCUserInfo? = null

        init {
            offlineLayout.isVisible = false
            videoLayout.isVisible = false
            emptyLayout.isVisible = true

            videoContainer.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View?, outline: Outline?) {
                    view?.let { v ->
                        view.clipToOutline = true
                        outline?.setRoundRect(0, 0, v.width, v.height,
                            layout.context.resources.getDimensionPixelSize(
                                R.dimen.broadcaster_recycler_item_radius).toFloat())
                    }
                }
            }
        }

        fun setSeatUser(user: RMCUserInfo?) {
            layout.post {
                offlineLayout.isVisible = false
                emptyLayout.isVisible = false
                videoLayout.isVisible = false

                if (this.user == null) {
                    this.user = user
                } else if (user != null) {
                    this.user?.set(user)
                }

                if (user == null) {
                    this.user = null
                    emptyLayout.isVisible = true
                } else if (!listener.onSeatUserOnline(user)) {
                    offlineLayout.isVisible = true
                } else {
                    videoLayout.isVisible = true
                    roleText.text = RMCUserRole.toName(layout.context, user.role)
                    nameText.text = user.userName
                    user.media?.let { setMicItem(it) }
                }
            }
        }

        fun playVolumeAnim() {
            (micImage.drawable as? AnimationDrawable)?.let {
                micImage.post {
                    it.stop()
                    it.start()
                }
            }
        }

        private fun setMicItem(mediaInfo: RMCMediaInfo) {
            val res = if (mediaInfo.audioStreamMuted()) {
                R.drawable.ic_mic_disable
            } else if (mediaInfo.micShouldOpen()) {
                R.drawable.mic_volume_anim
            } else {
                R.drawable.ic_mic_off
            }

            micImage.setImageResource(res)
        }
    }
}

interface OnSeatStateListener {
    fun onUpdateSeatUser(seat: BroadcasterManager.Seat, user: RMCUserInfo): RMCUserInfo?

    fun onSeatClicked(index: Int, userInfo: RMCUserInfo?)

    fun onSeatUserOnline(userInfo: RMCUserInfo): Boolean
}