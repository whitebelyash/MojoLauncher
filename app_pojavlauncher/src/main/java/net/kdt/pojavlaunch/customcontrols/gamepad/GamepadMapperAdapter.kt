package net.kdt.pojavlaunch.customcontrols.gamepad

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Spinner
import androidx.annotation.NonNull
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import git.artdeell.dnbootstrap.glfw.GrabListener
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode
import net.kdt.pojavlaunch.Tools
import android.widget.TextView

class GamepadMapperAdapter(context: Context) : RecyclerView.Adapter<GamepadMapperAdapter.ViewHolder>(), GamepadDataProvider {
    companion object {
        private const val BUTTON_COUNT = 20
    }

    private var mSimulatedGamepadMap: GamepadMap
    private var mRebinderButtons: Array<RebinderButton>
    private var mRealButtons: Array<GamepadEmulatedButton>
    private val mKeyAdapter: ArrayAdapter<String>
    private val mSpecialKeycodeCount: Int
    private var mGamepadGrabListener: GrabListener? = null
    private var mGrabState = false
    private var mOldState = false

    init {
        GamepadMapStore.load()
        mKeyAdapter = ArrayAdapter(context, R.layout.item_centered_textview_large)
        val specialKeycodeNames = GamepadMap.getSpecialKeycodeNames()
        mSpecialKeycodeCount = specialKeycodeNames.size
        mKeyAdapter.addAll(*specialKeycodeNames)
        mKeyAdapter.addAll(*EfficientAndroidLWJGLKeycode.generateKeyName())
        mRebinderButtons = arrayOf()
        mRealButtons = arrayOf()
        mSimulatedGamepadMap = GamepadMap()
        createRebinderMap()
        updateRealButtons()
    }

