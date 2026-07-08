package net.kdt.pojavlaunch.colorselector

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.kdt.SideDialogView
import git.artdeell.mojo.R

class ColorSelector : SideDialogView, HueSelectionListener, RectangleSelectionListener, AlphaSelectionListener, TextWatcher {

    private val mHueTemplate = floatArrayOf(0f, 1f, 1f)
    private val mHsvSelected = floatArrayOf(360f, 1f, 1f)
    private var mHueView: HueView? = null
    private var mLuminosityIntensityView: SVRectangleView? = null
    private var mAlphaView: AlphaView? = null
    private var mColorView: ColorSideBySideView? = null
    private var mTextView: EditText? = null
    private var mColorSelectionListener: ColorSelectionListener? = null
    private var mAlphaSelected = 0xff
    private var mTextColors: ColorStateList? = null
    private var mWatch = true
    private var mAlphaEnabled = true

    constructor(context: Context, parent: ViewGroup, colorSelectionListener: ColorSelectionListener?) : super(context, parent, R.layout.dialog_color_selector) {
        mColorSelectionListener = colorSelectionListener
    }

    override fun onInflate() {
        super.onInflate()
        mHueView = mDialogContent.findViewById(R.id.color_selector_hue_view)
        mLuminosityIntensityView = mDialogContent.findViewById(R.id.color_selector_rectangle_view)
        mAlphaView = mDialogContent.findViewById(R.id.color_selector_alpha_view)
        mColorView = mDialogContent.findViewById(R.id.color_selector_color_view)
        mTextView = mDialogContent.findViewById(R.id.color_selector_hex_edit)
        runColor(Color.RED)
        mHueView?.setHueSelectionListener(this)
        mLuminosityIntensityView?.setRectSelectionListener(this)
        mAlphaView?.setAlphaSelectionListener(this)
        mTextView?.addTextChangedListener(this)
        mTextColors = mTextView?.textColors
        mAlphaView?.visibility = if (mAlphaEnabled) View.VISIBLE else View.GONE

        val contentParent = mDialogContent.findViewById<View>(R.id.side_dialog_scrollview)
        if (contentParent != null) {
            val dialogLayout = mDialogContent.parent as ViewGroup
            dialogLayout.elevation = 11f
            dialogLayout.translationZ = 11f
        }
    }

    fun show(fromRight: Boolean) {
        show(fromRight, Color.RED)
    }

    fun show(fromRight: Boolean, previousColor: Int) {
        appear(fromRight)
        runColor(previousColor)
        dispatchColorChange()
    }

    override fun onHueSelected(hue: Float) {
        mHsvSelected[0] = hue
        mHueTemplate[0] = hue
        mLuminosityIntensityView?.setColor(Color.HSVToColor(mHueTemplate), true)
        dispatchColorChange()
    }

    override fun onLuminosityIntensityChanged(luminosity: Float, intensity: Float) {
        mHsvSelected[1] = intensity
        mHsvSelected[2] = luminosity
        dispatchColorChange()
    }

    override fun onAlphaSelected(alpha: Int) {
        mAlphaSelected = alpha
        dispatchColorChange()
    }

    companion object {
        private const val ALPHA_MASK = (0xFF shl 24).inv()

        fun setAlpha(color: Int, alpha: Int): Int {
            return color and ALPHA_MASK or (alpha and 0xFF shl 24)
        }
    }

    protected fun dispatchColorChange() {
        val color = Color.HSVToColor(mAlphaSelected, mHsvSelected)
        mColorView?.setColor(color)
        mWatch = false
        mTextView?.setText(String.format("%08X", color))
        notifyColorSelector(color)
    }

    protected fun runColor(color: Int) {
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), mHsvSelected)
        mHueTemplate[0] = mHsvSelected[0]
        mHueView?.setHue(mHsvSelected[0])
        mLuminosityIntensityView?.setColor(Color.HSVToColor(mHueTemplate), false)
        mLuminosityIntensityView?.setLuminosityIntensity(mHsvSelected[2], mHsvSelected[1])
        mAlphaSelected = Color.alpha(color)
        mAlphaView?.setAlpha(if (mAlphaEnabled) mAlphaSelected else 255)
        mColorView?.setColor(color)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        if (mWatch) {
            try {
                val color = Integer.parseInt(s.toString(), 16)
                mTextView?.setTextColor(mTextColors!!)
                runColor(color)
            } catch (_: NumberFormatException) {
                mTextView?.setTextColor(Color.RED)
            }
        } else {
            mWatch = true
        }
    }

    fun setColorSelectionListener(listener: ColorSelectionListener?) {
        mColorSelectionListener = listener
    }

    fun setAlphaEnabled(alphaEnabled: Boolean) {
        mAlphaEnabled = alphaEnabled
        if (mAlphaView != null) {
            mAlphaView?.visibility = if (alphaEnabled) View.VISIBLE else View.GONE
            mAlphaView?.setAlpha(255)
        }
    }

    private fun notifyColorSelector(color: Int) {
        mColorSelectionListener?.onColorSelected(color)
    }
}
