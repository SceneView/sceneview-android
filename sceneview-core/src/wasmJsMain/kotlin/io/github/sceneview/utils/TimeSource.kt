package io.github.sceneview.utils

actual fun nanoTime(): Long = (kotlinx.browser.window.performance.now() * 1_000_000).toLong()
