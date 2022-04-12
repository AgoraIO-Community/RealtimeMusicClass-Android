package io.agora.realtimemusicclass.base.ui.actions

import android.annotation.SuppressLint
import android.content.Context
import android.widget.RelativeLayout
import io.agora.realtimemusicclass.base.MusicClassApp

@SuppressLint("ViewConstructor")
abstract class ActionSheet(context: Context,
                           private val application: MusicClassApp,
                           private val type: ActionSheetType,
                           private val listener: DefaultActionSheetListener? =  null)
    : RelativeLayout(context) {

    fun type(): ActionSheetType {
        return this.type
    }

    fun application(): MusicClassApp {
        return application
    }

    fun getListener(): DefaultActionSheetListener? {
        return listener
    }
}

enum class ActionSheetType {
    BgMusic, Console, VoiceEffect, User, Chat
}

interface DefaultActionSheetListener {

}