package io.github.sceneview.ar.light

import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.ar.core.Config.LightEstimationMode
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import com.google.ar.core.Session
import dev.romainguy.kotlin.math.max
import io.github.sceneview.environment.IBLPrefilter
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.toLinearSpace
import io.github.sceneview.utils.exposureFactor
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
class LightEstimator(
    val engine: Engine,
    val iblPrefilter: IBLPrefilter
) {

    data class Estimation(
        var mainLightColor: Color? = null,
        var mainLightIntensity: Float? = null,
        var mainLightDirection: Direction? = null,
        var reflections: Texture? = null,
        var irradiance: FloatArray? = null
    )

    var isEnabled = true

    /**
     * Enable reflection cubemap
     *
     * - true if the AR Core reflection cubemap should be used
     * - false for using the default/static/fake environment reflections
     *
     * Use the HDR cubemap to render realistic reflections on virtual objects with medium to high
     * glossiness, such as shiny metallic surfaces. The cubemap also affects the shading and
     * appearance of objects. For example, the material of a specular object surrounded by a blue
     * environment will reflect blue hues. Calculating the HDR cubemap requires a small amount of
     * additional CPU computation.
     */
    var environmentalHdrReflections = true

    /**
     * Ambient spherical harmonics
     *
     * In addition to the light energy in the main directional light, ARCore provides spherical
     * harmonics, representing the overall ambient light coming in from all directions in the scene.
     * Add subtle cues that bring out the definition of virtual objects.
     */
    var environmentalHdrSphericalHarmonics = true

    /**
     * SpecularFilter applies a filter based on the BRDF used for lighting
     *
     * Specular highlights are the shiny bits of surfaces that reflect a light source directly.
     * Highlights on an object change relative to the position of a viewer in a scene.
     *
     * `true` = Reduce the amount of reflectivity a surface has. It is a key component in determining
     * the brightness of specular highlights, along with shininess to determine the size of the
     * highlights.
     */
    var environmentalHdrSpecularFilter = false

    /**
     * Move the directional light
     *
     * When the main light source or a lit object is in motion, the specular highlight on the
     * object adjusts its position in real time relative to the light source.
     *
     * Directional shadows also adjust their length and direction relative to the position of the
     * main light source, just as they do in the real world.
     */
    var environmentalHdrMainLightDirection = true

    /**
     * Modulate the main directional light (sun) intensity
     */
    var environmentalHdrMainLightIntensity = true

    private var timestamp: Long? = null
    private var cubeMapBuffer: ByteBuffer? = null
    private var cubeMapTexture: Texture? = null
        set(value) {
            runCatching { field?.let { engine.destroyTexture(it) } }
            field = value
        }
    private var cubeMapTextureSpecular: Texture? = null
        set(value) {
            runCatching { field?.let { engine.destroyTexture(it) } }
            field = value
        }

    fun update(session: Session, frame: Frame, camera: Camera): Estimation? {
        if (!isEnabled || session.config.lightEstimationMode == LightEstimationMode.DISABLED) {
            return null
        }

        val lightEstimate = frame.lightEstimate.takeIf {
            it.state == LightEstimate.State.VALID && it.timestamp != timestamp
        } ?: return null

        timestamp = lightEstimate.timestamp

        return when (session.config.lightEstimationMode) {
            LightEstimationMode.AMBIENT_INTENSITY -> Estimation().apply {
                val colorCorrections = FloatArray(4).apply {
                    // The float array the 4 component color correction values are written to.
                    // The four values are:
                    // - `colorCorrections[0]`: Color correction value for the red channel. This
                    // value is larger or equal to zero.
                    // - `colorCorrections[1]`: Color correction value for the green channel. This
                    // value is always 1.0 as the green channel is the reference baseline.
                    // - `colorCorrections[2]`: Color correction value for the blue channel. This
                    // value is larger or equal to zero.
                    // `colorCorrections[3]`: This value is identical to the average pixel intensity
                    // from [LightEstimate.getPixelIntensity] in the range `[0.0, 1.0]`.
                    // A value of a white colorCorrection (r=1.0, g=1.0, b=1.0) and pixelIntensity
                    // of 1.0 mean that no changes are made to the light settings.
                    // The color correction method uses the green channel as reference baseline and
                    // scales the red and blue channels accordingly. In this way the overall
                    // intensity will not be significantly changed
                    lightEstimate.getColorCorrection(this, 0)
                }

                val colorIntensitiesFactors = colorCorrections
                    .slice(0..2).let { (r, g, b) -> Color(r, g, b) }
                val maxIntensity = max(colorIntensitiesFactors)
                // Normalize color to fit into [0..1]
                // Rendering in linear space
                mainLightColor = (colorIntensitiesFactors / maxIntensity).toLinearSpace()

                // Normalize the pixel intensity by multiplying it by 1.8
                mainLightIntensity = colorCorrections[3] * 1.8f
            }

            LightEstimationMode.ENVIRONMENTAL_HDR -> Estimation().apply {
                // Returns the intensity of the main directional light based on the inferred
                // Environmental HDR Lighting Estimation. All return values are larger or equal to
                // zero.
                // The color correction method uses the green channel as reference baseline and
                // scales the red and blue channels accordingly. In this way the overall intensity
                // will not be significantly changed
                if (environmentalHdrMainLightIntensity) {
                    val colorIntensitiesFactors = lightEstimate.environmentalHdrMainLightIntensity
                        .let { (r, g, b) -> Color(r, g, b) }
                    val maxIntensity = max(colorIntensitiesFactors)
                    // ARCore's light estimation uses unit-less (relative) values while Filament
                    // uses a physically based camera model with lux or lumen values.
                    // In order to keep the "standard" Filament behavior we scale ARCore values.
                    // More info: [https://github.com/ThomasGorisse/SceneformMaintained/pull/156#issuecomment-911873565]
                    val exposureFactor = camera.exposureFactor
                    // Apply the camera exposure factor
                    mainLightColor = colorIntensitiesFactors * exposureFactor
                    // Average intensity
                    mainLightIntensity = mainLightColor?.toFloatArray()?.average()?.toFloat()
                }

                if (environmentalHdrMainLightDirection) {
                    lightEstimate.environmentalHdrMainLightDirection.let { (x, y, z) ->
                        mainLightDirection = Direction(-x, -y, -z)
                    }
                }

                if (environmentalHdrReflections) {
                    lightEstimate.acquireEnvironmentalHdrCubeMap()?.let { arImages ->
                        val (width, height) = arImages[0].width to arImages[0].height
                        val faceOffsets = IntArray(arImages.size)
                        // RGB Bytes per pixel : 6 * 2
                        val bufferSize =
                            width * height * arImages.size * 6 * 2
                        val buffer = cubeMapBuffer?.takeIf {
                            it.capacity() == bufferSize
                        }?.apply {
                            clear()
                        } ?: ByteBuffer.allocateDirect(bufferSize).apply {
                            // Use the device hardware's native byte order
                            order(ByteOrder.nativeOrder())
                            cubeMapBuffer = this
                        }
                        val rgbBytes = ByteArray(6) // RGB Bytes per pixel
                        arImages.forEachIndexed { index, image ->
                            faceOffsets[index] = buffer.position()
                            image.use {
                                val imageBuffer = image.planes[0].buffer
                                while (imageBuffer.hasRemaining()) {
                                    // Only take the RGB channels
                                    imageBuffer.get(rgbBytes)
                                    buffer.put(rgbBytes)
                                    // Skip the Alpha channel
                                    imageBuffer.position(imageBuffer.position() + 2)
                                }
                                imageBuffer.clear()
                            }
                        }
                        buffer.flip()

                        // Reuse the previous texture instead of creating a new one for
                        // performance and memory reasons
                        val cubeMapTexture = cubeMapTexture?.takeIf {
                            it.getWidth(0) == width &&
                                    it.getHeight(0) == height
                        } ?: Texture.Builder()
                            .width(width)
                            .height(height)
                            .levels(0xff)
                            .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
                            .format(Texture.InternalFormat.R11F_G11F_B10F)
                            .build(engine)
                            .also {
                                cubeMapTexture = it
                            }
                        cubeMapTexture.setImage(
                            engine,
                            0,
                            Texture.PixelBufferDescriptor(
                                buffer,
                                Texture.Format.RGB,
                                Texture.Type.HALF,
                                1, 0, 0, 0, null
                            ) {
                                arImages.forEach { it.close() }
                                buffer.clear()
                            },
                            faceOffsets
                        )
                        reflections = if (environmentalHdrSpecularFilter) {
                            iblPrefilter.specularFilter(cubeMapTexture).also {
                                cubeMapTextureSpecular = it
                            }
                        } else {
                            cubeMapTexture
                        }
                    }
                }
                if (environmentalHdrSphericalHarmonics) {
                    irradiance = lightEstimate.environmentalHdrAmbientSphericalHarmonics
                        .mapIndexed { index, sphericalHarmonic ->
                            // Convert Environmental HDR's spherical harmonics to Filament
                            // irradiance spherical harmonics.
                            sphericalHarmonic * SPHERICAL_HARMONICS_IRRADIANCE_FACTORS[index / 3]
                        }.toFloatArray()
                }
            }

            else -> null
        }
    }

    fun destroy() {
        cubeMapBuffer?.clear()
        cubeMapTexture?.let { engine.destroyTexture(it) }
        cubeMapTextureSpecular?.let { engine.destroyTexture(it) }
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