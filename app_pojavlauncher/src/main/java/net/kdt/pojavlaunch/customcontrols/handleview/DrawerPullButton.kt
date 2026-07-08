package net.kdt.pojavlaunch.customcontrols.handleview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Nullable
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import git.artdeell.mojo.R

class DrawerPullButton : View {
    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) { init() }

    private val mBackgroundPaint = Paint()
    private var mDrawable: VectorDrawableCompat? = null

    private fun init() {
        mDrawable = VectorDrawableCompat.create(context.resources, R.drawable.ic_sharp_settings_24, null)
        alpha = 0.33f
        mBackgroundPaint.color = Color.BLACK
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawArc(
            paddingLeft.toFloat(), (-height + paddingBottom).toFloat(),
            (width - paddingRight).toFloat(), (height - paddingBottom).toFloat(),
            0f, 180f, true, mBackgroundPaint
        )

        mDrawable?.setBounds(
            paddingLeft / 2, paddingTop / 2,
            height - paddingRight / 2, height - paddingBottom / 2
        )
        canvas.save()
        canvas.translate((width - height) / 2f, -paddingBottom / 2f)
        mDrawable?.draw(canvas)
        canvas.restore()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val parentWidth = (parent as View).width
        translationX = (parentWidth * 0.25).toInt().toFloat()
    }
}
