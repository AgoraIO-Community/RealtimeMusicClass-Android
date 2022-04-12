package io.agora.realtimemusicclass.chorus.view.broadcaster

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import io.agora.realtimemusicclass.chorus.R
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.chorus.databinding.SeatOpDialogLayoutBinding

class SeatOpDialog(context: Context,
                   private val listener: SeatOpDialogListener) : Dialog(context) {
    private var binding: SeatOpDialogLayoutBinding =
        SeatOpDialogLayoutBinding.inflate(LayoutInflater.from(context))
    private val titleFormat = context.getString(R.string.seat_op_dialog_title_format)

    init {
        setContentView(binding.root)
    }

    fun show(info: RMCUserInfo) {
        binding.seatOpDialogTitle.text = String.format(titleFormat, info.userName)
        binding.seatOpDialogCameraSwitch.isActivated = !info.videoStreamMuted()
        binding.seatOpDialogMicSwitch.isActivated = !info.audioStreamMuted()
        setCanceledOnTouchOutside(true)
        binding.seatOpDialogCloseBtn.setOnClickListener {
            listener.onDialogDismiss()
            dismiss()
        }

        binding.seatOpDialogCameraSwitch.setOnClickListener {
            it.isActivated = !it.isActivated
            listener.onVideoPublished(info, it.isActivated)
        }

        binding.seatOpDialogMicSwitch.setOnClickListener {
            it.isActivated = !it.isActivated
            listener.onAudioPublished(info, it.isActivated)
        }
        super.show()
    }
}

interface SeatOpDialogListener {
    fun onDialogDismiss()

    fun onVideoPublished(info: RMCUserInfo, published: Boolean)

    fun onAudioPublished(info: RMCUserInfo, published: Boolean)
}