package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import com.kdt.SideDialogView
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.CustomControlsActivity
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.colorselector.ColorSelector
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.utils.CropperUtils
import net.kdt.pojavlaunch.utils.interfaces.SimpleItemSelectedListener
import net.kdt.pojavlaunch.utils.interfaces.SimpleSeekBarListener
import net.kdt.pojavlaunch.utils.interfaces.SimpleTextWatcher

class EditControlSideDialog : SideDialogView {
    private val mKeycodeSpinners = arrayOfNulls<Spinner>(4)
    var internalChanges = false

    private val mLayoutChangedListener = View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
        if (internalChanges) return@OnLayoutChangeListener
        internalChanges = true
        val width = safeParseFloat(mWidthEditText!!.text.toString()).toInt()
        if (width >= 0 && kotlin.math.abs(right - width) > 1) {
            mWidthEditText!!.setText((right - left).toString())
        }
        val height = safeParseFloat(mHeightEditText!!.text.toString()).toInt()
        if (height >= 0 && kotlin.math.abs(bottom - height) > 1) {
            mHeightEditText!!.setText((bottom - top).toString())
        }
        internalChanges = false
    }

    private var mNameEditText: EditText? = null
    private var mWidthEditText: EditText? = null
    private var mHeightEditText: EditText? = null
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mToggleSwitch: Switch? = null
    private var mPassthroughSwitch: Switch? = null
    private var mSwipeableSwitch: Switch? = null
    private var mForwardLockSwitch: Switch? = null
    private var mAbsoluteTrackingSwitch: Switch? = null
    private var mOrientationSpinner: Spinner? = null
    private val mKeycodeTextviews = arrayOfNulls<TextView>(4)
    private var mStrokeWidthSeekbar: SeekBar? = null
    private var mCornerRadiusSeekbar: SeekBar? = null
    private var mAlphaSeekbar: SeekBar? = null
    private var mStrokePercentTextView: TextView? = null
    private var mCornerRadiusPercentTextView: TextView? = null
    private var mAlphaPercentTextView: TextView? = null
    private var mSelectBackgroundBitmap: TextView? = null
    private var mSelectBackgroundColor: TextView? = null
    private var mSelectStrokeColor: TextView? = null
    private var mAdapter: ArrayAdapter<String>? = null
    private var mSpecialArray: MutableList<String>? = null
    private var mDisplayInGameCheckbox: CheckBox? = null
    private var mDisplayInMenuCheckbox: CheckBox? = null
    private var mCurrentlyEditedButton: ControlInterface? = null
    private var mOrientationTextView: TextView? = null
    private var mMappingTextView: TextView? = null
    private var mNameTextView: TextView? = null
    private var mCornerRadiusTextView: TextView? = null
    private var mVisibilityTextView: TextView? = null
    private var mSizeTextview: TextView? = null
    private var mSizeXTextView: TextView? = null
    private var mStrokeWidthTextView: TextView? = null
    private var mColorSelectWarningTextView: TextView? = null
    private var mColorSelector: ColorSelector? = null
    private val mParent: ViewGroup

    constructor(context: Context, parent: ViewGroup) : super(context, parent, R.layout.dialog_control_button_setting) {
        mParent = parent
    }

    override fun onInflate() {
        bindLayout()
        buildColorSelector()
        loadAdapter()
        setupRealTimeListeners()
    }

    override fun onDestroy() {
        mColorSelector?.disappear(true)
    }

    private fun buildColorSelector() {
        mColorSelector = ColorSelector(mParent.context, mParent, null)
    }

    fun appearColor(fromRight: Boolean, color: Int) {
        mColorSelector!!.show(fromRight, if (color == -1) Color.WHITE else color)
    }

    fun disappearColor() {
        mColorSelector?.disappear(false)
    }

    fun disappearLayer(): Boolean {
        return if (mColorSelector!!.isDisplaying()) {
            disappearColor()
            false
        } else {
            disappear(false)
            true
        }
    }

    fun adaptPanelPosition() {
        if (!mDisplaying) return
        if (mCurrentlyEditedButton == null) return
        val parent = mCurrentlyEditedButton!!.getControlLayoutParent() ?: return
        val isAtRight = mCurrentlyEditedButton!!.getControlView().x + mCurrentlyEditedButton!!.getControlView().width / 2f <
                mCurrentlyEditedButton!!.getControlLayoutParent()!!.width / 2f
        appear(isAtRight)
        if (mColorSelector!!.isDisplaying()) {
            Tools.runOnUiThread {
                appearColor(isAtRight, mCurrentlyEditedButton!!.getProperties().bgColor)
            }
        }
    }

    companion object {
        fun setPercentageText(textView: TextView, progress: Int) {
            textView.text = textView.context.getString(R.string.percent_format, progress)
        }
    }

    fun loadValues(data: ControlData) {
        setDefaultVisibilitySetting()
        mOrientationTextView!!.visibility = View.GONE
        mOrientationSpinner!!.visibility = View.GONE
        mForwardLockSwitch!!.visibility = View.GONE
        mAbsoluteTrackingSwitch!!.visibility = View.GONE

        mNameEditText!!.setText(data.name)
        mWidthEditText!!.setText(data.getWidth().toInt().toString())
        mHeightEditText!!.setText(data.getHeight().toInt().toString())

        mAlphaSeekbar!!.progress = (data.opacity * 100).toInt()
        mStrokeWidthSeekbar!!.progress = (data.strokeWidth * 10).toInt()
        mCornerRadiusSeekbar!!.progress = data.cornerRadius.toInt()

        setPercentageText(mAlphaPercentTextView!!, (data.opacity * 100).toInt())
        setPercentageText(mStrokePercentTextView!!, (data.strokeWidth * 10).toInt())
        setPercentageText(mCornerRadiusPercentTextView!!, data.cornerRadius.toInt())

        mToggleSwitch!!.isChecked = data.isToggle
        mPassthroughSwitch!!.isChecked = data.passThruEnabled
        mSwipeableSwitch!!.isChecked = data.isSwipeable

        mDisplayInGameCheckbox!!.isChecked = data.displayInGame
        mDisplayInMenuCheckbox!!.isChecked = data.displayInMenu

        for (i in data.keycodes.indices) {
            if (data.keycodes[i] < 0) {
                mKeycodeSpinners[i]!!.setSelection(data.keycodes[i] + mSpecialArray!!.size)
            } else {
                mKeycodeSpinners[i]!!.setSelection(
                    EfficientAndroidLWJGLKeycode.getIndexByValue(data.keycodes[i]) + mSpecialArray!!.size
                )
            }
        }

        setHasBitmap(Tools.isValidString(data.bitmapTag))

        val viewContext = mCurrentlyEditedButton!!.getControlView().context
        if (viewContext !is CustomControlsActivity)
            mSelectBackgroundBitmap!!.visibility = View.GONE
    }

    fun loadValues(data: ControlDrawerData) {
        loadValues(data.properties)
        mOrientationSpinner!!.setSelection(ControlDrawerData.Orientation.orientationToInt(data.orientation))
        mMappingTextView!!.visibility = View.GONE
        for (i in mKeycodeSpinners.indices) {
            mKeycodeSpinners[i]!!.visibility = View.GONE
            mKeycodeTextviews[i]!!.visibility = View.GONE
        }
        mOrientationTextView!!.visibility = View.VISIBLE
        mOrientationSpinner!!.visibility = View.VISIBLE
        mSwipeableSwitch!!.visibility = View.GONE
        mPassthroughSwitch!!.visibility = View.GONE
        mToggleSwitch!!.visibility = View.GONE
    }

    fun loadJoystickValues(data: ControlJoystickData) {
        loadValues(data)
        mMappingTextView!!.visibility = View.GONE
        for (i in mKeycodeSpinners.indices) {
            mKeycodeSpinners[i]!!.visibility = View.GONE
            mKeycodeTextviews[i]!!.visibility = View.GONE
        }
        mNameTextView!!.visibility = View.GONE
        mNameEditText!!.visibility = View.GONE
        mCornerRadiusTextView!!.visibility = View.GONE
        mCornerRadiusSeekbar!!.visibility = View.GONE
        mCornerRadiusPercentTextView!!.visibility = View.GONE
        mSwipeableSwitch!!.visibility = View.GONE
        mPassthroughSwitch!!.visibility = View.GONE
        mToggleSwitch!!.visibility = View.GONE
        mForwardLockSwitch!!.visibility = View.VISIBLE
        mForwardLockSwitch!!.isChecked = data.forwardLock
        mAbsoluteTrackingSwitch!!.visibility = View.VISIBLE
        mAbsoluteTrackingSwitch!!.isChecked = data.absolute
        mSelectBackgroundBitmap!!.visibility = View.GONE
    }

    fun loadSubButtonValues(data: ControlData, drawerOrientation: ControlDrawerData.Orientation) {
        loadValues(data)
        if (drawerOrientation != ControlDrawerData.Orientation.FREE) {
            mSizeTextview!!.visibility = View.GONE
            mSizeXTextView!!.visibility = View.GONE
            mWidthEditText!!.visibility = View.GONE
            mHeightEditText!!.visibility = View.GONE
        }
        mVisibilityTextView!!.visibility = View.GONE
        mDisplayInMenuCheckbox!!.visibility = View.GONE
        mDisplayInGameCheckbox!!.visibility = View.GONE
    }

    private fun loadAdapter() {
        mAdapter = ArrayAdapter(mDialogContent.context, R.layout.item_centered_textview)
        mSpecialArray = ControlData.buildSpecialButtonArray() as MutableList<String>
        mAdapter!!.addAll(mSpecialArray!!)
        mAdapter!!.addAll(EfficientAndroidLWJGLKeycode.generateKeyName())
        mAdapter!!.setDropDownViewResource(android.R.layout.simple_list_item_single_choice)
        for (spinner in mKeycodeSpinners) {
            spinner!!.adapter = mAdapter
        }
        val adapter = ArrayAdapter<ControlDrawerData.Orientation>(
            mDialogContent.context, android.R.layout.simple_spinner_item
        )
        adapter.addAll(*ControlDrawerData.Orientation.getOrientations())
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice)
        mOrientationSpinner!!.adapter = adapter
    }

    private fun setDefaultVisibilitySetting() {
        for (i in 0 until (mDialogContent as ViewGroup).childCount) {
            (mDialogContent as ViewGroup).getChildAt(i).visibility = View.VISIBLE
        }
        for (s in mKeycodeSpinners) {
            s!!.visibility = View.INVISIBLE
        }
        mColorSelectWarningTextView!!.visibility = View.GONE
    }

    private fun setHasBitmap(hasBitmap: Boolean) {
        val visibility = if (!hasBitmap) View.VISIBLE else View.GONE
        val visibilityOpposite = if (hasBitmap) View.VISIBLE else View.GONE
        mSelectStrokeColor!!.visibility = visibility
        mStrokePercentTextView!!.visibility = visibility
        mStrokeWidthSeekbar!!.visibility = visibility
        mCornerRadiusSeekbar!!.visibility = visibility
        mCornerRadiusPercentTextView!!.visibility = visibility
        mCornerRadiusTextView!!.visibility = visibility
        mStrokeWidthTextView!!.visibility = visibility
        mColorSelectWarningTextView!!.visibility = visibilityOpposite
    }

    private fun bindLayout() {
        mNameEditText = mDialogContent.findViewById(R.id.editName_editText)
        mWidthEditText = mDialogContent.findViewById(R.id.editSize_editTextX)
        mHeightEditText = mDialogContent.findViewById(R.id.editSize_editTextY)
        mToggleSwitch = mDialogContent.findViewById(R.id.checkboxToggle)
        mPassthroughSwitch = mDialogContent.findViewById(R.id.checkboxPassThrough)
        mSwipeableSwitch = mDialogContent.findViewById(R.id.checkboxSwipeable)
        mForwardLockSwitch = mDialogContent.findViewById(R.id.checkboxForwardLock)
        mAbsoluteTrackingSwitch = mDialogContent.findViewById(R.id.checkboxAbsoluteFingerTracking)
        mKeycodeSpinners[0] = mDialogContent.findViewById(R.id.editMapping_spinner_1)
        mKeycodeSpinners[1] = mDialogContent.findViewById(R.id.editMapping_spinner_2)
        mKeycodeSpinners[2] = mDialogContent.findViewById(R.id.editMapping_spinner_3)
        mKeycodeSpinners[3] = mDialogContent.findViewById(R.id.editMapping_spinner_4)
        mKeycodeTextviews[0] = mDialogContent.findViewById(R.id.mapping_1_textview)
        mKeycodeTextviews[1] = mDialogContent.findViewById(R.id.mapping_2_textview)
        mKeycodeTextviews[2] = mDialogContent.findViewById(R.id.mapping_3_textview)
        mKeycodeTextviews[3] = mDialogContent.findViewById(R.id.mapping_4_textview)
        mOrientationSpinner = mDialogContent.findViewById(R.id.editOrientation_spinner)
        mStrokeWidthSeekbar = mDialogContent.findViewById(R.id.editStrokeWidth_seekbar)
        mCornerRadiusSeekbar = mDialogContent.findViewById(R.id.editCornerRadius_seekbar)
        mAlphaSeekbar = mDialogContent.findViewById(R.id.editButtonOpacity_seekbar)
        mSelectBackgroundBitmap = mDialogContent.findViewById(R.id.setBackgroundBitmap_textView)
        mSelectBackgroundColor = mDialogContent.findViewById(R.id.editBackgroundColor_textView)
        mSelectStrokeColor = mDialogContent.findViewById(R.id.editStrokeColor_textView)
        mStrokePercentTextView = mDialogContent.findViewById(R.id.editStrokeWidth_textView_percent)
        mAlphaPercentTextView = mDialogContent.findViewById(R.id.editButtonOpacity_textView_percent)
        mCornerRadiusPercentTextView = mDialogContent.findViewById(R.id.editCornerRadius_textView_percent)
        mDisplayInGameCheckbox = mDialogContent.findViewById(R.id.visibility_game_checkbox)
        mDisplayInMenuCheckbox = mDialogContent.findViewById(R.id.visibility_menu_checkbox)
        mMappingTextView = mDialogContent.findViewById(R.id.editMapping_textView)
        mOrientationTextView = mDialogContent.findViewById(R.id.editOrientation_textView)
        mNameTextView = mDialogContent.findViewById(R.id.editName_textView)
        mCornerRadiusTextView = mDialogContent.findViewById(R.id.editCornerRadius_textView)
        mVisibilityTextView = mDialogContent.findViewById(R.id.visibility_textview)
        mSizeTextview = mDialogContent.findViewById(R.id.editSize_textView)
        mSizeXTextView = mDialogContent.findViewById(R.id.editSize_x_textView)
        mStrokeWidthTextView = mDialogContent.findViewById(R.id.editStrokeWidth_textView)
        mColorSelectWarningTextView = mDialogContent.findViewById(R.id.editBackgroundColorWarning_textView)
    }

    private fun removeBitmap(button: ControlInterface) {
        val storage = button.getControlLayoutParent()!!.getBitmaps()!!
        val properties = button.getProperties()
        storage.putBitmap(null, properties.bitmapTag!!)
        properties.bitmapTag = null
        setHasBitmap(false)
    }

    @Suppress("SuspiciousNameCombination")
    private fun setupRealTimeListeners() {
        mNameEditText!!.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher
            mCurrentlyEditedButton!!.getProperties().name = s.toString()
            mCurrentlyEditedButton!!.setProperties(mCurrentlyEditedButton!!.getProperties(), false)
        })

        mWidthEditText!!.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher
            internalChanges = true
            val width = safeParseFloat(s.toString())
            if (width >= 0) {
                mCurrentlyEditedButton!!.getProperties().setWidth(width)
                if (mCurrentlyEditedButton!!.getProperties() is ControlJoystickData) {
                    mCurrentlyEditedButton!!.getProperties().setHeight(width)
                }
                mCurrentlyEditedButton!!.updateProperties()
            }
            mCurrentlyEditedButton!!.getControlView().post { internalChanges = false }
        })

        mHeightEditText!!.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher
            internalChanges = true
            val height = safeParseFloat(s.toString())
            if (height >= 0) {
                mCurrentlyEditedButton!!.getProperties().setHeight(height)
                if (mCurrentlyEditedButton!!.getProperties() is ControlJoystickData) {
                    mCurrentlyEditedButton!!.getProperties().setWidth(height)
                }
                mCurrentlyEditedButton!!.updateProperties()
            }
            mCurrentlyEditedButton!!.getControlView().post { internalChanges = false }
        })

        mSwipeableSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().isSwipeable = isChecked
        }
        mToggleSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().isToggle = isChecked
        }
        mPassthroughSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().passThruEnabled = isChecked
        }
        mForwardLockSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            if (mCurrentlyEditedButton!!.getProperties() is ControlJoystickData) {
                (mCurrentlyEditedButton!!.getProperties() as ControlJoystickData).forwardLock = isChecked
            }
        }
        mAbsoluteTrackingSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            if (mCurrentlyEditedButton!!.getProperties() is ControlJoystickData) {
                (mCurrentlyEditedButton!!.getProperties() as ControlJoystickData).absolute = isChecked
            }
        }

        mAlphaSeekbar!!.setOnSeekBarChangeListener(SimpleSeekBarListener { _, progress, _ ->
            if (internalChanges) return@SimpleSeekBarListener
            mCurrentlyEditedButton!!.getProperties().opacity = mAlphaSeekbar!!.progress / 100f
            mCurrentlyEditedButton!!.getControlView().alpha = mAlphaSeekbar!!.progress / 100f
            setPercentageText(mAlphaPercentTextView!!, progress)
        })

        mStrokeWidthSeekbar!!.setOnSeekBarChangeListener(SimpleSeekBarListener { _, progress, _ ->
            if (internalChanges) return@SimpleSeekBarListener
            mCurrentlyEditedButton!!.getProperties().strokeWidth = mStrokeWidthSeekbar!!.progress / 10f
            mCurrentlyEditedButton!!.setBackground()
            setPercentageText(mStrokePercentTextView!!, progress)
        })

        mCornerRadiusSeekbar!!.setOnSeekBarChangeListener(SimpleSeekBarListener { _, progress, _ ->
            if (internalChanges) return@SimpleSeekBarListener
            mCurrentlyEditedButton!!.getProperties().cornerRadius = mCornerRadiusSeekbar!!.progress.toFloat()
            mCurrentlyEditedButton!!.setBackground()
            setPercentageText(mCornerRadiusPercentTextView!!, progress)
        })

        for (i in mKeycodeSpinners.indices) {
            val finalI = i
            mKeycodeTextviews[i]!!.setOnClickListener { mKeycodeSpinners[finalI]!!.performClick() }
            mKeycodeSpinners[i]!!.setOnItemSelectedListener(SimpleItemSelectedListener { _, _, position, _ ->
                if (position < mSpecialArray!!.size) {
                    mCurrentlyEditedButton!!.getProperties().keycodes[finalI] =
                        mKeycodeSpinners[finalI]!!.selectedItemPosition - mSpecialArray!!.size
                } else {
                    mCurrentlyEditedButton!!.getProperties().keycodes[finalI] =
                        EfficientAndroidLWJGLKeycode.getValueByIndex(
                            mKeycodeSpinners[finalI]!!.selectedItemPosition - mSpecialArray!!.size
                        )
                }
                mKeycodeTextviews[finalI]!!.text = mKeycodeSpinners[finalI]!!.selectedItem as String
            })
        }

        mOrientationSpinner!!.setOnItemSelectedListener(SimpleItemSelectedListener { _, _, position, _ ->
            if (mCurrentlyEditedButton is ControlDrawer) {
                (mCurrentlyEditedButton as ControlDrawer).drawerData.orientation =
                    ControlDrawerData.Orientation.intToOrientation(mOrientationSpinner!!.selectedItemPosition)!!
                (mCurrentlyEditedButton as ControlDrawer).syncButtons()
            }
        })

        mDisplayInGameCheckbox!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().displayInGame = isChecked
        }

        mDisplayInMenuCheckbox!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().displayInMenu = isChecked
        }

        mSelectStrokeColor!!.setOnClickListener {
            mColorSelector!!.alphaEnabled = false
            mColorSelector!!.setColorSelectionListener { color ->
                removeBitmap(mCurrentlyEditedButton!!)
                mCurrentlyEditedButton!!.getProperties().strokeColor = color
                mCurrentlyEditedButton!!.setBackground()
            }
            appearColor(isAtRight(), mCurrentlyEditedButton!!.getProperties().strokeColor)
        }

        mSelectBackgroundBitmap!!.setOnClickListener {
            val mTargetView = mCurrentlyEditedButton!!.getControlView()
            val receiver = object : CropperUtils.CropperReceiver() {
                override fun getAspectRatio(): Float {
                    return mTargetView.width.toFloat() / mTargetView.height
                }

                override fun getTargetMaxSide(): Int {
                    return maxOf(mTargetView.width, mTargetView.height)
                }

                override fun onCropped(contentBitmap: Bitmap?) {
                    val buttonProperties = mCurrentlyEditedButton!!.getProperties()
                    val storage = mCurrentlyEditedButton!!.getControlLayoutParent()!!.getBitmaps()!!
                    val oldTag = buttonProperties.bitmapTag
                    buttonProperties.bitmapTag = storage.putBitmap(contentBitmap, oldTag!!)
                    setHasBitmap(true)
                    mCurrentlyEditedButton!!.setBackground()
                }

                override fun onFailed(exception: Exception?) {
                    Tools.showError(mTargetView.context, exception!!)
                }
            }
            val context = mTargetView.context
            if (context is CustomControlsActivity) {
                context.startCropping(receiver)
            }
        }

        mSelectBackgroundColor!!.setOnClickListener {
            mColorSelector!!.alphaEnabled = true
            mColorSelector!!.setColorSelectionListener { color ->
                removeBitmap(mCurrentlyEditedButton!!)
                mCurrentlyEditedButton!!.getProperties().bgColor = color
                mCurrentlyEditedButton!!.setBackground()
            }
            appearColor(isAtRight(), mCurrentlyEditedButton!!.getProperties().bgColor)
        }
    }

    private fun safeParseFloat(string: String): Float {
        return try {
            string.toFloat()
        } catch (e: NumberFormatException) {
            Log.e("EditControlPopup", e.toString())
            -1f
        }
    }

    fun setCurrentlyEditedButton(button: ControlInterface) {
        if (mCurrentlyEditedButton != null)
            mCurrentlyEditedButton!!.getControlView().removeOnLayoutChangeListener(mLayoutChangedListener)
        mCurrentlyEditedButton = button
        mCurrentlyEditedButton!!.getControlView().addOnLayoutChangeListener(mLayoutChangedListener)
    }
}
