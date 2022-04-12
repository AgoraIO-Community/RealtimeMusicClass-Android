package io.agora.realtimemusicclass.base.utils

object NumberUtil {
    fun intFromString(text: String): Int {
        return text.toLongOrNull()?.toInt() ?: 0
    }

    fun stringFromInt(value: Int): String {
        return (value.toLong() and 0x00000000FFFFFFFFL).toString()
    }
}