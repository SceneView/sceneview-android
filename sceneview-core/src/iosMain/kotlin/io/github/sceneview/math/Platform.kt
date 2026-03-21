package io.github.sceneview.math

actual fun ulp(value: Float): Float {
    if (value.isNaN()) return Float.NaN
    if (value.isInfinite()) return Float.POSITIVE_INFINITY
    if (value == 0.0f) return Float.MIN_VALUE
    val bits = value.toRawBits()
    val nextBits = if (bits >= 0) bits + 1 else bits - 1
    return kotlin.math.abs(Float.fromBits(nextBits) - value)
}
