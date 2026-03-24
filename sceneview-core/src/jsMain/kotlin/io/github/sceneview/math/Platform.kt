package io.github.sceneview.math

import kotlin.math.abs

actual fun ulp(value: Float): Float {
    if (value.isNaN()) return Float.NaN
    if (value.isInfinite()) return Float.POSITIVE_INFINITY
    if (value == 0f) return Float.MIN_VALUE
    val bits = value.toBits()
    val nextBits = if (bits >= 0) bits + 1 else bits - 1
    return abs(Float.fromBits(nextBits) - value)
}
