package io.agora.realtimemusicclass.base.utils

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes

object ToastUtil {
    private const val interval = 3000L
    private var lastTs = 0L

    fun toast(activity: Activity, @StringRes res: Int) {
        toast(activity, activity.resources.getString(res))
    }

    fun toast(activity: Activity, message: String) {
        val ts = System.currentTimeMillis()
        if (ts - lastTs > interval) {
            showToast(activity, message)
            lastTs = ts
        }
    }

    private fun showToast(activity: Activity, message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }
}