    private fun createRebinderMap() {
        mRebinderButtons = Array(BUTTON_COUNT) { RebinderButton(0, 0) }
        mRealButtons = Array(BUTTON_COUNT) { GamepadEmulatedButton() }
        mSimulatedGamepadMap = GamepadMap()
        var index = 0

        val btnA = RebinderButton(R.drawable.button_a, R.string.controller_button_a)
        mRebinderButtons[index] = btnA
        mSimulatedGamepadMap.BUTTON_A = btnA
        index++

        val btnB = RebinderButton(R.drawable.button_b, R.string.controller_button_b)
        mRebinderButtons[index] = btnB
        mSimulatedGamepadMap.BUTTON_B = btnB
        index++

        val btnX = RebinderButton(R.drawable.button_x, R.string.controller_button_x)
        mRebinderButtons[index] = btnX
        mSimulatedGamepadMap.BUTTON_X = btnX
        index++

        val btnY = RebinderButton(R.drawable.button_y, R.string.controller_button_y)
        mRebinderButtons[index] = btnY
        mSimulatedGamepadMap.BUTTON_Y = btnY
        index++

        val btnStart = RebinderButton(R.drawable.button_start, R.string.controller_button_start)
        mRebinderButtons[index] = btnStart
        mSimulatedGamepadMap.BUTTON_START = btnStart
        index++

        val btnSelect = RebinderButton(R.drawable.button_select, R.string.controller_button_select)
        mRebinderButtons[index] = btnSelect
        mSimulatedGamepadMap.BUTTON_SELECT = btnSelect
        index++

        val triggerRight = RebinderButton(R.drawable.trigger_right, R.string.controller_button_trigger_right)
        mRebinderButtons[index] = triggerRight
        mSimulatedGamepadMap.TRIGGER_RIGHT = triggerRight
        index++

        val triggerLeft = RebinderButton(R.drawable.trigger_left, R.string.controller_button_trigger_left)
        mRebinderButtons[index] = triggerLeft
        mSimulatedGamepadMap.TRIGGER_LEFT = triggerLeft
        index++

        val shoulderRight = RebinderButton(R.drawable.shoulder_right, R.string.controller_button_shoulder_right)
        mRebinderButtons[index] = shoulderRight
        mSimulatedGamepadMap.SHOULDER_RIGHT = shoulderRight
        index++

        val shoulderLeft = RebinderButton(R.drawable.shoulder_left, R.string.controller_button_shoulder_left)
        mRebinderButtons[index] = shoulderLeft
        mSimulatedGamepadMap.SHOULDER_LEFT = shoulderLeft
        index++

        val dirForward = RebinderButton(R.drawable.stick_right, R.string.controller_direction_forward)
        mRebinderButtons[index] = dirForward
        mSimulatedGamepadMap.DIRECTION_FORWARD = dirForward
        index++

        val dirRight = RebinderButton(R.drawable.stick_right, R.string.controller_direction_right)
        mRebinderButtons[index] = dirRight
        mSimulatedGamepadMap.DIRECTION_RIGHT = dirRight
        index++

        val dirLeft = RebinderButton(R.drawable.stick_right, R.string.controller_direction_left)
        mRebinderButtons[index] = dirLeft
        mSimulatedGamepadMap.DIRECTION_LEFT = dirLeft
        index++

        val dirBackward = RebinderButton(R.drawable.stick_right, R.string.controller_direction_backward)
        mRebinderButtons[index] = dirBackward
        mSimulatedGamepadMap.DIRECTION_BACKWARD = dirBackward
        index++

        val thumbstickRight = RebinderButton(R.drawable.stick_right_click, R.string.controller_stick_press_r)
        mRebinderButtons[index] = thumbstickRight
        mSimulatedGamepadMap.THUMBSTICK_RIGHT = thumbstickRight
        index++

        val thumbstickLeft = RebinderButton(R.drawable.stick_left_click, R.string.controller_stick_press_l)
        mRebinderButtons[index] = thumbstickLeft
        mSimulatedGamepadMap.THUMBSTICK_LEFT = thumbstickLeft
        index++

        val dpadUp = RebinderButton(R.drawable.dpad_up, R.string.controller_dpad_up)
        mRebinderButtons[index] = dpadUp
        mSimulatedGamepadMap.DPAD_UP = dpadUp
        index++

        val dpadDown = RebinderButton(R.drawable.dpad_down, R.string.controller_dpad_down)
        mRebinderButtons[index] = dpadDown
        mSimulatedGamepadMap.DPAD_DOWN = dpadDown
        index++

        val dpadRight = RebinderButton(R.drawable.dpad_right, R.string.controller_dpad_right)
        mRebinderButtons[index] = dpadRight
        mSimulatedGamepadMap.DPAD_RIGHT = dpadRight
        index++

        val dpadLeft = RebinderButton(R.drawable.dpad_left, R.string.controller_dpad_left)
        mRebinderButtons[index] = dpadLeft
        mSimulatedGamepadMap.DPAD_LEFT = dpadLeft
    }

