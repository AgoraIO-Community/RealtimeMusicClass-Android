package io.agora.realtimemusicclass.base.server

import com.google.gson.Gson
import io.agora.realtimemusicclass.base.BuildConfig
import io.agora.realtimemusicclass.base.server.callback.ThrowableCallback
import io.agora.realtimemusicclass.base.server.network.BusinessException
import io.agora.realtimemusicclass.base.server.network.RetrofitManager
import io.agora.realtimemusicclass.base.server.service.ClassService
import io.agora.realtimemusicclass.base.server.struct.ServerResponseBody
import io.agora.realtimemusicclass.base.server.struct.body.ClassCreateBody
import io.agora.realtimemusicclass.base.server.struct.body.ClassJoinBody
import io.agora.realtimemusicclass.base.server.struct.body.UserUpdateBody
import io.agora.realtimemusicclass.base.server.struct.response.*

class ServerRestful {
    private val service = RetrofitManager.getService(BuildConfig.API_BASE_URL, ClassService::class.java)

    companion object {
        const val successCode = 0
    }

    fun createClass(body: ClassCreateBody,
                    callback: RestfulCallback<ServerResponseBody<ClassBriefInfoResp>>) {
        service.createClass(body).enqueue(buildRetrofitCallback(callback))
    }

    fun deleteClass(className: String,
                   callback: RestfulCallback<ServerResponseBody<String>>) {
        service.deleteClass(className).enqueue(buildRetrofitCallback(callback))
    }

    fun getClassInfo(className: String,
                    callback: RestfulCallback<ServerResponseBody<ClassBriefInfoResp>>) {
        service.getClassInfo(className).enqueue(buildRetrofitCallback(callback))
    }

    fun getClassList(pageNo: Int,
                     callback: RestfulCallback<ServerResponseBody<List<ClassBriefInfoResp>>>) {
        service.getClassList(pageNo).enqueue(buildRetrofitCallback(callback))
    }

    fun getClassListByCreator(creator: String,
                              callback: RestfulCallback<ServerResponseBody<List<ClassBriefInfoResp>>>) {
        service.getClassListByCreator(creator).enqueue(buildRetrofitCallback(callback))
    }

    fun joinClass(className: String, body: ClassJoinBody,
                  callback: RestfulCallback<ServerResponseBody<ClassJoinResp>>) {
        service.joinClass(className, body).enqueue(buildRetrofitCallback(callback))
    }

    fun leaveClass(className: String, userName: String,
                   callback: RestfulCallback<ServerResponseBody<String>>) {
        service.leaveClass(className, userName).enqueue(buildRetrofitCallback(callback))
    }

    fun heartbeat(className: String,
                  userName: String,
                  callback: RestfulCallback<ServerResponseBody<String>>? = null) {
        service.heartbeat(className, userName).enqueue(buildRetrofitCallback(callback))
    }

    fun getClassUserList(className: String, userType: String?,
                         callback: RestfulCallback<ServerResponseBody<List<UserInfoResp>>>) {
        userType?.let {
            service.getClassUserListByRole(className, userType).enqueue(buildRetrofitCallback(callback))
        } ?: run {
            service.getClassFullUserList(className).enqueue(buildRetrofitCallback(callback))
        }
    }

    fun getClassUserInfo(className: String, userName: String,
                         callback: RestfulCallback<ServerResponseBody<UserInfoResp>>) {
        service.getClassUserInfo(className, userName).enqueue(buildRetrofitCallback(callback))
    }

    fun updateUserInfo(className: String, userName: String, body: UserUpdateBody,
                       callback: RestfulCallback<ServerResponseBody<String>>) {
        service.updateUserInfo(className, userName, body).enqueue(buildRetrofitCallback(callback))
    }

    private fun <T: ServerResponseBody<*>?> buildRetrofitCallback(
        callback: RestfulCallback<T>?): RetrofitManager.RetrofitCallback<T> {
        return RetrofitManager.RetrofitCallback(
            successCode, InternalThrowableCallback(callback))
    }
}

internal class InternalThrowableCallback<T : ServerResponseBody<*>?>(
    private val callback: RestfulCallback<T>? = null
) : ThrowableCallback<T> {
    override fun onSuccess(res: T?) {
        res?.let {
            callback?.onSuccess(res)
        } ?: Runnable {
            callback?.onFailure(RestfulError.default, "response is empty")
        }
    }

    override fun onFailure(throwable: Throwable?) {
        (throwable as? BusinessException)?.let {
            callback?.onFailure(it.code, it.message ?: Gson().toJson(it))
        } ?: Runnable {
            callback?.onFailure(RestfulError.default, throwable?.message ?: "")
        }
    }
}

interface RestfulCallback<T> {
    fun onSuccess(t: T?)

    fun onFailure(code: Int, msg: String?)
}

class RestfulError {
    companion object ErrorCode {
        const val default = -1
    }
}