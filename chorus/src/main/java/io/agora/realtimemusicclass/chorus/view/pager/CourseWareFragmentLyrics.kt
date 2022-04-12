package io.agora.realtimemusicclass.chorus.view.pager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import io.agora.mediaplayer.Constants
import io.agora.realtimemusicclass.base.edu.classroom.MusicManager
import io.agora.realtimemusicclass.base.edu.classroom.MusicManagerListener
import io.agora.realtimemusicclass.base.edu.core.RMCCore
import io.agora.realtimemusicclass.base.edu.core.RMCPlayerListener
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole
import io.agora.realtimemusicclass.base.ui.actions.*
import io.agora.realtimemusicclass.base.ui.activities.ActionActivity
import io.agora.realtimemusicclass.base.ui.activities.BaseClassActivity
import io.agora.realtimemusicclass.base.ui.activities.ClassLifecycleListener
import io.agora.realtimemusicclass.chorus.ChorusActivity
import io.agora.realtimemusicclass.chorus.R
import io.agora.realtimemusicclass.chorus.helper.NotifyChange2Lyric
import io.agora.realtimemusicclass.chorus.view.lrcview.LrcLoadUtils
import io.agora.realtimemusicclass.chorus.view.lrcview.LrcView
import io.agora.rtc2.Constants.VOICE_BEAUTIFIER_OFF
import java.io.File
import java.util.concurrent.CountDownLatch

class CourseWareFragmentLyrics(private val activity: BaseClassActivity) : Fragment() {
    private val logTag = "FragmentLyrics"

    private var lrcView: LrcView? = null
    private var musicNameView: AppCompatTextView? = null
    private var pauseIconView: AppCompatImageView? = null
    private var playTimeTextView: AppCompatTextView? = null

    private var effectLayout: LinearLayout? = null
    private var consoleLayout: LinearLayout? = null
    private var switchMusicLayout: LinearLayout? = null

    private var role: RMCUserRole = RMCUserRole.ROLE_TYPE_UNKNOWN
    private var duration: Long = 0
    private var currentVoiceEffect: Int = io.agora.rtc2.Constants.VOICE_CHANGER_OFF

    private var currentMusicId: String? = null
    private var musicPlaying: Boolean = false
    private var musicCDLock: CountDownLatch? = null

    private val classLifecycleListener = object : ClassLifecycleListener {
        override fun onClassJoined(rmcCore: RMCCore) {
            val channelId = activity.rmcCore().room().channelId
            role = RMCUserRole.fromValue(activity.rmcCore().user().localUser()!!.role)
            musicCDLock = CountDownLatch(2)
            activity.rmcCore().player().join(channelId, role, object : RMCPlayerListener() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.d(this@CourseWareFragmentLyrics.logTag, "mediaplayer join success")

                    showMusicIfReady()
                    (activity as? ChorusActivity)?.let {
                        it.initMusicSyncHelper()
                        it.musicSyncHelper?.registerNotifyChange2Lyric(notifyChange2Lyric)

                        if (role.isTeacher()) {
                            lrcView?.setEnableDrag(true)
                        } else {
                            lrcView?.setEnableDrag(false)
                        }
                        lrcView?.setActionListener(it.musicSyncHelper?.onLyricActionListener)
                    }
                }

                override fun onPositionChanged(position: Long) {
                    this@CourseWareFragmentLyrics.activity.runOnUiThread {
                        //sync time
                    }
                }

                override fun onPlayerStateChanged(
                    state: Constants.MediaPlayerState,
                    error: Constants.MediaPlayerError
                ) {
                    if (state == Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_COMPLETED) {
                        setMusicPlayIcon(false)
                        (activity as? ChorusActivity)?.let {
                            it.musicSyncHelper?.onStartPause(false)
                        }
                    }
                }
            })
            Log.d(this@CourseWareFragmentLyrics.logTag, "mediaPlayer Joined")

            this@CourseWareFragmentLyrics.activity.runOnUiThread {
                if (role.isStudent()) {
                    effectLayout?.visibility = View.VISIBLE
                }
                if (role.isTeacher()) {
                    effectLayout?.visibility = View.VISIBLE
                    switchMusicLayout?.visibility = View.VISIBLE
                }
                consoleLayout?.visibility = View.VISIBLE
            }

