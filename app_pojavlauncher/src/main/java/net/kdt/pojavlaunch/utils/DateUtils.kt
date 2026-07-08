package net.kdt.pojavlaunch.utils

import androidx.annotation.NonNull
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.Tools
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

object DateUtils {
    fun parseReleaseDate(releaseTime: String?): Date? {
        if (releaseTime == null) return null
        var processed = releaseTime
        val tIndexOf = processed.indexOf('T')
        if (tIndexOf != -1) processed = processed.substring(0, tIndexOf)
        return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(processed)
    }

    fun dateBefore(@NonNull date: Date, year: Int, month: Int, dayOfMonth: Int): Boolean {
        return date.before(Date(GregorianCalendar(year, month, dayOfMonth).timeInMillis))
    }

    @Throws(ParseException::class)
    fun getOriginalReleaseDate(gameVersion: JVersionList.Version): Date? {
        var gv = gameVersion
        if (Tools.isValidString(gv.inheritsFrom)) {
            gv = Tools.getVersionInfo(gv.inheritsFrom, true)
        } else {
            gv = Tools.getVersionInfo(gv.id, true)
        }
        return parseReleaseDate(gv.releaseTime)
    }
}
