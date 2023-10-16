package io.github.sceneview.loaders

import android.content.Context
import android.content.res.AssetManager
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import io.github.sceneview.environment.Environment
import io.github.sceneview.environment.KTXEnvironment
import io.github.sceneview.utils.loadFileBuffer
import io.github.sceneview.utils.readFileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer
import com.google.android.filament.utils.KTX1Loader as KTXLoader


/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files.
 */
@JvmOverloads
suspend fun KTXLoader.loadEnvironment(
    engine: Engine,
    context: Context,
    iblKtxFileLocation: String,
    skyboxKtxFileLocation: String? = null
): Environment {
    val iblBuffer = context.loadFileBuffer(iblKtxFileLocation)
    val skyboxBuffer = skyboxKtxFileLocation?.let { context.loadFileBuffer(it) }
    return withContext(Dispatchers.Main) {
        createEnvironment(engine, iblBuffer, skyboxBuffer).also {
            iblBuffer?.clear()
            skyboxBuffer?.clear()
        }
    }
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files locally.
 */
fun KTXLoader.createEnvironment(
    engine: Engine,
    assets: AssetManager,
    iblKtxFileLocation: String,
    skyboxKtxFileLocation: String? = null
): Environment {
    val iblBuffer = assets.readFileBuffer(iblKtxFileLocation)
    val skyboxBuffer = skyboxKtxFileLocation?.let { assets.readFileBuffer(it) }
    return createEnvironment(engine, iblBuffer, skyboxBuffer).also {
        iblBuffer.clear()
        skyboxBuffer?.clear()
    }
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files.
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
    engine: Engine,
    iblKtxBuffer: Buffer?,
    skyboxKtxBuffer: Buffer? = null
) = KTXEnvironment(
    engine = engine,
    indirectLight = iblKtxBuffer?.let { createIndirectLight(engine, it) },
    sphericalHarmonics = iblKtxBuffer?.rewind()?.let { getSphericalHarmonics(it) },
    skybox = skyboxKtxBuffer?.let { createSkybox(engine, it) }
)

/**
 * Consumes the content of a KTX file and produces an [IndirectLight] object.
 *
 * @param buffer The content of the KTX File.
 * @param options Loader options.
 * @return The resulting Filament texture, or null on failure.
 */
fun KTXLoader.createIndirectLight(
    engine: Engine,
    buffer: Buffer,
    options: KTXLoader.Options = KTXLoader.Options()
) = createIndirectLight(engine, buffer, options)

/**
 * Consumes the content of a KTX file and produces a [Skybox] object.
 *
 * @param buffer The content of the KTX File.
 * @param options Loader options.
 * @return The resulting Filament texture, or null on failure.
 */
fun KTXLoader.createSkybox(
    engine: Engine,
    buffer: Buffer,
    options: KTXLoader.Options = KTXLoader.Options()
) = createSkybox(engine, buffer, options)