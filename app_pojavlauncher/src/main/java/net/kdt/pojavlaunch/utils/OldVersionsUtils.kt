package net.kdt.pojavlaunch.utils

import android.util.Log
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import java.text.ParseException

object OldVersionsUtils {
    fun selectOpenGlVersion(version: JVersionList.Version) {
        val creationTime = version.time
        if (!Tools.isValidString(creationTime)) {
            ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, "2")
            return
        }

        try {
            val creationDate = DateUtils.parseReleaseDate(creationTime)
            if (creationDate == null) {
                Log.e("GL_SELECT", "Failed to parse version date")
                ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, "2")
                return
            }
            val openGlVersion = if (DateUtils.dateBefore(creationDate, 2011, 6, 8)) "1" else "2"
            Log.i("GL_SELECT", openGlVersion)
            ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, openGlVersion)
        } catch (exception: ParseException) {
            Log.e("GL_SELECT", exception.toString())
            ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, "2")
        }
    }
}
