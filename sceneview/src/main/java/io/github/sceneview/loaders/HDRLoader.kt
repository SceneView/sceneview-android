package io.github.sceneview.loaders

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.utils.HDRLoader
import io.github.sceneview.SceneView
import io.github.sceneview.utils.IBLPrefilter
import io.github.sceneview.utils.loadFileBuffer
import kotlinx.coroutines.*

/**
 * Utility for decoding an HDR file and producing a Filament texture
 *
 * Consumes the content of an HDR file and produces a [Texture] object.
 *
 * @param fileLocation the HDR Image (.hdr) file location
 * @param options Loader options
 */
suspend fun HDRLoader.loadTexture(
    engine: Engine,
    context: Context,
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options()
): Texture? = context.loadFileBuffer(fileLocation)?.let { buffer ->
    withContext(Dispatchers.Main) {
        createTexture(engine, buffer, options)
    }
}

/**
 * Utility for decoding an HDR file and producing a Filament texture
 *
 * @param fileLocation the HDR Image (.hdr) file location
 * @param options Loader options
 *
 * @see HDRLoader.loadTexture
 */
suspend fun HDRLoader.loadTexture(
    sceneView: SceneView,
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options()
): Texture? = loadTexture(sceneView.engine, sceneView.context, fileLocation, options)?.also {
    sceneView.textures += it
}

/**
 * Utility for decoding an HDR file and producing a Filament texture
 *
 * @param fileLocation the HDR Image (.hdr) file location
 * @param options Loader options
 *
 * @see HDRLoader.loadTexture
 */
fun HDRLoader.loadTextureAsync(
    engine: Engine,
    context: Context,
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    onResult: (texture: Texture?) -> Unit
): Job = CoroutineScope(Dispatchers.IO).launch {
    loadTexture(engine, context, fileLocation, options).also(onResult)
}

/**
 * Utility for decoding an HDR file and producing a Filament texture
 *
 * @param fileLocation the HDR Image (.hdr) file location
 * @param options Loader options
 *
 * @see HDRLoader.loadTexture
 */
fun HDRLoader.loadTextureAsync(
    sceneView: SceneView,
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    onResult: (texture: Texture?) -> Unit
): Job = loadTextureAsync(sceneView.engine, sceneView.context, fileLocation, options, onResult)
    .also {
        sceneView.loadingJobs += it
    }

/**
 * Utility for decoding an HDR file and producing a Filament skybox texture
 *
 * Converts an equirectangular image to a cubemap
 *
 * @param hdrTexture Texture to convert to a cubemap.
 * - Can't be null.
 * - Must be a 2d texture
 * - Must have equirectangular geometry, that is width == 2*height.
 * - Must be allocated with all mip levels.
 * - Must be SAMPLEABLE
 *
 * @see HDRLoader.loadTexture
 */
fun HDRLoader.createSkyboxTexture(
    iblPrefilter: IBLPrefilter,
    hdrTexture: Texture
): Texture = iblPrefilter.equirectangularToCubemap(hdrTexture)

/**
 * Utility for decoding an HDR file and producing a Filament skybox texture
 *
 * Converts an equirectangular image to a cubemap
 *
 * @param hdrTexture Texture to convert to a cubemap.
 * - Can't be null.
 * - Must be a 2d texture
 * - Must have equirectangular geometry, that is width == 2*height.
 * - Must be allocated with all mip levels.
 * - Must be SAMPLEABLE
 *
 * @see HDRLoader.loadTexture
 */
fun HDRLoader.createSkyboxTexture(
    sceneView: SceneView,
    hdrTexture: Texture
): Texture = sceneView.iblPrefilter.equirectangularToCubemap(hdrTexture).also {
    sceneView.textures += it
}

/**
 * [IndirectLight] is used to simulate environment lighting, a form of global illumination
 *
 * @param skyboxTexture The reflections cubemap mipmap chain.
 * @param specularFilter Generates a prefiltered cubemap
 * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
 * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
 * @param apply Custom [IndirectLight.Builder] params
 *
 * @see HDRLoader.createSkyboxTexture
 */
