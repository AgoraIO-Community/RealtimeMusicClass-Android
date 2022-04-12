package io.agora.realtimemusicclass.base.ui.actions

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.agora.realtimemusicclass.base.MusicClassApp
import io.agora.realtimemusicclass.base.R
import io.agora.realtimemusicclass.base.utils.WindowUtil
import java.util.*

class ActionSheetUtil(private val application: MusicClassApp) {
    private var dialog: ActionBottomSheetDialog? = null

    private val stack: Stack<ActionSheet> = Stack()

    private fun getActionSheet(context: Context,
                               type: ActionSheetType,
                               listener: DefaultActionSheetListener? = null) : ActionSheet {
        return when (type) {
            ActionSheetType.User -> ActionSheetUser(
                context, application, ActionSheetType.User, listener)
            ActionSheetType.BgMusic -> ActionSheetMusic(
                context, application, ActionSheetType.BgMusic, listener)
            ActionSheetType.VoiceEffect -> ActionSheetVoiceEffect(
                context, application, ActionSheetType.VoiceEffect, listener)
            ActionSheetType.Console -> ActionSheetConsole(
                context, application, ActionSheetType.Console, listener
            )
            ActionSheetType.Chat -> ActionSheetChat(
                context, application, ActionSheetType.Chat, listener
            )
        }
    }

    private fun isShowing(): Boolean {
        return dialog?.isShowing ?: false
    }

    private fun dialog(): ActionBottomSheetDialog? {
        return dialog
    }

    /**
     * Returns current or the top-most action sheet (if
     * action sheets are stacked)
     */
    fun getCurrentAction(): ActionSheet? {
        return if (isShowing()) {
            dialog()?.getActionSheet()
        } else null
    }

    fun showActionSheetDialog(context: Context,
                              type: ActionSheetType,
                              listener: DefaultActionSheetListener? = null,
                              pushToStack: Boolean = false) {
        showActionSheetDialog(context, getActionSheet(context, type, listener), pushToStack)
    }

    private fun showActionSheetDialog(context: Context,
                                      actionSheet: ActionSheet,
                                      pushToStack: Boolean = false) {
        if (!pushToStack) {
            stack.clear()
        }
        stack.push(actionSheet)
        show(context, actionSheet)
    }

    private fun show(context: Context, actionSheet: ActionSheet) {
        dismiss()

        ActionBottomSheetDialog(context, R.style.bottom_action_dialog, actionSheet).let {
            dialog = it
            it.setCanceledOnTouchOutside(true)
            it.dismissWithAnimation = true

            it.window?.let { window ->
                WindowUtil.hideStatusBar(window, true)
            }

            it.setOnDismissListener {
                if (stack.isNotEmpty() && stack.peek() == actionSheet) {
                    stack.pop()
                    if (stack.isNotEmpty()) {
                        val prevSheet = stack.peek()
                        (prevSheet.parent as? ViewGroup)?.removeAllViews()
                        show(context, prevSheet)
                    }
                }
            }

            it.show()
        }
    }

    fun dismiss() {
        dialog?.let {
            synchronized(this@ActionSheetUtil) {
                if (it.isShowing) {
                    ContextCompat.getMainExecutor(application).execute {
                        it.dismiss()
                        dialog = null
                    }
                }
            }
        }
    }
}

class ActionBottomSheetDialog(context: Context,
                              @StyleRes theme: Int,
                              private val actionSheet: ActionSheet)
    : BottomSheetDialog(context, theme) {

    fun getActionSheet(): ActionSheet {
        return actionSheet
    }

    init {
        setContentView(actionSheet)
    }
}