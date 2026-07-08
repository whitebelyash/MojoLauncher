package net.kdt.pojavlaunch.imgcropper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import net.kdt.pojavlaunch.utils.MatrixUtils

open class BitmapCropBehaviour(protected var mHostView: CropperView) : CropperBehaviour {
    private val mTranslateInverse = Matrix()
    protected val mTranslateMatrix = Matrix()
    private val mPrescaleMatrix = Matrix()
    private val mImageMatrix = Matrix()
    protected val mZoomMatrix = Matrix()
    private var mTranslateInverseOutdated = true
    protected var mOriginalBitmap: Bitmap? = null

    override fun pan(panX: Float, panY: Float) {
        var px = panX
        var py = panY
        if (mHostView.horizontalLock) px = 0f
        if (mHostView.verticalLock) py = 0f
        if (px != 0f || py != 0f) {
            mTranslateMatrix.postTranslate(px, py)
            mTranslateInverseOutdated = true
            refresh()
        }
    }

    override fun zoom(zoomLevel: Float, midpointX: Float, midpointY: Float) {
        if (mTranslateInverseOutdated) {
            MatrixUtils.inverse(mTranslateMatrix, mTranslateInverse)
            mTranslateInverseOutdated = false
        }
        val zoomCenter = floatArrayOf(midpointX, midpointY)
        val realZoomCenter = FloatArray(2)
        mTranslateInverse.mapPoints(realZoomCenter, 0, zoomCenter, 0, 1)
        mZoomMatrix.postScale(zoomLevel, zoomLevel, realZoomCenter[0], realZoomCenter[1])
        refresh()
    }

    override fun getLargestImageSide(): Int {
        if (mOriginalBitmap == null) return 0
        return Math.max(mOriginalBitmap!!.width, mOriginalBitmap!!.height)
    }

    override fun drawPreHighlight(canvas: Canvas) {
        canvas.drawBitmap(mOriginalBitmap!!, mImageMatrix, null)
    }

    override fun onSelectionRectUpdated() {
        computeLocalPrescaleMatrix()
    }

    override fun applyImage() {
        mHostView.reset()
        computeLocalPrescaleMatrix()
        resetTransforms()
        refresh()
    }

    fun setBitmap(bitmap: Bitmap) {
        mOriginalBitmap = bitmap
    }

    protected fun refresh() {
        mImageMatrix.set(mPrescaleMatrix)
        mImageMatrix.postConcat(mZoomMatrix)
        mImageMatrix.postConcat(mTranslateMatrix)
        mHostView.invalidate()
    }

    override fun crop(targetMaxSide: Int): Bitmap? {
        val imageInverse = Matrix()
        MatrixUtils.inverse(mImageMatrix, imageInverse)
        val targetRect = Rect()
        MatrixUtils.transformRect(mHostView.mSelectionRect, targetRect, imageInverse)
        val targetWidth: Int
        val targetHeight: Int
        val targetMinDimension = Math.min(targetRect.width(), targetRect.height())
        if (targetMaxSide < targetMinDimension) {
            val ratio = targetMaxSide.toFloat() / targetMinDimension
            targetWidth = (targetRect.width() * ratio).toInt()
            targetHeight = (targetRect.height() * ratio).toInt()
        } else {
            targetWidth = targetRect.width()
            targetHeight = targetRect.height()
        }
        val croppedBitmap = Bitmap.createBitmap(
            targetWidth, targetHeight,
            mOriginalBitmap!!.config!!
        )
        val drawCanvas = Canvas(croppedBitmap)
        drawCanvas.drawBitmap(
            mOriginalBitmap!!,
            targetRect,
            Rect(0, 0, targetWidth, targetHeight),
            null
        )
        return croppedBitmap
    }

    protected fun computePrescaleMatrix(inMatrix: Matrix, imageWidth: Int, imageHeight: Int) {
        if (mOriginalBitmap == null) return
        val selectionRectWidth = mHostView.mSelectionRect.width()
        val selectionRectHeight = mHostView.mSelectionRect.height()
        val hRatio = selectionRectWidth.toFloat() / imageWidth
        val vRatio = selectionRectHeight.toFloat() / imageHeight
        val ratio = Math.min(hRatio, vRatio)
        val centerShift_x = (selectionRectWidth - imageWidth * ratio) / 2
        val centerShift_y = (selectionRectHeight - imageHeight * ratio) / 2
        inMatrix.setScale(ratio, ratio)
        inMatrix.postTranslate(
            centerShift_x + mHostView.mSelectionRect.left,
            centerShift_y + mHostView.mSelectionRect.top
        )
        refresh()
    }

    private fun computeLocalPrescaleMatrix() {
        computePrescaleMatrix(
            mPrescaleMatrix,
            mOriginalBitmap!!.width,
            mOriginalBitmap!!.height
        )
    }

    override fun resetTransforms() {
        mTranslateMatrix.reset()
        mTranslateInverse.reset()
        mZoomMatrix.reset()
        refresh()
    }
}
