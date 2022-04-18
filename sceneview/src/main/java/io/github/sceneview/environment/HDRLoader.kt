package io.github.sceneview.environment

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.utils.HDRLoader
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament
import io.github.sceneview.texture.destroy
import io.github.sceneview.texture.use
import io.github.sceneview.utils.fileBuffer
import io.github.sceneview.utils.useFileBufferNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer

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
    lifecycle: Lifecycle,
    hdrFileLocation: String,
    specularFilter: Boolean = defaultSpecularFilter,
    createSkybox: Boolean = defaultCreateSkybox
): HDREnvironment? = context.useFileBufferNotNull(hdrFileLocation) { buffer ->
    withContext(Dispatchers.Main) {
        createEnvironment(lifecycle, buffer, specularFilter, createSkybox)
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
    lifecycle: Lifecycle,
    hdrFileLocation: String,
    specularFilter: Boolean = defaultSpecularFilter,
    createSkybox: Boolean = defaultCreateSkybox,
    result: (HDREnvironment?) -> Unit
) = lifecycle.coroutineScope.launchWhenCreated {
    result(
        HDRLoader.loadEnvironment(
            context = context,
            lifecycle = lifecycle,
            hdrFileLocation = hdrFileLocation,
            specularFilter = specularFilter,
            createSkybox = createSkybox
        )
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
    lifecycle: Lifecycle,
    hdrBuffer: Buffer,
    specularFilter: Boolean = defaultSpecularFilter,
    createSkybox: Boolean = defaultCreateSkybox
) = // Since we directly destroy the texture we call the `use` function and so don't pass lifecycle
    // to createTexture because it can be destroyed immediately.
    createTexture(hdrBuffer)?.use { hdrTexture ->
        Filament.iblPrefilter.equirectangularToCubemap(hdrTexture)
    }?.let { cubemap ->
        HDREnvironment(
            lifecycle = lifecycle,
            cubemap = cubemap,
            indirectLightSpecularFilter = specularFilter,
            createSkybox = createSkybox
        )
    }

/**
 * ### Consumes the content of an HDR file and produces a [Texture] object.
 *
 * @param buffer The content of the HDR File.
 * @param options Loader options.
 * @return The resulting Filament texture, or null on failure.
 */
fun HDRLoader.createTexture(
    buffer: Buffer,
    options: HDRLoader.Options = HDRLoader.Options(),
    lifecycle: Lifecycle? = null
): Texture? = createTexture(Filament.engine, buffer, options).also { texture ->
    lifecycle?.observe(onDestroy = {
        // Prevent double destroy in case of manually destroyed
        runCatching { texture?.destroy() }
    })
}