package com.google.ar.sceneform.utilities

import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Looper
import androidx.annotation.VisibleForTesting
import io.github.sceneview.collision.Preconditions

/**
 * Helper class for common android specific preconditions used inside of RenderCore.
 *
 * @hide
 */
object AndroidPreconditions {
    private val IS_ANDROID_API_AVAILABLE: Boolean = checkAndroidApiAvailable()
    private val IS_MIN_ANDROID_API_LEVEL: Boolean = isMinAndroidApiLevelImpl()
    private var isUnderTesting = false

    /**
     * Ensure that the code is being executed on Android's UI thread. Null-Op if the Android API isn't
     * available (i.e. for unit tests.
     */
    @JvmStatic
    fun checkUiThread() {
        if (!isAndroidApiAvailable() || isUnderTesting()) {
            return
        }

        val isOnUIThread = Looper.getMainLooper().thread === Thread.currentThread()
        Preconditions.checkState(isOnUIThread, "Must be called from the UI thread.")
    }

    /**
     * Enforce the minimum Android api level
     *
     * @throws IllegalStateException if the api level is not high enough
     */
    @JvmStatic
    fun checkMinAndroidApiLevel() {
        Preconditions.checkState(isMinAndroidApiLevel(), "Sceneform requires Android N or later")
    }

    /**
     * Returns true if the Android API is currently available. Useful for branching functionality to
     * make it testable via junit. The android API is available for Robolectric tests and android
     * emulator tests.
     */
    @JvmStatic
    fun isAndroidApiAvailable(): Boolean {
        return IS_ANDROID_API_AVAILABLE
    }

    @JvmStatic
    fun isUnderTesting(): Boolean {
        return isUnderTesting
    }

    /**
     * Returns true if the Android api level is above the minimum or if not on Android.
     *
     * Also returns true if not on Android or in a test.
     */
    @JvmStatic
    fun isMinAndroidApiLevel(): Boolean {
        return isUnderTesting() || IS_MIN_ANDROID_API_LEVEL
    }

    @JvmStatic
    @VisibleForTesting
    fun setUnderTesting(isUnderTesting: Boolean) {
        AndroidPreconditions.isUnderTesting = isUnderTesting
    }

    private fun isMinAndroidApiLevelImpl(): Boolean {
        return !isAndroidApiAvailable() || (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
    }

    private fun checkAndroidApiAvailable(): Boolean {
        return try {
            Class.forName("android.app.Activity")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
