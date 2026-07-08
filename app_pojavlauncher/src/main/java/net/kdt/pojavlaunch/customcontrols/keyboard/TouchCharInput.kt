package net.kdt.pojavlaunch.customcontrols.keyboard

import android.content.Context
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import git.artdeell.mojo.R

class TouchCharInput : androidx.appcompat.widget.AppCompatEditText {
    companion object {
        const val TEXT_FILLER = "                              "
    }

    constructor(@NonNull context: Context) : this(context, null)
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet) : this(context, attrs, R.attr.editTextStyle)
    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) { setup() }

    private var mIsDoingInternalChanges = false
    private var mCharacterSender: CharacterSenderStrategy? = null

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        disable()
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            disable()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    fun switchKeyboardState() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (hasFocus()) {
            clear()
            disable()
        } else {
            enable()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun clear() {
        mIsDoingInternalChanges = true
        val editable = editableText
        editable.clear()
        editable.append(TEXT_FILLER)
        Selection.setSelection(editable, TEXT_FILLER.length)
        mIsDoingInternalChanges = false
    }

    fun enable() {
        isEnabled = true
        isFocusable = true
        visibility = VISIBLE
        requestFocus()
    }

    fun disable() {
        clear()
        visibility = GONE
        clearFocus()
        isEnabled = false
    }

    private fun sendEnter() {
        mCharacterSender!!.sendEnter()
        clear()
    }

    fun setCharacterSender(characterSender: CharacterSenderStrategy) {
        mCharacterSender = characterSender
    }

    private fun setup() {
        addTextChangedListener(InputTextWatcher())
        setOnEditorActionListener { _, _, _ ->
            sendEnter()
            clear()
            disable()
            false
        }
        clear()
        disable()
    }

    private inner class InputTextWatcher : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
            if (mIsDoingInternalChanges) return
            if (mCharacterSender != null) {
                for (i in 0 until lengthBefore) {
                    mCharacterSender!!.sendBackspace()
                }
                mCharacterSender!!.sendChars(text.subSequence(start, start + lengthAfter))
            }
        }

        override fun afterTextChanged(editable: Editable) {
            if (mIsDoingInternalChanges) return
            if (editable.length < 1) clear()
        }
    }
}
