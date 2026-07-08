package net.kdt.pojavlaunch

import android.content.res.Configuration
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.system.Os
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.fragments.MainMenuFragment
import net.kdt.pojavlaunch.fragments.MicrosoftLoginFragment
import net.kdt.pojavlaunch.fragments.SelectAuthFragment
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.IconCacheJanitor
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import net.kdt.pojavlaunch.services.ProgressServiceKeeper
import net.kdt.pojavlaunch.tasks.MoJsonExtras
import net.kdt.pojavlaunch.tasks.AsyncVersionList
import net.kdt.pojavlaunch.tasks.MoJsonDownloader
import net.kdt.pojavlaunch.utils.NotificationUtils
import git.artdeell.mojo.R

class LauncherActivity : BaseActivity() {
    companion object {
        const val SETTING_FRAGMENT_TAG = "SETTINGS_FRAGMENT"
        private var mRequestPermissionLauncher: ActivityResultLauncher<String>? = null
    }

    private var mFragmentView: FragmentContainerView? = null
    private var mSettingsButton: ImageButton? = null
    private var mProgressLayout: ProgressLayout? = null
    private var mProgressServiceKeeper: ProgressServiceKeeper? = null
    private var mNotificationManager: NotificationManager? = null

    private val mFragmentCallbackListener = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(@NonNull fm: FragmentManager, @NonNull f: Fragment) {
            mSettingsButton?.setImageDrawable(
                ContextCompat.getDrawable(baseContext,
                    if (f is MainMenuFragment) R.drawable.ic_px_sliders else R.drawable.ic_px_home)
            )
        }
    }

    private val mBackPreferenceListener = ExtraListener<String?> { key, value ->
        if (value == "true") onBackPressed()
        false
    }

    private val mSelectAuthMethod = ExtraListener<Boolean> { key, value ->
        val manager = supportFragmentManager
        if (!value || manager.isStateSaved) return@ExtraListener false
        val fragment = manager.findFragmentById(mFragmentView!!.id)
        if (fragment !is MainMenuFragment) return@ExtraListener false
        Tools.swapFragment(this, SelectAuthFragment::class.java, SelectAuthFragment.TAG, null)
        false
    }

    private val mSettingButtonListener = View.OnClickListener { v ->
        val manager = supportFragmentManager
        if (manager.isStateSaved) return@OnClickListener
        val fragment = manager.findFragmentById(mFragmentView!!.id)
        if (fragment is MainMenuFragment) {
            Tools.swapFragment(this, LauncherPreferenceFragment::class.java, SETTING_FRAGMENT_TAG, null)
        } else {
            Tools.backToMainMenu(this)
        }
    }

    private val mLaunchGameListener = ExtraListener<Boolean> { key, value ->
        if (mProgressLayout!!.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return@ExtraListener false
        }
        val selectedInstance = Instances.loadSelectedInstance()
        if (selectedInstance == null) {
            Toast.makeText(this, R.string.no_instance, Toast.LENGTH_LONG).show()
            return@ExtraListener false
        }
        val installer = selectedInstance.installer
        if (installer != null) {
            installer.start()
            return@ExtraListener false
        }
        val versionId = selectedInstance.versionId
        if (!Tools.isValidString(versionId)) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show()
            return@ExtraListener false
        }
        if (Accounts.getCurrent() == null) {
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show()
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true)
            return@ExtraListener false
        }
        val normalizedVersionId = MoJsonExtras.normalizeVersionId(versionId!!)
        val mcVersion = MoJsonExtras.getListedVersion(normalizedVersionId)
        MoJsonDownloader().start(
            this.assets,
            mcVersion,
            normalizedVersionId,
            ContextAwareDoneListener(this, normalizedVersionId)
        )
        false
    }

    private val mDoubleLaunchPreventionListener = object : TaskCountListener {
        override fun onUpdateTaskCount(taskCount: Int): Boolean {
            if (taskCount > 0) {
                Tools.runOnUiThread {
                    mNotificationManager?.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START)
                }
            }
            return false
        }
    }

    override fun shouldIgnoreNotch(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    override fun setFullscreen(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pojav_launcher)
        try {
            Os.setenv("TMPDIR", Tools.DIR_CACHE.absolutePath, true)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        IconCacheJanitor.runJanitor()
        window.setBackgroundDrawable(null)
        bindViews()
        mRequestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isAllowed ->
            if (!isAllowed) Tools.runOnUiThread {
                Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show()
            }
        }
        checkNotificationPermission()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener)
        mProgressServiceKeeper = ProgressServiceKeeper(this)
        ProgressKeeper.addTaskCountListener(mProgressServiceKeeper!!)
        mSettingsButton!!.setOnClickListener(mSettingButtonListener)
        ProgressKeeper.addTaskCountListener(mProgressLayout!!)
        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener)
        ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod)
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener)
        AsyncVersionList.getVersionList { versions -> ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions) }
        mProgressLayout!!.observe(ProgressLayout.DOWNLOAD_GAME)
        mProgressLayout!!.observe(ProgressLayout.UNPACK_RUNTIME)
        mProgressLayout!!.observe(ProgressLayout.INSTALL_MODPACK)
        mProgressLayout!!.observe(ProgressLayout.AUTHENTICATE)
        mProgressLayout!!.observe(ProgressLayout.DOWNLOAD_VERSION_LIST)
        mProgressLayout!!.observe(ProgressLayout.INSTANCE_INSTALL)
        mProgressLayout!!.observe(ProgressLayout.DATA_MIGRATION)
    }

    override fun onResume() {
        super.onResume()
        ContextExecutor.setActivity(this)
        InstanceInstaller.postInstallCheck(this)
    }

    override fun onPause() {
        super.onPause()
        ContextExecutor.clearActivity()
    }

    override fun onStart() {
        super.onStart()
        supportFragmentManager.registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        mProgressLayout!!.cleanUpObservers()
        ProgressKeeper.removeTaskCountListener(mProgressLayout!!)
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper!!)
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener)
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod)
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener)
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener)
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentByTag(MicrosoftLoginFragment.TAG) as? MicrosoftLoginFragment
        if (fragment != null) {
            if (fragment.canGoBack()) {
                fragment.goBack()
                return
            }
        }
        super.onBackPressed()
    }

    @Suppress("SameParameterValue")
    private fun getVisibleFragment(tag: String): Fragment? {
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        return if (fragment != null && fragment.isVisible) fragment else null
    }

    @Suppress("unused")
    private fun getVisibleFragment(id: Int): Fragment? {
        val fragment = supportFragmentManager.findFragmentById(id)
        return if (fragment != null && fragment.isVisible) fragment else null
    }

    fun askForPermission(minApi: Int, permission: String) {
        if (Build.VERSION.SDK_INT < minApi) return
        mRequestPermissionLauncher?.launch(permission)
    }

    fun checkForPermission(minApi: Int, permission: String): Boolean {
        return Build.VERSION.SDK_INT < minApi ||
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_DENIED
    }

    fun checkForPermissionRationale(minApi: Int, permission: String): Boolean {
        return checkForPermission(minApi, permission) || ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }

    private fun checkNotificationPermission() {
        if (LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK ||
            checkForPermission(33, Manifest.permission.POST_NOTIFICATIONS)) {
            return
        }
        showNotificationPermissionReasoning()
    }

    private fun showNotificationPermissionReasoning() {
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_permission_dialog_title)
            .setMessage(R.string.notification_permission_dialog_text)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                askForPermission(33, Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> handleNoNotificationPermission() }
            .show()
    }

    private fun handleNoNotificationPermission() {
        LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = true
        LauncherPreferences.DEFAULT_PREF?.edit()
            ?.putBoolean(LauncherPreferences.PREF_KEY_SKIP_NOTIFICATION_CHECK, true)
            ?.apply()
    }

    private fun bindViews() {
        mFragmentView = findViewById(R.id.container_fragment)
        mSettingsButton = findViewById(R.id.setting_button)
        mProgressLayout = findViewById(R.id.progress_layout)
    }

    fun hasActiveProcesses(): Boolean {
        return mProgressLayout!!.hasProcesses()
    }
}
