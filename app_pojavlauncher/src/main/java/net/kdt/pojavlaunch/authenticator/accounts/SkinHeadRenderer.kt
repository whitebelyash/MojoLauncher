package net.kdt.pojavlaunch.authenticator.accounts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log
import java.util.*

class SkinHeadRenderer {
    private var mCoordScale = 1
    private val mMeshBuffer = FloatArray(8)
    private val mTempBitmaps: MutableSet<Bitmap> = HashSet()

    private fun getSubregion(src: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Bitmap {
        return getSubregion(src, left, top, right, bottom, false)
    }

    private fun internalGetSubregion(
        src: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        mirror: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(src, left, top, right - left, bottom - top)
        if (!mirror) return bitmap
        val mirroredBitmap = Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width, bitmap.height,
            MIRROR_MATRIX, false
        )
        bitmap.recycle()
        return mirroredBitmap
    }

    private fun getSubregion(
        src: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        mirror: Boolean
    ): Bitmap {
        var l = left
        var t = top
        var r = right
        var b = bottom
        l *= mCoordScale
        t *= mCoordScale
        r *= mCoordScale
        b *= mCoordScale
        val subregion = internalGetSubregion(src, l, t, r, b, mirror)
        mTempBitmaps.add(subregion)
        return subregion
    }

    private fun applyFace(dst: FloatArray, face: Int, mul: Float, off: Float) {
        when (face) {
            FACE_LEFT -> {
                dst[0] = ISO_POINTS[5][0] * mul + off
                dst[1] = ISO_POINTS[5][1] * mul + off
                dst[2] = ISO_POINTS[6][0] * mul + off
                dst[3] = ISO_POINTS[6][1] * mul + off
                dst[4] = ISO_POINTS[2][0] * mul + off
                dst[5] = ISO_POINTS[2][1] * mul + off
                dst[6] = ISO_POINTS[0][0] * mul + off
                dst[7] = ISO_POINTS[0][1] * mul + off
            }

            FACE_RIGHT -> {
                dst[0] = ISO_POINTS[6][0] * mul + off
                dst[1] = ISO_POINTS[6][1] * mul + off
                dst[2] = ISO_POINTS[4][0] * mul + off
                dst[3] = ISO_POINTS[4][1] * mul + off
                dst[4] = ISO_POINTS[0][0] * mul + off
                dst[5] = ISO_POINTS[0][1] * mul + off
                dst[6] = ISO_POINTS[1][0] * mul + off
                dst[7] = ISO_POINTS[1][1] * mul + off
            }

            FACE_TOP -> {
                dst[0] = ISO_POINTS[5][0] * mul + off
                dst[1] = ISO_POINTS[5][1] * mul + off
                dst[2] = ISO_POINTS[3][0] * mul + off
                dst[3] = ISO_POINTS[3][1] * mul + off
                dst[4] = ISO_POINTS[6][0] * mul + off
                dst[5] = ISO_POINTS[6][1] * mul + off
                dst[6] = ISO_POINTS[4][0] * mul + off
                dst[7] = ISO_POINTS[4][1] * mul + off
            }

            FACE_REAR_RIGHT -> {
                dst[0] = ISO_POINTS[3][0] * mul + off
                dst[1] = ISO_POINTS[3][1] * mul + off
                dst[2] = ISO_POINTS[4][0] * mul + off
                dst[3] = ISO_POINTS[4][1] * mul + off
                dst[4] = ISO_POINTS[6][0] * mul + off
                dst[5] = ISO_POINTS[6][1] * mul + off
                dst[6] = ISO_POINTS[1][0] * mul + off
                dst[7] = ISO_POINTS[1][1] * mul + off
            }

            FACE_REAR_LEFT -> {
                dst[0] = ISO_POINTS[5][0] * mul + off
                dst[1] = ISO_POINTS[5][1] * mul + off
                dst[2] = ISO_POINTS[3][0] * mul + off
                dst[3] = ISO_POINTS[3][1] * mul + off
                dst[4] = ISO_POINTS[2][0] * mul + off
                dst[5] = ISO_POINTS[2][1] * mul + off
                dst[6] = ISO_POINTS[6][0] * mul + off
                dst[7] = ISO_POINTS[6][1] * mul + off
            }

            FACE_BOTTOM -> {
                dst[0] = ISO_POINTS[2][0] * mul + off
                dst[1] = ISO_POINTS[2][1] * mul + off
                dst[2] = ISO_POINTS[6][0] * mul + off
                dst[3] = ISO_POINTS[6][1] * mul + off
                dst[4] = ISO_POINTS[0][0] * mul + off
                dst[5] = ISO_POINTS[0][1] * mul + off
                dst[6] = ISO_POINTS[1][0] * mul + off
                dst[7] = ISO_POINTS[1][1] * mul + off
            }
        }
    }

