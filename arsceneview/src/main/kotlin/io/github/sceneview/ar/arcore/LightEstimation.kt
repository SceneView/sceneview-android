package io.github.sceneview.ar.arcore

import com.google.android.filament.IndirectLight
import com.google.android.filament.Texture
import com.google.ar.core.Config
import com.google.ar.core.LightEstimate
import dev.romainguy.kotlin.math.max
import dev.romainguy.kotlin.math.pow
import io.github.sceneview.*
import io.github.sceneview.environment.Environment
import io.github.sceneview.environment.HDREnvironment
import io.github.sceneview.light.*
import io.github.sceneview.utils.Color
import io.github.sceneview.utils.Direction
import io.github.sceneview.utils.colorOf
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ### ARCore light estimation configuration
 *
 * ARCore estimate lighting to provide directional light, ambient spherical harmonics,
 * and reflection cubemap estimation
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
data class LightEstimationMode @JvmOverloads constructor(
    /**
     * ### The behavior of the lighting estimation subsystem.
     *
     * These modes consist of separate APIs that allow for granular and realistic lighting
     * estimation for directional lighting, shadows, specular highlights, and reflections.
     */
    val sessionConfigMode: Config.LightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR,

    /**
     * ### Enable reflection cubemap
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
    val environmentalHdrReflections: Boolean = true,

    /**
     * ### Ambient spherical harmonics
     *
     * In addition to the light energy in the main directional light, ARCore provides spherical
     * harmonics, representing the overall ambient light coming in from all directions in the scene.
     * Add subtle cues that bring out the definition of virtual objects.
     */
    val environmentalHdrSphericalHarmonics: Boolean = true,

    /**
     * ### SpecularFilter applies a filter based on the BRDF used for lighting
     *
     * Specular highlights are the shiny bits of surfaces that reflect a light source directly.
     * Highlights on an object change relative to the position of a viewer in a scene.
     *
     * `true` = Reduce the amount of reflectivity a surface has. It is a key component in determining
     * the brightness of specular highlights, along with shininess to determine the size of the
     * highlights.
     */
    val environmentalHdrSpecularFilter: Boolean = true,

    /**
     * ### Move the directional light
     *
     * When the main light source or a lit object is in motion, the specular highlight on the
     * object adjusts its position in real time relative to the light source.
     *
     * Directional shadows also adjust their length and direction relative to the position of the
     * main light source, just as they do in the real world.
     */
    val environmentalHdrMainLightDirection: Boolean = true,

    /**
     * ### Modulate the main directional light (sun) intensity
     */
    val environmentalHdrMainLightIntensity: Boolean = true
) {

    companion object {
        /**
         * ### Use this mode if you want your objects to be more like if they where real.
         *
         * Target a more realistic [IndirectLight] such as it makes objects in your scene getting
         * little reflections like if they were in real world.
         *
         * The [environmentalHdrSpecularFilter] will be true.
         *
         * The reflected environment will the one given by ARCore
         */
        val REALISTIC get() = LightEstimationMode(
            sessionConfigMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR,
            environmentalHdrSpecularFilter = true
        )

        /**
         * ### Use this mode if you want your objects to be more spectacular.
         *
         * Specular highlights are the shiny bits of surfaces that reflect a light source directly.
         * More highlights on an object change relative to the position of a viewer in a scene.
         *
         * The [environmentalHdrSpecularFilter] will be true.
         *
         * The reflected environment will the one given by ARCore
         */
        val SPECTACULAR get() = LightEstimationMode(
            sessionConfigMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR,
            environmentalHdrSpecularFilter = false
        )

        /**
         * ### Use this mode if you only want to apply ARCore lights colors and intensity
         *
         * The reflected environment will the default one or the one defined by
         * [SceneView.environment]
         */
        val AMBIENT_INTENSITY get() =
            LightEstimationMode(sessionConfigMode = Config.LightEstimationMode.AMBIENT_INTENSITY)


        /**
         * ### Use this mode if you only want to disable all ARCore light estimation lights
         *
         * The reflected environment will the default one or the one defined by
         * [SceneView.environment]
         */
        val DISABLED get() =
            LightEstimationMode(sessionConfigMode = Config.LightEstimationMode.DISABLED)
    }
}

