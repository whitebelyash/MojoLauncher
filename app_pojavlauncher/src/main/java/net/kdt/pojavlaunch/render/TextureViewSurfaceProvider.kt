package net.kdt.pojavlaunch.render

import net.kdt.pojavlaunch.CallbackBridge.windowHeight
import net.kdt.pojavlaunch.CallbackBridge.windowWidth
import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.annotation.NonNull
import net.kdt.pojavlaunch.Tools

class TextureViewSurfaceProvider : SurfaceProvider {
    private var mTextureView: TextureView? = null
    private var mCallback: SurfaceCallback? = null

    override fun create(context: Context, callback: SurfaceCallback): View {
        mCallback = callback
        mTextureView = TextureView(context)
        mTextureView!!.isOpaque = true
        mTextureView!!.alpha = 1.0f
        mTextureView!!.surfaceTextureListener = CallbackAdapter()
        return mTextureView!!
    }

    override fun updateSize() {
        val surfaceTexture = mTextureView!!.surfaceTexture
        if (surfaceTexture != null) {
            surfaceTexture.setDefaultBufferSize(windowWidth, windowHeight)
            Tools.runOnUiThread { mCallback!!.onSurfaceResized() }
        }
    }

    private inner class CallbackAdapter : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            @NonNull surfaceTexture: SurfaceTexture,
            i: Int,
            i1: Int
        ) {
            if (windowWidth != 0 && windowHeight != 0)
                surfaceTexture.setDefaultBufferSize(windowWidth, windowHeight)
            mCallback!!.onSurfaceAvailable(Surface(surfaceTexture))
        }

        override fun onSurfaceTextureDestroyed(@NonNull surfaceTexture: SurfaceTexture): Boolean {
            mCallback!!.onSurfaceDestroyed()
            return true
        }

        override fun onSurfaceTextureSizeChanged(
            @NonNull surfaceTexture: SurfaceTexture,
            i: Int,
            i1: Int
        ) {
            mCallback!!.onSurfaceResized()
        }

        override fun onSurfaceTextureUpdated(@NonNull surfaceTexture: SurfaceTexture) {}
    }
}
