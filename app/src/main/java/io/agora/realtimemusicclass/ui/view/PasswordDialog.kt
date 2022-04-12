package io.agora.realtimemusicclass.ui.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.realtimemusicclass.R
import io.agora.realtimemusicclass.base.utils.WindowUtil
import io.agora.realtimemusicclass.ui.fragments.RoomListInfoItem

class PasswordDialog(context: Context,
                     private val classInfo: RoomListInfoItem,
                     private val listener: OnConfirmButtonClickListener? = null)
    : Dialog(context, R.style.AgoraMusicBaseDialog) {
    private var pwdEdit: AppCompatEditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.let { WindowUtil.hideStatusBar(it, true) }
    }

    private fun initView() {
        setContentView(R.layout.password_input_dialog)
        pwdEdit = findViewById(R.id.password_input_edit)!!

        findViewById<AppCompatImageView>(R.id.password_input_close_btn)?.setOnClickListener {
            dismiss()
        }

        findViewById<AppCompatImageView>(R.id.password_input_edit_area_clear_btn)?.setOnClickListener {
            pwdEdit?.setText("")
        }

        findViewById<AppCompatTextView>(R.id.password_input_confirm_btn)?.setOnClickListener {
            enterRoom()
        }
    }

    private fun enterRoom() {
        val pwd = pwdEdit?.text.toString()
        listener?.onRoomEnter(classInfo, pwd)
    }

    override fun show() {
        super.show()
        initView()
    }
}

interface OnConfirmButtonClickListener {
    fun onRoomEnter(classInfo: RoomListInfoItem, password: String)
}