            initData()
        }

        override fun onClassLeft(rmcCore: RMCCore) {
            activity.rmcCore().player().leave()
            (activity as? ChorusActivity)?.let {
                it.destroyMusicSyncHelper()
            }
            Log.d(this@CourseWareFragmentLyrics.logTag, "mediaPlayer Left")
        }
    }

    private val musicManagerListener = object : MusicManagerListener {
        override fun onMusicInitialized() {
            showMusicIfReady()
        }
    }

    private val actionSheetMusicListener = object : ActionSheetMusicListener {
        override fun onMusicSelected(id: String, name: String) {
            currentMusicId = id
            loadLrcFromUrl(MusicManager.getMusicInfo(id)?.lyricDesPath!!)
            (activity as? ChorusActivity)?.let {
                activity.musicSyncHelper?.onChangeBgm(
                    id,
                    MusicManager.getMusicInfo(id)?.fileDesPath!!
                )
            }
            musicNameView?.text = MusicManager.getMusicInfo(id)?.name
            if (pauseIconView?.visibility == View.GONE) {
                pauseIconView?.visibility = View.VISIBLE
            }
            setMusicPlayIcon(false)
        }
    }

    private val notifyChange2Lyric = object : NotifyChange2Lyric {
        private fun time2String(time: Long): String {
            return if (time > 0) {
                val minutes = time / 1000 / 60
                val seconds = time / 1000 % 60
                String.format("%02d:%02d", minutes, seconds)
            } else {
                "00:00"
            }
        }

        override fun changePosition(position: Long, duration: Long) {
            this@CourseWareFragmentLyrics.activity.runOnUiThread {
                if (position >= 0) {
                    lrcView!!.updateTime(position)
                }
            }
            val formatTimeString = time2String(position) + "/" + time2String(duration)
            this@CourseWareFragmentLyrics.activity.runOnUiThread {
                playTimeTextView!!.text = formatTimeString
            }
        }

        override fun changeBgm(id: String) {
            loadLrcFromUrl(MusicManager.getMusicInfo(id)?.lyricDesPath!!)
            (musicNameView as? AppCompatTextView)?.text = MusicManager.getMusicInfo(id)?.name
        }
    }

    // todo:move it to activity
    private val consoleActionSheetCallback = object : ActionSheetConsoleListener {
        override fun onVolumeChanged(id: Int, value: Int, type: String) {
            (activity as ChorusActivity).let { act ->
                act.audioParametersHelper?.volumeList()!!.forEach {
                    if (it.id == id) {
                        act.audioParametersHelper?.updateVolumeItem(value, type)
                        if (type == context?.getString(R.string.volume_mic)) {
                            activity.rmcCore().audio().adjustRecordingSignalVolume(value)
                        } else if (type == context?.getString(R.string.volume_ear_monitor)) {
                            activity.rmcCore().audio().setInEarVolume(value)
                        } else if (type == context?.getString(R.string.volume_bgm)) {
                            if (role == RMCUserRole.ROLE_TYPE_AUDIENCE ||
                                role == RMCUserRole.ROLE_TYPE_UNKNOWN) {
                                activity.rmcCore().audio().adjustUserPlaybackSignalVolume(1, value)
                            } else {
                                activity.rmcCore().player().adjustPlayoutVolume(value)
                            }
                        } else if (type == it.type) {
                            activity.rmcCore().audio().adjustUserPlaybackSignalVolume(it.id, value)
                        }
                    }
                }
            }
        }
    }

    private val voiceEffectActionSheetCallback = object : ActionSheetVoiceEffectListener {
        override fun onVoiceEffectSelected(value: Int) {
            if (value == VOICE_BEAUTIFIER_OFF) {
                activity.rmcCore().audio().setVoiceBeautifierPreset(VOICE_BEAUTIFIER_OFF)
                currentVoiceEffect = VOICE_BEAUTIFIER_OFF
                return
            }

            currentVoiceEffect = value
            if (currentVoiceEffect >= io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_VIGOROUS &&
                    currentVoiceEffect <= io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RINGING) {
                activity.rmcCore().audio().setVoiceBeautifierPreset(currentVoiceEffect)
            }
            when (currentVoiceEffect) {
                io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RINGING + 1 ->
                    activity.rmcCore().audio().setVoiceBeautifierParameters(1, 1)
                io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RINGING + 2 ->
                    activity.rmcCore().audio().setVoiceBeautifierParameters(1, 2)
                io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RINGING + 3 ->
                    activity.rmcCore().audio().setVoiceBeautifierParameters(1, 3)
                io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RINGING + 4 ->
                    activity.rmcCore().audio().setVoiceBeautifierParameters(2, 1)
                io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RINGING + 5 ->
                    activity.rmcCore().audio().setVoiceBeautifierParameters(2, 2)
                io.agora.rtc2.Constants.TIMBRE_TRANSFORMATION_RINGING + 6 ->
                    activity.rmcCore().audio().setVoiceBeautifierParameters(2, 3)
            }
        }
    }

    private fun loadLrcFromUrl(path: String) {
        this@CourseWareFragmentLyrics.activity.runOnUiThread {
            lrcView!!.reset()
            val file = File(path)
            lrcView!!.setLrcData(LrcLoadUtils.parse(file))
        }
    }

    private fun setMusicPlayIcon(playOrPause: Boolean) {
        this@CourseWareFragmentLyrics.activity.runOnUiThread {
            if (playOrPause) {
                pauseIconView?.setBackgroundResource(R.drawable.ic_pause)
            } else {
                pauseIconView?.setBackgroundResource(R.drawable.ic_play)
            }
            this.musicPlaying = playOrPause
        }
    }

    private fun initLayout(view: View) {
        lrcView = view.findViewById(R.id.lrcView)

        musicNameView = view.findViewById(R.id.course_ware_fragment_music_name)
        musicNameView?.text = context?.getString(R.string.no_music)

        pauseIconView = view.findViewById(R.id.course_ware_fragment_play_icon)
        pauseIconView?.visibility = View.GONE
        pauseIconView?.setOnClickListener { _ ->
            (activity as? ChorusActivity)?.let { activity ->
                setMusicPlayIcon(!this.musicPlaying)
                activity.musicSyncHelper?.onStartPause(this.musicPlaying)
            }
        }

        playTimeTextView = view.findViewById(R.id.course_ware_fragment_music_play_progress)

        effectLayout = view.findViewById(R.id.course_ware_voice_effect)
        effectLayout?.setOnClickListener { _ ->
            (activity as? ActionActivity)?.let {
                it.actionSheetUtil()?.showActionSheetDialog(
                    it,
                    ActionSheetType.VoiceEffect,
                    voiceEffectActionSheetCallback
                )
                val actionSheet = it.actionSheetUtil()?.getCurrentAction()
                (actionSheet as? ActionSheetVoiceEffect)?.let {
                    actionSheet.setSelectedVoiceEffect(currentVoiceEffect)
                }
            }
        }

        consoleLayout = view.findViewById(R.id.course_ware_console)
        consoleLayout?.setOnClickListener { _ ->
            (activity as? ChorusActivity)?.let {
                it.actionSheetUtil()?.showActionSheetDialog(
                    it,
                    ActionSheetType.Console,
                    consoleActionSheetCallback
                )
                // todo:move it to activity
                it.refreshVolumeListActionSheet()
            }
        }

        switchMusicLayout = view.findViewById(R.id.course_ware_console_switch_music)
        switchMusicLayout?.setOnClickListener {
            this@CourseWareFragmentLyrics.activity.runOnUiThread {
                showMusicActionSheet()
            }
        }
    }

    private fun showMusicIfReady() {
        // 1、mediaPlayer has joined; 2、MusicManager has inited
        musicCDLock?.countDown()
        if (musicCDLock?.count?:0 == 0L) {
            musicCDLock = null
            if (role == RMCUserRole.ROLE_TYPE_TEACHER) {
                this@CourseWareFragmentLyrics.activity.runOnUiThread {
                    showMusicActionSheet()
                }
            }
        }
    }

    private fun showMusicActionSheet() {
        (activity as? ActionActivity)?.let { activity ->
            activity.actionSheetUtil()?.showActionSheetDialog(
                activity,
                ActionSheetType.BgMusic,
                actionSheetMusicListener
            )
            val actionSheet = activity.actionSheetUtil()?.getCurrentAction()
            (actionSheet as? ActionSheetMusic)?.let {
                val musicItemList = mutableListOf<MusicItem>()
                MusicManager.getMusicInfoList().forEach { s ->
                    musicItemList.add(MusicItem(s.id!!, s.name!!))
                }
                it.refreshList(musicItemList)
                it.setSelectedMusic(currentMusicId)
            }
        }
    }

    private fun initData() {
        if (MusicManager.isInitialized()) {
            showMusicIfReady()
        } else {
            MusicManager.registerMusicManagerListener(musicManagerListener)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity.registerClassLifecycleListener(classLifecycleListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.course_ware_fragment_music_layout, container, false)
        initLayout(layout)
        return layout
    }

    override fun onDestroy() {
        super.onDestroy()
        lrcView?.reset()
        activity.removeClassLifecycleListener(classLifecycleListener)
    }
}