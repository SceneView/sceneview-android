package io.github.sceneview.loaders

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.annotation.DrawableRes
import com.google.android.filament.Engine
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.utils.TextureType
import io.github.sceneview.material.ImageMaterial
import io.github.sceneview.material.VideoMaterial
import io.github.sceneview.material.kMaterialDefaultMetallic
import io.github.sceneview.material.kMaterialDefaultReflectance
import io.github.sceneview.material.kMaterialDefaultRoughness
import io.github.sceneview.material.setColor
import io.github.sceneview.material.setMetallic
import io.github.sceneview.material.setParameter
import io.github.sceneview.material.setReflectance
import io.github.sceneview.material.setRoughness
import io.github.sceneview.material.setTexture
import io.github.sceneview.math.Color
import io.github.sceneview.math.colorOf
import io.github.sceneview.safeDestroyMaterial
import io.github.sceneview.safeDestroyMaterialInstance
import io.github.sceneview.texture.ImageTexture
import io.github.sceneview.utils.loadFileBuffer
import io.github.sceneview.utils.readFileBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.Buffer

private const val kMaterialsAssetFolder = "sceneview/materials"

/**
 * A Filament Material defines the visual appearance of an object.
 *
 * Materials function as a templates from which [MaterialInstance]s can be spawned.
 */
