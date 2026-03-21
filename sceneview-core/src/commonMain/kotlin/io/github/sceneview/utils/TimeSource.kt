package io.github.sceneview.utils

/**
 * Returns the current monotonic time in nanoseconds.
 *
 * Platform-specific implementations:
 * - Android/JVM: `System.nanoTime()`
 * - iOS: `mach_absolute_time()` converted to nanoseconds
 *
 * Use this as the time source for physics simulation, animation playback,
 * and frame timing in cross-platform code.
 */
expect fun nanoTime(): Long
