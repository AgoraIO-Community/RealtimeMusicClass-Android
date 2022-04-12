package io.agora.realtimemusicclass.base.edu.core.data

data class RMCMediaInfo(
    var index: Int,
    var micDeviceState: Int,
    var audioStreamState: Int,
    var cameraDeviceState: Int,
    var videoStreamState: Int,
    var streamId: Int) {

    fun micShouldOpen(): Boolean {
        return micDeviceState == RMCDeviceState.On.value
    }

    fun cameraShouldOpen(): Boolean {
        return cameraDeviceState == RMCDeviceState.On.value
    }

    fun videoStreamMuted(): Boolean {
        return videoStreamState == RMCStreamState.Mute.value
    }

    fun audioStreamMuted(): Boolean {
        return audioStreamState == RMCStreamState.Mute.value
    }

    fun copy(): RMCMediaInfo {
        return RMCMediaInfo(
            this.index,
            this.micDeviceState,
            this.audioStreamState,
            this.cameraDeviceState,
            this.videoStreamState,
            this.streamId)
    }
}

enum class RMCDeviceState(val value: Int) {
    Off(0), On(1), Broken(2)
}

enum class RMCStreamState(val value: Int) {
    Mute(0), Publish(1)
}