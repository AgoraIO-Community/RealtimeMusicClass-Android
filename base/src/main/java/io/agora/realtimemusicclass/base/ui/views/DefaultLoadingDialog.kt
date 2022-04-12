package io.agora.realtimemusicclass.base.ui.views

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import io.agora.realtimemusicclass.base.R

class DefaultLoadingDialog(context: Context) : Dialog(context, R.style.loading_dialog) {
    init {
        setContentView(initLayout(context))
        this.setCanceledOnTouchOutside(false)

        window?.attributes?.let {
            it.width = 200
            it.height = 200
            window?.attributes = it
        }
    }

    @SuppressLint("InflateParams")
    private fun initLayout(context: Context): View {
        return LayoutInflater.from(context).inflate(
            R.layout.loading_dialog, null, false)
    }
}