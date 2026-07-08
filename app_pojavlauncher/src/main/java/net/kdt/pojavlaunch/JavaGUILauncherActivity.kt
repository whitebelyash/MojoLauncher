package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog

import com.kdt.LoggerView

import net.kdt.pojavlaunch.customcontrols.keyboard.AwtCharSender
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.utils.MathUtils
import net.kdt.pojavlaunch.utils.jre.JavaRunner

import org.apache.commons.io.IOUtils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.zip.ZipEntry

import git.artdeell.mojo.R

class JavaGUILauncherActivity : BaseActivity(), View.OnTouchListener {

    companion object {
        private var CLIPBOARD: ClipboardManager? = null

        @Keep
        fun querySystemClipboard() {
            Tools.runOnUiThread {
                val clipData = CLIPBOARD?.primaryClip
                if (clipData == null) {
                    AWTInputBridge.nativeClipboardReceived(null, null)
                    return@runOnUiThread
                }
                val firstClipItem = clipData.getItemAt(0)
                val clipItemText = firstClipItem.text
                if (clipItemText == null) {
                    AWTInputBridge.nativeClipboardReceived(null, null)
                    return@runOnUiThread
                }
                AWTInputBridge.nativeClipboardReceived(clipItemText.toString(), "plain")
            }
        }

        @Keep
        fun putClipboardData(data: String, mimeType: String) {
            Tools.runOnUiThread {
                val clipData = when (mimeType) {
                    "text/plain" -> ClipData.newPlainText("AWT Paste", data)
                    "text/html" -> ClipData.newHtmlText("AWT Paste", data, data)
                    else -> null
                }
                if (clipData != null) CLIPBOARD?.setPrimaryClip(clipData)
            }
        }

        private fun getJavaVersion(jarFile: JarFile, mainClass: String): Int {
            val className = mainClass.trim { it <= ' ' }.replace('.', '/') + ".class"
            val mainClassFile = jarFile.getEntry(className) ?: return -1

            val bytesWeNeed = ByteArray(8)
            try {
                jarFile.getInputStream(mainClassFile).use { classStream ->
                    val readCount = classStream.read(bytesWeNeed)
                    if (readCount < bytesWeNeed.size) return -1
                }
            } catch (_: IOException) {
                return -1
            }
            val byteBuffer = ByteBuffer.wrap(bytesWeNeed)
            if (byteBuffer.getInt().toLong() != 0xCAFEBABE) return -1
            val majorVersion = byteBuffer.getShort().toInt()
            Log.i("JavaGUILauncher", "$majorVersion,$mainClass")
            return classVersionToJavaVersion(majorVersion)
        }

        fun classVersionToJavaVersion(majorVersion: Int): Int {
            if (majorVersion < 46) return 2
            return majorVersion - 44
        }
    }

    private var mTextureView: AWTCanvasView? = null
    private var mLoggerView: LoggerView? = null
    private var mTouchCharInput: TouchCharInput? = null

    private var mTouchPad: LinearLayout? = null
    private var mMousePointerImageView: ImageView? = null
    private var mGestureDetector: GestureDetector? = null

