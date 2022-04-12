package io.agora.realtimemusicclass.base

import android.app.Application
import android.os.Handler
import android.os.HandlerThread

class MusicClassApp: Application() {
    private lateinit var worker: HandlerThread
    private var handler: Handler? = null

    override fun onCreate() {
        super.onCreate()
        initWorker()
    }

    @Synchronized
    fun worker(): Handler? {
        return handler
    }

    override fun onTerminate() {
        super.onTerminate()
        destroyWorker()
    }

    @Synchronized
    private fun initWorker() {
        worker = HandlerThread("defaultWorker")
        worker.start()
        handler = Handler(worker.looper)
    }

    @Synchronized
    private fun destroyWorker() {
        handler = null
        worker.quitSafely()
    }
}