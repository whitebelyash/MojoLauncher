package net.kdt.pojavlaunch.render

import android.content.Context
import android.view.Surface
import android.view.View

interface SurfaceProvider {
    fun create(context: Context, callback: SurfaceCallback): View
    fun updateSize()

    interface SurfaceCallback {
        fun onSurfaceAvailable(surface: Surface)
        fun onSurfaceResized()
        fun onSurfaceDestroyed()
    }
}
