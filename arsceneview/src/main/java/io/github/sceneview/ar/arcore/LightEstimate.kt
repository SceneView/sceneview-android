package io.github.sceneview.ar.arcore

import com.google.ar.core.LightEstimate
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.max
import io.github.sceneview.math.toLinearSpace
import kotlin.math.max

fun LightEstimate.getColorCorrection(): Pair<Float3, Float> = FloatArray(4).apply {
    // A value of a white colorCorrection (r=1.0, g=1.0, b=1.0) and pixelIntensity
    // of 1.0 mean that no changes are made to the light settings.
    // The color correction method uses the green channel as reference baseline and
    // scales the red and blue channels accordingly. In this way the overall
    // intensity will not be significantly changed
    getColorCorrection(this, 0)
}.toLinearSpace().let { (r, g, b, pixelIntensity) ->// Rendering in linear space
    Float3(r, g, b) to pixelIntensity
}

fun LightEstimate.getColorIntensities(): Float3 {
    val (colorCorrection, _) = getColorCorrection()
    val maxColorCorrection = max(max(colorCorrection), 0.0001f)
    // Scale max r or b or g value and fit in range [0.0, 1.0)
    // if `max == green` then
    // `colorIntensitiesFactors = Color(r=(0.0,1.0}, g=1.0, b=(0.0,1.0}))`
    return colorCorrection / maxColorCorrection
}