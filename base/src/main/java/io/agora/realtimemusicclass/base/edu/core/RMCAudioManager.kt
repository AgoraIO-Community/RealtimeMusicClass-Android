package io.agora.realtimemusicclass.base.edu.core

import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.ui.actions.AECMode
import io.agora.rtc2.Constants
import io.agora.rtc2.Constants.SINGING_BEAUTIFIER

class RMCAudioManager(private val core: RMCCore) {
    private val tag = "RMCAudioManager"
    private var isInEarEnabled = false
    private var recordingEnabled = false
    private var audioPublished = false

    private val callbacks = mutableListOf<RMCAudioCallback>()

    private val rtcListener = object : RMCRtcEventListener() {
        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?,
                                             totalVolume: Int) {
            val itemList = mutableListOf<RMCAudioVolume>()
            speakers?.forEach { volume ->
                core.user().getUserInfoFromMediaId(volume.uid)?.let { user ->
                    val item = RMCAudioVolume(user, volume.volume)
                    itemList.add(item)
                }
            }

            if (itemList.isNotEmpty()) {
                callbacks.forEach {
                    it.onAudioVolumesChanged(itemList)
                }
            }
        }
    }

    fun registerCallback(callback: RMCAudioCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    fun removeCallback(callback: RMCAudioCallback) {
        callbacks.remove(callback)
    }

    init {
        core.registerRtcEventListener(rtcListener)
        setInEarEnabled(isInEarEnabled)
        setLocalRecordingEnabled(recordingEnabled)
        muteLocalAudio(!audioPublished)
    }

    @Synchronized
    fun muteLocalAudio(muted: Boolean) {
        audioPublished = !muted
        core.engine().rtcEngine().let {
            it.muteLocalAudioStream(muted)

            // A workaround for rtc engine
            // Rtc will automatically enable local mic recording
            // when un-mute local audio stream.
            // So we here disable mic recording if we do not
            // want according to local recorded state
            if (!muted && !recordingEnabled) {
                it.enableLocalAudio(false)
            }
        }
    }

    fun muteRemoteAudio(muted: Boolean, uid: Int) {
        core.engine().rtcEngine().muteRemoteAudioStream(uid, muted)
    }

    fun setVoiceBeautifierPreset(preset: Int) {
        core.engine().rtcEngine().setVoiceBeautifierPreset(preset)
    }

    fun setVoiceBeautifierParameters(gender: Int, room: Int) {
        core.engine().rtcEngine().setVoiceBeautifierParameters(SINGING_BEAUTIFIER, gender, room)
    }

    @Synchronized
    fun setInEarEnabled(enabled: Boolean) {
        isInEarEnabled = enabled
        core.engine().rtcEngine().enableInEarMonitoring(enabled, Constants.EAR_MONITORING_FILTER_NONE)
    }

    @Synchronized
    fun isInEarEnabled(): Boolean {
        return isInEarEnabled
    }

    fun setAudioProfile(profile : Int,  scenario: Int) {
        core.engine().rtcEngine().setAudioProfile(profile, scenario)
    }

    // ear monitor
    fun setInEarVolume(volume: Int) {
        if (isInEarEnabled) {
            core.engine().rtcEngine().setInEarMonitoringVolume(volume)
        }
    }

    // mic
    fun adjustRecordingSignalVolume(volume: Int) {
        core.engine().rtcEngine().adjustRecordingSignalVolume(volume)
    }

    // remote volume
    fun adjustUserPlaybackSignalVolume(uid: Int, volume: Int) {
        core.engine().rtcEngine().adjustUserPlaybackSignalVolume(uid, volume)
    }

    @Synchronized
    fun setLocalRecordingEnabled(enabled: Boolean) {
        recordingEnabled = enabled
        core.engine().rtcEngine().enableLocalAudio(enabled)
    }

    fun isLocalAudioRecording(): Boolean {
        return recordingEnabled
    }

    fun isLocalAudioPublishEnabled(): Boolean {
        return audioPublished
    }

    fun setDefaultAecMode(userInfo: RMCUserInfo) {
        setLocalAecMode(core.user().getUserAecMode(userInfo))
    }

    fun setLocalAecMode(mode: AECMode) {
        var m = mode
        if (m == AECMode.None) {
            m = AECMode.NoEcho
        }

        core.engine().rtcEngine().setParameters("{\"rtc.audio.music_mode\": ${m.value}}")
        core.user().setLocalUserExt("aecMode", m.value)
        core.user().localUser()?.let {
            core.user().notifyUserUpdate(core.room().className, it)
        }
    }

    fun recycle() {
        core.removeRtcEventListener(rtcListener)
    }
}

abstract class RMCAudioCallback {
    open fun onAudioVolumesChanged(volumes: List<RMCAudioVolume>) {

    }
}

data class RMCAudioVolume(
    val user: RMCUserInfo,
    val volume: Int
)