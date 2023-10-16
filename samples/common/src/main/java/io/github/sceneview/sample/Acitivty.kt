package io.github.sceneview.sample

import android.app.Activity
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment

fun Activity.setKeepScreenOn(keepScreenOn: Boolean = true) {
    if (keepScreenOn) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

/**
 * Sets the Android FitsSystemWindows and WindowInsetsController properties.
 */
fun Fragment.setFullScreen(
    fullScreen: Boolean = true,
    hideSystemBars: Boolean = true,
    fitsSystemWindows: Boolean = true
) {
    requireActivity().setFullScreen(
        this.requireView(),
        fullScreen,
        hideSystemBars,
        fitsSystemWindows
    )
}

fun Activity.setFullScreen(
    rootView: View,
    fullScreen: Boolean = true,
    hideSystemBars: Boolean = true,
    fitsSystemWindows: Boolean = true
) {
    rootView.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
        if (hasFocus) {
            WindowCompat.setDecorFitsSystemWindows(window, fitsSystemWindows)
            WindowInsetsControllerCompat(window, rootView).apply {
                if (hideSystemBars) {
                    if (fullScreen) {
                        hide(
                            WindowInsetsCompat.Type.statusBars() or
                                    WindowInsetsCompat.Type.navigationBars()
                        )
                    } else {
                        show(
                            WindowInsetsCompat.Type.statusBars() or
                                    WindowInsetsCompat.Type.navigationBars()
                        )
                    }
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }
}

fun View.doOnApplyWindowInsets(action: (systemBarsInsets: Insets) -> Unit) {
    doOnAttach {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            action(insets.getInsets(WindowInsetsCompat.Type.systemBars()))
            WindowInsetsCompat.CONSUMED
        }
    }
}
