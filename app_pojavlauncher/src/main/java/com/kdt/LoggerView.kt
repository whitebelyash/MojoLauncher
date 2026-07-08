package com.kdt

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ToggleButton

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.constraintlayout.widget.ConstraintLayout

import net.kdt.pojavlaunch.Logger
import git.artdeell.mojo.R

class LoggerView : ConstraintLayout {
    private var mLogListener: Logger.eventLogListener? = null
    private var mLogToggle: ToggleButton? = null
    private var mScrollView: DefocusableScrollView? = null
    private var mLogTextView: TextView? = null

    constructor(@NonNull context: Context) : this(context, null)

    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        mLogToggle?.isChecked = visibility == VISIBLE
    }

    private fun init() {
        inflate(context, R.layout.view_logger, this)
        mLogTextView = findViewById(R.id.content_log_view)
        mLogTextView!!.typeface = Typeface.MONOSPACE
        mLogTextView!!.maxLines = Integer.MAX_VALUE
        mLogTextView!!.ellipsize = null
        mLogTextView!!.visibility = GONE

        mLogToggle = findViewById(R.id.content_log_toggle_log)
        mLogToggle!!.setOnCheckedChangeListener { _, isChecked ->
            mLogTextView!!.visibility = if (isChecked) VISIBLE else GONE
            if (isChecked) {
                Logger.setLogListener(mLogListener)
            } else {
                mLogTextView!!.text = ""
                Logger.setLogListener(null)
            }
        }
        mLogToggle!!.isChecked = false

        val cancelButton = findViewById<ImageButton>(R.id.log_view_cancel)
        cancelButton.setOnClickListener { visibility = GONE }

        mScrollView = findViewById(R.id.content_log_scroll)
        mScrollView!!.setKeepFocusing(true)

        val autoscrollToggle = findViewById<ToggleButton>(R.id.content_log_toggle_autoscroll)
        autoscrollToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) mScrollView!!.fullScroll(View.FOCUS_DOWN)
            mScrollView!!.setKeepFocusing(isChecked)
        }
        autoscrollToggle.isChecked = true

        mLogListener = object : Logger.eventLogListener {
            override fun onEventLogged(text: String) {
                if (mLogTextView!!.visibility != VISIBLE) return
                post {
                    mLogTextView!!.append("$text\n")
                    if (mScrollView!!.isKeepFocusing()) mScrollView!!.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }
}
