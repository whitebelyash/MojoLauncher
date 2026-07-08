package net.kdt.pojavlaunch.imgcropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.math.MathUtils
import net.kdt.pojavlaunch.Tools
import top.defaults.checkerboarddrawable.CheckerboardDrawable
import kotlin.math.sqrt

class CropperView : View {
    private val mSelectionHighlight = RectF()
    val mSelectionRect = Rect()
    var horizontalLock = false
    var verticalLock = false
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mHighlightThickness = 0f
    private var mLastDistance = -1f
    private var mSelectionPadding = 0f
    private var mAspectRatio = 1f
    private var mLastTrackedPointer = 0
    private var mSelectionPaint: Paint? = null
    private var mCropperBehaviour: CropperBehaviour = CropperBehaviour.DUMMY

    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }

    fun setAspectRatio(ratio: Float) {
        mAspectRatio = ratio
    }

    protected fun init() {
        background = CheckerboardDrawable.Builder().build()
        mSelectionPadding = Tools.dpToPx(24)
        mHighlightThickness = Tools.dpToPx(3)
        mSelectionPaint = Paint()
        mSelectionPaint!!.color = Color.DKGRAY
        mSelectionPaint!!.strokeWidth = mHighlightThickness
        mHighlightThickness /= 2
        mSelectionPaint!!.style = Paint.Style.STROKE
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        var x1 = event.getX(0)
        var y1 = event.getY(0)
        if (event.pointerCount > 1) {
            var x2 = event.getX(1)
            var y2 = event.getY(1)
            val deltaXSquared = (x2 - x1) * (x2 - x1)
            val deltaYSquared = (y2 - y1) * (y2 - y1)
            val distance = sqrt(deltaXSquared + deltaYSquared)
            if (mLastDistance != -1f) {
                val distanceDelta = distance - mLastDistance
                val multiplier = 0.005f
                if (horizontalLock) {
                    x1 = mSelectionRect.left.toFloat()
                    x2 = mSelectionRect.right.toFloat()
                }
                if (verticalLock) {
                    y1 = mSelectionRect.top.toFloat()
                    y2 = mSelectionRect.bottom.toFloat()
                }
                val midpointX = (x1 + x2) / 2
                val midpointY = (y1 + y2) / 2
                mCropperBehaviour.zoom(1 + distanceDelta * multiplier, midpointX, midpointY)
            }
            mLastDistance = distance
            return true
        } else {
            mLastDistance = -1f
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLastTouchX = x1
                mLastTouchY = y1
                mLastTrackedPointer = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val trackedIndex = findPointerIndex(event, mLastTrackedPointer)
                if (trackedIndex > 0) {
                    x1 = event.getX(trackedIndex)
                    y1 = event.getY(trackedIndex)
                }
                if (trackedIndex != -1) {
                    mCropperBehaviour.pan(x1 - mLastTouchX, y1 - mLastTouchY)
                } else {
                    mLastTrackedPointer = event.getPointerId(0)
                }
                mLastTouchX = x1
                mLastTouchY = y1
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        mCropperBehaviour.drawPreHighlight(canvas)
        canvas.restore()
        canvas.drawRect(mSelectionHighlight, mSelectionPaint!!)
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return dispatchGenericMotionEvent(event)
    }

    private fun findPointerIndex(event: MotionEvent, id: Int): Int {
        for (i in 0 until event.pointerCount) {
            if (event.getPointerId(i) == id) return i
        }
        return -1
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        val lesserDimension = (Math.min(w, h) - mSelectionPadding).toInt()
        var targetWidth = lesserDimension
        var centerShiftX = (w - lesserDimension) / 2
        var targetHeight = lesserDimension
        var centerShiftY = (h - lesserDimension) / 2
        if (mAspectRatio < 1) {
            targetWidth = (lesserDimension * mAspectRatio).toInt()
            centerShiftX = (w - targetWidth) / 2
        } else if (mAspectRatio > 1) {
            targetHeight = (lesserDimension * (1f / mAspectRatio)).toInt()
            centerShiftY = (h - targetHeight) / 2
        }

        mSelectionRect.left = centerShiftX
        mSelectionRect.top = centerShiftY
        mSelectionRect.right = centerShiftX + targetWidth
        mSelectionRect.bottom = centerShiftY + targetHeight
        mCropperBehaviour.onSelectionRectUpdated()

        mSelectionHighlight.left = mSelectionRect.left - mHighlightThickness
        mSelectionHighlight.top = mSelectionRect.top + mHighlightThickness
        mSelectionHighlight.right = mSelectionRect.right + mHighlightThickness
        mSelectionHighlight.bottom = mSelectionRect.bottom - mHighlightThickness
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthSpec)
        val widthSize = MeasureSpec.getSize(widthSpec)
        val heightMode = MeasureSpec.getMode(heightSpec)
        val heightSize = MeasureSpec.getSize(heightSpec)
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSize, heightSize)
            return
        }
        var biggestAllowedDimension = mCropperBehaviour.getLargestImageSide()
        if (widthMode == MeasureSpec.EXACTLY) biggestAllowedDimension = widthSize
        if (heightMode == MeasureSpec.EXACTLY) biggestAllowedDimension = heightSize
        setMeasuredDimension(
            pickDesiredDimension(widthMode, widthSize, biggestAllowedDimension),
            pickDesiredDimension(heightMode, heightSize, biggestAllowedDimension)
        )
    }

    private fun pickDesiredDimension(mode: Int, size: Int, desired: Int): Int {
        return when (mode) {
            MeasureSpec.EXACTLY -> size
            MeasureSpec.AT_MOST -> Math.min(size, desired)
            MeasureSpec.UNSPECIFIED -> desired
            else -> desired
        }
    }

    fun setCropperBehaviour(cropperBehaviour: CropperBehaviour) {
        mCropperBehaviour = cropperBehaviour
        cropperBehaviour.onSelectionRectUpdated()
    }

    fun resetTransforms() {
        mCropperBehaviour.resetTransforms()
    }

    @CallSuper
    protected fun reset() {
        mLastDistance = -1f
    }

    fun crop(targetMaxSide: Int): Bitmap? {
        return mCropperBehaviour.crop(targetMaxSide)
    }
}
