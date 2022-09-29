package io.github.sceneview.loaders

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.utils.KTX1Loader
import io.github.sceneview.SceneView
import io.github.sceneview.utils.loadFileBuffer
import kotlinx.coroutines.*

/**
 * Indirect Based Light
 *
 * @property indirectLight Filament texture, or null on failure.
 * @property sphericalHarmonics Spherical harmonics from the content of a KTX file.
 * The resulting array of 9 * 3 floats, or null on failure.
 */
class IBL(val indirectLight: IndirectLight, val sphericalHarmonics: FloatArray?)

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * Consumes the content of KTX files and produces an [IBL] containing an [IndirectLight] and
 * Spherical Harmonics.
 *
 * You can generate ktx ibl and skybox files using:
 *
 * `cmgen --deploy ./output --format=ktx --size=256 --extract-blur=0.1 environment.hdr`
 *
 * Documentation: [Filament - Bake environment map](https://github.com/google/filament/blob/main/web/docs/tutorial_redball.md#bake-environment-map)
 *
 * @param fileLocation the KTX Image Bases Light (_ibl.ktx) file location
 * @param options Loader options
 *
 * @return The resulting Filament texture and spherical harmonics from the content of the KTX file
 * or null on failure
 */
suspend fun KTX1Loader.loadIndirectLight(
    engine: Engine,
    context: Context,
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options()
): IBL? = context.loadFileBuffer(fileLocation)?.let { buffer ->
    withContext(Dispatchers.Main) {
        IBL(
            indirectLight = createIndirectLight(engine, buffer, options),
            sphericalHarmonics = getSphericalHarmonics(buffer)
        )
    }
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Image Bases Light (_ibl.ktx) file location
 * @param options Loader options
 *
 * @return The resulting Filament texture and spherical harmonics from the content of the KTX file
 * or null on failure
 *
 * @see KTX1Loader.loadIndirectLight
 */
suspend fun KTX1Loader.loadIndirectLight(
    sceneView: SceneView,
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
): IBL? = loadIndirectLight(sceneView.engine, sceneView.context, fileLocation, options)
    ?.also {
        sceneView.indirectLights += it.indirectLight
    }

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Image Bases Light (_ibl.ktx) file location
 * @param options Loader options
 *
 * @return The resulting Filament texture and spherical harmonics from the content of the KTX file
 * or null on failure
 *
 * @see KTX1Loader.loadIndirectLight
 */
suspend fun SceneView.loadKtxIndirectLight(
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
): IBL? = KTX1Loader.loadIndirectLight(this, fileLocation, options)
    ?.also {
        indirectLight = it.indirectLight
    }

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Image Bases Light (_ibl.ktx) file location
 * @param options Loader options
 * @param onResult The resulting Filament texture and spherical harmonics from the content of the
 * KTX file or null on failure
 *
 * @see KTX1Loader.loadIndirectLight
 */
fun KTX1Loader.loadIndirectLightAsync(
    engine: Engine,
    context: Context,
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
    onResult: (ibl: IBL?) -> Unit
): Job = CoroutineScope(Dispatchers.IO).launch {
    loadIndirectLight(engine, context, fileLocation, options).also(onResult)
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Image Bases Light (_ibl.ktx) file location
 * @param options Loader options
 * @param onResult The resulting Filament texture and spherical harmonics from the content of the
 * KTX file or null on failure
 *
 * @see KTX1Loader.loadIndirectLight
 */
fun KTX1Loader.loadIndirectLightAsync(
    sceneView: SceneView,
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
    onResult: (ibl: IBL?) -> Unit
): Job =
    loadIndirectLightAsync(sceneView.engine, sceneView.context, fileLocation, options, onResult)
        .also {
            sceneView.loadingJobs += it
        }

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Image Bases Light (_ibl.ktx) file location
 * @param options Loader options
 * @param onResult The resulting Filament texture and spherical harmonics from the content of the
 * KTX file or null on failure
 *
 * @see KTX1Loader.loadIndirectLight
 */
fun SceneView.loadKtxIndirectLightAsync(
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
    onResult: ((ibl: IBL?) -> Unit)? = null
): Job = KTX1Loader.loadIndirectLightAsync(this, fileLocation, options) { ibl ->
    indirectLight = ibl?.indirectLight
    sphericalHarmonics = ibl?.sphericalHarmonics
    onResult?.invoke(ibl)
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * Consumes the content of KTX files and produces a [Skybox]
 *
 * You can generate ktx ibl and skybox files using:
 *
 * `cmgen --deploy ./output --format=ktx --size=256 --extract-blur=0.1 environment.hdr`
 *
 * Documentation: [Filament - Bake environment map](https://github.com/google/filament/blob/main/web/docs/tutorial_redball.md#bake-environment-map)
 *
 * @param fileLocation the KTX Skybox (_skybox.ktx) file location
 * @param options Loader options
 */
suspend fun KTX1Loader.loadSkybox(
    engine: Engine,
    context: Context,
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options()
): Skybox? = context.loadFileBuffer(fileLocation)?.let { buffer ->
    withContext(Dispatchers.Main) {
        createSkybox(engine, buffer, options)
    }
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Skybox (_skybox.ktx) file location
 * @param options Loader options
 *
 * @see KTX1Loader.loadSkybox
 */
suspend fun KTX1Loader.loadSkybox(
    sceneView: SceneView,
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
): Skybox? = loadSkybox(sceneView.engine, sceneView.context, fileLocation, options)?.also {
    sceneView.skyboxes += it
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Skybox (_skybox.ktx) file location
 * @param options Loader options
 *
 * @see KTX1Loader.loadSkybox
 */
suspend fun SceneView.loadKtxSkybox(
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
): Skybox? = KTX1Loader.loadSkybox(this, fileLocation, options)?.also {
    skybox = it
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Skybox (_skybox.ktx) file location
 * @param options Loader options
 *
 * @see KTX1Loader.loadSkybox
 */
fun KTX1Loader.loadSkyboxAsync(
    engine: Engine,
    context: Context,
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
    onResult: (skybox: Skybox?) -> Unit
): Job = CoroutineScope(Dispatchers.IO).launch {
    loadSkybox(engine, context, fileLocation, options).also(onResult)
}

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Skybox (_skybox.ktx) file location
 * @param options Loader options
 *
 * @see KTX1Loader.loadSkybox
 */
fun KTX1Loader.loadSkyboxAsync(
    sceneView: SceneView,
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
    onResult: (skybox: Skybox?) -> Unit
): Job = loadSkyboxAsync(sceneView.engine, sceneView.context, fileLocation, options, onResult)
    .also {
        sceneView.loadingJobs += it
    }

/**
 * Utility for producing environment resources from precompiled cmgen generated KTX files
 *
 * @param fileLocation the KTX Skybox (_skybox.ktx) file location
 * @param options Loader options
 *
 * @see KTX1Loader.loadSkybox
 */
fun SceneView.loadKtxSkyboxAsync(
    fileLocation: String,
    options: KTX1Loader.Options = KTX1Loader.Options(),
    onResult: (skybox: Skybox?) -> Unit = {}
): Job = KTX1Loader.loadSkyboxAsync(this, fileLocation, options) {
    skybox = it
    onResult(it)
}