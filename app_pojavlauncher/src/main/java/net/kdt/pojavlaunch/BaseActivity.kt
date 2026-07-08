package net.kdt.pojavlaunch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.kdt.pojavlaunch.utils.LocaleUtils

import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_IGNORE_NOTCH

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleUtils.setLocale(this)
        Tools.setInsetsMode(this, setFullscreen(), shouldIgnoreNotch())
        Tools.getDisplayMetrics(this)
    }

    open fun setFullscreen(): Boolean {
        return true
    }

    override fun startActivity(i: Intent) {
        super.startActivity(i)
    }

    override fun onResume() {
        super.onResume()
        Tools.checkStorageInteractive(this)
    }

    override fun onPostResume() {
        super.onPostResume()
        Tools.setInsetsMode(this, setFullscreen(), shouldIgnoreNotch())
        Tools.getDisplayMetrics(this)
    }

    protected open fun shouldIgnoreNotch(): Boolean {
        return PREF_IGNORE_NOTCH
    }
}
