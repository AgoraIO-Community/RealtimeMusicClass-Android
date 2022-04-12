package io.agora.realtimemusicclass.base.edu.classroom

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import io.agora.realtimemusicclass.base.edu.classroom.ClassManagerDataConvertor.toRMCRoomInfo
import io.agora.realtimemusicclass.base.edu.core.RMCInternalSimpleCallback
import io.agora.realtimemusicclass.base.edu.core.RMCInternalTransformCallback
import io.agora.realtimemusicclass.base.edu.core.RMCServiceDataTransformer
import io.agora.realtimemusicclass.base.edu.core.data.RMCCallback
import io.agora.realtimemusicclass.base.edu.core.data.RMCClassInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.server.ServerRestful
import io.agora.realtimemusicclass.base.server.struct.body.ClassCreateBody
import io.agora.realtimemusicclass.base.server.struct.body.ClassJoinBody

object ClassManager {
    private val api = ServerRestful()

    const val KEY_CLASS_NAME = "class-name"
    const val KEY_CHANNEL_ID = "channel-id"
    const val KEY_USER_NAME = "user-name"
    const val KEY_USER_ROLE = "user-role"

    const val KEY_USER_INFO = "full-user-info"

    fun createClass(request: ClassCreateRequest, callback: RMCCallback<RMCClassInfo>?) {
        val body = ClassCreateBody(
            request.className,
            request.creator,
            request.password)
        api.createClass(body, RMCInternalTransformCallback(callback) {
            toRMCRoomInfo(it)
        })
    }

    fun deleteClass(className: String?, callback: RMCCallback<String>?) {
        api.deleteClass(className!!, RMCInternalSimpleCallback(callback))
    }

    fun getClassInfo(className: String, callback: RMCCallback<RMCClassInfo>?) {
        api.getClassInfo(className, RMCInternalTransformCallback(callback) {
            toRMCRoomInfo(it)
        })
    }

    fun getClassList(pageNo: Int, callback: RMCCallback<List<RMCClassInfo>>?) {
        api.getClassList(pageNo, RMCInternalTransformCallback(callback) {
            val result: MutableList<RMCClassInfo> = ArrayList()
            it?.forEach { resp ->
                toRMCRoomInfo(resp)?.let { info ->
                    result.add(info)
                }
            }
            result
        })
    }

    fun getClassListByCreator(creator: String, callback: RMCCallback<List<RMCClassInfo>>?) {
        api.getClassListByCreator(creator, RMCInternalTransformCallback(callback) {
            val result: MutableList<RMCClassInfo> = ArrayList()
            it?.forEach { resp ->
                toRMCRoomInfo(resp)?.let { info ->
                    result.add(info)
                }
            }
            result
        })
    }

    fun preClassCheck(className: String, password: String?, userInfo: RMCUserInfo,
                 callback: RMCCallback<RMCUserInfo>? = null) {
        val body = ClassJoinBody(userInfo.userName, userInfo.role, userInfo.avatar, password)
        api.joinClass(className, body, RMCInternalTransformCallback(callback) {
            if (it == null) {
                RMCUserInfo(userInfo.userName, userInfo.role, userInfo.avatar, userInfo.gender, null)
            } else {
                RMCUserInfo(it.userName, it.role, it.avatar, it.gender,
                    RMCServiceDataTransformer.toRMCMediaInfo(it.media), it.ext?.toMutableMap())
            }
        })
    }

    fun launchClass(context: Context, intent: Intent, className: String,
                    channelId: String, userInfo: RMCUserInfo) {
        intent.putExtra(KEY_CLASS_NAME, className)
        intent.putExtra(KEY_CHANNEL_ID, channelId)
        intent.putExtra(KEY_USER_INFO, Gson().toJson(userInfo))
        context.startActivity(intent)
    }
}