    private fun drawMesh(canvas: Canvas, bitmap: Bitmap, face: Int, multiplier: Float, offset: Float) {
        applyFace(mMeshBuffer, face, multiplier, offset)
        canvas.drawBitmapMesh(bitmap, 1, 1, mMeshBuffer, 0, null, 0, null)
    }

    private fun prepareCoordScale(sourceDimension: Int): Boolean {
        if (sourceDimension % 64 != 0) {
            Log.e("SkinHeadRenderer", "Invalid skin dimension: $sourceDimension")
            return false
        }
        mCoordScale = sourceDimension / 64
        return true
    }

    fun render(side: Int, sourceSkin: Bitmap): Bitmap? {
        if (!prepareCoordScale(sourceSkin.width)) return null

        val overlayTopFace = getSubregion(sourceSkin, 40, 0, 48, 8)
        val overlayLeftFace = getSubregion(sourceSkin, 32, 8, 40, 16)
        val overlayRightFace = getSubregion(sourceSkin, 40, 8, 48, 16)
        val overlayBottomFace = getSubregion(sourceSkin, 48, 0, 56, 8)
        val overlayRearLeftFace = getSubregion(sourceSkin, 56, 8, 64, 16, true)
        val overlayRearRightFace = getSubregion(sourceSkin, 48, 8, 56, 16, true)

        val topFace = getSubregion(sourceSkin, 8, 0, 16, 8)
        val leftFace = getSubregion(sourceSkin, 0, 8, 8, 16)
        val rightFace = getSubregion(sourceSkin, 8, 8, 16, 16)

        val renderTarget = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(renderTarget)

        val multiplier = 1f * side
        val headOffset = multiplier / 16f
        val headMultiplier = multiplier * 14f / 16f

        drawMesh(canvas, overlayRearLeftFace, FACE_REAR_LEFT, multiplier, 0f)
        drawMesh(canvas, overlayRearRightFace, FACE_REAR_RIGHT, multiplier, 0f)
        drawMesh(canvas, overlayBottomFace, FACE_BOTTOM, multiplier, 0f)

        drawMesh(canvas, leftFace, FACE_LEFT, headMultiplier, headOffset)
        drawMesh(canvas, rightFace, FACE_RIGHT, headMultiplier, headOffset)
        drawMesh(canvas, topFace, FACE_TOP, headMultiplier, headOffset)

        drawMesh(canvas, overlayLeftFace, FACE_LEFT, multiplier, 0f)
        drawMesh(canvas, overlayRightFace, FACE_RIGHT, multiplier, 0f)
        drawMesh(canvas, overlayTopFace, FACE_TOP, multiplier, 0f)

        for (region in mTempBitmaps) region.recycle()
        mTempBitmaps.clear()

        return renderTarget
    }

    companion object {
        private val MIRROR_MATRIX = Matrix().apply { setScale(-1f, 1f) }

        private val ISO_POINTS = arrayOf(
            floatArrayOf(0.5f, 1.0f),
            floatArrayOf(0.9330127f, 0.75f),
            floatArrayOf(0.066987306f, 0.75f),
            floatArrayOf(0.5f, 0.0f),
            floatArrayOf(0.9330127f, 0.25f),
            floatArrayOf(0.066987306f, 0.25f),
            floatArrayOf(0.5f, 0.5f)
        )

        private const val FACE_LEFT = 0
        private const val FACE_RIGHT = 1
        private const val FACE_TOP = 2
        private const val FACE_REAR_RIGHT = 3
        private const val FACE_REAR_LEFT = 4
        private const val FACE_BOTTOM = 5
    }
}
