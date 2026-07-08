package net.kdt.pojavlaunch.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Insets
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.gson.JsonSyntaxException
import com.kdt.pickafile.FileListView
import com.kdt.pickafile.FileSelectedListener
import git.artdeell.dnbootstrap.glfw.GLFW
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.LauncherGLSurface
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.buttons.ControlButton
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.customcontrols.buttons.ControlJoystick
import net.kdt.pojavlaunch.customcontrols.buttons.ControlSubButton
import net.kdt.pojavlaunch.customcontrols.handleview.ActionRow
import net.kdt.pojavlaunch.customcontrols.handleview.ControlHandleView
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

class ControlLayout : FrameLayout {
    protected var mLayout: CustomControls? = null
    private var mGameSurface: LauncherGLSurface? = null
    private var mButtons: MutableList<ControlInterface>? = null
    private var mModifiable = false
    private var mIsModified = false
    private var mControlVisible = false
    private var mControlDialog: EditControlSideDialog? = null
    private var mHandleView: ControlHandleView? = null
    private var mMenuListener: ControlButtonMenuListener? = null
    var mActionRow: ActionRow? = null
    var mLayoutFileName: String? = null

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    @Throws(IOException::class, JsonSyntaxException::class)
    fun loadLayout(jsonPath: String) {
        val size = Point(width, height)
        try {
            val layout = LayoutConverter.loadAndConvertIfNecessary(size, jsonPath)
            loadLayout(layout)
            updateLoadedFileName(jsonPath)
        } catch (e: IOException) {
            val customControls = CustomControls()
            customControls.mLayoutBitmaps = LayoutBitmaps.createEmpty()
            loadLayout(customControls)
            throw e
        } catch (e: JsonSyntaxException) {
            val customControls = CustomControls()
            customControls.mLayoutBitmaps = LayoutBitmaps.createEmpty()
            loadLayout(customControls)
            throw e
        }
    }

    fun loadLayout(controlLayout: CustomControls?) {
        var sanitizedModified = false
        if (controlLayout != null) {
            sanitizedModified = LayoutSanitizer.sanitizeLayout(controlLayout)
        }
        if (mActionRow == null) {
            mActionRow = ActionRow(context)
            addView(mActionRow)
        }

        removeAllButtons()
        if (mLayout != null) {
            mLayout!!.mControlDataList = ArrayList()
            mLayout = null
        }

        System.gc()
        mapTable.clear()

        if (controlLayout == null) return

        mLayout = controlLayout

        for (joystick in mLayout!!.mJoystickDataList) {
            addJoystickView(joystick)
        }

        for (button in controlLayout.mControlDataList) {
            addControlView(button)
        }

        for (drawerData in controlLayout.mDrawerDataList) {
            val drawer = addDrawerView(drawerData)
            if (mModifiable) drawer.areButtonsVisible = true
        }

        mLayout!!.scaledAt = LauncherPreferences.PREF_BUTTONSIZE

        setModified(sanitizedModified)
        mButtons = null
        getButtonChildren()
    }

    fun addControlButton(controlButton: ControlData) {
        mLayout!!.mControlDataList.add(controlButton)
        addControlView(controlButton)
    }

