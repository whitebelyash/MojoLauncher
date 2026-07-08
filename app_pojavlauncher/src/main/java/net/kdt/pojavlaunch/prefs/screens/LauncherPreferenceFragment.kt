package net.kdt.pojavlaunch.prefs.screens

import android.Manifest
import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.annotation.NonNull
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.kdt.pojavlaunch.LauncherActivity
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.prefs.LauncherPreferences

open class LauncherPreferenceFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    protected var mVisibilityUpdater: Runnable = Runnable {}

    override fun onViewCreated(@NonNull view: View, savedInstanceState: Bundle?) {
        view.setBackgroundColor(resources.getColor(R.color.background_app))
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreatePreferences(b: Bundle?, str: String?) {
        mVisibilityUpdater = Runnable { updateVisibility() }
        addPreferencesFromResource(R.xml.pref_main)
        setupNotificationRequestPreference()
    }

    private fun updateVisibility() {
        requirePreference("notification_permission_request").isVisible = !getLauncherActivity().checkForPermission(33, Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun setupNotificationRequestPreference() {
        val mRequestNotificationPermissionPreference = requirePreference("notification_permission_request")
        val activity = activity
        if (activity is LauncherActivity) {
            mRequestNotificationPermissionPreference.setOnPreferenceClickListener {
                activity.askForPermission(33, Manifest.permission.POST_NOTIFICATIONS)
                true
            }
        } else {
            mRequestNotificationPermissionPreference.isVisible = false
        }
        updateVisibility()
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = preferenceManager.sharedPreferences
        sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        mVisibilityUpdater.run()
    }

    override fun onPause() {
        val sharedPreferences = preferenceManager.sharedPreferences
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences, s: String) {
        LauncherPreferences.loadPreferences(context)
    }

    protected fun requirePreference(key: CharSequence): Preference {
        val preference = findPreference(key)
        if (preference != null) return preference
        throw IllegalStateException("Preference $key is null")
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T : Preference> requirePreference(key: CharSequence, preferenceClass: Class<T>): T {
        val preference = requirePreference(key)
        if (preferenceClass.isInstance(preference)) return preference as T
        throw IllegalStateException("Preference $key is not an instance of ${preferenceClass.simpleName}")
    }

    protected fun getLauncherActivity(): LauncherActivity {
        return activity as LauncherActivity
    }
}
