package com.google.ar.sceneform

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import io.github.sceneview.BuildConfig

/**
 * Global static Sceneform access class
 */
object Sceneform {
    private val TAG = Sceneform::class.java.simpleName

    private const val MIN_BUILD_VERSION: Double = Build.VERSION_CODES.N.toDouble()
    private const val MIN_OPENGL_VERSION: Double = 3.0

    /**
     * Returns true if Sceneform can run and is compatible on this device.
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     */
    @JvmStatic
    fun isSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < MIN_BUILD_VERSION) {
            Log.e(TAG, "Sceneform requires Android N or later")
            return false
        }
        val openGlVersionString =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
            return false
        }
        return true
    }

    @JvmStatic
    fun versionName(): String {
        return BuildConfig.VERSION_NAME
    }
}
