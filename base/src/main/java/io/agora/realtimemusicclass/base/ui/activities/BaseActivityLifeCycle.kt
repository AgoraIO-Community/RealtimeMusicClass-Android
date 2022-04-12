package io.agora.realtimemusicclass.base.ui.activities

import androidx.lifecycle.LifecycleOwner

abstract class BaseActivityLifeCycleCallback {
    open fun onCreated(owner: LifecycleOwner) {

    }

    open fun onStarted(owner: LifecycleOwner) {

    }

    open fun onResumed(owner: LifecycleOwner) {

    }

    open fun onPaused(owner: LifecycleOwner) {

    }

    open fun onStopped(owner: LifecycleOwner) {

    }

    open fun onDestroyed(owner: LifecycleOwner) {

    }
}