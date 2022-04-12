package io.agora.realtimemusicclass.base.edu.core

import android.view.TextureView
import io.agora.rtc2.Constants
import io.agora.rtc2.video.VideoCanvas

class RMCVideoManager(private val core: RMCCore) {
    private var localVideoPublished = false
    private var localVideoCaptured = false

    init {
        muteLocalVideo(!localVideoPublished)
        enableLocalVideoCapturing(localVideoCaptured)
    }

    fun renderLocalVideo(view: TextureView) {
        createAndRenderVideoForUid(view, true)
    }

    fun renderRemoteVideo(view: TextureView, uid: Int) {
        createAndRenderVideoForUid(view, false, uid)
    }

    private fun createAndRenderVideoForUid(view: TextureView, self: Boolean, uid: Int = 0) {
        core.engine().rtcEngine().let { engine ->
            val canvas = VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, uid)
            if (self) {
                engine.setupLocalVideo(canvas)
                engine.startPreview()
            } else {
                engine.setupRemoteVideo(canvas)
            }
        }
    }

    fun clearLocalVideo() {
        clearVideoForUid(true)
    }

    fun clearRemoteVideo(uid: Int) {
        clearVideoForUid(false, uid)
    }

    private fun clearVideoForUid(self: Boolean, uid: Int = 0) {
        core.engine().rtcEngine().let { engine ->
            val canvas = VideoCanvas(null, Constants.RENDER_MODE_HIDDEN, uid)
            if (self) {
                engine.stopPreview()
                engine.setupLocalVideo(canvas)
            } else {
                engine.setupRemoteVideo(canvas)
            }
        }
    }

    @Synchronized
    fun muteLocalVideo(muted: Boolean) {
        localVideoPublished = !muted
        core.engine().rtcEngine().muteLocalVideoStream(muted)
    }

    fun muteRemoteVideo(muted: Boolean, uid: Int) {
        core.engine().rtcEngine().muteRemoteVideoStream(uid, muted)
    }

    @Synchronized
    fun enableLocalVideoCapturing(enabled: Boolean) {
        localVideoCaptured = enabled
        core.engine().rtcEngine().let {
            it.enableLocalVideo(enabled)
            if (enabled) {
                it.startPreview()
            } else {
                it.stopPreview()
            }

            // rtc engine violation that when open local
            // video capturing, it will also publish
            // video stream at the same time, thus we
            // must compulsively reset the mute state if
            // the stored flag tells us so
            if (enabled && !localVideoPublished) {
                muteLocalVideo(true)
            }
        }
    }

    fun switchCamera() {
        core.engine().rtcEngine().switchCamera()
    }

    fun isLocalVideoCaptureEnabled(): Boolean {
        return localVideoCaptured
    }

    fun isLocalVideoPublishEnabled(): Boolean {
        return localVideoCaptured
    }
}