package io.github.sceneview.utils

import android.app.Activity
import android.view.WindowManager

fun Activity.setKeepScreenOn(keepScreenOn: Boolean = true) {
    if (keepScreenOn) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
