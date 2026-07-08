package git.artdeell.dnbootstrap.glfw

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.NonNull
import git.artdeell.dnbglfw.R

class GLFWCursorView : View, CursorImplementor {
    private var cursorDrawable: Drawable? = null
    private val customCursorPaint = Paint()
    private var noDraw = false
    private var mouseScale = 1f

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        GLFW.setCursorImpl(this)
        if (attrs != null) {
            val arr = context.obtainStyledAttributes(attrs, R.styleable.GLFWCursorView)
            arr.use { typedArray ->
                cursorDrawable = typedArray.getDrawable(R.styleable.GLFWCursorView_defaultCursorDrawable)
            }
        }
        if (cursorDrawable == null) cursorDrawable = FallbackCursorDrawable()
        cursorDrawable!!.setBounds(0, 0, 36, 54)
    }

    override fun onDraw(@NonNull canvas: Canvas) {
        if (noDraw) return
        canvas.translate((GLFW.cursorX * width).toFloat(), (GLFW.cursorY * height).toFloat())
        val cursor = GLFW.getCursor()
        canvas.scale(mouseScale, mouseScale)
        if (cursor == null) {
            cursorDrawable!!.draw(canvas)
        } else {
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX.toFloat(), -cursor.hotY.toFloat(), customCursorPaint)
        }
    }

    override fun onCursorPosition() {
        if (!noDraw) post { invalidate() }
    }

    override fun onCursorChanged() {
        post { invalidate() }
    }

    override fun onGrabState(isGrabbing: Boolean) {
        noDraw = isGrabbing
        invalidate()
    }

    fun setCursorScale(scale: Float) {
        mouseScale = scale
    }
}
