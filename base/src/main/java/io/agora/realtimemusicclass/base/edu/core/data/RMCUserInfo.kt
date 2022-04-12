package io.agora.realtimemusicclass.base.edu.core.data

import android.content.Context
import io.agora.realtimemusicclass.base.R

data class RMCUserInfo(
    var userName: String,
    var role: Int,
    var avatar: String?,
    var gender: Int,
    var media: RMCMediaInfo? = null,
    var ext: MutableMap<String, Any>? = null) {

    fun isTeacher(): Boolean {
        return role == RMCUserRole.ROLE_TYPE_TEACHER.value
    }

    fun isStudent(): Boolean {
        return role == RMCUserRole.ROLE_TYPE_STUDENT.value
    }

    fun role(): RMCUserRole {
        return RMCUserRole.fromValue(this.role)
    }

    companion object {
        fun defaultUserInfo(): RMCUserInfo {
            return RMCUserInfo("Default",
                RMCUserRole.ROLE_TYPE_UNKNOWN.value,
            null, 0, null)
        }
    }

    fun copy(): RMCUserInfo {
        return RMCUserInfo(this.userName, this.role,
            this.avatar, this.gender,
            if (media != null) media!!.copy() else null, ext)
    }

    fun set(info: RMCUserInfo) {
        this.userName = info.userName
        this.role = info.role
        this.avatar = info.avatar
        this.gender = info.gender

        if (info.media == null) {
            this.media = null
        } else if (this.media == null) {
            this.media = info.media
        } else {
            this.media!!.index = info.media!!.index
            this.media!!.audioStreamState = info.media!!.audioStreamState
            this.media!!.micDeviceState = info.media!!.micDeviceState
            this.media!!.cameraDeviceState = info.media!!.cameraDeviceState
            this.media!!.videoStreamState = info.media!!.videoStreamState
            this.media!!.streamId = info.media!!.streamId
        }

        this.ext = info.ext
    }

    fun videoStreamMuted(): Boolean {
        return media?.videoStreamMuted() ?: true
    }

    fun audioStreamMuted(): Boolean {
        return media?.audioStreamMuted() ?: true
    }

    fun cameraShouldOpen(): Boolean {
        return media?.cameraShouldOpen() ?: false
    }

    fun micShouldOpen(): Boolean {
        return media?.micShouldOpen() ?: false
    }
}

enum class RMCUserRole(val value: Int) {
    ROLE_TYPE_UNKNOWN(-1),
    ROLE_TYPE_TEACHER(0),
    ROLE_TYPE_STUDENT(1),
    ROLE_TYPE_AUDIENCE(2);

    fun isTeacher(): Boolean {
        return this.value == ROLE_TYPE_TEACHER.value
    }

    fun isStudent(): Boolean {
        return this.value == ROLE_TYPE_STUDENT.value
    }

    companion object {
        fun fromValue(value: Int): RMCUserRole {
            return when (value) {
                ROLE_TYPE_TEACHER.value -> ROLE_TYPE_TEACHER
                ROLE_TYPE_STUDENT.value -> ROLE_TYPE_STUDENT
                ROLE_TYPE_AUDIENCE.value -> ROLE_TYPE_AUDIENCE
                else -> ROLE_TYPE_UNKNOWN
            }
        }

        fun toName(context: Context, value: Int) : String {
            return toName(context, fromValue(value))
        }

        fun toName(context: Context, role: RMCUserRole) : String {
            return when (role) {
                ROLE_TYPE_TEACHER -> context.resources.getString(R.string.role_name_teacher)
                ROLE_TYPE_STUDENT -> context.resources.getString(R.string.role_name_student)
                ROLE_TYPE_AUDIENCE -> context.resources.getString(R.string.role_name_audience)
                else -> context.resources.getString(R.string.role_name_unknown)
            }
        }
    }
}