    private fun addControlView(controlButton: ControlData) {
        val view = ControlButton(this, controlButton)
        if (!mModifiable) {
            view.alpha = view.getProperties().opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
        setModified(true)
    }

    fun addDrawer(drawerData: ControlDrawerData) {
        mLayout!!.mDrawerDataList.add(drawerData)
        addDrawerView(null)
    }

    private fun addDrawerView(): ControlDrawer = addDrawerView(null)

    private fun addDrawerView(drawerData: ControlDrawerData?): ControlDrawer {
        val view = ControlDrawer(this, drawerData ?: mLayout!!.mDrawerDataList.last())
        if (!mModifiable) {
            view.alpha = view.getProperties().opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
        for (subButton in view.drawerData.buttonProperties) {
            addSubView(view, subButton)
        }
        setModified(true)
        return view
    }

    fun addSubButton(drawer: ControlDrawer, controlButton: ControlData) {
        drawer.drawerData.buttonProperties.add(controlButton)
        addSubView(drawer, drawer.drawerData.buttonProperties.last())
    }

    private fun addSubView(drawer: ControlDrawer, controlButton: ControlData) {
        val view = ControlSubButton(this, controlButton, drawer)
        if (!mModifiable) {
            view.alpha = view.getProperties().opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        } else {
            view.setVisible(true)
        }
        addView(view)
        drawer.addButton(view)
        setModified(true)
    }

    fun addJoystickButton(data: ControlJoystickData) {
        mLayout!!.mJoystickDataList.add(data)
        addJoystickView(data)
    }

    private fun addJoystickView(data: ControlJoystickData) {
        val view = ControlJoystick(this, data)
        if (!mModifiable) {
            view.alpha = view.getProperties().opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
    }

    private fun removeAllButtons() {
        for (button in getButtonChildren()) {
            removeView(button.getControlView())
        }
        System.gc()
    }

    @Throws(Exception::class)
    fun saveLayout(path: String) {
        mLayout!!.save(path)
        setModified(false)
    }

    fun toggleControlVisible() {
        mControlVisible = !mControlVisible
        setControlVisible(mControlVisible)
    }

    fun getLayoutScale(): Float = mLayout!!.scaledAt

    fun getLayout(): CustomControls? = mLayout

    fun setControlVisible(isVisible: Boolean) {
        if (mModifiable) return
        mControlVisible = isVisible
        for (button in getButtonChildren()) {
            button.setVisible(
                (button.getProperties().displayInGame && GLFW.isGrabbing() ||
                        button.getProperties().displayInMenu && !GLFW.isGrabbing()) && isVisible
            )
        }
    }

    fun setModifiable(isModifiable: Boolean) {
        if (!isModifiable && mModifiable) {
            removeEditWindow()
        }
        mModifiable = isModifiable
        if (isModifiable) {
            for (button in getButtonChildren()) {
                button.setVisible(true)
            }
        }
    }

    fun getModifiable(): Boolean = mModifiable

    fun setModified(isModified: Boolean) {
        mIsModified = isModified
    }

    fun getButtonChildren(): MutableList<ControlInterface> {
        if (mModifiable || mButtons == null) {
            mButtons = ArrayList()
            for (i in 0 until childCount) {
                val v = getChildAt(i)
                if (v is ControlInterface) mButtons!!.add(v)
            }
        }
        return mButtons!!
    }

    fun refreshControlButtonPositions() {
        requestLayout()
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        if (child is ControlInterface && mControlDialog != null) {
            mControlDialog!!.disappearColor()
            mControlDialog!!.disappear(false)
        }
    }

    fun editControlButton(button: ControlInterface) {
        if (mControlDialog == null) {
            mControlDialog = EditControlSideDialog(context, this)
            post { editControlButton(button) }
            return
        }
        mControlDialog!!.internalChanges = true
        mControlDialog!!.setCurrentlyEditedButton(button)
        mControlDialog!!.appear(
            button.getControlView().x + button.getControlView().width / 2f < width / 2f
        )
        button.loadEditValues(mControlDialog!!)
        mControlDialog!!.internalChanges = false
        mControlDialog!!.disappearColor()
        if (mHandleView == null) {
            mHandleView = ControlHandleView(context)
            addView(mHandleView)
        }
        mHandleView!!.setControlButton(button)
    }

    fun adaptPanelPosition() {
        mControlDialog?.adaptPanelPosition()
    }

    val mapTable = HashMap<View, ControlInterface>()

    companion object {
        private fun eventInViewBounds(event: MotionEvent, view: View): Boolean {
            val x = event.x
            val y = event.y
            return x > view.left && x < view.right && y > view.top && y < view.bottom
        }
    }

    fun onTouch(v: View, ev: MotionEvent) {
        val action = ev.actionMasked
        val lastControlButton = mapTable[v]
        ev.offsetLocation(v.x, v.y)

        if (action == MotionEvent.ACTION_UP
            || action == MotionEvent.ACTION_CANCEL
            || action == MotionEvent.ACTION_POINTER_UP) {
            lastControlButton?.handleReleased()
            mapTable[v] = null
            return
        }

        if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_DOWN) return

        if (lastControlButton != null) {
            if (eventInViewBounds(ev, lastControlButton.getControlView())) {
                return
            }
        }

        lastControlButton?.handleReleased()
        mapTable.remove(v)

        for (button in getButtonChildren()) {
            if (!button.getProperties().isSwipeable) continue
            if (eventInViewBounds(ev, button.getControlView())) {
                if (button != lastControlButton) {
                    button.handlePressed()
                    mapTable[v] = button
                    return
                }
            }
        }
    }

    @RequiresApi(30)
    private fun isKeyboardShown(): Boolean {
        val windowInsets = rootWindowInsets
        val imeInsets = windowInsets.getInsets(WindowInsets.Type.ime())
        return imeInsets.bottom != 0 || imeInsets.left != 0 || imeInsets.top != 0 || imeInsets.right != 0
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if ((mModifiable && event.actionMasked != MotionEvent.ACTION_UP) || mControlDialog == null) return true
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        val isKeyboardHidden: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val keyboardShown = isKeyboardShown()
            isKeyboardHidden = !keyboardShown
            if (keyboardShown) imm.hideSoftInputFromWindow(windowToken, 0)
        } else {
            isKeyboardHidden = !imm.hideSoftInputFromWindow(windowToken, 0)
        }
        if (isKeyboardHidden) {
            if (mControlDialog!!.disappearLayer()) {
                mActionRow?.setFollowedButton(null)
                mHandleView?.hide()
            }
        }
        return true
    }

    fun removeEditWindow() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
        mControlDialog?.let {
            it.disappearColor()
            it.disappear(true)
        }
        mActionRow?.setFollowedButton(null)
        mHandleView?.hide()
    }

