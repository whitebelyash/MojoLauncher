package net.kdt.pojavlaunch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import git.artdeell.mojo.R

class FatalErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }
        val storageAllow = extras.getBoolean("storageAllow", false)
        val throwable = extras.getSerializable("throwable") as? Throwable
        val stackTrace = if (throwable != null) Tools.printToString(throwable) else "<null>"
        val strSavePath = extras.getString("savePath")
        val errHeader = if (storageAllow)
            "Crash stack trace saved to $strSavePath."
        else
            "Storage permission is required to save crash stack trace!"

        AlertDialog.Builder(this)
            .setTitle(R.string.error_fatal)
            .setMessage("$errHeader\n\n$stackTrace")
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setNegativeButton(R.string.global_restart) { _, _ ->
                startActivity(Intent(this@FatalErrorActivity, LauncherActivity::class.java))
            }
            .setNeutralButton(android.R.string.copy) { _, _ ->
                val mgr = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                mgr.setPrimaryClip(ClipData.newPlainText("error", stackTrace))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        fun showError(ctx: Context, savePath: String, storageAllow: Boolean, th: Throwable) {
            val fatalErrorIntent = Intent(ctx, FatalErrorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("throwable", th)
                putExtra("savePath", savePath)
                putExtra("storageAllow", storageAllow)
            }
            ctx.startActivity(fatalErrorIntent)
        }
    }
}
