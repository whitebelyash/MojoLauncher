package net.kdt.pojavlaunch.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.preference.Preference
import git.artdeell.mojo.R
import fr.spse.gamepad_remapper.Remapper

class GamepadRemapPreference : Preference {

    constructor(@NonNull context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(@NonNull context: Context) : super(context) {
        init()
    }

    private fun init() {
        setOnPreferenceClickListener {
            Remapper.wipePreferences(context)
            Toast.makeText(context, R.string.preference_controller_map_wiped, Toast.LENGTH_SHORT).show()
            true
        }
    }
}
