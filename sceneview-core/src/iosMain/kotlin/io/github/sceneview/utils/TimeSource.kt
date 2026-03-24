package io.github.sceneview.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr

@OptIn(ExperimentalForeignApi::class)
actual fun nanoTime(): Long = memScoped {
    val ts = alloc<timespec>()
    clock_gettime(CLOCK_MONOTONIC.toUInt(), ts.ptr)
    ts.tv_sec * 1_000_000_000L + ts.tv_nsec.toLong()
}
