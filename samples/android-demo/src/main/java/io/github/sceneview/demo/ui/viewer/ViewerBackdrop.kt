package io.github.sceneview.demo.ui.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.google.android.filament.Engine
import com.google.android.filament.Skybox
import io.github.sceneview.demo.theme.SceneViewTokens
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The Model Viewer's backdrop while the environment is hidden: a flat skybox that reaches the
 * screen as `stage-background` (DESIGN.md), the colour iOS shows behind the same models.
 *
 * The viewer's surface is opaque, so the renderer's clear — black — covers the stage colour the
 * layout paints behind it; the backdrop has to be drawn by Filament. And everything Filament draws
 * goes through the view's tone mapper, [com.google.android.filament.ToneMapper.PBRNeutralToneMapper]
 * by default, whose toe pulls the darkest channel of a colour this dark close to black, and whose
 * shoulder compresses the light theme's near-white. So the backdrop is painted in the colour the
 * tone mapper maps *onto* the token — its exact inverse, the same idea as the AR camera materials'
 * `Inverse_Tonemap_Filmic` (see `createARView`).
 *
 * The Lighting demos keep `StageFade.stageBackdrop`: their floor fades into that same uncompensated
 * tone, and the two must stay one colour.
 */
object ViewerBackdrop {

    private const val START_COMPRESSION = 0.8f - 0.04f
    private const val DESATURATION = 0.15f

    /** Filament's `ToneMapper.PBRNeutralToneMapper` (Khronos PBR Neutral), one linear colour. */
    fun pbrNeutral(rgb: FloatArray): FloatArray {
        val x = min(rgb[0], min(rgb[1], rgb[2]))
        val offset = if (x < 0.08f) x - 6.25f * x * x else 0.04f
        val c = FloatArray(3) { rgb[it] - offset }
        val peak = max(c[0], max(c[1], c[2]))
        if (peak < START_COMPRESSION) return c
        val d = 1f - START_COMPRESSION
        val newPeak = 1f - d * d / (peak + d - START_COMPRESSION)
        val g = 1f - 1f / (DESATURATION * (peak - newPeak) + 1f)
        return FloatArray(3) { c[it] * newPeak / peak * (1f - g) + newPeak * g }
    }

    /** The exact inverse of [pbrNeutral], for an output below 1 on every channel. */
    fun inversePbrNeutral(rgb: FloatArray): FloatArray {
        var c = rgb.copyOf()
        val outPeak = max(rgb[0], max(rgb[1], rgb[2]))
        if (outPeak >= START_COMPRESSION) {
            val np = min(outPeak, 0.9999f)
            val d = 1f - START_COMPRESSION
            val peak = d * d / (1f - np) + START_COMPRESSION - d
            val g = 1f - 1f / (DESATURATION * (peak - np) + 1f)
            c = FloatArray(3) { (rgb[it] - np * g) / (1f - g) * peak / np }
        }
        val m = min(c[0], min(c[1], c[2]))
        val offset = if (m < 0.04f) sqrt(max(m, 0f) / 6.25f) - m else 0.04f
        return FloatArray(3) { c[it] + offset }
    }

    /** The linear colour to draw so that [color] is what the PBR Neutral tone mapper outputs. */
    fun linearBeforeToneMap(color: Color): FloatArray =
        color.convert(ColorSpaces.LinearSrgb).let { inversePbrNeutral(floatArrayOf(it.red, it.green, it.blue)) }

    /** Creates the backdrop skybox; the caller owns it and frees it with `engine.destroySkybox`. */
    fun create(engine: Engine, color: Color = SceneViewTokens.Stage.background): Skybox =
        linearBeforeToneMap(color).let { (r, g, b) -> Skybox.Builder().color(r, g, b, 1f).build(engine) }
}
