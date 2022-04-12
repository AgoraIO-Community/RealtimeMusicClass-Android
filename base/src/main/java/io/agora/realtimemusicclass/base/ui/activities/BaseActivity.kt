package io.agora.realtimemusicclass.base.ui.activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.agora.realtimemusicclass.base.MusicClassApp
import io.agora.realtimemusicclass.base.R
import io.agora.realtimemusicclass.base.databinding.ActivityBaseBinding
import io.agora.realtimemusicclass.base.edu.core.RMCCore
import io.agora.realtimemusicclass.base.edu.core.RMCCoreStateListener
import io.agora.realtimemusicclass.base.ui.actions.ActionSheetUtil
import io.agora.realtimemusicclass.base.ui.views.DefaultLoadingDialog
import io.agora.realtimemusicclass.base.utils.WindowUtil

abstract class ActionActivity : BaseClassActivity() {
    private var actionSheetUtil: ActionSheetUtil? = null

    fun actionSheetUtil(): ActionSheetUtil? {
        return actionSheetUtil
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionSheetUtil = ActionSheetUtil(application())
    }

    override fun onStop() {
        actionSheetUtil()?.dismiss()
        super.onStop()
    }
}

interface ClassLifecycleListener {
    fun onClassJoined(rmcCore: RMCCore)

    fun onClassLeft(rmcCore: RMCCore)
}

abstract class BaseClassActivity : BaseActivity(), RMCCoreStateListener {
    private lateinit var rmcCore: RMCCore

    private val requestCode = 1
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )

    private var headphoneWithMicPlugged = false

    private val headPhoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (AudioManager.ACTION_HEADSET_PLUG == action) {
                val plugged = intent.getIntExtra("state", -1) == 1
                val hasMic = intent.getIntExtra("microphone", -1) == 1
                headphoneWithMicPlugged = plugged && hasMic
                onHeadsetWithMicPlugged(headphoneWithMicPlugged)
            }
        }
    }

    fun headphoneInEarEnabled(): Boolean {
        return headphoneWithMicPlugged
    }

    open fun onHeadsetWithMicPlugged(plugged: Boolean) {

    }

    private val classLifecycleListeners = mutableListOf<ClassLifecycleListener>()

    fun registerClassLifecycleListener(listener: ClassLifecycleListener) {
        if (!classLifecycleListeners.contains(listener)) {
            classLifecycleListeners.add(listener)
        }
    }

    fun removeClassLifecycleListener(listener: ClassLifecycleListener) {
        classLifecycleListeners.remove(listener)
    }

    fun callClassJoined(rmcCore: RMCCore) {
        classLifecycleListeners.forEach {
            it.onClassJoined(rmcCore)
        }
    }

    fun callClassLeft(rmcCore: RMCCore) {
        classLifecycleListeners.forEach {
            it.onClassLeft(rmcCore)
        }
    }

    fun application(): MusicClassApp {
        return application as MusicClassApp
    }

    fun worker(): Handler? {
        return application().worker()
    }

    override fun onRMCCoreCreateSuccess() {

    }

    override fun onRMCCoreCreateFail(code: Int, msg: String?) {

    }

    override fun onRMCCoreAbort() {
        synchronized(this) {
            if (!isFinishing) {
                runOnUiThread {
                    AlertDialog.Builder(this).create().let {
                        it.setTitle(R.string.dialog_kicked_out_by_remote_user_title)
                        it.setCancelable(false)
                        it.setButton(AlertDialog.BUTTON_NEUTRAL,
                            getText(R.string.dialog_kicked_out_by_remote_user_button_text)) { _, _ ->
                            it.dismiss()
                            finish()
                        }
                        it.show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onIncomingData()

        val headPhoneFilter = IntentFilter()
        headPhoneFilter.addAction(AudioManager.ACTION_HEADSET_PLUG)
        registerReceiver(headPhoneReceiver, headPhoneFilter)

        rmcCore = RMCCore(applicationContext, getString(R.string.agora_app_id), this)
        addLifecycleCallbacks(rmcCore)

        // Class will prevent the screen from lighting off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun rmcCore(): RMCCore {
        return rmcCore
    }

    abstract fun onIncomingData()

    private fun checkPermissions(): Boolean {
        var granted = true
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it)
                != PackageManager.PERMISSION_GRANTED) {
                granted = false
                return@forEach
            }
        }
        return granted
    }

    fun requestPermissions() {
        if (checkPermissions()) {
            onPermissionGranted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, requestCode)
            } else {
                onPermissionFail()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            var granted = true
            if (grantResults.size == permissions.size) {
                grantResults.forEach {
                    if (it != PackageManager.PERMISSION_GRANTED) {
                        granted = false
                        return@forEach
                    }
                }
            }

            if (!granted) {
                checkPermissions()
            } else {
                onPermissionGranted()
            }
        }
    }

    open fun onPermissionGranted() {

    }

    open fun onPermissionFail() {

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(headPhoneReceiver)
        removeLifecycleCallback(rmcCore)
    }
}

