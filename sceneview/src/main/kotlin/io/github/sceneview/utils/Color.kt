package io.github.sceneview.utils

import com.google.android.filament.utils.pow

/**
 * @see FloatArray.toLinearSpace
 */
fun Color.toLinearSpace() = colorOf(this.toFloatArray().toLinearSpace())

/**
 * If rendering in linear space, first convert the gray scaled values to linear space by rising to
 * the power 2.2
 */
fun FloatArray.toLinearSpace() = map { pow(it, 2.2f) }