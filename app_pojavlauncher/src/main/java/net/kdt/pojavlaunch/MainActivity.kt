package net.kdt.pojavlaunch

import net.kdt.pojavlaunch.Tools.dialogForceClose
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_ENABLE_GYRO
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_SUSTAINED_PERFORMANCE
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_USE_ALTERNATE_SURFACE
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_VIRTUAL_MOUSE_START

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout

import com.kdt.LoggerView

import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.CustomControls
import net.kdt.pojavlaunch.customcontrols.EditorExitable
import net.kdt.pojavlaunch.customcontrols.keyboard.LwjglCharSender
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput
import net.kdt.pojavlaunch.customcontrols.mouse.GyroControl
import net.kdt.pojavlaunch.customcontrols.mouse.HotbarView
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.prefs.QuickSettingSideDialog
import net.kdt.pojavlaunch.services.GameService
import net.kdt.pojavlaunch.tasks.AsyncAssetManager
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.utils.MCOptionUtils
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.utils.RendererCompatUtil
import net.kdt.pojavlaunch.utils.jre.GameRunner

import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Objects

import git.artdeell.dnbootstrap.glfw.AndroidClipboardProvider
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.dnbootstrap.glfw.GLFWCursorView
import git.artdeell.mojo.R

class MainActivity : BaseActivity(), ControlButtonMenuListener, EditorExitable, ServiceConnection {
    companion object {
        const val INTENT_LAUNCH_VERSION = "intent_version"
        const val INTENT_LAUNCH_CLASSPATH = "intent_classpath"
        var touchCharInput: TouchCharInput? = null
        private var weakCursor: WeakReference<GLFWCursorView>? = null
        var mForceFullPanning = false
        var mImeHeight = 0
        fun toggleMouse(ctx: Context) {
            if (GLFW.isGrabbing()) return
            val cursorView = Tools.getWeakReference(weakCursor) ?: return
            var toastString = 0
            when (cursorView.visibility) {
                View.GONE, View.INVISIBLE -> {
                    toastString = R.string.control_mouseon
                    cursorView.visibility = View.VISIBLE
                }
                View.VISIBLE -> {
                    toastString = R.string.control_mouseoff
                    cursorView.visibility = View.GONE
                }
            }
            if (toastString != 0) Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show()
        }

        fun switchKeyboardState(panning: Boolean) {
            touchCharInput?.let {
                it.switchKeyboardState()
                mForceFullPanning = panning
            }
        }
    }

    private var launcherGLView: LauncherGLSurface? = null
    private var cursor: GLFWCursorView? = null
    private var loggerView: LoggerView? = null
    private var drawerLayout: DrawerLayout? = null
    private var navDrawer: ListView? = null
    private var mDrawerPullButton: View? = null
    private var mGyroControl: GyroControl? = null
    private var mControlLayout: ControlLayout? = null
    private var mHotbarView: HotbarView? = null
    private var mClipboardProvider: AndroidClipboardProvider? = null

    var instance: Instance? = null
    var account: Account? = null

    private var gameActionArrayAdapter: ArrayAdapter<String>? = null
    private var gameActionClickListener: AdapterView.OnItemClickListener? = null
    var ingameControlsEditorArrayAdapter: ArrayAdapter<String>? = null
    var ingameControlsEditorListener: AdapterView.OnItemClickListener? = null
    private var mServiceBinder: GameService.LocalBinder? = null

    private var mQuickSettingSideDialog: QuickSettingSideDialog? = null

    var isInEditor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = Instances.loadSelectedInstance()
        account = Accounts.getCurrent()
        if (instance == null) {
            Toast.makeText(this, R.string.instance_dir_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        AsyncAssetManager.extractDefaultSettings(this, instance!!.gameDirectory)
        MCOptionUtils.load(instance!!.gameDirectory.absolutePath)

        val gameServiceIntent = Intent(this, GameService::class.java)
        ContextCompat.startForegroundService(this, gameServiceIntent)
        initLayout(R.layout.activity_basemain)
        GLFW.addGrabListener(launcherGLView!!)

        mGyroControl = GyroControl(this)

        if (PREF_USE_ALTERNATE_SURFACE) window.setBackgroundDrawable(null)
        else window.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            window.setSustainedPerformanceMode(PREF_SUSTAINED_PERFORMANCE)

        val androidCompat = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
        if (androidCompat)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            if (launcherGLView!!.mSurface == null) return@setOnApplyWindowInsetsListener insets
            val animSurface = launcherGLView!!.mSurface.animate().setDuration(100)
            val animCursor = cursor!!.animate().setDuration(100)
            if (!insets.isVisible(WindowInsetsCompat.Type.ime())) {
                animSurface.translationY(0f).start()
                animCursor.translationY(0f).start()
                mImeHeight = 0
                if (androidCompat) {
                    view.postDelayed({
                        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_FULLSCREEN)
                    }, 150)
                }
                return@setOnApplyWindowInsetsListener insets
            }
            if (!mForceFullPanning && !LauncherPreferences.PREF_KEYBOARD_AUTOPANNING)
                return@setOnApplyWindowInsetsListener insets
            mImeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val translationY: Int
            if (!mForceFullPanning) {
                val cursorY = (GLFW.cursorY * launcherGLView!!.mSurface!!.height).toInt() + 100
                translationY = Tools.getTranslationFromCursorY(
                    cursorY,
                    launcherGLView!!.mSurface!!.height,
                    mImeHeight,
                    0
                )
            } else translationY = mImeHeight
            animSurface.translationY(-translationY.toFloat()).start()
            animCursor.translationY(-translationY.toFloat()).start()
            insets
        }

