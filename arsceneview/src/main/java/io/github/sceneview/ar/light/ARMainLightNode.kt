package io.github.sceneview.ar.light

import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.ar.core.Config
import com.google.ar.core.LightEstimate
import com.google.ar.core.Session
import dev.romainguy.kotlin.math.max
import io.github.sceneview.components.FilamentCamera
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.math.Direction
import io.github.sceneview.math.toLinearSpace
import io.github.sceneview.nodes.LightNode
import io.github.sceneview.scene.exposureFactor
import io.github.sceneview.utils.Color
import io.github.sceneview.utils.colorOf

/**
 * Per frame AR light estimation
 *
 * ARCore will estimate lighting to provide directional light, ambient spherical harmonics,
 * and reflection cubemap estimation
 *
 * A key part for creating realistic AR experiences is getting the lighting right. When a virtual
 * object is missing a shadow or has a shiny material that doesn't reflect the surrounding space,
 * users can sense that the object doesn't quite fit, even if they can't explain why.
 * This is because humans subconsciously perceive cues regarding how objects are lit in their
 * environment. The Lighting Estimation API analyzes given images for such cues, providing detailed
 * information about the lighting in a scene. You can then use this information when rendering
 * virtual objects to light them under the same conditions as the scene they're placed in,
 * keeping users grounded and engaged.
 */
class ARMainLightNode(
    engine: Engine,
    nodeManager: NodeManager,
    type: LightManager.Type,
    builder: LightManager.Builder.() -> Unit
) : LightNode(engine, nodeManager, type, builder) {

    var timestamp: Long? = null
    var lightEstimate: LightEstimate? = null

    /**
     * @param camera Needed to retrieve the exposure normalization factor from the camera's EV100
     * ARCore's light estimation uses unit-less (relative) values while Filament uses a
     * physically based camera model with lux or lumen values.
     * In order to keep the "standard" Filament behavior we scale AR Core values.
     * Ref: https://github.com/ThomasGorisse/SceneformMaintained/pull/156#issuecomment-911873565
     */
    fun update(session: Session, camera: FilamentCamera, lightEstimate: LightEstimate) {
        this.lightEstimate = lightEstimate

        if (lightEstimate.state != LightEstimate.State.VALID
            || lightEstimate.timestamp == timestamp
        ) {
            return
        }

        val cameraExposureFactor = camera.exposureFactor
        when (session.config.lightEstimationMode) {
            Config.LightEstimationMode.AMBIENT_INTENSITY -> {
                val colorCorrections = FloatArray(4).apply {
                    // A value of a white colorCorrection (r=1.0, g=1.0, b=1.0) and pixelIntensity
                    // of 1.0 mean that no changes are made to the light settings.
                    // The color correction method uses the green channel as reference baseline and
                    // scales the red and blue channels accordingly. In this way the overall
                    // intensity will not be significantly changed
                    lightEstimate.getColorCorrection(this, 0)
                }.toLinearSpace() // Rendering in linear space
                // Scale max r or b or g value and fit in range [0.0, 1.0)
                // if `max == green` then
                // `colorIntensitiesFactors = Color(r=(0.0,1.0}, g=1.0, b=(0.0,1.0}))`
                val colorIntensities = colorCorrections.slice(0..2)
                    .maxOrNull()?.takeIf { it > 0 }?.let { maxIntensity ->
                        Color(
                            colorCorrections[0] / maxIntensity,
                            colorCorrections[1] / maxIntensity,
                            colorCorrections[2] / maxIntensity
                        )
                    } ?: Color(0.0001f, 0.0001f, 0.0001f)
                // Normalize the pixel intensity by multiplying it by 1.8
                val pixelIntensity = colorCorrections[3] * 1.8f

                val maxIntensity = max(colorIntensities)
                if (maxIntensity > 0) {
                    // Normalize value if max = green:
                    // colorIntensitiesFactors = Color(r=(0.0,1.0}, g=1.0, b=(0.0,1.0}))
                    val colorIntensitiesFactors = (colorIntensities / maxIntensity)
                    color *= colorIntensitiesFactors
                }
                intensity *= pixelIntensity
            }
            Config.LightEstimationMode.ENVIRONMENTAL_HDR -> {
                direction = lightEstimate.environmentalHdrMainLightDirection.let { (x, y, z) ->
                    Direction(-x, -y, -z)
                }
                // Returns the intensity of the main directional light based on the inferred
                // Environmental HDR Lighting Estimation. All return values are larger or equal to zero.
                // The color correction method uses the green channel as reference baseline and scales the
                // red and blue channels accordingly. In this way the overall intensity will not be
                // significantly changed
                val colorCorrections = lightEstimate.environmentalHdrMainLightIntensity
                    // Rendering in linear space
                    .toLinearSpace()
                // Scale max r or b or g value and fit in range [0.0, 1.0)
                // Note that if we were not using the HDR cubemap from ARCore for specular
                // lighting, we would be adding a specular contribution from the main light
                // here.
                val maxIntensity = colorCorrections.maxOrNull() ?: 0.0f
                val colorIntensitiesFactors = if (maxIntensity > 0) {
                    colorOf(
                        r = colorCorrections[0] / maxIntensity,
                        g = colorCorrections[1] / maxIntensity,
                        b = colorCorrections[2] / maxIntensity
                    )
                } else colorOf(r = 0.0001f, g = 0.0001f, b = 0.0001f)
                // Apply the camera exposure factor
                color *= colorIntensitiesFactors * cameraExposureFactor

                val colorIntensity = colorIntensitiesFactors.toFloatArray().average().toFloat()
                intensity *= colorIntensity * cameraExposureFactor
            }
            else -> {}
        }
    }

    companion object {
        /**
         * Convert Environmental HDR's spherical harmonics to Filament spherical harmonics.
         *
         * This conversion is calculated to include the following:
         *     - pre-scaling by SH basis normalization factor [shader optimization]
         *     - sqrt(2) factor coming from keeping only the real part of the basis
         *     [shader optimization]
         *     - 1/pi factor for the diffuse lambert BRDF [shader optimization]
         *     - |dot(n,l)| spherical harmonics [irradiance]
         *     - scaling for convolution of SH function by radially symmetrical SH function
         *     [irradiance]
         */
        internal val SPHERICAL_HARMONICS_IRRADIANCE_FACTORS =
            //  SH coefficients at indices 6 and 7 are swapped between the two implementations.
            floatArrayOf(
                0.282095f, -0.325735f, 0.325735f,
                -0.325735f, 0.273137f, -0.273137f,
                0.078848f, -0.273137f, 0.136569f
            ).let {
                it.mapIndexed { index, value ->
                    // TODO : Check if we still got to swap those indexes
                    // SH coefficients are not in the same order in Filament and Environmental HDR.
                    // SH coefficients at indices 6 and 7 are swapped between the two
                    // implementations.
                    when (index) {
                        6 -> it[7]
                        7 -> it[6]
                        else -> value
                    }
                }
            }
    }
}