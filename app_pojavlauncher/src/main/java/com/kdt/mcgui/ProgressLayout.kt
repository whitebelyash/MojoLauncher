package com.kdt.mcgui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout

import git.artdeell.mojo.R

import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.ProgressListener
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener

import java.util.ArrayList

class ProgressLayout : ConstraintLayout, View.OnClickListener, TaskCountListener {
    companion object {
        const val UNPACK_RUNTIME = "unpack_runtime"
        const val DOWNLOAD_GAME = "download_minecraft"
        const val DOWNLOAD_VERSION_LIST = "download_verlist"
        const val AUTHENTICATE = "authenticate"
        const val INSTALL_MODPACK = "install_modpack"
        const val EXTRACT_COMPONENTS = "extract_components"
        const val EXTRACT_SINGLE_FILES = "extract_single_files"
        const val INSTANCE_INSTALL = "instance_install"
        const val DATA_MIGRATION = "data_migration"

        fun setProgress(progressKey: String, progress: Int) {
            ProgressKeeper.submitProgress(progressKey, progress, -1, null as Any?)
        }

        fun setProgress(progressKey: String, progress: Int, @StringRes resource: Int, vararg message: Any?) {
            ProgressKeeper.submitProgress(progressKey, progress, resource, *message)
        }

        fun setProgress(progressKey: String, progress: Int, message: String) {
            setProgress(progressKey, progress, -1, message)
        }

        fun clearProgress(progressKey: String) {
            setProgress(progressKey, -1, -1)
        }
    }

    constructor(@NonNull context: Context) : super(context) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) { init() }

    private val mMap = ArrayList<LayoutProgressListener>()
    private var mLinearLayout: LinearLayout? = null
    private var mTaskNumberDisplayer: TextView? = null
    private var mFlipArrow: ImageView? = null

    fun observe(progressKey: String) {
        mMap.add(LayoutProgressListener(progressKey))
    }

    fun cleanUpObservers() {
        for (progressListener in mMap) {
            ProgressKeeper.removeListener(progressListener.progressKey, progressListener)
        }
    }

    fun hasProcesses(): Boolean {
        return ProgressKeeper.getTaskCount() > 0
    }

    private fun init() {
        inflate(context, R.layout.view_progress, this)
        mLinearLayout = findViewById(R.id.progress_linear_layout)
        mTaskNumberDisplayer = findViewById(R.id.progress_textview)
        mFlipArrow = findViewById(R.id.progress_flip_arrow)
        setBackgroundColor(resources.getColor(R.color.background_bottom_bar))
        setOnClickListener(this)
    }

    override fun onClick(v: View) {
        mLinearLayout!!.visibility = if (mLinearLayout!!.visibility == GONE) VISIBLE else GONE
        mFlipArrow!!.rotation = if (mLinearLayout!!.visibility == GONE) 0f else 180f
    }

    override fun onUpdateTaskCount(tc: Int): Boolean {
        post {
            if (tc > 0) {
                mTaskNumberDisplayer!!.text = context.getString(R.string.progresslayout_tasks_in_progress, tc)
                visibility = VISIBLE
            } else {
                visibility = GONE
            }
        }
        return false
    }

    inner class LayoutProgressListener(val progressKey: String) : ProgressListener {
        val textView = TextProgressBar(context)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelOffset(R.dimen._20sdp)
        ).apply {
            bottomMargin = resources.getDimensionPixelOffset(R.dimen._6sdp)
        }
        var isAttached = false

        init {
            textView.setTextPadding(context.resources.getDimensionPixelOffset(R.dimen._6sdp))
            ProgressKeeper.addListener(progressKey, this)
        }

        override fun onProgressStarted() {
            post {
                Log.i("ProgressLayout", "onProgressStarted")
                if (!isAttached) mLinearLayout!!.addView(textView, params)
                isAttached = true
            }
        }

        override fun onProgressUpdated(progress: Int, resid: Int, vararg va: Any?) {
            post {
                textView.progress = progress
                if (resid != -1) textView.setText(context.getString(resid, *va))
                else if (va.isNotEmpty() && va[0] != null) textView.setText(va[0] as String)
                else textView.setText("")
            }
        }

        override fun onProgressEnded() {
            post {
                mLinearLayout!!.removeView(textView)
                isAttached = false
            }
        }
    }
}
