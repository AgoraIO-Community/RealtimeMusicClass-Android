package io.agora.realtimemusicclass.base.utils

import org.junit.Test

class NumberUtilTest {
    @Test
    fun testIntStringTransform() {
        val num1 = NumberUtil.stringFromInt(-1)
        println("-1 transformed to $num1")

        val num2 = NumberUtil.stringFromInt(-1203)
        println("-1203 transformed to $num2")
    }
}