package io.github.sceneview.environment

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.utils.KTXLoader
import io.github.sceneview.Filament
import io.github.sceneview.utils.fileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer


/**
 * ### Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * [Documentation][KTXLoader.createEnvironment]
 *
 * @param iblKtxFileLocation the ibl file location
 * [Documentation][fileBuffer]
 * @param skyboxKtxFileLocation the skybox file location
 * [Documentation][fileBuffer]
 *
 * @return [Documentation][KTXLoader.createEnvironment]
 */
@JvmOverloads
suspend fun KTXLoader.loadEnvironment(
    context: Context,
    iblKtxFileLocation: String,
    skyboxKtxFileLocation: String? = null
): Environment? {
    return try {
        val ibl = context.fileBuffer(iblKtxFileLocation)
        val skybox = skyboxKtxFileLocation?.let { context.fileBuffer(it) }
        withContext(Dispatchers.Main) {
            createEnvironment(ibl, skybox)
        }
    } finally {
        // TODO: See why the finally is called before the onDestroy()
//        environment?.destroy()
    }
}

/**
 * ### Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * For Java compatibility usage.
 *
 * Kotlin developers should use [KTXLoader.loadEnvironment]
 *
 * [Documentation][KTXLoader.loadEnvironment]
 *
 */
@JvmOverloads
fun KTXLoader.loadEnvironmentAsync(
    context: Context,
    iblKtxFileLocation: String,
    skyboxKtxFileLocation: String? = null,
    coroutineScope: LifecycleCoroutineScope,
    result: (Environment?) -> Unit
) = coroutineScope.launchWhenCreated {
    result(loadEnvironment(context, iblKtxFileLocation, skyboxKtxFileLocation))
}

/**
 * ### Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * Consumes the content of KTX files and produces an [IndirectLight], SphericalHarmonics and a
 * [Skybox]
 *
 * You can generate ktx ibl and skybox files using:
 *
 * `cmgen --deploy ./output --format=ktx --size=256 --extract-blur=0.1 environment.hdr`
 *
 * Documentation: [Filament - Bake environment map](https://github.com/google/filament/blob/main/web/docs/tutorial_redball.md#bake-environment-map)
 *
 * @param iblKtxBuffer The content of the ibl KTX File.
 * @param skyboxKtxBuffer The content of the skybox KTX File.
 *
 * @return the generated environment indirect light, sphericalHarmonics and skybox from the ktxs.
 *
 * @see KTXLoader.createIndirectLight
 * @see KTXLoader.getSphericalHarmonics
 * @see KTXLoader.createSkybox
 */
@JvmOverloads
fun KTXLoader.createEnvironment(
    iblKtxBuffer: Buffer?,
    skyboxKtxBuffer: Buffer? = null
) = KTXEnvironment(
    indirectLight = iblKtxBuffer?.let { createIndirectLight(Filament.engine, it) },
    sphericalHarmonics = iblKtxBuffer?.rewind()?.let { getSphericalHarmonics(it) },
    skybox = skyboxKtxBuffer?.let { createSkybox(Filament.engine, it) })