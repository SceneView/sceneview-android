package io.github.sceneview.ar.light

import android.media.Image
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Texture
import com.google.ar.core.Config
import com.google.ar.core.LightEstimate
import io.github.sceneview.Filament
import io.github.sceneview.environment.HDREnvironment
import io.github.sceneview.light.*
import io.github.sceneview.math.toLinearSpace
import io.github.sceneview.texture.destroy
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Per frame AR light estimation.
 *
 * ARCore will estimate lighting to provide directional light, ambient spherical harmonics and
 * reflection cubemap estimation.
 *
 * A key part for creating realistic AR experiences is getting the lighting right. When a virtual
 * object is missing a shadow or has a shiny material that doesn't reflect the surrounding space,
 * users can sense that the object doesn't quite fit, even if they can't explain why.
 *
 * This is because humans subconsciously perceive cues regarding how objects are lit in their
 * environment. The Lighting Estimation API analyzes given images for such cues, providing detailed
 * information about the lighting in a scene. You can then use this information when rendering
 * virtual objects to light them under the same conditions as the scene they're placed in,
 * keeping users grounded and engaged.
 *
 * Light bounces off of surfaces differently depending on whether the surface has specular
 * (highly reflective) or diffuse (not reflective) properties.
 * For example, a metallic ball will be highly specular and reflect its environment, while
 * another ball painted a dull matte gray will be diffuse. Most real-world objects have a
 * combination of these properties — think of a scuffed-up bowling ball or a well-used credit
 * card.
 *
 * Reflective surfaces also pick up colors from the ambient environment. The coloring of an
 * object can be directly affected by the coloring of its environment. For example, a white ball
 * in a blue room will take on a bluish hue.
 *
 * The main directional light API calculates the direction and intensity of the scene's
 * main light source. This information allows virtual objects in your scene to show reasonably
 * positioned specular highlights, and to cast shadows in a direction consistent with other
 * visible real objects.
 */
class ARIndirectLight {
    object Builder : IndirectLight.Builder() {

        var lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        var baseIntensity = 30_000.0f
        var specularFilter = false

        var intensity = baseIntensity
            private set(value) {
                field = value
                super.intensity(value)
            }

        var cubemapImages: Array<Image>? = null
        private set
        var cubemapTexture: Texture? = null
            private set

        private var cubemapBuffer: ByteBuffer? = null

        /**
         * Sets the desired lighting estimation mode.
         *
         * @param lightEstimationMode The lighting estimation mode to select.
         *
         * @see LightEstimationMode
         */
        fun lightEstimationMode(lightEstimationMode: Config.LightEstimationMode) =
            apply { Builder.lightEstimationMode = lightEstimationMode }

        /**
         * Environment intensity (optional).
         *
         * Because the environment is encoded usually relative to some reference, the
         * range can be adjusted with this method.
         *
         * @param intensity Scale factor applied to the environment and irradiance such that the
         * result is in <i>lux</i>, or <i>lumen/m^2</i> (default = 30000)
         *
         * @return This Builder, for chaining calls.
         */
        override fun intensity(intensity: Float): IndirectLight.Builder =
            super.intensity(intensity).also {
                baseIntensity = intensity
            }

        fun specularFilter(specularFilter: Boolean) = apply { Builder.specularFilter = specularFilter }

