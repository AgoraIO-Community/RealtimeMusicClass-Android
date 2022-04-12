package io.agora.realtimemusicclass.base.edu.core

import io.agora.realtimemusicclass.base.edu.core.data.RMCCallback
import io.agora.realtimemusicclass.base.edu.core.data.RMCError
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.server.RestfulCallback
import io.agora.realtimemusicclass.base.server.ServerRestful
import io.agora.realtimemusicclass.base.server.struct.ServerResponseBody
import io.agora.realtimemusicclass.base.server.struct.body.UserUpdateBody

class RMCService {
    private val restfulApi = ServerRestful()

    fun heartbeat(className: String, userName: String) {
        restfulApi.heartbeat(className, userName)
    }

    fun leaveClass(className: String, userName: String, callback: RMCCallback<String>? = null) {
        restfulApi.leaveClass(className, userName, RMCInternalSimpleCallback(callback))
    }

    fun updateUserInfo(request: UserInfoUpdateRequest, callback: RMCCallback<String>) {
        val body = UserUpdateBody(
            request.avatar,
            request.gender,
            request.videoDeviceState,
            request.micDeviceState,
            request.videoStreamState,
            request.audioStreamState,
            request.streamId,
            request.ext)
        restfulApi.updateUserInfo(request.className,
            request.userName, body, RMCInternalSimpleCallback(callback))
    }

    fun getClassUserInfoList(className: String, userType: String?,
                             callback: RMCCallback<List<RMCUserInfo>>? = null) {
        restfulApi.getClassUserList(className, userType,
            RMCInternalTransformCallback(callback) { respList ->
                val list = mutableListOf<RMCUserInfo>()
                respList?.forEach {
                    RMCServiceDataTransformer.toRMCUserInfo(it)?.let { result ->
                        list.add(result)
                    }
                }
                list
            })
    }

    fun getClassUserInfo(className: String, userName: String,
                         callback: RMCCallback<RMCUserInfo>? = null) {
        restfulApi.getClassUserInfo(className, userName,
            RMCInternalTransformCallback(callback) {
                RMCServiceDataTransformer.toRMCUserInfo(it)
            })
    }
}

class RMCInternalSimpleCallback<T>(
    private val callback: RMCCallback<T>? = null
) : RestfulCallback<ServerResponseBody<T>> {
    override fun onSuccess(t: ServerResponseBody<T>?) {
        t?.let {
            callback?.onSuccess(it.data)
        }
    }

    override fun onFailure(code: Int, msg: String?) {
        callback?.onFailure(RMCError(code, msg))
    }
}

class RMCInternalTransformCallback<T, S>(
    private val callback: RMCCallback<S>? = null,
    private val transformer: ((T?) -> S?)
) : RestfulCallback<ServerResponseBody<T>> {
    override fun onSuccess(t: ServerResponseBody<T>?) {
        t?.let {
            callback?.onSuccess(transformer.invoke(it.data))
        }
    }

    override fun onFailure(code: Int, msg: String?) {
        callback?.onFailure(RMCError(code, msg))
    }
}