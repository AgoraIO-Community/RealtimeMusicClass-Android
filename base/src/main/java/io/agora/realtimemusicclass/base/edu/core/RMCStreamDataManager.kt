package io.agora.realtimemusicclass.base.edu.core

import android.util.Log

class RMCStreamDataManager(private val core: RMCCore) : RMCRtcEventListener() {
    private var listener: RMCStreamDataListener? = null

    init {
        core.registerRtcEventListener(this)
    }

    fun createDataStream(): Int {
        return core.engine().rtcEngine().createDataStream(false, false)
    }

    fun sendStreamData(streamId: Int, data: ByteArray) {
        core.engine().rtcEngine().sendStreamMessage(streamId, data)
    }

    fun setStreamDataListener(listener: RMCStreamDataListener) {
        this.listener = listener
    }

    override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
        this.listener?.onStreamData(uid, streamId, data)
    }

    override fun onStreamMessageError(
        uid: Int,
        streamId: Int,
        error: Int,
        missed: Int,
        cached: Int
    ) {
        Log.e("RMCStreamDatamanager", "error")
    }

    fun recycle() {
        core.removeRtcEventListener(this)
    }
}

interface RMCStreamDataListener {
    fun onStreamData(uid: Int, streamId: Int, data: ByteArray?)
}