    private fun updateRealButtons() {
        val currentRealMap = if (mGrabState) GamepadMapStore.getGameMap() else GamepadMapStore.getMenuMap()
        var index = 0
        mRealButtons[index++] = currentRealMap.BUTTON_A
        mRealButtons[index++] = currentRealMap.BUTTON_B
        mRealButtons[index++] = currentRealMap.BUTTON_X
        mRealButtons[index++] = currentRealMap.BUTTON_Y
        mRealButtons[index++] = currentRealMap.BUTTON_START
        mRealButtons[index++] = currentRealMap.BUTTON_SELECT
        mRealButtons[index++] = currentRealMap.TRIGGER_RIGHT
        mRealButtons[index++] = currentRealMap.TRIGGER_LEFT
        mRealButtons[index++] = currentRealMap.SHOULDER_RIGHT
        mRealButtons[index++] = currentRealMap.SHOULDER_LEFT
        mRealButtons[index++] = currentRealMap.DIRECTION_FORWARD
        mRealButtons[index++] = currentRealMap.DIRECTION_RIGHT
        mRealButtons[index++] = currentRealMap.DIRECTION_LEFT
        mRealButtons[index++] = currentRealMap.DIRECTION_BACKWARD
        mRealButtons[index++] = currentRealMap.THUMBSTICK_RIGHT
        mRealButtons[index++] = currentRealMap.THUMBSTICK_LEFT
        mRealButtons[index++] = currentRealMap.DPAD_UP
        mRealButtons[index++] = currentRealMap.DPAD_DOWN
        mRealButtons[index++] = currentRealMap.DPAD_RIGHT
        mRealButtons[index] = currentRealMap.DPAD_LEFT
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_controller_mapping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {
        holder.attach(position)
    }

    override fun onViewRecycled(@NonNull holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.detach()
    }

    override fun getItemCount(): Int = mRebinderButtons.size

    private fun updateStickIcons() {
        val stickIcon = if (mGrabState) R.drawable.stick_left else R.drawable.stick_right
        (mSimulatedGamepadMap.DIRECTION_FORWARD as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_BACKWARD as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_RIGHT as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_LEFT as RebinderButton).iconResourceId = stickIcon
    }

    private class RebinderButton(
        var iconResourceId: Int,
        val localeResourceId: Int
    ) : GamepadButton() {
        var mButtonHolder: ViewHolder? = null

        fun changeViewHolder(viewHolder: ViewHolder?) {
            mButtonHolder = viewHolder
            if (mButtonHolder != null) mButtonHolder!!.setPressed(mIsDown)
        }

        override fun onDownStateChanged(isDown: Boolean) {
            if (mButtonHolder == null) return
            mButtonHolder!!.setPressed(isDown)
        }
    }

    inner class ViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView),
        AdapterView.OnItemSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        companion object {
            private const val COLOR_ACTIVE_BUTTON = 0x2000FF00.toInt()
        }

        private val mContext: Context = itemView.context
        private val mButtonIcon: ImageView = itemView.findViewById(R.id.controller_mapper_button)
        private val mExpansionIndicator: ImageView = itemView.findViewById(R.id.controller_mapper_expand_button)
        private val mKeySpinners: Array<Spinner>
        private val mExpandedView: View = itemView.findViewById(R.id.controller_mapper_expanded_view)
        private val mToggleableSwitch: SwitchCompat = itemView.findViewById(R.id.controller_mapper_toggleable_switch)
        private val mKeycodeLabel: TextView = itemView.findViewById(R.id.controller_mapper_keycode_label)
        private var mAttachedPosition = -1
        private var mAttachedButton: GamepadEmulatedButton? = null
        private var mKeycodes: ShortArray = ShortArray(0)

        init {
            mToggleableSwitch.setOnCheckedChangeListener(this)
            val defaultView = itemView.findViewById<View>(R.id.controller_mapper_default_view)
            defaultView.setOnClickListener(this)
            mKeySpinners = arrayOf(
                itemView.findViewById(R.id.controller_mapper_key_spinner1),
                itemView.findViewById(R.id.controller_mapper_key_spinner2),
                itemView.findViewById(R.id.controller_mapper_key_spinner3),
                itemView.findViewById(R.id.controller_mapper_key_spinner4)
            )
            for (spinner in mKeySpinners) {
                spinner.adapter = mKeyAdapter
                spinner.onItemSelectedListener = this
            }
        }

        fun attach(index: Int) {
            val rebinderButton = mRebinderButtons[index]
            mExpandedView.visibility = View.GONE
            mButtonIcon.setImageResource(rebinderButton.iconResourceId)
            val buttonName = mContext.getString(rebinderButton.localeResourceId)
            mButtonIcon.contentDescription = buttonName
            rebinderButton.changeViewHolder(this)

            val realButton = mRealButtons[index]

            mAttachedButton = realButton

            if (realButton is GamepadButton) {
                mToggleableSwitch.isChecked = realButton.isToggleable
                mToggleableSwitch.visibility = View.VISIBLE
            } else {
                mToggleableSwitch.visibility = View.GONE
            }

            mKeycodes = realButton.keycodes

            var spinnerIndex: Int
            for (spinnerIndex in mKeycodes.indices) {
                val keySpinner = mKeySpinners[spinnerIndex]
                keySpinner.isEnabled = true
                val keyCode = mKeycodes[spinnerIndex].toInt()
                val selected: Int = if (keyCode < 0) keyCode + mSpecialKeycodeCount
                else EfficientAndroidLWJGLKeycode.getIndexByValue(keyCode) + mSpecialKeycodeCount
                keySpinner.setSelection(selected)
            }
            for (si in mKeycodes.size until mKeySpinners.size) {
                mKeySpinners[si].isEnabled = false
            }
            updateKeycodeLabel()

            mAttachedPosition = index
        }

