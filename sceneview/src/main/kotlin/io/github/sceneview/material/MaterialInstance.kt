package io.github.sceneview.material

import com.google.android.filament.MaterialInstance
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.Filament
import io.github.sceneview.texture.TextureSampler2D
import io.github.sceneview.texture.TextureSamplerExternal
import io.github.sceneview.texture.build

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