    fun save(path: String) {
        try {
            mLayout!!.save(path)
        } catch (e: IOException) {
            Log.e("ControlLayout", "Failed to save the layout at:$path")
        }
    }

    fun hasMenuButton(): Boolean {
        for (controlInterface in getButtonChildren()) {
            for (keycode in controlInterface.getProperties().keycodes) {
                if (keycode == ControlData.SPECIALBTN_MENU) return true
            }
        }
        return false
    }

    fun setMenuListener(menuListener: ControlButtonMenuListener?) {
        this.mMenuListener = menuListener
    }

    fun notifyAppMenu() {
        mMenuListener?.onClickedMenu()
    }

    fun getGameSurface(): LauncherGLSurface? {
        if (mGameSurface == null) {
            mGameSurface = findViewById(R.id.main_game_render_view)
        }
        return mGameSurface
    }

    fun askToExit(editorExitable: EditorExitable?) {
        if (mIsModified) {
            openSaveDialog(editorExitable)
        } else {
            openExitDialog(editorExitable)
        }
    }

    fun updateLoadedFileName(path: String) {
        var p = path.replace(Tools.CTRLMAP_PATH, ".")
        p = p.substring(0, p.length - 5)
        mLayoutFileName = p
    }

    @Throws(Exception::class)
    fun saveToDirectory(name: String): String {
        val jsonPath = "${Tools.CTRLMAP_PATH}/$name.json"
        saveLayout(jsonPath)
        return jsonPath
    }

    inner class OnClickExitListener(
        private val mDialog: AlertDialog,
        private val mEditText: EditText,
        private val mListener: EditorExitable?
    ) : View.OnClickListener {
        override fun onClick(v: View) {
            val context = v.context
            if (mEditText.text.toString().isEmpty()) {
                mEditText.error = context.getString(R.string.global_error_field_empty)
                return
            }
            try {
                val jsonPath = saveToDirectory(mEditText.text.toString())
                Toast.makeText(context, context.getString(R.string.global_save) + ": $jsonPath", Toast.LENGTH_SHORT).show()
                mDialog.dismiss()
                mListener?.exitEditor()
            } catch (th: Throwable) {
                Tools.showError(context, th, mListener != null)
            }
        }
    }

    fun openSaveDialog(editorExitable: EditorExitable?) {
        val context = context
        val edit = EditText(context)
        edit.isSingleLine = true
        edit.setText(mLayoutFileName)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.global_save)
        builder.setView(edit)
        builder.setPositiveButton(android.R.string.ok, null)
        builder.setNegativeButton(android.R.string.cancel, null)
        if (editorExitable != null) builder.setNeutralButton(R.string.global_save_and_exit, null)
        val dialog = builder.create()
        dialog.setOnShowListener { dialogInterface ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(OnClickExitListener(dialog, edit, null))
            if (editorExitable != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(OnClickExitListener(dialog, edit, editorExitable))
        }
        dialog.show()
    }