fun HDRLoader.createIndirectLight(
    engine: Engine,
    iblPrefilter: IBLPrefilter,
    skyboxTexture: Texture,
    specularFilter: Boolean = true,
    apply: IndirectLight.Builder.() -> Unit = {}
): IndirectLight = IndirectLight.Builder()
    .reflections(skyboxTexture.apply {
        if (specularFilter) {
            iblPrefilter.specularFilter(this)
        }
    })
    .apply(apply)
    .build(engine)

/**
 * [IndirectLight] is used to simulate environment lighting, a form of global illumination
 *
 * @param skyboxTexture The reflections cubemap mipmap chain.
 * @param specularFilter Generates a prefiltered cubemap
 * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
 * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
 * @param apply Custom [IndirectLight.Builder] params
 *
 * @see HDRLoader.createSkyboxTexture
 * @see HDRLoader.createIndirectLight
 */
fun HDRLoader.createIndirectLight(
    sceneView: SceneView,
    skyboxTexture: Texture,
    specularFilter: Boolean = true,
    apply: IndirectLight.Builder.() -> Unit = {}
): IndirectLight = createIndirectLight(
    sceneView.engine,
    sceneView.iblPrefilter,
    skyboxTexture,
    specularFilter,
    apply
)

/**
 * [IndirectLight] is used to simulate environment lighting, a form of global illumination
 *
 * @param fileLocation The HDR Image (.hdr) file location
 * @param options Loader options
 * @param specularFilter Generates a prefiltered cubemap
 * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
 * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
 * @param apply Custom [IndirectLight.Builder] params
 *
 * @see HDRLoader.loadTexture
 * @see HDRLoader.createIndirectLight
 */
suspend fun HDRLoader.loadIndirectLight(
    sceneView: SceneView,
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    specularFilter: Boolean = true,
    apply: IndirectLight.Builder.() -> Unit = {}
): IndirectLight? = loadTexture(sceneView, fileLocation, options)?.let { hdrTexture ->
    val skyboxTexture = createSkyboxTexture(sceneView, hdrTexture)
    createIndirectLight(sceneView, skyboxTexture, specularFilter, apply)
}

/**
 * [IndirectLight] is used to simulate environment lighting, a form of global illumination
 *
 * @param fileLocation The HDR Image (.hdr) file location
 * @param options Loader options
 * @param specularFilter Generates a prefiltered cubemap
 * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
 * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
 * @param apply Custom [IndirectLight.Builder] params
 *
 * @see HDRLoader.loadTexture
 * @see HDRLoader.createIndirectLight
 */
suspend fun SceneView.loadHdrIndirectLight(
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    specularFilter: Boolean = true,
    apply: IndirectLight.Builder.() -> Unit = {}
): IndirectLight? =
    HDRLoader.loadIndirectLight(this, fileLocation, options, specularFilter, apply)?.also {
        indirectLight = it
    }

/**
 * [IndirectLight] is used to simulate environment lighting, a form of global illumination
 *
 * @param fileLocation The HDR Image (.hdr) file location
 * @param options Loader options
 * @param specularFilter Generates a prefiltered cubemap
 * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
 * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
 * @param apply Custom [IndirectLight.Builder] params
 *
 * @see HDRLoader.loadIndirectLight
 * @see HDRLoader.createIndirectLight
 */
fun HDRLoader.loadIndirectLightAsync(
    sceneView: SceneView,
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    specularFilter: Boolean = true,
    apply: IndirectLight.Builder.() -> Unit = {},
    onResult: (indirectLight: IndirectLight?) -> Unit
): Job = CoroutineScope(Dispatchers.IO).launch {
    loadIndirectLight(sceneView, fileLocation, options, specularFilter, apply).also(onResult)
}.also {
    sceneView.loadingJobs += it
}

/**
 * [IndirectLight] is used to simulate environment lighting, a form of global illumination
 *
 * @param fileLocation The HDR Image (.hdr) file location
 * @param options Loader options
 * @param specularFilter Generates a prefiltered cubemap
 * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
 * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
 * @param apply Custom [IndirectLight.Builder] params
 *
 * @see HDRLoader.loadTexture
 * @see HDRLoader.createIndirectLight
 */
