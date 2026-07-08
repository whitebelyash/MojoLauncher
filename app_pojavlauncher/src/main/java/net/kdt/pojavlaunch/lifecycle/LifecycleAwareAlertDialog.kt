package net.kdt.pojavlaunch.lifecycle

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import net.kdt.pojavlaunch.Tools
import java.util.concurrent.atomic.AtomicBoolean

abstract class LifecycleAwareAlertDialog : LifecycleEventObserver {
    private var mLifecycle: Lifecycle? = null
    private var mDialog: AlertDialog? = null
    private var mLifecycleEnded = false

    fun show(lifecycle: Lifecycle, context: Context, dialogCreator: DialogCreator) {
        mLifecycleEnded = false
        mLifecycle = lifecycle
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            mLifecycleEnded = true
            dialogHidden(mLifecycleEnded)
            return
        }
        val builder = AlertDialog.Builder(context)
        builder.setOnDismissListener(wrapDismissListener(null))
        dialogCreator.createDialog(this, builder)
        lifecycle.addObserver(this)
        mDialog = builder.show()
    }

    protected abstract fun dialogHidden(lifecycleEnded: Boolean)

    protected fun dispatchDialogHidden() {
        Exception().printStackTrace()
        dialogHidden(mLifecycleEnded)
        mLifecycle?.removeObserver(this)
    }

    override fun onStateChanged(@NonNull source: LifecycleOwner, @NonNull event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            mDialog?.dismiss()
            mLifecycleEnded = true
        }
    }

    fun wrapDismissListener(listener: DialogInterface.OnCancelListener?): DialogInterface.OnDismissListener {
        return DialogInterface.OnDismissListener { dialog ->
            dispatchDialogHidden()
            listener?.onCancel(dialog)
        }
    }

    interface DialogCreator {
        fun createDialog(alertDialog: LifecycleAwareAlertDialog, dialogBuilder: AlertDialog.Builder)
    }

    companion object {
        @Throws(InterruptedException::class)
        fun haltOnDialog(
            lifecycle: Lifecycle,
            context: Context,
            dialogCreator: DialogCreator
        ): Boolean {
            val waitLock = Any()
            val hasLifecycleEnded = AtomicBoolean(false)
            val showDialogRunnable = Runnable {
                val lifecycleAwareDialog = object : LifecycleAwareAlertDialog() {
                    override fun dialogHidden(lifecycleEnded: Boolean) {
                        hasLifecycleEnded.set(lifecycleEnded)
                        synchronized(waitLock) { waitLock.notifyAll() }
                    }
                }
                lifecycleAwareDialog.show(lifecycle, context, dialogCreator)
            }
            synchronized(waitLock) {
                Tools.runOnUiThread(showDialogRunnable)
                waitLock.wait()
            }
            return hasLifecycleEnded.get()
        }
    }
}
