package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.annotation.Keep
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import git.artdeell.mojo.R

@Keep
class ExitActivity : AppCompatActivity() {

    @SuppressLint("StringFormatInvalid")
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var code = -1
        var isSignal = false
        val extras = intent.extras
        if (extras != null) {
            code = extras.getInt("code", -1)
            isSignal = extras.getBoolean("isSignal", false)
        }

        val message = if (isSignal) getString(R.string.mcn_abort_title) else getString(R.string.mcn_exit_title, code)

        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.main_share_logs) { _, _ -> Tools.shareLog(this) }
            .setOnDismissListener { finish() }
            .show()
    }

    companion object {
        @Suppress("unused")
        fun showExitMessage(ctx: Context?, code: Int, isSignal: Boolean) {
            if ((!isSignal && code == 0) || ctx == null) {
                System.exit(0)
                return
            }

            val lock = Any()
            Tools.runOnUiThread {
                val i = Intent(ctx, ExitActivity::class.java).apply {
                    putExtra("code", code)
                    putExtra("isSignal", isSignal)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(i)
                synchronized(lock) {
                    (lock as Object).notify()
                }
            }
            synchronized(lock) {
                try {
                    (lock as Object).wait()
                } catch (e: InterruptedException) {
                    Log.e("ExitActivity", "Waiting on lock failed: $e")
                }
            }
            System.exit(0)
        }
    }
}
