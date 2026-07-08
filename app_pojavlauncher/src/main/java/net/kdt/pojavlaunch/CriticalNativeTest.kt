package net.kdt.pojavlaunch

import androidx.annotation.Keep
import dalvik.annotation.optimization.CriticalNative

@Keep
class CriticalNativeTest {
    companion object {
        @CriticalNative
        external fun testCriticalNative(arg0: Int, arg1: Int)

        fun invokeTest() {
            testCriticalNative(0, 0)
        }
    }
}
