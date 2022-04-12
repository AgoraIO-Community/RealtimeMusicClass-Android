package io.agora.realtimemusicclass.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.agora.realtimemusicclass.base.edu.classroom.MusicManager
import io.agora.realtimemusicclass.base.ui.activities.BaseActivity
import io.agora.realtimemusicclass.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private val pendingDelay = 3000L

    private lateinit var binding: ActivitySplashBinding

    private lateinit var handler: Handler

    private val startMainRunnable = Runnable {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initGlobally()
        gotoMainActivity()
    }

    private fun initGlobally() {
        Thread {
            MusicManager.initMusicManager(applicationContext)
        }.start()
    }

    private fun gotoMainActivity() {
        handler = Handler(Looper.getMainLooper())
        handler.postDelayed(startMainRunnable, pendingDelay)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (this::handler.isInitialized) {
            handler.removeCallbacks(startMainRunnable)
        }
    }
}