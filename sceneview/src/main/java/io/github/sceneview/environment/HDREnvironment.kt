package io.github.sceneview.environment

import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.utils.IBLPrefilterContext

const val defaultSpecularFilter = true
const val defaultCreateSkybox = true

open class HDREnvironment(
    engine: Engine,
    iblPrefilter: IBLPrefilter,
    cubemap: Texture? = null,
    indirectLightIrradiance: FloatArray? = null,
    indirectLightIntensity: Float? = null,
    indirectLightSpecularFilter: Boolean = defaultSpecularFilter,
    createSkybox: Boolean = defaultCreateSkybox,
    val sharedCubemap: Boolean = false
) : Environment(
    engine = engine,
    indirectLight = IndirectLight.Builder().apply {
        cubemap?.let { cubemap ->
            reflections(
                if (indirectLightSpecularFilter) {
                    // TODO: Find a better way to destroy cubemap
                    iblPrefilter.specularFilter(cubemap).also {
                        if (!createSkybox) {
                            engine.destroyTexture(cubemap)
                        }
                    }
                } else {
                    cubemap
                }
            )
        }
        indirectLightIrradiance?.let {
            irradiance(3, it)
        }
        indirectLightIntensity?.let {
            intensity(it)
        }
    }.build(engine),
    sphericalHarmonics = indirectLightIrradiance,
    skybox = cubemap?.takeIf { createSkybox }?.let {
        Skybox.Builder()
            .environment(it)
            .build(engine)
    }
) {
    var cubemap: Texture? = cubemap
        internal set
    var indirectLightIntensity: Float? = indirectLightIntensity
        private set

    /**
     * ### Destroys the EnvironmentLights and frees all its associated resources.
     */
    override fun destroy() {
        super.destroy()

        if (!sharedCubemap) {
            // Use runCatching because it could already be destroyed at Environment creation time
            // in case of no Skybox needed.
            cubemap?.let { runCatching { engine.destroyTexture(it) } }
            cubemap = null
        }

        indirectLightIntensity = null
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
    fun equirectangularToCubemap(equirect: Texture): Texture =
        equirectangularToCubemap.run(equirect)

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

    fun destroy() {
        runCatching { specularFilter.destroy() }
        runCatching { equirectangularToCubemap.destroy() }
        runCatching { context.destroy() }
    }
}