open class BaseActivity : AppCompatActivity() {
    private lateinit var baseBind: ActivityBaseBinding

    private val lifecycleManager = BaseActivityLifecycleManager()

    private var loadingDialog: DefaultLoadingDialog? = null

    fun showLoading() {
        dismissLoadingDialog()
        DefaultLoadingDialog(this).let {
            loadingDialog = it
            it.show()
        }
    }

    fun isLoading(): Boolean {
        return loadingDialog?.isShowing ?: false
    }

    fun dismissLoadingDialog() {
        if (isLoading()) {
            loadingDialog?.dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBaseLayout()
        lifecycle.addObserver(lifecycleManager)
    }

    private fun initBaseLayout() {
        WindowUtil.hideStatusBar(window, true)
        baseBind = ActivityBaseBinding.inflate(layoutInflater)
        val statusBarHeight = WindowUtil.getStatusBarHeight(this)
        val params = baseBind.baseLayout.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = statusBarHeight
        baseBind.baseLayout.layoutParams = params
        super.setContentView(baseBind.root)
    }

    fun baseBinding(): ActivityBaseBinding {
        return baseBind
    }

    override fun setContentView(layoutResID: Int) {
        layoutInflater.inflate(layoutResID, baseBind.baseLayout)
    }

    override fun setContentView(view: View?) {
        view?.let {
            baseBind.baseLayout.addView(it)
        }
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        view?.let { v ->
            params?.let { p ->
                baseBind.baseLayout.addView(v, p)
            } ?: Runnable {
                baseBind.baseLayout.addView(v)
            }
        }
    }

    fun addLifecycleCallbacks(callback: BaseActivityLifeCycleCallback) {
        lifecycleManager.addCallbacks(callback)
    }

    fun removeLifecycleCallback(callback: BaseActivityLifeCycleCallback) {
        lifecycleManager.removeCallback(callback)
    }

    fun removeAllCallbacks() {
        lifecycleManager.removeAllCallbacks()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(lifecycleManager)
    }
}

class BaseActivityLifecycleManager : DefaultLifecycleObserver {
    private val lifecycleCallbacks = mutableListOf<BaseActivityLifeCycleCallback>()

    @Synchronized
    fun addCallbacks(callback: BaseActivityLifeCycleCallback) {
        if (!lifecycleCallbacks.contains(callback)) {
            lifecycleCallbacks.add(callback)
        }
    }

    @Synchronized
    fun removeCallback(callback: BaseActivityLifeCycleCallback) {
        lifecycleCallbacks.remove(callback)
    }

    @Synchronized
    fun removeAllCallbacks() {
        lifecycleCallbacks.clear()
    }

    @Synchronized
    override fun onCreate(owner: LifecycleOwner) {
        lifecycleCallbacks.forEach {
            it.onCreated(owner)
        }
    }

    @Synchronized
    override fun onStart(owner: LifecycleOwner) {
        lifecycleCallbacks.forEach {
            it.onStarted(owner)
        }
    }

    @Synchronized
    override fun onResume(owner: LifecycleOwner) {
        lifecycleCallbacks.forEach {
            it.onResumed(owner)
        }
    }

    @Synchronized
    override fun onPause(owner: LifecycleOwner) {
        lifecycleCallbacks.forEach {
            it.onPaused(owner)
        }
    }

    @Synchronized
    override fun onStop(owner: LifecycleOwner) {
        lifecycleCallbacks.forEach {
            it.onStopped(owner)
        }
    }

    @Synchronized
    override fun onDestroy(owner: LifecycleOwner) {
        lifecycleCallbacks.forEach {
            it.onDestroyed(owner)
        }
    }
}