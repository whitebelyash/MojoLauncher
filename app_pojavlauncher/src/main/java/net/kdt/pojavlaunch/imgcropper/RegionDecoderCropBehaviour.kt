package net.kdt.pojavlaunch.imgcropper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture
import net.kdt.pojavlaunch.utils.MatrixUtils

class RegionDecoderCropBehaviour(hostView: CropperView) : BitmapCropBehaviour(hostView) {
    private var mBitmapDecoder: BitmapRegionDecoder? = null
    private var mOverlayBitmap: Bitmap? = null
    private val mOverlayDst = RectF(0f, 0f, 0f, 0f)
    private var mRequiresOverlayBitmap = false
    private val mDecoderPrescaleMatrix = Matrix()
    private val mHiresLoadHandler = Handler(Looper.getMainLooper())
    private var mDecodeFuture: Future<*>? = null
    private val mHiresLoadRunnable = Runnable {
        val subsectionRect = RectF(0f, 0f, mHostView.width.toFloat(), mHostView.height.toFloat())
        val overlayDst = RectF()
        discardDecodeFuture()
        mDecodeFuture = SelfReferencingFuture { myFuture ->
            val overlayBitmap = decodeRegionBitmap(overlayDst, subsectionRect)
            mHiresLoadHandler.post {
                if (myFuture.isCancelled) return@post
                mOverlayBitmap = overlayBitmap
                mOverlayDst.set(overlayDst)
                mHostView.invalidate()
            }
        }.startOnExecutor(PojavApplication.sExecutorService)
    }

    private fun decodeRegionBitmap(targetDrawRect: RectF, subsectionRect: RectF): Bitmap? {
        val decoderRect = RectF(0f, 0f, mBitmapDecoder!!.width.toFloat(), mBitmapDecoder!!.height.toFloat())
        val matrix = createDecoderImageMatrix()
        val inverse = Matrix()
        MatrixUtils.inverse(matrix, inverse)
        MatrixUtils.transformRect(subsectionRect, inverse)
        if (subsectionRect.width() > decoderRect.width()
            || subsectionRect.height() > decoderRect.height()) return null
        if (!subsectionRect.setIntersect(decoderRect, subsectionRect)) return null
        if (subsectionRect.width() < 16 || subsectionRect.height() < 16) return null
        val bitmapRegionRect = Rect(
            subsectionRect.left.toInt(),
            subsectionRect.top.toInt(),
            subsectionRect.right.toInt(),
            subsectionRect.bottom.toInt()
        )
        MatrixUtils.transformRect(subsectionRect, matrix)
        targetDrawRect.set(subsectionRect)
        return mBitmapDecoder!!.decodeRegion(bitmapRegionRect, null)
    }

    private fun discardDecodeFuture() {
        mDecodeFuture?.cancel(false)
    }

    fun setRegionDecoder(bitmapRegionDecoder: BitmapRegionDecoder) {
        mBitmapDecoder = bitmapRegionDecoder
    }

    override fun getLargestImageSide(): Int {
        if (mBitmapDecoder == null) return 0
        return Math.max(mBitmapDecoder!!.width, mBitmapDecoder!!.height)
    }

    override fun drawPreHighlight(canvas: Canvas) {
        if (mOverlayBitmap != null) {
            canvas.drawBitmap(mOverlayBitmap!!, null, mOverlayDst, null)
        } else {
            super.drawPreHighlight(canvas)
        }
    }

    override fun refresh() {
        if (mOverlayBitmap != null) {
            mOverlayBitmap!!.recycle()
            mOverlayBitmap = null
        }
        mHiresLoadHandler.removeCallbacks(mHiresLoadRunnable)
        discardDecodeFuture()
        if (mRequiresOverlayBitmap) {
            mHiresLoadHandler.postDelayed(mHiresLoadRunnable, 200)
        }
        super.refresh()
    }

    override fun applyImage() {
        createScaledSourceBitmap()
        computeDecoderPrescaleMatrix()
        super.applyImage()
    }

    override fun onSelectionRectUpdated() {
        createScaledSourceBitmap()
        computeDecoderPrescaleMatrix()
        super.onSelectionRectUpdated()
    }

    private fun createScaledSourceBitmap() {
        if (mBitmapDecoder == null) return
        val width = mHostView.width
        val height = mHostView.height
        val imageWidth = mBitmapDecoder!!.width
        val imageHeight = mBitmapDecoder!!.height
        val hRatio = width.toFloat() / imageWidth
        val vRatio = height.toFloat() / imageHeight
        var ratio = Math.max(hRatio, vRatio)
        val options = BitmapFactory.Options()
        if (ratio < 1 && ratio != 0f) {
            ratio = 1 / ratio
            options.inSampleSize = Math.floor(ratio.toDouble()).toInt()
            mRequiresOverlayBitmap = true
        } else {
            mRequiresOverlayBitmap = false
        }
        mOriginalBitmap = mBitmapDecoder!!.decodeRegion(
            Rect(0, 0, imageWidth, imageHeight),
            options
        )
    }

    private fun computeDecoderPrescaleMatrix() {
        computePrescaleMatrix(
            mDecoderPrescaleMatrix,
            mBitmapDecoder!!.width,
            mBitmapDecoder!!.height
        )
    }

    private fun createDecoderImageMatrix(): Matrix {
        val decoderImageMatrix = Matrix(mDecoderPrescaleMatrix)
        decoderImageMatrix.postConcat(mZoomMatrix)
        decoderImageMatrix.postConcat(mTranslateMatrix)
        return decoderImageMatrix
    }

    override fun crop(targetMaxSide: Int): Bitmap? {
        val hostSelection = mHostView.mSelectionRect
        val drawRect = RectF()
        val regionBitmap = decodeRegionBitmap(drawRect, RectF(hostSelection))
        if (regionBitmap == null) {
            return super.crop(targetMaxSide)
        }
        drawRect.offset(-hostSelection.left.toFloat(), -hostSelection.top.toFloat())
        val selectionDims = Rect(mHostView.mSelectionRect)
        selectionDims.offsetTo(0, 0)
        val maxSide = Math.max(selectionDims.width(), selectionDims.height())
        val scaleFactor = targetMaxSide.toFloat() / maxSide
        val drawRectScaleMatrix = Matrix()
        drawRectScaleMatrix.setScale(scaleFactor, scaleFactor)
        MatrixUtils.transformRect(drawRect, drawRectScaleMatrix)
        MatrixUtils.transformRect(selectionDims, drawRectScaleMatrix)
        val returnBitmap = Bitmap.createBitmap(selectionDims.width(), selectionDims.height(), regionBitmap.config!!)
        val canvas = Canvas(returnBitmap)
        canvas.drawBitmap(regionBitmap, null, drawRect, null)
        regionBitmap.recycle()
        return returnBitmap
    }
}
