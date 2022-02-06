package io.github.sceneview.environment

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.IBLPrefilterContext
import io.github.sceneview.Filament
import io.github.sceneview.light.build
import io.github.sceneview.texture.destroy
import io.github.sceneview.texture.use
import io.github.sceneview.utils.fileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer

const val defaultSpecularFilter = true

open class HDREnvironment(
    cubemap: Texture? = null,
    irradiance: FloatArray? = null,
    intensity: Float? = null,
    skyboxEnvironment: Texture? = null,
    specularFilter: Boolean = defaultSpecularFilter
) : Environment(
    indirectLight = IndirectLight.Builder().apply {
        cubemap?.let {
            reflections(
                if (specularFilter) {
                    Filament.iblPrefilter.specularFilter(it)
                } else {
                    it
                }
            )
        }
        irradiance?.let {
            irradiance(3, it)
        }
        intensity?.let {
            intensity(it)
        }
    }.build(),
    sphericalHarmonics = irradiance,
    skybox = skyboxEnvironment?.let {
        Skybox.Builder().apply {
            environment(it)
        }.build()
    }
) {

    var cubemap: Texture? = cubemap
        internal set
    var intensity: Float? = intensity
        private set
    var skyboxEnvironment: Texture? = skyboxEnvironment
        private set

    var sharedCubemap = false

    /**
     * ### Destroys the EnvironmentLights and frees all its associated resources.
     */
    override fun destroy() {
        super.destroy()

        if (!sharedCubemap) {
            cubemap?.destroy()
            cubemap = null
        }
        intensity = null
        skyboxEnvironment?.destroy()
        skyboxEnvironment = null
    }
}

/**
 * ### Utility for decoding and producing environment resources from an HDR file
 *
 * [Documentation][HDRLoader.createEnvironment]
 *
 * @param hdrFileLocation the hdr file location. See [Documentation][fileBuffer]
 * @param iblFilter See [Documentation][HDRLoader.createEnvironment]
 *
 * @return [Documentation][HDRLoader.createEnvironment]
 */
@JvmOverloads
suspend fun HDRLoader.loadEnvironment(
    context: Context,
    hdrFileLocation: String,
    specularFilter: Boolean = defaultSpecularFilter
): HDREnvironment? {
    return try {
        context.fileBuffer(hdrFileLocation)?.let { buffer ->
            withContext(Dispatchers.Main) {
                createEnvironment(buffer, specularFilter)
            }
        }
    } finally {
        // TODO: See why the finally is called before the onDestroy()
//        environment?.destroy()
    }
}

/**
 * ### Utility for decoding and producing environment resources from an HDR file
 *
 * For Java compatibility usage.
 *
 * Kotlin developers should use [HDRLoader.loadEnvironment]
 *
 * [Documentation][HDRLoader.loadEnvironment]
 *
 */
fun HDRLoader.loadEnvironmentAsync(
    context: Context,
    hdrFileLocation: String,
    specularFilter: Boolean = defaultSpecularFilter,
    coroutineScope: LifecycleCoroutineScope,
    result: (HDREnvironment?) -> Unit
) = coroutineScope.launchWhenCreated {
    result(
        HDRLoader.loadEnvironment(context, hdrFileLocation, specularFilter)
    )
}

/**
 * ### Utility for decoding and producing environment resources from an HDR file
 *
 * Consumes the content of an HDR file and produces an [IndirectLight] and a [Skybox].
 *
 * @param hdrBuffer The content of the HDR File
 * @param iblFilter A filter to apply to the resulting indirect light reflexions texture.
 * Default generates a specular prefiltered cubemap reflection texture.
 *
 * @return the generated environment indirect light and skybox from the hdr
 *
 * @see HDRLoader.createTexture
 */
@JvmOverloads
fun HDRLoader.createEnvironment(
    hdrBuffer: Buffer,
    specularFilter: Boolean = defaultSpecularFilter
) = createTexture(Filament.engine, hdrBuffer)?.use { hdrTexture ->
    Filament.iblPrefilter.equirectangularToCubemap(hdrTexture)
}?.let { cubemap ->
    HDREnvironment(cubemap = cubemap, skyboxEnvironment = cubemap, specularFilter = specularFilter)
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