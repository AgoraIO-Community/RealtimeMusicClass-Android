package io.agora.realtimemusicclass.base.edu.core.data

interface RMCCallback<T> {
    fun onSuccess(res: T?)
    fun onFailure(error: RMCError)
}

data class RMCError(
    val type: Int,
    val msg: String?) {

    companion object {
        const val defaultError = -1
    }
}