package net.kdt.pojavlaunch.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import git.artdeell.mojo.R

class CustomSeekBarPreference : SeekBarPreference {

    private var mSuffix = ""
    private var mMin = 0
    private var mTextView: TextView? = null
    private val mIncrement: Int

    @SuppressLint("PrivateResource")
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        TypedArray(context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, defStyleAttr, defStyleRes)).use { a ->
            mMin = a.getInt(R.styleable.SeekBarPreference_min, 0)
            mIncrement = a.getInt(R.styleable.SeekBarPreference_seekBarIncrement, 0)
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, R.attr.seekBarPreferenceStyle)

    @Suppress("unused")
    constructor(context: Context) : this(context, null)

    override fun setMin(min: Int) {
        super.setMin(min)
        if (min != mMin) mMin = min
    }

    override fun onBindViewHolder(@NonNull view: PreferenceViewHolder) {
        super.onBindViewHolder(view)
        val titleTextView = view.findViewById(android.R.id.title) as TextView
        titleTextView.setTextColor(Color.WHITE)

        mTextView = view.findViewById(R.id.seekbar_value) as TextView
        mTextView!!.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        val seekBar = view.findViewById(R.id.seekbar) as SeekBar

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                var p = progress + mMin
                p = p / seekBarIncrement
                p = p * seekBarIncrement
                p -= mMin
                mTextView!!.text = (p + mMin).toString()
                updateTextViewWithSuffix()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                var progress = seekBar.progress + mMin
                progress /= seekBarIncrement
                progress *= seekBarIncrement
                progress -= mMin
                value = progress + mMin
                updateTextViewWithSuffix()
            }
        })

        updateTextViewWithSuffix()
    }

    fun setSuffix(suffix: String) {
        this.mSuffix = suffix
    }

    fun setRange(min: Int, max: Int) {
        setMin(min)
        setMaxKeepIncrement(max)
    }

    fun setMaxKeepIncrement(max: Int) {
        super.setMax(max)
        seekBarIncrement = mIncrement
    }

    private fun updateTextViewWithSuffix() {
        if (!mTextView!!.text.toString().endsWith(mSuffix)) {
            mTextView!!.text = String.format("%s%s", mTextView!!.text, mSuffix)
        }
    }
}