    private var mIsVirtualMouseEnabled = false
    private var mIsTrusted = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_java_gui_launcher)

        try {
            val latestLogFile = File(Tools.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw IOException("Failed to create a new log file")
            Logger.begin(latestLogFile.absolutePath)
        } catch (e: IOException) {
            Tools.showError(this, e, true)
        }

        CLIPBOARD = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        mTouchCharInput = findViewById(R.id.awt_touch_char)
        mTouchCharInput!!.setCharacterSender(AwtCharSender())

        mTouchPad = findViewById(R.id.main_touchpad)
        mLoggerView = findViewById(R.id.launcherLoggerView)
        mMousePointerImageView = findViewById(R.id.main_mouse_pointer)
        mTextureView = findViewById(R.id.installmod_surfaceview)
        mGestureDetector = GestureDetector(this, SingleTapConfirm())
        mTouchPad!!.isFocusable = false
        mTouchPad!!.visibility = View.GONE

        findViewById<View>(R.id.installmod_mouse_pri).setOnTouchListener(this)
        findViewById<View>(R.id.installmod_mouse_sec).setOnTouchListener(this)
        findViewById<View>(R.id.installmod_window_moveup).setOnTouchListener(this)
        findViewById<View>(R.id.installmod_window_movedown).setOnTouchListener(this)
        findViewById<View>(R.id.installmod_window_moveleft).setOnTouchListener(this)
        findViewById<View>(R.id.installmod_window_moveright).setOnTouchListener(this)

        mMousePointerImageView!!.post {
            val params = mMousePointerImageView!!.layoutParams
            params.width = (36 * LauncherPreferences.PREF_MOUSESCALE).toInt()
            params.height = (54 * LauncherPreferences.PREF_MOUSESCALE).toInt()
        }

        mTouchPad!!.setOnTouchListener { v, event ->
            val action = event.actionMasked
            val x = event.x
            val y = event.y
            var mouseX = mMousePointerImageView!!.x
            var mouseY = mMousePointerImageView!!.y

            if (mGestureDetector!!.onTouchEvent(event)) {
                sendScaledMousePosition(mouseX, mouseY)
                AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK)
            } else {
                if (action == MotionEvent.ACTION_MOVE) {
                    mouseX = Math.max(0f, Math.min(v.width.toFloat(), mouseX + x - prevX))
                    mouseY = Math.max(0f, Math.min(v.height.toFloat(), mouseY + y - prevY))
                    placeMouseAt(mouseX, mouseY)
                    sendScaledMousePosition(mouseX, mouseY)
                }
            }

            prevY = y
            prevX = x
            true
        }

        mTextureView!!.setOnTouchListener { v, event ->
            val x = event.x
            val y = event.y
            if (mGestureDetector!!.onTouchEvent(event)) {
                sendScaledMousePosition(x + mTextureView!!.x, y)
                AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK)
                return@setOnTouchListener true
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {}
                MotionEvent.ACTION_MOVE -> sendScaledMousePosition(x + mTextureView!!.x, y)
            }
            true
        }

        try {
            val extras = intent.extras
            if (extras == null) {
                finish()
                return
            }
            mIsTrusted = extras.getBoolean("trusted", false)
            val javaArgs = extras.getStringArrayList("javaArgs")
            val resourceUri = extras.getParcelable<Uri>("modUri")
            val jarPath = extras.getString("modPath")
            if (jarPath != null) {
                val jarFile = File(jarPath)
                startModInstaller(jarFile, javaArgs)
            } else {
                PojavApplication.sExecutorService.execute { startModInstallerWithUri(resourceUri, javaArgs) }
            }
            if (extras.getBoolean("openLogOutput", false)) openLogOutput(null)
        } catch (th: Throwable) {
            Tools.showError(this, th, true)
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Tools.dialogForceClose(this@JavaGUILauncherActivity)
            }
        })
    }

    private var prevX = 0f
    private var prevY = 0f

    private fun startModInstallerWithUri(uri: Uri?, javaArgs: List<String>?) {
        if (uri == null) {
            startModInstaller(null, javaArgs)
            return
        }
        try {
            val cacheFile = File(cacheDir!!, "mod-installer-temp")
            val contentStream = contentResolver.openInputStream(uri)
            if (contentStream == null) throw IOException("Failed to open content stream")
            FileOutputStream(cacheFile).use { fileOutputStream ->
                IOUtils.copy(contentStream, fileOutputStream)
            }
            contentStream.close()
            startModInstaller(cacheFile, javaArgs)
        } catch (e: IOException) {
            Tools.showError(this, e, true)
        }
    }

    fun selectRuntime(javaVersion: Int): Runtime? {
        if (javaVersion == -1) {
            finalErrorDialog(getString(R.string.execute_jar_failed_to_read_file))
            return null
        }
        val nearestRuntime = MultiRTUtils.getNearestJreName(javaVersion) ?: run {
            finalErrorDialog(getString(R.string.multirt_nocompatiblert, javaVersion))
            return null
        }
        return MultiRTUtils.forceReread(nearestRuntime)
    }

    private class JarFileProperties(val mainClass: String, val minJavaVersion: Int) {
        companion object {
            fun read(file: File): JarFileProperties? {
                try {
                    JarFile(file).use { jarFile ->
                        val manifest = jarFile.manifest ?: return null
                        val mainAttrs = manifest.mainAttributes ?: return null
                        val mainClass = mainAttrs.getValue("Main-Class") ?: return null
                        val javaVersion = getJavaVersion(jarFile, mainClass)
                        return JarFileProperties(mainClass, javaVersion)
                    }
                } catch (_: IOException) {
                    return null
                }
            }
        }
    }

    private fun runModInstaller(modFile: File, javaArgs: List<String>?) {
        val jarFileProperties = JarFileProperties.read(modFile)
        if (jarFileProperties == null) {
            finalErrorDialog(getString(R.string.execute_jar_failed_to_read_file))
            return
        }
        val selectedRuntime = selectRuntime(jarFileProperties.minJavaVersion) ?: return
        launchJavaRuntime(selectedRuntime, javaArgs, modFile, jarFileProperties.mainClass)
    }

    private fun startModInstaller(modFile: File?, javaArgs: List<String>?) {
        Thread({ runModInstaller(modFile!!, javaArgs) }, "JREMainThread").start()
    }

    private fun finalErrorDialog(msg: CharSequence) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(R.string.global_error)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        window.decorView.systemUiVisibility = uiOptions
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val isDown: Boolean
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> isDown = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> isDown = false
            else -> return false
        }

        when (v.id) {
            R.id.installmod_mouse_pri -> AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK, isDown)
            R.id.installmod_mouse_sec -> AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON3_DOWN_MASK, isDown)
        }
        if (isDown) when (v.id) {
            R.id.installmod_window_moveup -> AWTInputBridge.nativeMoveWindow(0, -10)
            R.id.installmod_window_movedown -> AWTInputBridge.nativeMoveWindow(0, 10)
            R.id.installmod_window_moveleft -> AWTInputBridge.nativeMoveWindow(-10, 0)
            R.id.installmod_window_moveright -> AWTInputBridge.nativeMoveWindow(10, 0)
        }
        return true
    }

    fun placeMouseAt(x: Float, y: Float) {
        mMousePointerImageView!!.x = x
        mMousePointerImageView!!.y = y
    }

    @Suppress("SuspiciousNameCombination")
    fun sendScaledMousePosition(x: Float, y: Float) {
        val clampedX = androidx.core.math.MathUtils.clamp(x, mTextureView!!.x, mTextureView!!.x + mTextureView!!.width)
        val clampedY = androidx.core.math.MathUtils.clamp(y, mTextureView!!.y, mTextureView!!.y + mTextureView!!.height)

        AWTInputBridge.sendMousePos(
            MathUtils.map(clampedX, mTextureView!!.x, mTextureView!!.x + mTextureView!!.width, 0f, AWTCanvasView.AWT_CANVAS_WIDTH.toFloat()).toInt(),
            MathUtils.map(clampedY, mTextureView!!.y, mTextureView!!.y + mTextureView!!.height, 0f, AWTCanvasView.AWT_CANVAS_HEIGHT.toFloat()).toInt()
        )
    }

    fun forceClose(v: View?) {
        Tools.dialogForceClose(this)
    }

    fun openLogOutput(v: View?) {
        mLoggerView!!.visibility = View.VISIBLE
    }

    fun toggleVirtualMouse(v: View?) {
        mIsVirtualMouseEnabled = !mIsVirtualMouseEnabled
        mTouchPad!!.visibility = if (mIsVirtualMouseEnabled) View.VISIBLE else View.GONE
        if (mIsVirtualMouseEnabled && mMousePointerImageView!!.x == 0f && mMousePointerImageView!!.y == 0f) {
            mTouchPad!!.post { placeMouseAt(mTouchPad!!.width / 2f, mTouchPad!!.height / 2f) }
        }
        Toast.makeText(this,
            if (mIsVirtualMouseEnabled) R.string.control_mouseon else R.string.control_mouseoff,
            Toast.LENGTH_SHORT).show()
    }

    fun launchJavaRuntime(runtime: Runtime, javaArgs: List<String>?, modFile: File, mainClass: String) {
        JREUtils.redirectAndPrintJRELog()
        try {
            val javaArgList = ArrayList<String>()
            if (javaArgs != null) {
                javaArgList.addAll(javaArgs)
            }

            if (LauncherPreferences.PREF_JAVA_SANDBOX && !mIsTrusted) {
                Collections.reverse(javaArgList)
                javaArgList.add("-Xbootclasspath/a:" + Tools.DIR_DATA + "/security/pro-grade.jar")
                javaArgList.add("-Djava.security.manager=net.sourceforge.prograde.sm.ProGradeJSM")
                javaArgList.add("-Djava.security.policy=" + Tools.DIR_DATA + "/security/java_sandbox.policy")
                Collections.reverse(javaArgList)
            }

            Logger.appendToLog("Info: Java arguments: $javaArgList")

            JavaRunner.nativeSetupExit(this.applicationContext)
            JavaRunner.startJvm(runtime, javaArgList, listOf(modFile.absolutePath), mainClass, emptyList())

            JREUtils.launchJavaVM(this, runtime, null, javaArgList, LauncherPreferences.PREF_CUSTOM_JAVA_ARGS)
        } catch (th: Throwable) {
            Tools.showError(this, th, true)
        }
    }

    fun toggleKeyboard(view: View?) {
        mTouchCharInput!!.switchKeyboardState()
    }

    fun performCopy(view: View?) {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 1)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_C)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 0)
    }

    fun performPaste(view: View?) {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 1)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_V)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 0)
    }
}