        ingameControlsEditorArrayAdapter = ArrayAdapter(this,
            android.R.layout.simple_list_item_1, resources.getStringArray(R.array.menu_customcontrol))
        ingameControlsEditorListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            when (position) {
                0 -> mControlLayout!!.addControlButton(ControlData("New"))
                1 -> mControlLayout!!.addDrawer(ControlDrawerData())
                2 -> mControlLayout!!.addJoystickButton(ControlJoystickData())
                3 -> mControlLayout!!.openLoadDialog()
                4 -> mControlLayout!!.openSaveDialog(this)
                5 -> mControlLayout!!.openSetDefaultDialog()
                6 -> mControlLayout!!.openExitDialog(this)
            }
        }

        val optionListener = { MCOptionUtils.getMcScale() }
        MCOptionUtils.addMCOptionListener(optionListener)
        mControlLayout!!.isModifiable = false

        ContextExecutor.setActivity(this)
        bindService(gameServiceIntent, this, 0)
    }

    protected fun initLayout(resId: Int) {
        setContentView(resId)
        bindValues()
        mControlLayout!!.setMenuListener(this)

        mDrawerPullButton!!.setOnClickListener { onClickedMenu() }
        drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        cursor!!.cursorScale = LauncherPreferences.PREF_MOUSESCALE

        try {
            val latestLogFile = File(Tools.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw IOException("Failed to create a new log file")
            Logger.begin(latestLogFile.absolutePath)

            mClipboardProvider = AndroidClipboardProvider(applicationContext)
            GLFW.setClipboardImpl(mClipboardProvider)

            touchCharInput!!.setCharacterSender(LwjglCharSender())

            val extras = Objects.requireNonNull(intent.extras)
            val version = extras.getString(INTENT_LAUNCH_VERSION)
            val classpath = extras.getSerializable(INTENT_LAUNCH_CLASSPATH) as Array<File>?

            setTitle("MojoLauncher ($version)")

            gameActionArrayAdapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1, resources.getStringArray(R.array.menu_ingame))
            gameActionClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                when (position) {
                    0 -> dialogForceClose(this@MainActivity)
                    1 -> openLogOutput()
                    2 -> dialogSendCustomKey()
                    3 -> openQuickSettings()
                    4 -> openCustomControls()
                }
                drawerLayout!!.closeDrawers()
            }
            navDrawer!!.adapter = gameActionArrayAdapter
            navDrawer!!.onItemClickListener = gameActionClickListener
            drawerLayout!!.closeDrawers()

            launcherGLView!!.setSurfaceReadyListener {
                try {
                    Tools.runOnUiThread { if (PREF_VIRTUAL_MOUSE_START) cursor!!.visibility = View.VISIBLE }
                    runCraft(version!!, classpath)
                } catch (e: Throwable) {
                    Tools.showErrorRemote(e)
                }
            }
        } catch (e: Throwable) {
            Tools.showError(this, e, true)
        }
    }

    private fun loadControls() {
        try {
            mControlLayout!!.loadLayout(instance!!.launchControls)
        } catch (e: IOException) {
            try {
                Log.w("MainActivity", "Unable to load the control file, loading the default now", e)
                mControlLayout!!.loadLayout(Tools.CTRLDEF_FILE)
            } catch (ioException: IOException) {
                Tools.showError(this, ioException)
            }
        } catch (th: Throwable) {
            Tools.showError(this, th)
        }
        mDrawerPullButton!!.visibility = if (mControlLayout!!.hasMenuButton()) View.GONE else View.VISIBLE
        mControlLayout!!.toggleControlVisible()
    }

    override fun onAttachedToWindow() {
        mControlLayout!!.post {
            Tools.getDisplayMetrics(this)
            loadControls()
        }
    }

    private fun bindValues() {
        mControlLayout = findViewById(R.id.main_control_layout)
        launcherGLView = findViewById(R.id.main_game_render_view)
        cursor = findViewById(R.id.main_touchpad)
        weakCursor = WeakReference(cursor)
        drawerLayout = findViewById(R.id.main_drawer_options)
        navDrawer = findViewById(R.id.main_navigation_view)
        loggerView = findViewById(R.id.mainLoggerView)
        touchCharInput = findViewById(R.id.mainTouchCharInput)
        mDrawerPullButton = findViewById(R.id.drawer_button)
        mHotbarView = findViewById(R.id.hotbar_view)
    }

    override fun onResume() {
        super.onResume()
        ContextExecutor.setActivity(this)
        if (PREF_ENABLE_GYRO) mGyroControl!!.enable()
    }

    override fun onPause() {
        ContextExecutor.clearActivity()
        mGyroControl!!.disable()
        if (GLFW.isGrabbing()) {
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE)
        }
        mQuickSettingSideDialog?.cancel()
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        ContextExecutor.clearActivity()
    }

    override fun onConfigurationChanged(@NonNull newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mGyroControl?.updateOrientation()
        if (mControlLayout == null) return
        mControlLayout!!.requestLayout()
        mControlLayout!!.post {
            launcherGLView!!.refreshSize()
            mControlLayout!!.refreshControlButtonPositions()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (launcherGLView != null)
            Tools.MAIN_HANDLER.postDelayed({ launcherGLView!!.refreshSize() }, 500)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (!Tools.checkStorageRoot(this)) return
            LauncherPreferences.loadPreferences(applicationContext)
            try {
                mControlLayout!!.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(Throwable::class)
    private fun runCraft(versionId: String, classpath: Array<File>?) {
        var renderer = instance!!.launchRenderer
        if (!RendererCompatUtil.checkRendererCompatible(this, renderer)) {
            val renderersList = RendererCompatUtil.getCompatibleRenderers(this)
            val firstCompatibleRenderer = renderersList.rendererIds[0]
            Log.w("runCraft", "Incompatible renderer $renderer will be replaced with $firstCompatibleRenderer")
            renderer = firstCompatibleRenderer
        }
        Logger.appendToLog("--------- Starting game with Launcher Debug!")
        Tools.printLauncherInfo(versionId, instance!!.launchArgs, renderer, this)
        JREUtils.redirectAndPrintJRELog()
        GameRunner.launchGame(this, account, instance, versionId, classpath, renderer)
        Tools.runOnUiThread { mServiceBinder!!.isActive = false }
    }

    private fun dialogSendCustomKey() {
        AlertDialog.Builder(this)
            .setTitle(R.string.control_customkey)
            .setItems(EfficientAndroidLWJGLKeycode.generateKeyName()) { _: android.content.DialogInterface?, position: Int ->
                EfficientAndroidLWJGLKeycode.execKeyIndex(position)
            }
            .show()
    }

    private fun openCustomControls() {
        if (ingameControlsEditorListener == null || ingameControlsEditorArrayAdapter == null) return
        mControlLayout!!.isModifiable = true
        navDrawer!!.adapter = ingameControlsEditorArrayAdapter
        navDrawer!!.onItemClickListener = ingameControlsEditorListener
        mDrawerPullButton!!.visibility = View.VISIBLE
        isInEditor = true
    }

    private fun openLogOutput() {
        loggerView!!.visibility = View.VISIBLE
    }

    private fun openQuickSettings() {
        if (mQuickSettingSideDialog == null) {
            mQuickSettingSideDialog = object : QuickSettingSideDialog(this, mControlLayout!!) {
                override fun onResolutionChanged() {
                    launcherGLView!!.refreshSize()
                    mHotbarView!!.onResolutionChanged()
                }

                override fun onGyroStateChanged() {
                    mGyroControl!!.updateOrientation()
                    if (PREF_ENABLE_GYRO) {
                        mGyroControl!!.enable()
                    } else {
                        mGyroControl!!.disable()
                    }
                }
            }
        }
        mQuickSettingSideDialog!!.appear(true)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isInEditor) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN) mControlLayout!!.askToExit(this)
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        var handleEvent: Boolean
        if (!(launcherGLView!!.processKeyEvent(event)).also { handleEvent = it }) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && !touchCharInput!!.isEnabled()) {
                if (event.action != KeyEvent.ACTION_UP) return true
                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE)
                return true
            }
        }
        return handleEvent
    }

    override fun onClickedMenu() {
        drawerLayout!!.openDrawer(navDrawer!!)
        navDrawer!!.requestLayout()
    }

    override fun exitEditor() {
        try {
            mControlLayout!!.loadLayout(null as CustomControls?)
            mControlLayout!!.isModifiable = false
            System.gc()
            mControlLayout!!.loadLayout(instance!!.launchControls)
            mDrawerPullButton!!.visibility = if (mControlLayout!!.hasMenuButton()) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            Tools.showError(this, e)
        }
        navDrawer!!.adapter = gameActionArrayAdapter
        navDrawer!!.onItemClickListener = gameActionClickListener
        isInEditor = false
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val localBinder = service as GameService.LocalBinder
        mServiceBinder = localBinder
        launcherGLView!!.start(localBinder.isActive, cursor!!)
        localBinder.isActive = true
    }

    override fun onServiceDisconnected(name: ComponentName) {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun checkCaptureDispatchConditions(event: MotionEvent): Boolean {
        val eventSource = event.source
        return (eventSource and InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource and InputDevice.SOURCE_MOUSE) != 0
    }

    override fun dispatchTrackballEvent(ev: MotionEvent): Boolean {
        return if (Tools.isAndroid8OrHigher() && checkCaptureDispatchConditions(ev))
            launcherGLView!!.dispatchCapturedPointerEvent(ev)
        else super.dispatchTrackballEvent(ev)
    }
}
