package git.artdeell.dnbootstrap.glfw

import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import git.artdeell.dnbootstrap.utils.Utils
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Collections
import java.util.WeakHashMap

object GLFW {
    private val grabListeners = Collections.newSetFromMap(WeakHashMap<GrabListener, Boolean>())
    private var cursorImpl: WeakReference<CursorImplementor>? = null
    private var clipboardImpl: WeakReference<ClipboardProvider>? = null
    private var gamepadEnable: WeakReference<GamepadEnableHandler>? = null
    private var grabbing = false
    private var cursor: GLFWCursor? = null
    var cursorX = 0.5
    var cursorY = 0.5
    var gamepadButtonBuffer: ByteBuffer? = null
    var gamepadAxisBuffer: FloatBuffer? = null

    init {
        System.loadLibrary("glfw")
        initialize()
    }

    fun setCursorImpl(cursorImpl: CursorImplementor) {
        GLFW.cursorImpl = WeakReference(cursorImpl)
        addGrabListener(cursorImpl)
    }

    fun setClipboardImpl(clipboardImpl: ClipboardProvider) {
        GLFW.clipboardImpl = WeakReference(clipboardImpl)
    }

    fun setGamepadEnableHandler(handler: GamepadEnableHandler) {
        GLFW.gamepadEnable = WeakReference(handler)
    }

    fun addGrabListener(grabListener: GrabListener) {
        grabListeners.add(grabListener)
    }

    fun isGrabbing(): Boolean = grabbing

    fun getCursor(): GLFWCursor? = cursor

    fun sendMousePos() {
        if (!grabbing) {
            if (cursorX < 0) cursorX = 0.0
            else if (cursorX > 1) cursorX = 1.0
            if (cursorY < 0) cursorY = 0.0
            else if (cursorY > 1) cursorY = 1.0
        }
        val cursor = Utils.getWeakReference(cursorImpl)
        cursor?.onCursorPosition()
        sendMousePosition0(cursorX, cursorY)
    }

    @Suppress("unused")
    private fun receiveGrabState(isGrabbing: Boolean) {
        val wasGrabbing = grabbing
        grabbing = isGrabbing
        Utils.runOnUiThread(Runnable {
            for (grabListener in grabListeners) grabListener.onGrabState(isGrabbing)
        })
        if (!isGrabbing && wasGrabbing) {
            cursorX = 0.5
            cursorY = 0.5
            sendMousePos()
        }
    }

    @Suppress("unused")
    private fun receiveCursorPos(x: Double, y: Double) {
        cursorX = x
        cursorY = y
        val cursor = Utils.getWeakReference(cursorImpl)
        cursor?.onCursorPosition()
    }

    @Suppress("unused")
    private fun loadCursor(imageBytes: ByteBuffer, width: Int, height: Int, xhot: Int, yhot: Int): GLFWCursor? {
        return try {
            val cursorBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            cursorBitmap.copyPixelsFromBuffer(imageBytes)
            GLFWCursor(cursorBitmap, xhot, yhot)
        } catch (t: Throwable) {
            Log.w("GLFW", "Failed to load cursor", t)
            null
        }
    }

    @Suppress("unused")
    private fun useCursor(glfwCursor: GLFWCursor?) {
        cursor = glfwCursor
        val impl = Utils.getWeakReference(cursorImpl)
        impl?.onCursorChanged()
    }

    @Suppress("unused")
    private fun getClipboardString(): String? {
        val clipboardProvider = Utils.getWeakReference(clipboardImpl) ?: return null
        return clipboardProvider.getClipboardString()
    }

    @Suppress("unused")
    private fun setClipboardString(str: String) {
        val clipboardProvider = Utils.getWeakReference(clipboardImpl) ?: return
        clipboardProvider.setClipboardString(str)
    }

    @Suppress("unused")
    private fun enableDirectGamepad(buttonBuffer: ByteBuffer, axisBuffer: ByteBuffer) {
        buttonBuffer.order(ByteOrder.nativeOrder())
        val axisFloatBuffer = axisBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
        if (buttonBuffer.capacity() != 14 || axisFloatBuffer.capacity() != 6) {
            Log.i("GLFW", "Not enabling direct gamepad: unexpected buffer capacities (${buttonBuffer.capacity()} ${axisFloatBuffer.capacity()})")
            return
        }
        gamepadAxisBuffer = axisFloatBuffer
        gamepadButtonBuffer = buttonBuffer
        val enableHandler = Utils.getWeakReference(gamepadEnable)
        enableHandler?.onEnableGamepad()
    }

    fun sendKeyEvent(glfwCode: Int, state: Boolean, mods: Int) {
        sendKeyEvent(glfwCode, if (state) 1 else 0, mods)
    }

    @JvmStatic external fun initialize()
    private external fun sendMousePosition0(x: Double, y: Double)
    @JvmStatic external fun sendKeyEvent(glfwCode: Int, state: Int, mods: Int)
    @JvmStatic external fun sendRawKeyEvent(androidCode: Int, state: Int, mods: Int, codepoint: Char)
    @JvmStatic external fun sendMouseEvent(glfwMouseKey: Int, state: Int, mods: Int)
    @JvmStatic external fun sendBulkUnicodeEvent(input: String, mods: Int)
    @JvmStatic external fun sendScrollEvent(xoffset: Double, yoffset: Double)
    @JvmStatic external fun nativeSurfaceCreated(surface: Surface)
    @JvmStatic external fun nativeSurfaceDestroyed()
    @JvmStatic external fun nativeSurfaceUpdated()
    @JvmStatic external fun nativeNotifyGamepadConnected()
}
