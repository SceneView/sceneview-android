package io.github.sceneview.loaders

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.utils.HDRLoader
import io.github.sceneview.environment.HDREnvironment
import io.github.sceneview.environment.IBLPrefilter
import io.github.sceneview.environment.defaultCreateSkybox
import io.github.sceneview.environment.defaultSpecularFilter
import io.github.sceneview.texture.use
import io.github.sceneview.utils.loadFileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer

/**
 * Utility for decoding and producing environment resources from an HDR file.
 */
@JvmOverloads
suspend fun HDRLoader.loadEnvironment(
    engine: Engine,
    iblPrefilter: IBLPrefilter,
    context: Context,
    hdrFileLocation: String,
    specularFilter: Boolean = defaultSpecularFilter,
    createSkybox: Boolean = defaultCreateSkybox
): HDREnvironment? = context.loadFileBuffer(hdrFileLocation)?.let { buffer ->
    withContext(Dispatchers.Main) {
        createEnvironment(engine, iblPrefilter, buffer, specularFilter, createSkybox)
    }
}

/**
 * Utility for decoding and producing environment resources from an HDR file.
 */
fun HDRLoader.loadEnvironmentAsync(
    engine: Engine,
    iblPrefilter: IBLPrefilter,
    context: Context,
    coroutineScope: LifecycleCoroutineScope,
    hdrFileLocation: String,
    specularFilter: Boolean = defaultSpecularFilter,
    createSkybox: Boolean = defaultCreateSkybox,
    result: (HDREnvironment?) -> Unit
) = coroutineScope.launchWhenCreated {
    result(
        HDRLoader.loadEnvironment(
            engine = engine,
            iblPrefilter = iblPrefilter,
            context = context,
            hdrFileLocation = hdrFileLocation,
            specularFilter = specularFilter,
            createSkybox = createSkybox
        )
    )
}

/**
 * Utility for decoding and producing environment resources from an HDR file.
 *
 * Consumes the content of an HDR file and produces an [IndirectLight] and a [Skybox].
 *
 * @param hdrBuffer The content of the HDR File.
 * @param iblPrefilter A filter to apply to the resulting indirect light reflexions texture.
 * Default generates a specular prefiltered cubemap reflection texture.
 *
 * @return the generated environment indirect light and skybox from the hdr.
 *
 * @see HDRLoader.createTexture
 */
@JvmOverloads
fun HDRLoader.createEnvironment(
    engine: Engine,
    iblPrefilter: IBLPrefilter,
    hdrBuffer: Buffer,
    specularFilter: Boolean = defaultSpecularFilter,
    createSkybox: Boolean = defaultCreateSkybox
) = // Since we directly destroy the texture we call the `use` function and so don't pass lifecycle
    // to createTexture because it can be destroyed immediately.
    createTexture(engine, hdrBuffer)?.use(engine) { hdrTexture ->
        iblPrefilter.equirectangularToCubemap(hdrTexture)
    }?.let { cubemap ->
        HDREnvironment(
            engine = engine,
            iblPrefilter = iblPrefilter,
            cubemap = cubemap,
            indirectLightSpecularFilter = specularFilter,
            createSkybox = createSkybox
        )
    }

/**
 * Consumes the content of an HDR file and produces a [Texture] object.
 *
 * @param buffer The content of the HDR File.
 * @param options Loader options.
 * @return The resulting Filament texture, or null on failure.
 */
fun HDRLoader.createTexture(
    engine: Engine,
    buffer: Buffer,
    options: HDRLoader.Options = HDRLoader.Options()
): Texture? = createTexture(engine, buffer, options)