        fun lightEstimate(lightEstimate: LightEstimate) =
            apply {
                cubemapImages?.forEach { it.close() }
                when (lightEstimationMode) {
                    Config.LightEstimationMode.AMBIENT_INTENSITY -> {
                        // Rendering in linear space + Normalize the pixel intensity by multiplying it by 1.8
                        val pixelIntensity = lightEstimate.pixelIntensity.toLinearSpace() * 1.8f
                        // Sets light estimate to modulate the scene lighting and intensity. The rendered lights
                        // will use a combination of these values and the color and intensity of the lights.
                        intensity = baseIntensity * pixelIntensity
                    }
                    Config.LightEstimationMode.ENVIRONMENTAL_HDR -> {
                        cubemapImages = lightEstimate.acquireEnvironmentalHdrCubeMap()

                        val (width, height) = cubemapImages[0].let { it.width to it.height }
                        val faceOffsets = IntArray(cubeMapImages.size)
                        // RGB Bytes per pixel : 6 * 2
                        val bufferSize = width * height * cubeMapImages.size * 6 * 2
                        val buffer = cubemapBuffer?.takeIf {
                            it.capacity() == bufferSize
                        }?.apply {
                            clear()
                        } ?: ByteBuffer.allocateDirect(bufferSize).apply {
                            // Use the device hardware's native byte order
                            order(ByteOrder.nativeOrder())
                        }.also {
                            cubemapBuffer = it
                        }
                        val rgbBytes = ByteArray(6) // RGB Bytes per pixel
                        cubeMapImages.forEachIndexed { index, image ->
                            faceOffsets[index] = buffer.position()
                            image.planes[0].buffer.let { imageBuffer ->
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
                        val texture = cubemapTexture?.takeIf {
                            it.getWidth(0) == width && it.getHeight(0) == height
                        } ?: Texture.Builder()
                            .width(width)
                            .height(height)
                            .levels(0xff)
                            .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
                            .format(Texture.InternalFormat.R11F_G11F_B10F)
                            .build()
                            .also {
                                cubemapTexture = it
                            }
                        texture.setImage(
                            engine
                            0,
                            Texture.PixelBufferDescriptor(
                                buffer,
                                Texture.Format.RGB,
                                Texture.Type.HALF,
                                1, 0, 0, 0, null
                            ) {
                                cubeMapImages.forEach { it.close() }
                                buffer.clear()
                            },
                            faceOffsets
                        )

                        reflections(
                            if (specularFilter) {
                                // TODO: Find a better way to destroy cubemap
                                Filament.iblPrefilter.specularFilter(texture).also {
                                    if (!createSkybox) {
                                        cubemap.destroy()
                                    }
                                }
                            } else {
                                texture
                            }
                        )
                        val sphericalHarmonics =
                            lightEstimate.environmentalHdrAmbientSphericalHarmonics
                        val irradiance = sphericalHarmonics.mapIndexed { index, sphericalHarmonic ->
                            // Convert Environmental HDR's spherical harmonics to Filament
                            // irradiance spherical harmonics.
                            sphericalHarmonic * SPHERICAL_HARMONICS_IRRADIANCE_FACTORS[index / 3]
                        }.toFloatArray()

                        irradiance(3, irradiance)

                        indirectLightIntensity?.let {
                            intensity(it)
                        }

                        val colorIntensity =
                            colorIntensitiesFactors.toFloatArray().average().toFloat()
                        IndirectLight.Builder().build(engine).irradianceTexture.environment =
                            HDREnvironment(
                                lifecycle = lifecycle,
                                cubemap = when {
                                    environmentalHdrReflections -> {

                                    }
                                    defaultEnvironmentReflections -> {
                                        sceneView.indirectLight?.reflectionsTexture
                                    }
                                    else -> null
                                },
                                indirectLightIrradiance = if (environmentalHdrSphericalHarmonics) {

                                } else {
                                    sceneView.environment?.sphericalHarmonics
                                },
                                indirectLightSpecularFilter = environmentalHdrReflections &&
                                        environmentalHdrSpecularFilter,
                                indirectLightIntensity = sceneView.environment?.indirectLight?.let {
                                    it.intensity * colorIntensity
                                },
                                createSkybox = false,
                                sharedCubemap = true
                            )
                    }
                    else -> {
                        environment = null
                        mainLight = null
                    }
                }
            }

        override fun build(engine: Engine): IndirectLight {

            return super.build(engine)
        }
    }

    companion object {

        /**
         * ### Convert Environmental HDR's spherical harmonics to Filament spherical harmonics.
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