    fun openLoadDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.global_load)
        builder.setPositiveButton(android.R.string.cancel, null)

        val dialog = builder.create()
        val flv = FileListView(dialog, "json")
        if (Build.VERSION.SDK_INT < 29) flv.listFileAt(File(Tools.CTRLMAP_PATH))
        else flv.lockPathAt(File(Tools.CTRLMAP_PATH))
        flv.setFileSelectedListener(object : FileSelectedListener {
            override fun onFileSelected(file: File, path: String) {
                try {
                    loadLayout(path)
                } catch (e: IOException) {
                    Tools.showError(context, e)
                }
                dialog.dismiss()
            }
        })
        dialog.setView(flv)
        dialog.show()
    }

    fun openSetDefaultDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.customctrl_selectdefault)
        builder.setPositiveButton(android.R.string.cancel, null)

        val dialog = builder.create()
        val flv = FileListView(dialog, "json")
        flv.lockPathAt(File(Tools.CTRLMAP_PATH))
        flv.setFileSelectedListener(object : FileSelectedListener {
            override fun onFileSelected(file: File, path: String) {
                try {
                    LauncherPreferences.DEFAULT_PREF.edit().putString("defaultCtrl", path).apply()
                    LauncherPreferences.PREF_DEFAULTCTRL_PATH = path
                    loadLayout(path)
                } catch (e: IOException) {
                    Tools.showError(context, e)
                } catch (e: JsonSyntaxException) {
                    Tools.showError(context, e)
                }
                dialog.dismiss()
            }
        })
        dialog.setView(flv)
        dialog.show()
    }

    private fun openExitDialog(exitListener: EditorExitable?) {
        AlertDialog.Builder(context)
            .setTitle(R.string.customctrl_editor_exit_title)
            .setMessage(R.string.customctrl_editor_exit_msg)
            .setPositiveButton(R.string.global_yes) { _, _ -> exitListener?.exitEditor() }
            .setNegativeButton(R.string.global_no) { _, _ -> }
            .show()
    }

    @Suppress("RtlHardcoded")
    private fun layoutNonButtonChildren(left: Int, top: Int, right: Int, bottom: Int) {
        val count = childCount
        val parentLeft = paddingLeft
        val parentRight = right - left - paddingRight
        val parentTop = paddingTop
        val parentBottom = bottom - top - paddingBottom
        val layoutDirection = layoutDirection
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child is ControlInterface || child.visibility == View.GONE) continue
            val lp = child.layoutParams as LayoutParams
            val width = child.measuredWidth
            val height = child.measuredHeight
            var childLeft: Int
            var childTop: Int
            var gravity = lp.gravity
            if (gravity == -1) {
                gravity = Gravity.START or Gravity.TOP
            }
            val absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
            childLeft = when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.CENTER_HORIZONTAL -> parentLeft + (parentRight - parentLeft - width) / 2 +
                        lp.leftMargin - lp.rightMargin
                Gravity.RIGHT -> parentRight - width - lp.rightMargin
                else -> parentLeft + lp.leftMargin
            }
            childTop = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                Gravity.TOP -> parentTop + lp.topMargin
                Gravity.CENTER_VERTICAL -> parentTop + (parentBottom - parentTop - height) / 2 +
                        lp.topMargin - lp.bottomMargin
                Gravity.BOTTOM -> parentBottom - height - lp.bottomMargin
                else -> parentTop + lp.topMargin
            }
            child.layout(childLeft, childTop, childLeft + width, childTop + height)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        layoutNonButtonChildren(left, top, right, bottom)
        val w = right - left
        val h = bottom - top

        for (controlInterface in getButtonChildren()) {
            val properties = controlInterface.getProperties()
            val interfaceView = controlInterface.getControlView()

            val width = properties.width.toInt()
            val height = properties.height.toInt()

            interfaceView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )

            if (!changed && !interfaceView.isLayoutRequested) {
                interfaceView.layout(
                    interfaceView.left, interfaceView.top,
                    interfaceView.right, interfaceView.bottom
                )
            } else {
                val l = (properties.insertDynamicPos(properties.dynamicX, w, h) + left).toInt()
                val t = (properties.insertDynamicPos(properties.dynamicY, w, h) + top).toInt()
                val r = l + width
                val b = t + height
                interfaceView.layout(l, t, r, b)
            }
        }
    }

    fun areControlVisible(): Boolean = mControlVisible

    fun getBitmaps(): LayoutBitmaps? = mLayout?.mLayoutBitmaps
}
