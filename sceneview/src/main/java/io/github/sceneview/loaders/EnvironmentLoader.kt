package io.github.sceneview.loaders

import android.content.Context
import androidx.annotation.RawRes
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.IBLPrefilterContext
import com.google.android.filament.utils.KTX1Loader
import io.github.sceneview.safeDestroyIndirectLight
import io.github.sceneview.safeDestroySkybox
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.texture.use
import io.github.sceneview.utils.loadFileBuffer
import io.github.sceneview.utils.readBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.io.File
import java.nio.Buffer

/**
 * Utility for decoding an HDR file or consuming KTX1 files and producing Filament textures, IBLs,
 * and sky boxes.
 *
 * KTX is a simple container format that makes it easy to bundle miplevels and cubemap faces into a
 * single file.
 *
 * Consuming the content of an HDR file and produces a [Texture] object to enerates a prefiltered
 * indirect light cubemap with specular filter is a GPU based implementation of the specular
 * probe pre-integration filter.
 * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
 */
class EnvironmentLoader(
    val engine: Engine,
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    val iblPrefilter = IBLPrefilter(engine)

    private val environments = mutableListOf<Environment>()

    fun createEnvironment(
        indirectLight: IndirectLight? = null,
        skybox: Skybox? = null,
        sphericalHarmonics: FloatArray? = null
    ) = Environment(
        indirectLight = indirectLight,
        skybox = skybox,
        sphericalHarmonics = sphericalHarmonics?.toList()
    ).also {
        environments += it
    }

    /**
     * Utility for decoding and producing environment resources from an HDR file.
     *
     * Consumes the content of an HDR file and produces an [IndirectLight] and a [Skybox].
     *
     * @param buffer The content of the HDR File.
     * @param indirectLightSpecularFilter Generates a prefiltered indirect light cubemap.
     * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
     * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
     * @param textureOptions texture loader options
     * @param createSkybox Disable the skybox creation if you don't need it.
     *
     * @return the generated environment indirect light and skybox from the hdr.
     *
     * @see HDRLoader.createTexture
     */
    fun createHDREnvironment(
        buffer: Buffer,
        indirectLightSpecularFilter: Boolean = true,
        indirectLightApply: IndirectLight.Builder.() -> Unit = {},
        textureOptions: HDRLoader.Options = HDRLoader.Options(),
        createSkybox: Boolean = true,
    ): Environment? {
        // Since we directly destroy the texture we call the `use` function and so don't pass lifecycle
        // to createTexture because it can be destroyed immediately.
        val textureCubemap = HDRLoader.createTexture(
            engine = engine,
            buffer = buffer,
            options = textureOptions
        )?.use(engine) { hdrTexture ->
            iblPrefilter.equirectangularToCubemap(equirect = hdrTexture)
        } ?: return null

        val reflections = if (indirectLightSpecularFilter) {
            iblPrefilter.specularFilter(textureCubemap).also {
                if (!createSkybox) {
                    engine.safeDestroyTexture(textureCubemap)
                }
            }
        } else {
            textureCubemap
        }
        val indirectLight = IndirectLight.Builder()
            .reflections(reflections)
            .apply(indirectLightApply)
            .build(engine)

        val skybox = textureCubemap.takeIf { createSkybox }?.let {
            Skybox.Builder()
                .environment(it)
                .build(engine)
        }
        return createEnvironment(indirectLight = indirectLight, skybox = skybox)
    }

    /**
     * Utility for decoding and producing environment resources from an HDR file.
     *
     * Consumes the content of an HDR file and produces an [IndirectLight] and a [Skybox].
     *
     * @param assetFileLocation The HDR asset file location.
     * @param indirectLightSpecularFilter Generates a prefiltered indirect light cubemap.
     * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
     * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
     * @param textureOptions texture loader options
     * @param createSkybox Disable the skybox creation if you don't need it.
     *
     * @return the generated environment indirect light and skybox from the hdr.
     *
     * @see HDRLoader.createTexture
     */
    fun createHDREnvironment(
        assetFileLocation: String,
        indirectLightSpecularFilter: Boolean = true,
        textureOptions: HDRLoader.Options = HDRLoader.Options(),
        createSkybox: Boolean = true,
    ): Environment? = createHDREnvironment(
        buffer = context.assets.readBuffer(assetFileLocation),
        indirectLightSpecularFilter = indirectLightSpecularFilter,
        textureOptions = textureOptions,
        createSkybox = createSkybox
    )

    /**
     * Utility for decoding and producing environment resources from an HDR file.
     *
     * Consumes the content of an HDR file and produces an [IndirectLight] and a [Skybox].
     *
     * @param rawResId The HDR File raw resource id.
     * @param indirectLightSpecularFilter Generates a prefiltered indirect light cubemap.
     * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
     * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
     * @param textureOptions texture loader options
     * @param createSkybox Disable the skybox creation if you don't need it.
     *
     * @return the generated environment indirect light and skybox from the hdr.
     *
     * @see HDRLoader.createTexture
     */
    fun createHDREnvironment(
        @RawRes rawResId: Int,
        indirectLightSpecularFilter: Boolean = true,
        textureOptions: HDRLoader.Options = HDRLoader.Options(),
        createSkybox: Boolean = true,
    ): Environment? = createHDREnvironment(
        buffer = context.resources.readBuffer(rawResId),
        indirectLightSpecularFilter = indirectLightSpecularFilter,
        textureOptions = textureOptions,
        createSkybox = createSkybox
    )

    /**
     * Utility for decoding and producing environment resources from an HDR file.
     *
     * Consumes the content of an HDR file and produces an [IndirectLight] and a [Skybox].
     *
     * @param file The HDR File.
     * @param indirectLightSpecularFilter Generates a prefiltered indirect light cubemap.
     * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
     * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
     * @param textureOptions texture loader options
     * @param createSkybox Disable the skybox creation if you don't need it.
     *
     * @return the generated environment indirect light and skybox from the hdr.
     *
     * @see HDRLoader.createTexture
     */
    fun createHDREnvironment(
        file: File,
        indirectLightSpecularFilter: Boolean = true,
        textureOptions: HDRLoader.Options = HDRLoader.Options(),
        createSkybox: Boolean = true,
    ): Environment? = createHDREnvironment(
        buffer = file.readBuffer(),
        indirectLightSpecularFilter = indirectLightSpecularFilter,
        textureOptions = textureOptions,
        createSkybox = createSkybox
    )

    /**
     * Utility for decoding and producing environment resources from an HDR file.
     *
     * Consumes the content of an HDR file and produces an [IndirectLight] and a [Skybox].
     *
     * @param url The HDR File url.
     * @param indirectLightSpecularFilter Generates a prefiltered indirect light cubemap.
     * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
     * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
     * @param textureOptions texture loader options
     * @param createSkybox Disable the skybox creation if you don't need it.
     *
     * @return the generated environment indirect light and skybox from the hdr.
     *
     * @see HDRLoader.createTexture
     */
    suspend fun loadHDREnvironment(
        url: String,
        indirectLightSpecularFilter: Boolean = true,
        textureOptions: HDRLoader.Options = HDRLoader.Options(),
        createSkybox: Boolean = true,
    ): Environment? = context.loadFileBuffer(url)?.let { buffer ->
        createHDREnvironment(
            buffer = buffer,
            indirectLightSpecularFilter = indirectLightSpecularFilter,
            textureOptions = textureOptions,
            createSkybox = createSkybox
        )
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
     * @param iblBuffer The content of the ibl KTX File.
     * @param skyboxBuffer The content of the skybox KTX File.
     *
     * @return the generated environment indirect light, sphericalHarmonics and skybox from the ktxs.
     */
    fun createKTX1Environment(
        iblBuffer: Buffer? = null,
        skyboxBuffer: Buffer? = null
    ) = createEnvironment(
        indirectLight = iblBuffer?.let { KTX1Loader.createIndirectLight(engine, it) },
        skybox = skyboxBuffer?.let { KTX1Loader.createSkybox(engine, it) },
        sphericalHarmonics = iblBuffer?.rewind()?.let { KTX1Loader.getSphericalHarmonics(it) }
    )

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
     * @param iblAssetFile The ibl KTX asset file location.
     * @param skyboxAssetFile The skybox KTX asset file location.
     *
     * @return the generated environment indirect light, sphericalHarmonics and skybox from the ktxs.
     */
    fun createKTX1Environment(
        iblAssetFile: String? = null,
        skyboxAssetFile: String? = null
    ): Environment = createKTX1Environment(
        iblBuffer = iblAssetFile?.let { context.assets.readBuffer(it) },
        skyboxBuffer = skyboxAssetFile?.let { context.assets.readBuffer(it) },
    )

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
     * @param iblRawResId The ibl KTX file raw resource id.
     * @param skyboxRawResId The skybox KTX file raw resource id.
     *
     * @return the generated environment indirect light, sphericalHarmonics and skybox from the ktxs.
     */
    fun createKTX1Environment(
        iblRawResId: Int? = null,
        skyboxRawResId: Int? = null
    ): Environment = createKTX1Environment(
        iblBuffer = iblRawResId?.let { context.resources.readBuffer(it) },
        skyboxBuffer = skyboxRawResId?.let { context.resources.readBuffer(it) },
    )

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
     * @param iblFile The ibl KTX File.
     * @param skyboxFile The skybox KTX File.
     *
     * @return the generated environment indirect light, sphericalHarmonics and skybox from the ktxs.
     */
    fun createKTX1Environment(
        iblFile: File? = null,
        skyboxFile: File? = null
    ): Environment = createKTX1Environment(
        iblBuffer = iblFile?.let { it.readBuffer() },
        skyboxBuffer = skyboxFile?.let { it.readBuffer() },
    )

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
     * @param iblUrl The ibl KTX file url.
     * @param skyboxUrl The skybox KTX file url.
     *
     * @return the generated environment indirect light, sphericalHarmonics and skybox from the ktxs.
     */
    suspend fun loadKTX1Environment(
        iblUrl: String? = null,
        skyboxUrl: String? = null
    ): Environment = createKTX1Environment(
        iblBuffer = iblUrl?.let { context.loadFileBuffer(iblUrl) },
        skyboxBuffer = skyboxUrl?.let { context.loadFileBuffer(skyboxUrl) }
    )

    /**
     * Utility for decoding and producing environment resources from an HDR file.
     *
     * Consumes the content of an HDR file and produces an [IndirectLight] and a [Skybox].
     *
     * @param url The HDR File url.
     * @param indirectLightSpecularFilter Generates a prefiltered indirect light cubemap.
     * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
     * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
     * @param textureOptions texture loader options
     * @param createSkybox Disable the skybox creation if you don't need it.
     *
     * @return the generated environment indirect light and skybox from the hdr.
     *
     * @see HDRLoader.createTexture
     */
    suspend fun loadKTX1Environment(
        url: String,
        indirectLightSpecularFilter: Boolean = true,
        textureOptions: HDRLoader.Options = HDRLoader.Options(),
        createSkybox: Boolean = true,
    ): Environment? = context.loadFileBuffer(url)?.let { buffer ->
        createHDREnvironment(
            buffer = buffer,
            indirectLightSpecularFilter = indirectLightSpecularFilter,
            textureOptions = textureOptions,
            createSkybox = createSkybox
        )
    }

    fun destroyEnvironment(environment: Environment) {
        environment.indirectLight?.let { engine.safeDestroyIndirectLight(it) }
        environment.skybox?.let { engine.safeDestroySkybox(it) }
        environments -= environment
    }

    fun clear() {
        runCatching { coroutineScope.cancel() }
        environments.toList().forEach { destroyEnvironment(it) }
        environments.clear()
    }

    fun destroy() {
        clear()

        iblPrefilter.destroy()
    }
}


/**
 *
 * Indirect light and skybox environment for a scene
 *
 * Environments are usually captured as high-resolution HDR equirectangular images and processed by
 * the cmgen tool to generate the data needed by IndirectLight.
 *
 * You can also process an hdr at runtime but this is more consuming.
 *
 * - Currently IndirectLight is intended to be used for "distant probes", that is, to represent
 * global illumination from a distant (i.e. at infinity) environment, such as the sky or distant
 * mountains.
 * Only a single IndirectLight can be used in a Scene. This limitation will be lifted in the future.
 *
 * - When added to a Scene, the Skybox fills all untouched pixels.
 *
 * Defines the lighting environment and the skybox for the scene
 *
 *
 * @property indirectLight ### IndirectLight is used to simulate environment lighting.
 * Environment lighting has a two components:
 * - irradiance
 * - reflections (specular component)
 *
 * @property sphericalHarmonics ### Array of 9 * 3 floats
 *
 * @property skybox ### The Skybox is drawn last and covers all pixels not touched by geometry.
 * The Skybox to use to fill untouched pixels, or null to unset the Skybox.
 *
 * @see [IndirectLight]
 * @see [EnvironmentLoader]
 * @see [HDRLoader.loadEnvironment]
 */
data class Environment(
    val indirectLight: IndirectLight? = null,
    val skybox: Skybox? = null,
    val sphericalHarmonics: List<Float>? = null
)

/**
 * IBLPrefilter creates and initializes GPU state common to all environment map filters.
 * Typically, only one instance per filament Engine of this object needs to exist.
 *
 * @see [IBLPrefilterContext]
 */
class IBLPrefilter(engine: Engine) {

    /**
     * Created IBLPrefilterContext, keeping it around if several cubemap will be processed.
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
     * Converts an equirectangular image to a cubemap.
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
     * Generates a prefiltered cubemap.
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

//open class HDREnvironment(
//    engine: Engine,
//    iblPrefilter: IBLPrefilter,
//    cubemap: Texture? = null,
//    indirectLightIrradiance: FloatArray? = null,
//    indirectLightIntensity: Float? = null,
//    /**
//     * Generates a prefiltered indirect light cubemap.
//     *
//     * SpecularFilter is a GPU based implementation of the specular probe pre-integration filter.
//     * ** Launch the heaver computation. Expect 100-100ms on the GPU.**
//     */
//    indirectLightSpecularFilter: Boolean = defaultSpecularFilter,
//    createSkybox: Boolean = defaultCreateSkybox,
//    val sharedCubemap: Boolean = false
//) : Environment(
//    indirectLight = IndirectLight.Builder().apply {
//        cubemap?.let { cubemap ->
//            reflections(
//                if (indirectLightSpecularFilter) {
//                    iblPrefilter.specularFilter(cubemap).also {
//                        if (!createSkybox) {
//                            engine.safeDestroyTexture(cubemap)
//                        }
//                    }
//                } else {
//                    cubemap
//                }
//            )
//        }
//        indirectLightIrradiance?.let {
//            irradiance(3, it)
//        }
//        indirectLightIntensity?.let {
//            intensity(it)
//        }
//    }.build(engine),
//    sphericalHarmonics = indirectLightIrradiance,
//    skybox = cubemap?.takeIf { createSkybox }?.let {
//        Skybox.Builder()
//            .environment(it)
//            .build(engine)
//    }
//) {
//    var cubemap: Texture? = cubemap
//        internal set
//    var indirectLightIntensity: Float? = indirectLightIntensity
//        private set
//
//    /**
//     * ### Destroys the EnvironmentLights and frees all its associated resources.
//     */
//    override fun destroy() {
//        super.destroy()
//
//        if (!sharedCubemap) {
//            // Use runCatching because it could already be destroyed at Environment creation time
//            // in case of no Skybox needed.
//            cubemap?.let { runCatching { engine.destroyTexture(it) } }
//            cubemap = null
//        }
//
//        indirectLightIntensity = null
//    }
//}