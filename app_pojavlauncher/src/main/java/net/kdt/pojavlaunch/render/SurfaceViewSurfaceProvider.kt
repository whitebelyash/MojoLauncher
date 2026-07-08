package net.kdt.pojavlaunch.render

import net.kdt.pojavlaunch.CallbackBridge.windowHeight
import net.kdt.pojavlaunch.CallbackBridge.windowWidth
import android.content.Context
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.annotation.NonNull
import net.kdt.pojavlaunch.CallbackBridge

class SurfaceViewSurfaceProvider : SurfaceProvider {
    private var mSurfaceView: SurfaceView? = null

    override fun create(context: Context, callback: SurfaceCallback): View {
        mSurfaceView = SurfaceView(context)
        mSurfaceView!!.holder.addCallback(CallbackAdapter(callback))
        if (windowWidth != 0 && windowHeight != 0)
            mSurfaceView!!.holder.setFixedSize(windowWidth, windowHeight)
        return mSurfaceView!!
    }

    override fun updateSize() {
        mSurfaceView!!.holder.setFixedSize(windowWidth, windowHeight)
    }

    private class CallbackAdapter(private val mCallback: SurfaceCallback) : SurfaceHolder.Callback {
        override fun surfaceChanged(
            @NonNull surfaceHolder: SurfaceHolder,
            fmt: Int,
            width: Int,
            height: Int
        ) {
            mCallback.onSurfaceResized()
        }

        override fun surfaceCreated(@NonNull surfaceHolder: SurfaceHolder) {
            mCallback.onSurfaceAvailable(surfaceHolder.surface)
        }

        override fun surfaceDestroyed(@NonNull surfaceHolder: SurfaceHolder) {
            mCallback.onSurfaceDestroyed()
        }
    }
}
