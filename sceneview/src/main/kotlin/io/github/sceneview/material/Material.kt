package io.github.sceneview.material

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.filament.*
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.Float4
import io.github.sceneview.Filament
import io.github.sceneview.utils.fileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer

object MaterialLoader {

    var cache = mutableMapOf<String, Material>()

    /**
     * ### Load a Material object from an filamat file
     *
     * The material file is a binary blob produced by libfilamat or by matc.
     *
     * @param filamatFileLocation the filamat file location.
     * - A relative file location *materials/mymaterial.filamat*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymaterial)*
     * - A File path *Uri.fromFile(myMaterialFile).path*
     * - An http or https url *https://mydomain.com/mymaterial.filamat*
     */
    @JvmOverloads
    suspend fun loadMaterial(
        context: Context,
        filamatFileLocation: String
    ): MaterialInstance? {
        return try {
            cache[filamatFileLocation]?.createInstance()
                ?: context.fileBuffer(filamatFileLocation)
                    ?.let { buffer ->
                        withContext(Dispatchers.Main) {
                            createMaterial(buffer).also {
                                cache += filamatFileLocation to it.material
                            }
                        }
                    }
        } finally {
            // TODO: See why the finally is called before the onDestroy()
//        material?.destroy()
        }
    }

    /**
     * ### Load a Material object from an filamat file
     *
     * The material file is a binary blob produced by libfilamat or by matc.
     *
     * For Java compatibility usage.
     *
     * Kotlin developers should use [HDRLoader.loadEnvironment]
     *
     * [See][loadMaterial]
     */
    fun loadMaterialAsync(
        context: Context,
        filamatFileLocation: String,
        coroutineScope: LifecycleCoroutineScope,
        result: (MaterialInstance?) -> Unit
    ) = coroutineScope.launchWhenCreated {
        result(loadMaterial(context, filamatFileLocation))
    }

    /**
     * ### Creates and returns the Material object
     *
     * The material data is a binary blob produced by libfilamat or by matc.
     *
     * @param filamatBuffer The content of the Filamat File
     * @return the newly created object
     */
    @JvmOverloads
    fun createMaterial(filamatBuffer: Buffer): MaterialInstance {
        return Material.Builder().payload(filamatBuffer, filamatBuffer.remaining())
            .build(Filament.engine)
            .defaultInstance
    }

    fun clearCache() {
        cache.clear()
    }

    fun destroy() {
        cache.forEach { (_, material) -> material.destroy() }
        cache.clear()
    }
}

fun Material.destroy(removeFromCache: Boolean = true) {
    if (removeFromCache) {
        MaterialLoader.cache -= MaterialLoader.cache.filter { (_, material) -> material == this }.keys
    }
    Filament.engine.destroyMaterial(this)
}

fun MaterialInstance.newInstance(): MaterialInstance = material.createInstance()

fun MaterialInstance.destroy() {
    if (material.defaultInstance == this) {
        material.destroy()
    }
    Filament.engine.destroyMaterialInstance(this)
}

fun MaterialInstance.setParameter(name: String, value: Float4) =
    setParameter(name, value.x, value.y, value.z, value.w)

fun MaterialInstance.setParameter(name: String, value: Float3) =
    setParameter(name, value.x, value.y, value.z)

fun MaterialInstance.setTexture(
    name: String,
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setParameter(name, texture, textureSampler)

fun MaterialInstance.setExternalTexture(
    name: String,
    texture: Texture
) = setTexture(name, texture, TextureSamplerExternal())

fun MaterialInstance.setExternalStreamTexture(
    name: String,
    stream: Stream
) = setTexture(
    name,
    Texture.Builder().sampler(Texture.Sampler.SAMPLER_EXTERNAL).format(Texture.InternalFormat.RGB8)
        .build(), TextureSamplerExternal()
)

// **********
// Base Color
// **********
fun MaterialInstance.setBaseColorIndex(value: Int) = setParameter("baseColorIndex", value)
fun MaterialInstance.setBaseColor(
    r: Float = 0.0f,
    g: Float = 0.0f,
    b: Float = 0.0f,
    a: Float = 1.0f
) = setParameter("baseColorFactor", Float4(r, g, b, a))

fun MaterialInstance.setBaseColorMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("baseColorMap", texture, textureSampler)

// **********************
// Metallic-Roughness Map
// **********************
fun MaterialInstance.setMetallicRoughnessIndex(value: Int) =
    setParameter("metallicRoughnessIndex", value)

fun MaterialInstance.setMetallicFactor(value: Float) = setParameter("metallicFactor", value)
fun MaterialInstance.setRoughnessFactor(value: Float) = setParameter("roughnessFactor", value)
fun MaterialInstance.setMetallicRoughnessMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("metallicRoughnessMap", texture, textureSampler)

// **********
// Normal Map
// **********
fun MaterialInstance.setNormalIndex(value: Int) = setParameter("normalIndex", value)
fun MaterialInstance.setNormalScale(value: Float) = setParameter("normalScale", value)
fun MaterialInstance.setNormalMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("normalMap", texture, textureSampler)

// *****************
// Ambient Occlusion
// *****************
fun MaterialInstance.setAoIndex(value: Int) = setParameter("aoIndex", value)
fun MaterialInstance.setAoStrength(value: Float) = setParameter("aoStrength", value)
fun MaterialInstance.setOcclusionMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("occlusionMap", texture, textureSampler)

// ************
// Emissive Map
// ************
fun MaterialInstance.setEmissiveIndex(value: Int) = setParameter("emissiveIndex", value)
fun MaterialInstance.setEmissiveColor(r: Float = 0.0f, g: Float = 0.0f, b: Float = 0.0f) =
    setParameter("emissiveFactor", Float3(r, g, b))

fun MaterialInstance.setEmissiveMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("emissiveMap", texture, textureSampler)

fun MaterialInstance.setBaseTexture(
    texture: Texture, textureSampler: TextureSampler = TextureSampler2D()
) = setBaseColorMap(texture, textureSampler)