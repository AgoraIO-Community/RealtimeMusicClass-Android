package io.agora.realtimemusicclass.chorus.view.bottom

import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole

class BottomActionBarWrapper(private val bottomAction: BottomActionBar,
                             private var role: RMCUserRole,
                             private val listener: BottomActionListener) : BottomActionBarClickListener {
    init {
        bottomAction.setListener(this)
        initBottomActionBar()
    }

    private fun initBottomActionBar() {
        if (!granted()) {
            bottomAction.setEnabled(BottomAction.InEar, false)
            bottomAction.setEnabled(BottomAction.Camera, false)
            bottomAction.setEnabled(BottomAction.Mic, false)
        } else {
            bottomAction.setEnabled(BottomAction.InEar, listener.onInEarEnabled())
            bottomAction.setEnabled(BottomAction.Camera, listener.onCameraEnabled())
            bottomAction.setEnabled(BottomAction.Mic, listener.onMicEnabled())
        }
    }

    /**
     * Currently we do not need to change the role
     * dynamically in a class, but leave this
     * for future use.
     */
    fun changeRole(role: RMCUserRole) {
        if (role.value != this.role.value) {
            this.role = role
        }
    }

    private fun granted(): Boolean {
        return role.isTeacher() || role.isStudent()
    }

    /**
     * Called when in-ear is enabled/disabled via
     * outside operation, not triggered by clicking
     * bottom action buttons
     */
    fun setInEarEnabled(enabled: Boolean) {
        if (granted()) {
            bottomAction.setEnabled(BottomAction.InEar, enabled)
        }
    }

    /**
     * Called when camera is enabled/disabled via
     * outside operations, not triggered by clicking
     * bottom action buttons
     */
    fun setCameraEnabled(enabled: Boolean) {
        if (granted()) {
            bottomAction.setEnabled(BottomAction.Camera, enabled)
        }
    }

    /**
     * Called when microphone is enabled/disabled via
     * outside operations, not triggered by clicking
     * bottom action buttons
     */
    fun setMicEnabled(enabled: Boolean) {
        if (granted()) {
            bottomAction.setEnabled(BottomAction.Mic, enabled)
        }
    }

    override fun onActionClicked(action: BottomAction, enabled: Boolean) {
        if (action == BottomAction.Help ||
                action == BottomAction.Chat) {
            listener.onActionPerformed(action, enabled)
        } else {
            if (!granted()) {
                listener.onError("You are not enable to perform " +
                        "this action because you are audience")
                return
            }

            if (action == BottomAction.InEar &&
                    !listener.onInEarEnabled()) {
                listener.onError("In-ear is not currently usable, please " +
                        "plug in a wired headphone with microphone.")
                return
            }

            val state = !enabled
            bottomAction.setEnabled(action, state)
            listener.onActionPerformed(action, state)
        }
    }
}

interface BottomActionListener {
    fun onActionPerformed(action: BottomAction, enabled: Boolean)

    fun onInEarEnabled(): Boolean

    fun onCameraEnabled(): Boolean

    fun onMicEnabled(): Boolean

    fun onError(message: String)
}