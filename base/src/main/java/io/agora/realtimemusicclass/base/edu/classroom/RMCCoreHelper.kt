package io.agora.realtimemusicclass.base.edu.classroom

import io.agora.realtimemusicclass.base.edu.core.RMCCore
import io.agora.realtimemusicclass.base.edu.core.data.*

class RMCCoreHelper(private val className: String,
                    private val rmcCore: RMCCore) {

    /**
     * Start or stop local device states according to
     * current user media setting.
     */
    @Synchronized
    fun syncLocalDevices(userInfo: RMCUserInfo) {
        userInfo.media?.let { mediaInfo ->
            rmcCore.video().enableLocalVideoCapturing(mediaInfo.cameraShouldOpen())
            rmcCore.video().muteLocalVideo(mediaInfo.videoStreamMuted())
            rmcCore.audio().setLocalRecordingEnabled(mediaInfo.micShouldOpen())
            rmcCore.audio().muteLocalAudio(mediaInfo.audioStreamMuted())
        }
    }

    fun startLocalVideoCapture(callback: RMCCallback<Boolean>? = null) {
        setLocalVideoCaptureEnabled(true, callback)
    }

    fun stopLocalVideoCapture(callback: RMCCallback<Boolean>? = null) {
        setLocalVideoCaptureEnabled(false, callback)
    }

    @Synchronized
    private fun setLocalVideoCaptureEnabled(enabled: Boolean,
                                            callback: RMCCallback<Boolean>? = null) {
        rmcCore.let { core ->
            core.user().localUser()?.let { local ->
                val info = local.copy()
                info.media?.let { media ->
                    media.cameraDeviceState = if (enabled) {
                        RMCDeviceState.On.value
                    } else {
                        RMCDeviceState.Off.value
                    }

                    core.user().setLocalCameraState(media.cameraDeviceState)
                    core.user().notifyUserUpdate(className, info, object : RMCCallback<Void> {
                        override fun onSuccess(res: Void?) {
                            core.video().enableLocalVideoCapturing(enabled)
                            callback?.onSuccess(enabled)
                        }

                        override fun onFailure(error: RMCError) {
                            callback?.onFailure(error)
                        }
                    })
                }
            }
        }
    }

    fun startLocalAudioRecording(callback: RMCCallback<Boolean>? = null) {
        setLocalAudioRecordingEnabled(true, callback)
    }

    fun stopLocalAudioRecording(callback: RMCCallback<Boolean>? = null) {
        setLocalAudioRecordingEnabled(false, callback)
    }

    @Synchronized
    private fun setLocalAudioRecordingEnabled(enabled: Boolean,
                                              callback: RMCCallback<Boolean>? = null) {
        rmcCore.let { core ->
            core.user().localUser()?.let { local ->
                val info = local.copy()
                info.media?.let { media ->
                    media.micDeviceState = if (enabled) {
                        RMCDeviceState.On.value
                    } else {
                        RMCDeviceState.Off.value
                    }

                    core.user().setLocalMicState(media.micDeviceState)
                    core.user().notifyUserUpdate(className, info, object : RMCCallback<Void> {
                        override fun onSuccess(res: Void?) {
                            core.audio().setLocalRecordingEnabled(enabled)
                            callback?.onSuccess(enabled)
                        }

                        override fun onFailure(error: RMCError) {

                        }
                    })
                }
            }
        }
    }

    fun muteLocalVideoStream(muted: Boolean,
                             callback: RMCCallback<Boolean>? = null) {
        setLocalVideoPublished(!muted, callback)
    }

    @Synchronized
    private fun setLocalVideoPublished(published: Boolean,
                                       callback: RMCCallback<Boolean>? = null) {
        rmcCore.let { core ->
            core.user().localUser()?.let { local ->
                val info = local.copy()
                info.media?.let { media ->
                    media.videoStreamState = if (published) {
                        RMCStreamState.Publish.value
                    } else {
                        RMCStreamState.Mute.value
                    }

                    core.user().setLocalVideoStreamState(media.videoStreamState)
                    core.user().notifyUserUpdate(className, info, object : RMCCallback<Void> {
                        override fun onSuccess(res: Void?) {
                            core.video().muteLocalVideo(!published)
                            callback?.onSuccess(published)
                        }

                        override fun onFailure(error: RMCError) {
                            callback?.onFailure(error)
                        }
                    })
                }
            }
        }
    }

    fun muteLocalAudioStream(muted: Boolean,
                             callback: RMCCallback<Boolean>? = null) {
        setLocalAudioPublished(!muted, callback)
    }

    @Synchronized
    private fun setLocalAudioPublished(published: Boolean,
                                       callback: RMCCallback<Boolean>? = null) {
        rmcCore.let { core ->
            core.user().localUser()?.let { local ->
                val info = local.copy()
                info.media?.let { media ->
                    media.audioStreamState = if (published) {
                        RMCStreamState.Publish.value
                    } else {
                        RMCStreamState.Mute.value
                    }

                    core.user().setLocalAudioStreamState(media.audioStreamState)
                    core.user().notifyUserUpdate(className, info, object : RMCCallback<Void> {
                        override fun onSuccess(res: Void?) {
                            core.audio().muteLocalAudio(!published)
                            callback?.onSuccess(published)
                        }

                        override fun onFailure(error: RMCError) {
                            callback?.onFailure(error)
                        }
                    })
                }
            }
        }
    }

    @Synchronized
    fun notifyRemoteVideoPublished(userName: String, published: Boolean) {
        rmcCore.user().publishRemoteUserCamera(published, userName)
    }

    @Synchronized
    fun notifyRemoteAudioPublished(userName: String, published: Boolean) {
        rmcCore.user().publishRemoteUserMic(published, userName)
    }
}