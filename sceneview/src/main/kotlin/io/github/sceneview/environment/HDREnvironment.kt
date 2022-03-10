package io.github.sceneview.environment

import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.utils.IBLPrefilterContext
import io.github.sceneview.Filament
import io.github.sceneview.texture.destroy

const val defaultSpecularFilter = true

open class HDREnvironment(
    indirectLightCubemap: Texture? = null,
    indirectLightIrradiance: FloatArray? = null,
    indirectLightIntensity: Float? = null,
    indirectLightSpecularFilter: Boolean = defaultSpecularFilter,
    skyboxCubemap: Texture? = null
) : Environment(
    indirectLight = IndirectLight.Builder().apply {
        indirectLightCubemap?.let {
            reflections(
                if (indirectLightSpecularFilter) {
                    Filament.iblPrefilter.specularFilter(it)
                } else {
                    it
                }
            )
        }
        indirectLightIrradiance?.let {
            irradiance(3, it)
        }
        indirectLightIntensity?.let {
            intensity(it)
        }
    }.build(),
    sphericalHarmonics = indirectLightIrradiance,
    skybox = skyboxCubemap?.let {
        Skybox.Builder().apply {
            environment(it)
        }.build()
    }
) {

    var indirectLightCubemap: Texture? = indirectLightCubemap
        internal set
    var indirectLightIntensity: Float? = indirectLightIntensity
        private set
    var skyboxCubemap: Texture? = skyboxCubemap
        private set

    var sharedCubemap = false

    /**
     * ### Destroys the EnvironmentLights and frees all its associated resources.
     */
    override fun destroy() {
        super.destroy()

        if (!sharedCubemap) {
            indirectLightCubemap?.destroy()
            indirectLightCubemap = null
        }
        indirectLightIntensity = null
        skyboxCubemap?.destroy()
        skyboxCubemap = null
    }
}

/**
 * ## IBLPrefilter creates and initializes GPU state common to all environment map filters.
 * Typically, only one instance per filament Engine of this object needs to exist.
 *
 * @see [IBLPrefilterContext]
 */
class IBLPrefilter(engine: Engine) {

    /**
     * ### Created IBLPrefilterContext, keeping it around if several cubemap will be processed.
     */
    val context by lazy { IBLPrefilterContext(engine) }

    /**
     * EquirectangularToCubemap is use to convert an equirectangluar image to a cubemap.
     *
     * Creates a EquirectangularToCubemap processor.
     */
    private val equirectangularToCubemap by lazy {
        IBLPrefilterContext.EquirectangularToCubemap(
            context
        )
    }

    /**
     * ### Converts an equirectangular image to a cubemap.
     *
     * @param equirect Texture to convert to a cubemap.
     * - Can't be null.
     * - Must be a 2d texture
     * - Must have equirectangular geometry, that is width == 2*height.
     * - Must be allocated with all mip levels.
     * - Must be SAMPLEABLE
     *
     * @return the cubemap texture
     *
     * @see [EquirectangularToCubemap]
     */
    fun equirectangularToCubemap(equirect: Texture) = equirectangularToCubemap.run(equirect)

    /**
     * Created specular (reflections) filter. This operation generates the kernel, so it's
     * important to keep it around if it will be reused for several cubemaps.
     * An instance of SpecularFilter is needed per filter configuration. A filter configuration
     * contains the filter's kernel and sample count.
     */
    private val specularFilter by lazy { IBLPrefilterContext.SpecularFilter(context) }

    /**
     * ### Generates a prefiltered cubemap.
     *
     * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
     *
     * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
     *
     * @param skybox Environment cubemap.
     * This cubemap is SAMPLED and have all its levels allocated.
     *
     * @return the reflections texture
     */
    fun specularFilter(skybox: Texture) = specularFilter.run(skybox)
}