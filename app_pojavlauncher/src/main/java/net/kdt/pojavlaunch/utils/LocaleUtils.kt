package net.kdt.pojavlaunch.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.preference.PreferenceManager
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.util.Locale

class LocaleUtils(base: Context) : ContextWrapper(base) {

    companion object {
        fun setLocale(context: Context): ContextWrapper {
            if (LauncherPreferences.DEFAULT_PREF == null) {
                LauncherPreferences.DEFAULT_PREF = PreferenceManager.getDefaultSharedPreferences(context)
                LauncherPreferences.PREF_FORCE_ENGLISH =
                    LauncherPreferences.DEFAULT_PREF.getBoolean("force_english", false)
            }

            var ctx = context
            if (LauncherPreferences.PREF_FORCE_ENGLISH) {
                val resources = ctx.resources
                val configuration = resources.configuration

                configuration.setLocale(Locale.ENGLISH)
                Locale.setDefault(Locale.ENGLISH)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localeList = LocaleList(Locale.ENGLISH)
                    LocaleList.setDefault(localeList)
                    configuration.setLocales(localeList)
                }

                resources.updateConfiguration(configuration, resources.displayMetrics)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    ctx = ctx.createConfigurationContext(configuration)
                }
            }

            return LocaleUtils(ctx)
        }
    }
}
