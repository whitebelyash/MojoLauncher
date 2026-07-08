package git.artdeell.dnbootstrap.utils

import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

object Utils {
    private val MAIN_HANDLER = Handler(Looper.getMainLooper())

    fun <T> getWeakReference(reference: WeakReference<T>?): T? {
        if (reference == null) return null
        return reference.get()
    }

    fun runOnUiThread(runnable: Runnable) {
        MAIN_HANDLER.post(runnable)
    }
}
