package net.kdt.pojavlaunch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.SurfaceTexture
import android.text.TextPaint
import android.os.Build
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup

import java.nio.ByteBuffer
import java.util.LinkedList

import net.kdt.pojavlaunch.utils.JREUtils

class AWTCanvasView : TextureView, TextureView.SurfaceTextureListener, Runnable {
    companion object {
        const val AWT_CANVAS_WIDTH = 720
        const val AWT_CANVAS_HEIGHT = 600
    }

    private val MAX_SIZE = 100
    private val NANOS = 1000000000.0
    private var mIsDestroyed = false
    private val mFpsPaint: TextPaint
    private val mTimes = LinkedList<Long>().apply { add(System.nanoTime()) }

    constructor(ctx: Context) : this(ctx, null)

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs) {
        mFpsPaint = TextPaint()
        mFpsPaint.color = Color.WHITE
        mFpsPaint.textSize = 20f

        isOpaque = true
        surfaceTextureListener = this

        post { refreshSize() }
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, w: Int, h: Int) {
        texture.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
        mIsDestroyed = false
        Thread(this, "AndroidAWTRenderer").start()
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        mIsDestroyed = true
        return true
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, w: Int, h: Int) {
        texture.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
        texture.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
    }

    override fun run() {
        val surface = Surface(surfaceTexture)
        val rgbArrayBitmap = Bitmap.createBitmap(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val targetBuffer = ByteBuffer.allocateDirect(rgbArrayBitmap.byteCount)
        val paint = Paint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = BlendMode.SRC
        } else {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        try {
            var canvas = surface.lockCanvas(null)
            while (!mIsDestroyed && surface.isValid) {
                surface.unlockCanvasAndPost(canvas)
                canvas = surface.lockCanvas(null)
                val mDrawing = JREUtils.renderAWTScreenFrame(targetBuffer)
                targetBuffer.rewind()
                if (mDrawing) {
                    canvas.save()
                    rgbArrayBitmap.copyPixelsFromBuffer(targetBuffer)
                    canvas.drawBitmap(rgbArrayBitmap, 0f, 0f, paint)
                    canvas.restore()
                } else {
                    canvas.drawRGB(0, 0, 0)
                }
                canvas.drawText("FPS: " + (Math.round(fps() * 10) / 10) + ", drawing=$mDrawing", 0f, 20f, mFpsPaint)
            }
        } catch (throwable: Throwable) {
            Tools.showError(context, throwable)
        }
        rgbArrayBitmap.recycle()
        surface.release()
    }

    private fun fps(): Double {
        val lastTime = System.nanoTime()
        val difference = (lastTime - mTimes.first) / NANOS
        mTimes.addLast(lastTime)
        val size = mTimes.size
        if (size > MAX_SIZE) {
            mTimes.removeFirst()
        }
        return if (difference > 0) mTimes.size / difference else 0.0
    }

    private fun refreshSize() {
        val layoutParams = layoutParams
        if (height < width) {
            layoutParams.width = AWT_CANVAS_WIDTH * height / AWT_CANVAS_HEIGHT
        } else {
            layoutParams.height = AWT_CANVAS_HEIGHT * width / AWT_CANVAS_WIDTH
        }
        setLayoutParams(layoutParams)
    }
}