class MaterialLoader(
    val engine: Engine,
    val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val opaqueColoredMaterial by lazy {
        createMaterial("$kMaterialsAssetFolder/opaque_colored.filamat")
    }
    private val transparentColoredMaterial by lazy {
        createMaterial("$kMaterialsAssetFolder/transparent_colored.filamat")
    }
    private val opaqueTexturedMaterial by lazy {
        createMaterial("$kMaterialsAssetFolder/opaque_textured.filamat")
    }
    private val transparentTexturedMaterial by lazy {
        createMaterial("$kMaterialsAssetFolder/transparent_textured.filamat")
    }
    private val imageTextureMaterial by lazy {
        createMaterial("$kMaterialsAssetFolder/image_texture.filamat")
    }
    private val videoTextureMaterial by lazy {
        createMaterial("$kMaterialsAssetFolder/video_texture.filamat")
    }
    private val videoTextureChromaKeyMaterial by lazy {
        createMaterial("$kMaterialsAssetFolder/video_texture_chroma_key.filamat")
    }

    private val materials = mutableListOf<Material>()
    private val imageMaterials = mutableListOf<ImageMaterial>()
    private val videoMaterials = mutableListOf<VideoMaterial>()
    private val materialInstances = mutableListOf<MaterialInstance>()

    /**
     * Creates and returns a [Material] object.
     *
     * A Filament Material defines the visual appearance of an object. Materials function as a
     * templates from which [MaterialInstance]s can be spawned.
     *
     * Documentation: [Filament Materials Guide](https://google.github.io/filament/Materials.html)
     *
     * @param payload Specifies the material data. The material data is a binary blob produced by
     * libfilamat or by matc.
     *
     * @see MaterialLoader.loadMaterial
     */
    fun createMaterial(payload: Buffer): Material =
        Material.Builder()
            .payload(payload, payload.remaining())
            .build(engine).also { material ->
                materials += material
            }

    /**
     * Creates and returns a [Material] object from Filamat asset file.
     *
     * @param assetFileLocation the .filamat asset file location *materials/mymaterial.filamat*
     *
     * @see createMaterial
     */
    fun createMaterial(assetFileLocation: String): Material =
        createMaterial(context.assets.readFileBuffer(assetFileLocation))

    /**
     * Loads a [Material] from the contents of a Filamat file.
     *
     * The material data is a binary blob produced by libfilamat or by matc.
     *
     * @param fileLocation the .filamat file location:
     * - A relative asset file location *materials/mymaterial.filamat*
     * - An Android resource from the res folder *context.getResourceUri(R.raw.mymaterial)*
     * - A File path *Uri.fromFile(myMaterialFile).path*
     * - An http or https url *https://mydomain.com/mymaterial.filamat*
     */
    suspend fun loadMaterial(fileLocation: String): Material? =
        context.loadFileBuffer(fileLocation)?.let { buffer ->
            withContext(Dispatchers.Main) {
                createMaterial(buffer)
            }
        }

    /**
     * Loads a [Material] from the contents of a Filamat file within a created coroutine scope.
     *
     * @param fileLocation the .filamat file location:
     * - A relative asset file location *materials/mymaterial.filamat*
     * - An Android resource from the res folder *context.getResourceUri(R.raw.mymaterial)*
     * - A File path *Uri.fromFile(myMaterialFile).path*
     * - An http or https url *https://mydomain.com/mymaterial.filamat*
     *
     * @see loadMaterial
     */
    fun loadMaterialAsync(fileLocation: String, onResult: (Material?) -> Unit) =
        coroutineScope.launch {
            loadMaterial(fileLocation).also(onResult)
        }

    fun createInstance(material: Material) = material.createInstance().also {
        materialInstances += it
    }

    /**
     * Creates an opaque or transparent [Material] depending on the color alpha with the [Color]
     * passed in.
     *
     * The [Color] can be modified by calling [MaterialInstance.setColor].
     * The metallicness, roughness, and reflectance can be modified using
     * [MaterialInstance.setMetallic], [MaterialInstance.setRoughness],
     * [MaterialInstance.setReflectance].
     */
    fun createColorMaterial(
        color: androidx.compose.ui.graphics.Color,
        metallic: Float = kMaterialDefaultMetallic,
        roughness: Float = kMaterialDefaultRoughness,
        reflectance: Float = kMaterialDefaultReflectance
    ) = createColorMaterial(colorOf(color), metallic, roughness, reflectance)

    /**
     * Creates an opaque or transparent [Material] depending on the color alpha with the [Color]
     * passed in.
     *
     * The [Color] can be modified by calling [MaterialInstance.setColor].
     * The metallicness, roughness, and reflectance can be modified using
     * [MaterialInstance.setMetallic], [MaterialInstance.setRoughness],
     * [MaterialInstance.setReflectance].
     */
    fun createColorMaterial(
        color: Int,
        metallic: Float = kMaterialDefaultMetallic,
        roughness: Float = kMaterialDefaultRoughness,
        reflectance: Float = kMaterialDefaultReflectance
    ) = createColorMaterial(colorOf(color), metallic, roughness, reflectance)

    /**
     * Creates an opaque or transparent [Material] depending on the color alpha with the [Color]
     * passed in.
     *
     * The [Color] can be modified by calling [MaterialInstance.setColor].
     * The metallicness, roughness, and reflectance can be modified using
     * [MaterialInstance.setMetallic], [MaterialInstance.setRoughness],
     * [MaterialInstance.setReflectance].
     */
    fun createColorMaterial(
        color: Color,
        metallic: Float = kMaterialDefaultMetallic,
        roughness: Float = kMaterialDefaultRoughness,
        reflectance: Float = kMaterialDefaultReflectance
    ): MaterialInstance =
        createInstance(if (color.a == 1.0f) opaqueColoredMaterial else transparentColoredMaterial)
            .apply {
                setColor(color)
                setMetallic(metallic)
                setRoughness(roughness)
                setReflectance(reflectance)
            }

    /**
     * Creates an an opaque or transparent [Material] with the [Texture] passed in.
     *
     * The [Texture] can be modified by calling [MaterialInstance.setTexture].
     * The metallicness, roughness, and reflectance can be modified using
     * [MaterialInstance.setMetallic], [MaterialInstance.setRoughness],
     * [MaterialInstance.setReflectance].
     */
    fun createTextureMaterial(
        texture: Texture,
        isOpaque: Boolean = true,
        metallic: Float = kMaterialDefaultMetallic,
        roughness: Float = kMaterialDefaultRoughness,
        reflectance: Float = kMaterialDefaultReflectance
    ): MaterialInstance =
        createInstance(if (isOpaque) opaqueTexturedMaterial else transparentTexturedMaterial)
            .apply {
                setTexture(texture)
                setMetallic(metallic)
                setRoughness(roughness)
                setReflectance(reflectance)
            }

    fun createImageMaterial(
        assets: AssetManager,
        fileLocation: String,
        type: TextureType = TextureType.COLOR
    ) = createImageMaterial(
        bitmap = ImageTexture.getBitmap(assets, fileLocation, type)
    )

    fun createImageMaterial(
        context: Context,
        @DrawableRes drawableResId: Int,
        type: TextureType = TextureType.COLOR
    ) = createImageMaterial(
        bitmap = ImageTexture.getBitmap(context, drawableResId, type)
    )

    fun createImageMaterial(bitmap: Bitmap, type: TextureType = TextureType.COLOR) = ImageMaterial(
        engine = engine,
        instance = createInstance(imageTextureMaterial),
        bitmap = bitmap,
        type = type
    ).also {
        imageMaterials += it
    }

    fun createVideoMaterial(mediaPlayer: MediaPlayer, chromaKeyColor: Int? = null) = VideoMaterial(
        engine = engine,
        instance = if (chromaKeyColor == null) {
            createInstance(videoTextureMaterial)
        } else {
            createInstance(videoTextureChromaKeyMaterial).apply {
                setParameter("chromaKeyColor", colorOf(chromaKeyColor))
            }
        },
        mediaPlayer = mediaPlayer
    ).also {
        videoMaterials += it
    }

    fun destroyMaterial(material: Material) {
        engine.safeDestroyMaterial(material)
        materials -= material
    }

    fun destroyMaterialInstance(materialInstance: MaterialInstance) {
        engine.safeDestroyMaterialInstance(materialInstance)
        materialInstances -= materialInstance
    }

    fun destroy() {
        coroutineScope.cancel()

        materialInstances.toList().forEach { destroyMaterialInstance(it) }
        materialInstances.clear()
        imageMaterials.toList().forEach { it.destroy() }
        imageMaterials.clear()
        videoMaterials.toList().forEach { it.destroy() }
        videoMaterials.clear()
        materials.toList().forEach { destroyMaterial(it) }
        materials.clear()
    }
}