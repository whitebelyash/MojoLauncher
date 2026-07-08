package net.kdt.pojavlaunch

import androidx.annotation.Keep

@Keep
object Logger {
    external fun appendToLog(text: String)
    external fun begin(logFilePath: String)

    @Keep
    interface eventLogListener {
        fun onEventLogged(text: String)
    }

    external fun setLogListener(logListener: eventLogListener?)
}
