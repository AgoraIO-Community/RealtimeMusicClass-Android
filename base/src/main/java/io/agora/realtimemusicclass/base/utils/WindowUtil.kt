package io.agora.realtimemusicclass.base.utils

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager

object WindowUtil {
    fun hideStatusBar(window: Window, darkText: Boolean) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        var flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && darkText) {
            flag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.decorView.systemUiVisibility = flag or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    fun getStatusBarHeight(context: Context?): Int {
        if (context == null) return 0
        val id = context.resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        )
        return if (id > 0) context.resources.getDimensionPixelSize(id) else id
    }
}