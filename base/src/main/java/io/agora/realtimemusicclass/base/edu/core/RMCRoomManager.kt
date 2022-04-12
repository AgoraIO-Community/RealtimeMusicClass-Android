package io.agora.realtimemusicclass.base.edu.core

import android.os.Handler
import io.agora.realtimemusicclass.base.edu.core.data.*
import java.util.*

class RMCRoomManager(private val mRmcCore: RMCCore) {
    private val mTimer: HeartBeatTimer = HeartBeatTimer(mRmcCore)
    lateinit var channelId: String
    lateinit var className: String

    internal fun join(token: String?, className: String, channelId: String,
             info: RMCUserInfo, params: String? = null, callback: RMCCallback<Int>? = null) {
        this.channelId = channelId
        this.className = className
        mRmcCore.engine().join(token, channelId, info.userName, 0, params,
            object : RMCCallback<Int> {
                override fun onSuccess(res: Int?) {
                    mRmcCore.user().applyLocalUserRole(RMCUserRole.fromValue(info.role))
                    mRmcCore.notification()?.userJoin(info)
                    mTimer.startHeartBeat(className, info.userName)
                    callback?.onSuccess(res)
                }

                override fun onFailure(error: RMCError) {
                    callback?.onFailure(error)
                }
            })
    }

    internal fun leave(className: String,
                       info: RMCUserInfo,
                       callback: RMCCallback<String>? = null,
                       handler: Handler? = null) {
        mTimer.stopHeartBeat()

        handler?.post {
            leave(className, info, callback)
        } ?: run {
            leave(className, info, callback)
        }
    }

    private fun leave(className: String, info: RMCUserInfo,
                      callback: RMCCallback<String>?) {
        mRmcCore.service().leaveClass(className, info.userName,
            object : RMCCallback<String> {
                override fun onSuccess(res: String?) {

                }
                override fun onFailure(error: RMCError) {

                }
            })

        mRmcCore.notification()?.userLeave(info)
        mRmcCore.engine().leave(null)
        callback?.onSuccess("leave success")
    }

    /**
     * Leave when kicked out by remote user login,
     * and local user must not send message to both
     * server and other users.
     */
    internal fun leaveWhenAborted() {
        mTimer.stopHeartBeat()
        mRmcCore.engine().leave(null)
    }

    companion object {
        private const val TAG = "RMCRoomManager"
    }
}

internal class HeartBeatTimer(private val core: RMCCore) {
    private val timer = Timer()
    fun startHeartBeat(className: String?, userName: String?) {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                core.service().heartbeat(className!!, userName!!)
            }
        }, 0, period)
    }

    fun stopHeartBeat() {
        timer.cancel()
    }

    companion object {
        // Send heart-beating signals between a fixed
        // period, currently 10 minutes.
        private const val period = 10 * 60 * 1000L
    }
}