class EnvironmentLightsEstimate(
    val timestamp: Long,

    /**
     * ### The retrieved environment
     *
     * Environmental HDR mode uses machine learning to analyze the camera images in real time
     * and synthesize environmental lighting to support realistic rendering of virtual objects.
     *
     * - Ambient spherical harmonics. Represents the remaining ambient light energy in the scene
     * - An HDR cubemap is used to render reflections in shiny metallic objects.
     */
    val environment: Environment,

    /**
     * ### Main directionnal light (Usually the sun)
     *
     * The main directional light API calculates the direction and intensity of the scene's
     * main light source. This information allows virtual objects in your scene to show reasonably
     * positioned specular highlights, and to cast shadows in a direction consistent with other
     * visible real objects.
     */
    val mainLight: Light? = null
) {
    fun destroy() {
        environment.destroy()
        mainLight?.destroy()
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

/**
 * ### The frame estimated environment
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
 *
 * @param cameraExposureFactor ARCore's light estimation uses unit-less (relative) values while
 * Filament uses a physically based camera model with lux or lumen values.
 * In order to keep the "standard" Filament behavior we scale AR Core values.
 * Infos: https://github.com/ThomasGorisse/SceneformMaintained/pull/156#issuecomment-911873565
 */
fun ArFrame.environmentLightsEstimate(
    config: LightEstimationMode,
    previousEstimate: EnvironmentLightsEstimate?,
    baseEnvironment: Environment?,
    baseLight: Light?,
    cameraExposureFactor: Float
): EnvironmentLightsEstimate? =
    frame.lightEstimate?.takeIf {
        it.state == LightEstimate.State.VALID &&
                it.timestamp != previousEstimate?.timestamp
    }?.let { lightEstimate ->
        when (config.sessionConfigMode) {
            Config.LightEstimationMode.AMBIENT_INTENSITY ->
                lightEstimate.ambientIntensityEnvironmentLights(
                    previousEstimate,
                    baseEnvironment,
                    baseLight
                )
            Config.LightEstimationMode.ENVIRONMENTAL_HDR ->
                lightEstimate.environmentalHdrEnvironmentLights(
                    previousEstimate,
                    baseEnvironment,
                    baseLight,
                    cameraExposureFactor,
                    config.environmentalHdrReflections,
                    config.environmentalHdrSphericalHarmonics,
                    config.environmentalHdrSpecularFilter,
                    config.environmentalHdrMainLightDirection,
                    config.environmentalHdrMainLightIntensity
                )
            else -> null
        }
    } ?: previousEstimate

/**
 * ### Ambient Intensity mode estimated environment
 *
 * Lighting estimation is enabled, generating a single-value intensity estimate and three (R, G, B)
 * color correction values
 *
 * Ambient Intensity mode determines the average pixel intensity and the color correction scalars
 * for a given image. It's a coarse setting designed for use cases where precise lighting is not
 * critical, such as objects that have baked-in lighting.
 */
fun LightEstimate.ambientIntensityEnvironmentLights(
    previousEstimate: EnvironmentLightsEstimate?,
    baseEnvironment: Environment?,
    baseLight: Light?
): EnvironmentLightsEstimate {
    val (colorIntensities, pixelIntensity) = FloatArray(4).apply {
        // A value of a white colorCorrection (r=1.0, g=1.0, b=1.0) and pixelIntensity of 1.0 mean
        // that no changes are made to the light settings.
        // The color correction method uses the green channel as reference baseline and scales the
        // red and blue channels accordingly. In this way the overall intensity will not be
        // significantly changed
        getColorCorrection(this, 0)
    }
        // Rendering in linear space
        .toLinearSpace()
        .let { colorCorrections ->
            // Scale max r or b or g value and fit in range [0.0, 1.0)
            // if max = green than colorIntensitiesFactors = Color(r=(0.0,1.0}, g=1.0, b=(0.0,1.0}))
            val colorIntensitiesFactors = colorCorrections.slice(0..2)
                .maxOrNull()?.takeIf { it > 0 }?.let { maxIntensity ->
                    Color(
                        colorCorrections[0] / maxIntensity,
                        colorCorrections[1] / maxIntensity,
                        colorCorrections[2] / maxIntensity
                    )
                } ?: Color(0.0001f, 0.0001f, 0.0001f)

            // Normalize the pixel intensity by multiplying it by 1.8
            val pixelIntensityFactor = colorCorrections[3] * 1.8f

            return@let colorIntensitiesFactors to pixelIntensityFactor
        }

    // Sets light estimate to modulate the scene lighting and intensity. The rendered lights
    // will use a combination of these values and the color and intensity of the lights.
    val environment = Environment(
        indirectLight = IndirectLight.Builder().apply {
            // Use default environment reflections
            baseEnvironment?.indirectLight?.reflectionsTexture?.let {
                reflections(it)
            }
            // Scale and bias the estimate to avoid over darkening. Modulates ambient color with
            // modulation factor.
            // irradianceData must have at least one vector of three floats.
            baseEnvironment?.sphericalHarmonics?.let { sphericalHarmonics ->
                irradiance(
                    3,
                    FloatArray(sphericalHarmonics.size) { index ->
                        when (index) {
                            in 0..2 -> {
                                // Use the RGinB scale factors (components 0-2) to match the color
                                // of the light in the scene
                                sphericalHarmonics[index] * colorIntensities[index]
                            }
                            else -> sphericalHarmonics[index]
                        }
                    })
            }
            baseEnvironment?.indirectLight?.intensity?.let { baseIntensity ->
                intensity(baseIntensity * pixelIntensity)
            }
        }.build(),
        sphericalHarmonics = baseEnvironment?.sphericalHarmonics
    )

    val mainLight = if (baseLight != null) {
        (previousEstimate?.mainLight ?: baseLight.clone()).apply {
            max(colorIntensities).takeIf { it > 0 }?.let { maxIntensity ->
                // Normalize value if max = green:
                // colorIntensitiesFactors = Color(r=(0.0,1.0}, g=1.0, b=(0.0,1.0}))
                val colorIntensitiesFactors = (colorIntensities / maxIntensity)
                color = baseLight.color * colorIntensitiesFactors
            }
            intensity = baseLight.intensity * pixelIntensity
        }
    } else null

    return EnvironmentLightsEstimate(timestamp, environment, mainLight)
}

/**
 * ### Environmental HDR mode estimated environment
 *
 * Lighting estimation is enabled, generating inferred Environmental HDR lighting estimation in
 * linear color space.
 *
 * This mode is incompatible with the front-facing (selfie) camera. If set on a Session created for
 * the front-facing camera, the call to configure will fail.
 * These modes consist of separate APIs that allow for granular and realistic lighting estimation
 * for directional lighting, shadows, specular highlights, and reflections.
 *
 * Environmental HDR mode uses machine learning to analyze the camera images in real time and
 * synthesize environmental lighting to support realistic rendering of virtual objects.
 *
 * This lighting estimation mode provides:
 * 1. Main directional light. Represents the main light source. Can be used to cast shadows.
 * 2.Ambient spherical harmonics. Represents the remaining ambient light energy in the scene.
 * 3.An HDR cubemap. Can be used to render reflections in shiny metallic objects.
 *
 * @param cameraExposureFactor ARCore's light estimation uses unit-less (relative) values while
 * Filament uses a physically based camera model with lux or lumen values.
 * In order to keep the "standard" Filament behavior we scale AR Core values.
 * Infos: https://github.com/ThomasGorisse/SceneformMaintained/pull/156#issuecomment-911873565
 */
fun LightEstimate.environmentalHdrEnvironmentLights(
    previousEstimate: EnvironmentLightsEstimate?,
    baseEnvironment: Environment?,
    baseLight: Light?,
    cameraExposureFactor: Float,
    withReflections: Boolean,
    withSphericalHarmonics: Boolean,
    withSpecularFilter: Boolean,
    withDirection: Boolean = true,
    withIntensity: Boolean = true
): EnvironmentLightsEstimate {
    // Returns the intensity of the main directional light based on the inferred
    // Environmental HDR Lighting Estimation. All return values are larger or equal to zero.
    // The color correction method uses the green channel as reference baseline and scales the
    // red and blue channels accordingly. In this way the overall intensity will not be
    // significantly changed
    val colorIntensitiesFactors: Color = if (withIntensity) {
        environmentalHdrMainLightIntensity
            // Rendering in linear space
            .toLinearSpace()
            // Scale max r or b or g value and fit in range [0.0, 1.0)
            // Note that if we were not using the HDR cubemap from ARCore for specular
            // lighting, we would be adding a specular contribution from the main light
            // here.
            .let { colorCorrections ->
                colorCorrections.maxOrNull()?.takeIf { it > 0 }?.let { maxIntensity ->
                    colorOf(
                        colorCorrections[0] / maxIntensity,
                        colorCorrections[1] / maxIntensity,
                        colorCorrections[2] / maxIntensity
                    )
                }
            } ?: Color(0.0001f, 0.0001f, 0.0001f)
    } else Color(1.0f, 1.0f, 1.0f)

    val colorIntensity = colorIntensitiesFactors.toFloatArray().average().toFloat()

    val environment = HDREnvironment(
        cubemap = if (withReflections) {
            acquireEnvironmentalHdrCubeMap()?.let { arImages ->
                val width = arImages[0].width
                val height = arImages[0].height
                val faceOffsets = IntArray(arImages.size)
                val buffer = Texture.PixelBufferDescriptor(
                    ByteBuffer.allocateDirect(
                        width * height *
                                arImages.size *
                                // RGB Bytes per pixel
                                6 * 2
                    ).apply {
                        // Use the device hardware's native byte order
                        order(ByteOrder.nativeOrder())

                        val rgbaBytes = ByteArray(8) // ARGB Bytes per pixel
                        arImages.forEachIndexed { index, image ->
                            faceOffsets[index] = position()
                            image.planes[0].buffer.let { imageBuffer ->
                                while (imageBuffer.hasRemaining()) {
                                    // Only take the RGB channels
                                    put(rgbaBytes.apply {
                                        imageBuffer.get(this)
                                    } // Skip the Alpha channel
                                        .sliceArray(0..5))
                                }
                            }
                            image.close()
                        }
                        flip()
                    },
                    Texture.Format.RGB,
                    Texture.Type.HALF
                )

                // Reuse the previous texture instead of creating a new one for performance and
                // memory
                val texture = (previousEstimate?.environment as? HDREnvironment)?.cubemap?.takeIf {
                    it.getWidth(0) == width && it.getHeight(0) == height
                } ?: Texture.Builder()
                    .width(width)
                    .height(height)
                    .levels(0xff)
                    .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
                    .format(Texture.InternalFormat.R11F_G11F_B10F)
                    .build(Filament.engine)
                texture.apply {
                    // TODO : Remove generatePrefilterMipmap and uncomment specularFilter =
                    //  withReflections &&  withSpecularFilter when Filament move async
                    //  For now, the execution of Filament.iblPrefilter.specularFilter(it) is making
                    //  the rendering too laggy
                    //  https://github.com/google/filament/discussions/4665
                    if (withSpecularFilter) {
                        generatePrefilterMipmap(
                            Filament.engine,
                            buffer,
                            faceOffsets,
                            Texture.PrefilterOptions().apply {
                                mirror = false
                            })
                    } else {
                        setImage(Filament.engine, 0, buffer, faceOffsets)
                    }
                }
            }
        } else {
            baseEnvironment?.indirectLight?.reflectionsTexture
        },
        irradiance = if (withSphericalHarmonics) {
            environmentalHdrAmbientSphericalHarmonics?.mapIndexed { index, sphericalHarmonic ->
                sphericalHarmonic *
                        // Convert Environmental HDR's spherical harmonics to Filament
                        // irradiance spherical harmonics.
                        EnvironmentLightsEstimate.SPHERICAL_HARMONICS_IRRADIANCE_FACTORS[index / 3]
            }?.toFloatArray()
        } else {
            baseEnvironment?.sphericalHarmonics
        },
        // TODO : Remove generatePrefilterMipmap and uncomment specularFilter = withReflections &&
        //  withSpecularFilter when Filament move async
        //  For now, the execution of Filament.iblPrefilter.specularFilter(it) is making
        //  the rendering too laggy
        //  https://github.com/google/filament/discussions/4665
//        specularFilter = withReflections && withSpecularFilter,
        specularFilter = false,
        intensity = baseEnvironment?.indirectLight?.intensity?.let { it * colorIntensity }
    ).apply {
        // Prevent destroying the reused cubemap
        sharedCubemap = true
    }

    val mainLight = if (baseLight != null) {
        (previousEstimate?.mainLight ?: baseLight.clone()).apply {
            if (withDirection) {
                environmentalHdrMainLightDirection.let { (x, y, z) ->
                    direction = Direction(-x, -y, -z)
                }
            }
            if (withIntensity) {
                // Apply the camera exposure factor
                color = baseLight.color * colorIntensitiesFactors * cameraExposureFactor
                intensity = baseLight.intensity * colorIntensity * cameraExposureFactor
            }
        }
    } else null

    return EnvironmentLightsEstimate(timestamp, environment, mainLight)
}

/**
 * If rendering in linear space, first convert the values to linear space by rising to the power 2.2
 */
fun FloatArray.toLinearSpace() = map { pow(it, 2.2f) }.toFloatArray()