package git.artdeell.dnbootstrap.glfw

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class AndroidClipboardProvider(context: Context) : ClipboardProvider {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override fun getClipboardString(): String? {
        if (!clipboardManager.hasPrimaryClip()) return null
        val clipData = clipboardManager.primaryClip ?: return null
        if (clipData.itemCount < 1) return null
        val text = clipData.getItemAt(0).text ?: return null
        return text.toString()
    }

    override fun setClipboardString(str: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("GLFW Paste", str))
    }
}
