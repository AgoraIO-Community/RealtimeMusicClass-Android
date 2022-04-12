package io.agora.realtimemusicclass.chorus.helper

import android.util.Log
import com.google.gson.Gson
import io.agora.mediaplayer.Constants
import io.agora.realtimemusicclass.base.edu.classroom.MusicManager
import io.agora.realtimemusicclass.base.edu.core.RMCCore
import io.agora.realtimemusicclass.base.edu.core.RMCPlayerManager
import io.agora.realtimemusicclass.base.edu.core.RMCStreamDataListener
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole
import io.agora.realtimemusicclass.chorus.view.lrcview.LrcView
import java.util.*
import kotlin.math.abs
import io.agora.realtimemusicclass.base.utils.NumberUtil

interface NotifyChange2Lyric {
    fun changePosition(position: Long, duration: Long)
    fun changeBgm(id: String)
}

//data sync for lyric manager
// 数据流分三个方向
// 1、(被动)dataStream 驱动 mediaPlayer start、stop 、seek 和 lyricView seek
// 2、(被动)lyricFragment 驱动 mediaPlayer start、stop 、seek，同时 send dataStream
// 3、(主动)定时获取mediaPlayer 位置 进行 lyricView seek，同时 send dataStream
class MusicSyncHelper(
    private var userInfo: RMCUserInfo, private val core: RMCCore
) : RMCStreamDataListener {
    private val tag = "MusicSyncHelper"

    private var delay: Long = 0
    private var delayWithBrod: Long = 0
    private var needSeek: Boolean = false
    private var seekTime: Long = 0
    private var lastSeekTime: Long = 0
    private var lastExpectLocalPosition: Long = 0
    private var bgmId: String = ""
    private var mediaPlayer: RMCPlayerManager? = null
    private val factory: DataStreamFactory = DataStreamFactory()
    private var notifyChange2Lyric: NotifyChange2Lyric? = null
    private var dataStreamId: Int = -1
    private val syncTimeErrorThres: Int = 40

    // timer
    private val localLrcSyncTimer = MusicHelperTimer({ updatePosition() }, 100)
    private val checkTsTimer = MusicHelperTimer({ startCheckTs() }, 2000)

    init {
        mediaPlayer = core.player()
        core.data().setStreamDataListener(this)
        dataStreamId = core.data().createDataStream()

        if (userInfo.isStudent()) {
            checkTsTimer.startTimer()
        }
    }

    // 响应 lrcView 的 postion change
    var onLyricActionListener: LrcView.OnActionListener = object : LrcView.OnActionListener {
        override fun onProgressChanged(time: Long) {
            if (userInfo.role == RMCUserRole.ROLE_TYPE_TEACHER.value) {
                if (mediaPlayer?.getStatus() == Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_COMPLETED) {
                    mediaPlayer!!.seek(time)
                    mediaPlayer!!.pause()
                } else {
                    mediaPlayer!!.seek(time)
                }
                // need to send datstream?
            }
        }

        override fun onStartTrackingTouch() {}
        override fun onStopTrackingTouch() {}
    }

    override fun onStreamData(uid: Int, streamId: Int, data: ByteArray?) {
        val temp: String = data!!.toString(Charsets.UTF_8)
        Log.i(tag, temp)
        val message = Gson().fromJson(temp, MusicSyncMessage::class.java)
        if (message.type == SyncCmdType.StartCheckTs.value) {
            message.msg?.let { this.onStartCheckTs(it) }
        } else if (message.type == SyncCmdType.CheckTsResp.value) {
            message.msg?.let { this.onCheckTsResp(it) }
        } else if (message.type == SyncCmdType.PlayStatus.value) {
            message.msg?.let { this.onPlayStatus(it) }
        } else if (message.type == SyncCmdType.PauseStatus.value) {
            message.msg?.let { this.onPauseStatus(it) }
        }
    }

    // 响应 fragment 的 start or pause
    fun onStartPause(isStart: Boolean) {
        if (!localLrcSyncTimer.running()) {
            localLrcSyncTimer.startTimer()
        }

        val currentTime = System.currentTimeMillis()
        if (isStart) {
            if (mediaPlayer?.getStatus() == Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_COMPLETED) {
                mediaPlayer!!.seek(0)
            }

            mediaPlayer!!.play()
            if (userInfo.isTeacher()) {
                val state = MusicState(0, bgmId, currentTime, mediaPlayer?.getDuration()!!)
                val msgString = factory.buildMusicPlayState(state)
                val temp: ByteArray = msgString.toByteArray(Charsets.UTF_8)
                core.data().sendStreamData(this.dataStreamId, temp)
            }
        } else {
            mediaPlayer!!.pause()
            if (userInfo.isTeacher()) {
                val state = MusicState(0, bgmId, currentTime, mediaPlayer?.getDuration()!!)
                val msgString = factory.buildMusicPauseState(state)
                val temp: ByteArray = msgString.toByteArray(Charsets.UTF_8)
                core.data().sendStreamData(this.dataStreamId, temp)
            }
        }
    }

    // 响应 fragment 的 change bgm
    fun onChangeBgm(id: String, path: String) {
        if (!localLrcSyncTimer.running()) {
            localLrcSyncTimer.startTimer()
        }

        bgmId = id
        mediaPlayer!!.stop()
        mediaPlayer!!.open(path, 0)
        mediaPlayer!!.setLoopCount(-1)
        // 发送 dataStream
        val state = MusicState(0, id, System.currentTimeMillis(), mediaPlayer?.getDuration()!!)
        val msgString = factory.buildMusicPauseState(state)
        val temp: ByteArray = msgString.toByteArray(Charsets.UTF_8)
        core.data().sendStreamData(this.dataStreamId, temp)
    }

    private fun startCheckTs() {
        val checkReq = CheckReq(
            NumberUtil.stringFromInt(userInfo.media!!.streamId),
            System.currentTimeMillis()
        )
        val msgString = factory.buildStartCheckTs(checkReq)
        val temp: ByteArray = msgString.toByteArray(Charsets.UTF_8)
        core.data().sendStreamData(this.dataStreamId, temp)
    }

    // dataStream 相关操作
    private fun onStartCheckTs(messageData: String) {
        val checkReq = Gson().fromJson(messageData, CheckReq::class.java)
        // 合唱的发起对时消息，主唱响应对时
        var position = 0
        val curr = System.currentTimeMillis()
        if (userInfo.role == RMCUserRole.ROLE_TYPE_TEACHER.value) {
            if (mediaPlayer!!.getStatus() == Constants.MediaPlayerState.PLAYER_STATE_PLAYING) {
                position = mediaPlayer?.getPlayPosition()!!.toInt()
            }
            val checkResp = CheckResp(checkReq.uid, checkReq.startTs, curr, position)
            val messageCheTsResp = factory.buildCheckTsResp(checkResp)
            core.data()
                .sendStreamData(this.dataStreamId, messageCheTsResp.toByteArray(Charsets.UTF_8))
            Log.d(tag, "onStartCheckTs: $messageCheTsResp")
        }
    }

    private fun onCheckTsResp(messageData: String) {
        val checkResp: CheckResp = Gson().fromJson(messageData, CheckResp::class.java)
        if (NumberUtil.intFromString(checkResp.remoteUid) == userInfo.media?.streamId) {
            // 主唱的响应对时消息，合唱计算延迟
            val curr = System.currentTimeMillis()
            delay = (curr - checkResp.remoteTS) / 2
            Log.d(tag, "onCheckTsResp: $messageData; delay:$delay")
            delayWithBrod = checkResp.broadTs + delay - curr
            if (needSeek && mediaPlayer!!.getStatus() ==
                Constants.MediaPlayerState.PLAYER_STATE_PLAYING
            ) {
                val expLocalTs = checkResp.broadTs - checkResp.position - delayWithBrod
                val localPosition = mediaPlayer!!.getPlayPosition()
                val timeErr = abs(curr - localPosition - expLocalTs)
                if (timeErr > syncTimeErrorThres) {
                    var expSeek = curr - expLocalTs
                    expSeek += seekTime
                    lastExpectLocalPosition = expSeek
                    lastSeekTime = curr
                    mediaPlayer!!.seek(expSeek)
                    Log.w(tag, "timeErr: $timeErr; delay: $delay")
                }
            }
            needSeek = false
        }
    }

    private fun onPlayStatus(messageData: String) {
        val playState = Gson().fromJson(messageData, MusicState::class.java)
        if (playState.bgmId != bgmId) {
            // student and audience change bgm
            changeBgm(playState.bgmId)
        }

        if (userInfo.role == RMCUserRole.ROLE_TYPE_AUDIENCE.value) {
            notifyChange2Lyric?.changePosition(playState.position, playState.Duration)
            return
        }

        if (mediaPlayer?.getStatus() ==
            Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED
        ) {
            if (playState.position == 0.toLong()) {
                mediaPlayer!!.play()

            } else if (playState.position > 0.toLong()) {
                mediaPlayer!!.seek(playState.position)
                mediaPlayer!!.play()
            }
        } else if (mediaPlayer?.getStatus() == Constants.MediaPlayerState.PLAYER_STATE_PLAYING) {
            val c1 = System.currentTimeMillis()
            val expLocalTs = playState.broadTs - playState.position - delayWithBrod
            if (lastSeekTime != 0L) {
                seekTime =
                    lastExpectLocalPosition + (c1 - lastSeekTime) - mediaPlayer?.getPlayPosition()!!
                lastSeekTime = 0
                lastExpectLocalPosition = 0
            }

            val timeErr = abs(c1 - mediaPlayer?.getPlayPosition()!! - expLocalTs)
            if (timeErr > syncTimeErrorThres) {
//                var expSeek = c1 - expLocalTs
//                expSeek = seekTime + expSeek
                Log.w(tag, "time err:$timeErr; delay:$delay")
                needSeek = true
            }
        } else {
            mediaPlayer!!.play()
        }
    }

    private fun onPauseStatus(messageData: String) {
        val pauseState = Gson().fromJson(messageData, MusicState::class.java)
        if (pauseState.bgmId != bgmId) {
            // student and audience change bgm
            changeBgm(pauseState.bgmId)
        }

        if (mediaPlayer!!.getStatus()
            == Constants.MediaPlayerState.PLAYER_STATE_PLAYING
        ) {
            // pause first
            mediaPlayer?.pause()
        }

        // audience change duration
        if (userInfo.role == RMCUserRole.ROLE_TYPE_AUDIENCE.value) {
            notifyChange2Lyric?.changePosition(pauseState.position, pauseState.Duration)
        } else if (userInfo.role == RMCUserRole.ROLE_TYPE_STUDENT.value) {
            if (abs(
                    mediaPlayer?.getPlayPosition()?.minus(pauseState.position) ?: 0
                ) > syncTimeErrorThres
            ) {
                mediaPlayer?.seek(pauseState.position)
            }
        }
    }

    private fun changeBgm(bgmId: String) {
        if (MusicManager.isInitialized()) {
            if (userInfo.isStudent()) {
                mediaPlayer?.stop()
                mediaPlayer?.open(MusicManager.getMusicInfo(bgmId)?.fileDesPath!!, 0)
                mediaPlayer?.setLoopCount(-1)
            }
            notifyChange2Lyric?.changeBgm(bgmId)
            this.bgmId = bgmId
        }

        if (userInfo.isStudent()) {
            localLrcSyncTimer.startTimer()
        }
    }

    // 来自 mediaPlayer 的 position 更新
    private fun updatePosition() {
        val position = mediaPlayer?.getPlayPosition()
        val duration = mediaPlayer?.getDuration()
        notifyChange2Lyric?.changePosition(position!!, duration!!)

        if (userInfo.isTeacher()) {
            val curr: Long = System.currentTimeMillis()
            if (mediaPlayer?.getStatus() == Constants.MediaPlayerState.PLAYER_STATE_PLAYING) {
                val state = MusicState(
                    mediaPlayer!!.getPlayPosition(),
                    bgmId,
                    curr,
                    mediaPlayer?.getDuration()!!
                )
                val msgString = factory.buildMusicPlayState(state)
                val temp: ByteArray = msgString.toByteArray(Charsets.UTF_8)
                core.data().sendStreamData(this.dataStreamId, temp)
            } else if (mediaPlayer?.getStatus() == Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED ||
                mediaPlayer?.getStatus() == Constants.MediaPlayerState.PLAYER_STATE_PAUSED
            ) {
                val state = MusicState(
                    mediaPlayer!!.getPlayPosition(),
                    bgmId,
                    curr,
                    mediaPlayer?.getDuration()!!
                )
                val msgString = factory.buildMusicPauseState(state)
                val temp: ByteArray = msgString.toByteArray(Charsets.UTF_8)
                core.data().sendStreamData(this.dataStreamId, temp)
            }
        }
    }

    fun registerNotifyChange2Lyric(notifyChange2Lyric: NotifyChange2Lyric) {
        this.notifyChange2Lyric = notifyChange2Lyric
    }

    fun destroy() {
        localLrcSyncTimer.stopTimer()
        checkTsTimer.stopTimer()
    }
}

internal class MusicHelperTimer(val callRunner: () -> Unit, private var period: Long) {
    private val timer = Timer()
    private var running = false

    fun startTimer() {
        running = true
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                callRunner()
            }
        }, 0, period)
    }

    fun running(): Boolean {
        return running
    }

    fun stopTimer() {
        running = false
        timer.cancel()
    }
}