fun SceneView.loadHdrIndirectLightAsync(
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    specularFilter: Boolean = true,
    apply: IndirectLight.Builder.() -> Unit = {},
    onResult: (indirectLight: IndirectLight?) -> Unit = {}
): Job =
    HDRLoader.loadIndirectLightAsync(this, fileLocation, options, specularFilter, apply) {
        indirectLight = it
        onResult(it)
    }

/**
 * The [Skybox] is rendered as though it were an infinitely large cube with the camera inside it
 *
 * @param skyboxTexture The environment map (i.e. the skybox content)
 * @param apply Custom [Skybox.Builder] params
 *
 * @see HDRLoader.loadTexture
 * @see HDRLoader.createSkyboxTexture
 */
fun HDRLoader.createSkybox(
    engine: Engine,
    skyboxTexture: Texture,
    apply: Skybox.Builder.() -> Unit = {}
): Skybox = Skybox.Builder()
    .environment(skyboxTexture)
    .apply(apply)
    .build(engine)

/**
 * The [Skybox] is rendered as though it were an infinitely large cube with the camera inside it
 *
 * @param skyboxTexture The environment map (i.e. the skybox content)
 * @param apply Custom [Skybox.Builder] params
 *
 * @see HDRLoader.loadTexture
 * @see HDRLoader.createSkyboxTexture
 */
fun HDRLoader.createSkybox(
    sceneView: SceneView,
    skyboxTexture: Texture,
    apply: Skybox.Builder.() -> Unit = {}
): Skybox = createSkybox(sceneView.engine, skyboxTexture, apply).also {
    sceneView.skyboxes += it
}

/**
 * The [Skybox] is rendered as though it were an infinitely large cube with the camera inside it
 *
 * @param fileLocation The HDR Image (.hdr) file location
 * @param options Loader options
 * @param apply Custom [Skybox.Builder] params
 *
 * @see HDRLoader.loadTexture
 */
suspend fun HDRLoader.loadSkybox(
    sceneView: SceneView,
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    apply: Skybox.Builder.() -> Unit = {}
): Skybox? = loadTexture(sceneView, fileLocation, options)?.let { hdrTexture ->
    val skyboxTexture = createSkyboxTexture(sceneView, hdrTexture)
    createSkybox(sceneView, skyboxTexture, apply)
}

/**
 * The [Skybox] is rendered as though it were an infinitely large cube with the camera inside it
 *
 * @param fileLocation The HDR Image (.hdr) file location
 * @param options Loader options
 * @param apply Custom [Skybox.Builder] params
 *
 * @see HDRLoader.loadTexture
 */
suspend fun SceneView.loadHdrSkybox(
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    apply: Skybox.Builder.() -> Unit = {}
): Skybox? = HDRLoader.loadSkybox(this, fileLocation, options, apply)?.also {
    skybox = it
}

/**
 * The [Skybox] is rendered as though it were an infinitely large cube with the camera inside it
 *
 * @param fileLocation The HDR Image (.hdr) file location
 * @param options Loader options
 * @param apply Custom [Skybox.Builder] params
 *
 * @see HDRLoader.loadTexture
 */
fun HDRLoader.loadSkyboxAsync(
    sceneView: SceneView,
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    apply: Skybox.Builder.() -> Unit = {},
    onResult: (skybox: Skybox?) -> Unit
): Job = CoroutineScope(Dispatchers.IO).launch {
    loadSkybox(sceneView, fileLocation, options, apply).also(onResult)
}.also {
    sceneView.loadingJobs += it
}

/**
 * The [Skybox] is rendered as though it were an infinitely large cube with the camera inside it
 *
 * @param fileLocation The HDR Image (.hdr) file location
 * @param options Loader options
 * @param apply Custom [Skybox.Builder] params
 *
 * @see HDRLoader.loadTexture
 */
fun SceneView.loadHdrSkyboxAsync(
    fileLocation: String,
    options: HDRLoader.Options = HDRLoader.Options(),
    apply: Skybox.Builder.() -> Unit = {},
    onResult: (skybox: Skybox?) -> Unit = {}
): Job = HDRLoader.loadSkyboxAsync(this, fileLocation, options, apply) {
    skybox = it
    onResult(it)
}