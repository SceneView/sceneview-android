package io.github.sceneview.environment

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import io.github.sceneview.Filament
import io.github.sceneview.utils.fileBuffer
import io.github.sceneview.utils.localFileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer
import com.google.android.filament.utils.KTX1Loader as KTXLoader


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
 * @return [KTXLoader.createEnvironment]
 */
@JvmOverloads
suspend fun KTXLoader.loadEnvironment(
    context: Context,
    iblKtxFileLocation: String,
    skyboxKtxFileLocation: String? = null
): Environment {
    val iblBuffer = context.fileBuffer(iblKtxFileLocation)
    val skyboxBuffer = skyboxKtxFileLocation?.let { context.fileBuffer(it) }
    return withContext(Dispatchers.Main) {
        createEnvironment(iblBuffer, skyboxBuffer).also {
            iblBuffer?.clear()
            skyboxBuffer?.clear()
        }
    }
}

/**
 * ### Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * For Java compatibility usage.
 *
 * Kotlin developers should use [KTXLoader.loadEnvironment]
 *
 */
@JvmOverloads
fun KTXLoader.loadEnvironmentAsync(
    context: Context,
    lifecycle: Lifecycle,
    iblKtxFileLocation: String,
    skyboxKtxFileLocation: String? = null,
    result: (Environment?) -> Unit
) = lifecycle.coroutineScope.launchWhenCreated {
    result(loadEnvironment(context, iblKtxFileLocation, skyboxKtxFileLocation))
}


/**
 * ### Utility for producing environment resources from precompiled cmgen generated KTX files
 * locally
 *
 * @see KTXLoader.loadEnvironment
 */
fun KTXLoader.loadEnvironmentSync(
    context: Context,
    lifecycle: Lifecycle,
    iblKtxFileLocation: String,
    skyboxKtxFileLocation: String? = null
): Environment {
    val iblBuffer = context.localFileBuffer(iblKtxFileLocation)
    val skyboxBuffer = skyboxKtxFileLocation?.let { context.localFileBuffer(it) }
    return createEnvironment(iblBuffer, skyboxBuffer).also {
        iblBuffer?.clear()
        skyboxBuffer?.clear()
    }
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
    indirectLight = iblKtxBuffer?.let { createIndirectLight(it) },
    sphericalHarmonics = iblKtxBuffer?.rewind()?.let { getSphericalHarmonics(it) },
    skybox = skyboxKtxBuffer?.let { createSkybox(it) }
)

/**
 * ### Consumes the content of a KTX file and produces an [IndirectLight] object.
 *
 * @param buffer The content of the KTX File.
 * @param options Loader options.
 * @return The resulting Filament texture, or null on failure.
 */
fun KTXLoader.createIndirectLight(
    buffer: Buffer,
    options: KTXLoader.Options = KTXLoader.Options()
) = createIndirectLight(Filament.engine, buffer, options)

/**
 * ### Consumes the content of a KTX file and produces a [Skybox] object.
 *
 * @param buffer The content of the KTX File.
 * @param options Loader options.
 * @return The resulting Filament texture, or null on failure.
 */
fun KTXLoader.createSkybox(
    buffer: Buffer,
    options: KTXLoader.Options = KTXLoader.Options()
) = createSkybox(Filament.engine, buffer, options)