        fun detach() {
            mRebinderButtons[mAttachedPosition].changeViewHolder(null)
            mAttachedPosition = -1
            mAttachedButton = null
        }

        fun setPressed(pressed: Boolean) {
            itemView.setBackgroundColor(if (pressed) COLOR_ACTIVE_BUTTON else Color.TRANSPARENT)
        }

        private fun updateKeycodeLabel() {
            val labelBuilder = StringBuilder()
            var first = true
            val unspecifiedPosition = GamepadMap.UNSPECIFIED.toInt() + mSpecialKeycodeCount
            for (keySpinner in mKeySpinners) {
                if (keySpinner.selectedItemPosition == unspecifiedPosition) continue
                if (!first) labelBuilder.append(" + ")
                else first = false
                labelBuilder.append(keySpinner.selectedItem.toString())
            }
            if (labelBuilder.isEmpty()) labelBuilder.append(mKeyAdapter.getItem(unspecifiedPosition))
            mKeycodeLabel.text = labelBuilder.toString()
        }

        override fun onItemSelected(adapterView: AdapterView<*>, view: View?, selectionIndex: Int, selectionId: Long) {
            if (mAttachedPosition == -1) return
            var editedKeycodeIndex = -1
            for (i in mKeySpinners.indices) {
                if (adapterView !== mKeySpinners[i]) continue
                editedKeycodeIndex = i
                break
            }
            if (editedKeycodeIndex == -1) return
            val keycodeOffset = selectionIndex - mSpecialKeycodeCount
            mKeycodes[editedKeycodeIndex] = if (selectionIndex <= mSpecialKeycodeCount) keycodeOffset.toShort()
            else EfficientAndroidLWJGLKeycode.getValueByIndex(keycodeOffset).toShort()
            updateKeycodeLabel()
            try {
                GamepadMapStore.save()
            } catch (e: Exception) {
                Tools.showError(adapterView.context, e)
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>) {}

        override fun onClick(view: View) {
            val visibility = mExpandedView.visibility
            when (visibility) {
                View.INVISIBLE, View.GONE -> {
                    mExpansionIndicator.rotation = 0f
                    mExpandedView.visibility = View.VISIBLE
                }
                View.VISIBLE -> {
                    mExpansionIndicator.rotation = 180f
                    mExpandedView.visibility = View.GONE
                }
            }
        }

        override fun onCheckedChanged(compoundButton: CompoundButton, checked: Boolean) {
            if (mAttachedButton !is GamepadButton) return
            (mAttachedButton as GamepadButton).isToggleable = checked
            try {
                GamepadMapStore.save()
            } catch (e: Exception) {
                Tools.showError(compoundButton.context, e)
            }
        }
    }

    override fun getMenuMap(): GamepadMap = mSimulatedGamepadMap

    override fun getGameMap(): GamepadMap = mSimulatedGamepadMap

    override fun isGrabbing(): Boolean = mGrabState

    override fun attachGrabListener(grabListener: GrabListener) {
        mGamepadGrabListener = grabListener
        grabListener.onGrabState(mGrabState)
    }

    fun setGrabState(newState: Boolean) {
        mGrabState = newState
        mGamepadGrabListener?.onGrabState(newState)
        if (mGrabState == mOldState) return
        updateRealButtons()
        updateStickIcons()
        notifyItemRangeChanged(0, mRebinderButtons.size)
        mOldState = mGrabState
    }
}
