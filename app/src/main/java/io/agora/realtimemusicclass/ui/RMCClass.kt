package io.agora.realtimemusicclass.ui

import android.content.Context
import android.content.Intent
import io.agora.realtimemusicclass.base.edu.classroom.ClassManager
import io.agora.realtimemusicclass.base.edu.core.data.RMCSceneType
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.ui.activities.BaseClassActivity
import io.agora.realtimemusicclass.chorus.ChorusActivity
import io.agora.realtimemusicclass.piano.PianoActivity
import io.agora.relatimemusicclass.instrument.InstrumentActivity

object RMCClass {
    fun launch(context: Context,
               classType: Int,
               className: String,
               classId: String,
               userInfo: RMCUserInfo) {

        classType(classType)?.let { target ->
            val intent = Intent(context, target)
            ClassManager.launchClass(context,
                intent, className, classId, userInfo)
        }
    }

    private fun classType(classType: Int): Class<out BaseClassActivity>? {
        return when (classType) {
            RMCSceneType.Chorus.value -> ChorusActivity::class.java
            RMCSceneType.Instruments.value -> InstrumentActivity::class.java
            RMCSceneType.Piano.value -> PianoActivity::class.java
            else -> null
        }
    }
}