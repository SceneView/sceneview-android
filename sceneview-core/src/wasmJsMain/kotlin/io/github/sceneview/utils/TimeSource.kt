package io.github.sceneview.utils

/**
 * Returns performance.now() from the browser, in milliseconds.
 */
@JsFun("() => performance.now()")
private external fun performanceNow(): Double

actual fun nanoTime(): Long = (performanceNow() * 1_000_000).toLong()
