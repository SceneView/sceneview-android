package io.github.sceneview.material

import com.google.android.filament.Stream
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.Float4
import com.google.ar.sceneform.rendering.Material

val TextureSampler2D = TextureSampler(
    TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
    TextureSampler.MagFilter.LINEAR,
    TextureSampler.WrapMode.REPEAT
)
val TextureSamplerExternal = TextureSampler(
    TextureSampler.MinFilter.LINEAR,
    TextureSampler.MagFilter.LINEAR,
    TextureSampler.WrapMode.CLAMP_TO_EDGE
)

fun Material.setParameter(name: String, value: Float4) =
    filamentMaterialInstance.setParameter(name, value.x, value.y, value.z, value.w)

fun Material.setParameter(name: String, value: Float3) =
    filamentMaterialInstance.setParameter(name, value.x, value.y, value.z)

fun Material.setTexture(
    name: String,
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D
) = filamentMaterialInstance.setParameter(name, texture, textureSampler)

fun Material.setExternalTexture(
    name: String,
    texture: Texture
) = setTexture(name, texture, TextureSamplerExternal)

fun Material.setExternalStreamTexture(
    name: String,
    stream: Stream
) = setTexture(
    name,
    Texture.Builder().sampler(Texture.Sampler.SAMPLER_EXTERNAL).format(Texture.InternalFormat.RGB8)
        .build(), TextureSamplerExternal
)

// Base Color
fun Material.setBaseColorIndex(value: Int) =
    filamentMaterialInstance.setParameter("baseColorIndex", value)

fun Material.setBaseColor(
    r: Float = 0.0f,
    g: Float = 0.0f,
    b: Float = 0.0f,
    a: Float = 1.0f
) = setParameter("baseColorFactor", Float4(r, g, b, a))

//fun Material.setBaseColorFactor(color: Int) = setParameter("baseColorFactor", )
fun Material.setBaseColorMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D
) = setTexture("baseColorMap", texture, textureSampler)

// Metallic-Roughness Map
fun Material.setMetallicRoughnessIndex(value: Int) =
    filamentMaterialInstance.setParameter("metallicRoughnessIndex", value)

fun Material.setMetallicFactor(value: Float) =
    filamentMaterialInstance.setParameter("metallicFactor", value)

fun Material.setRoughnessFactor(value: Float) =
    filamentMaterialInstance.setParameter("roughnessFactor", value)

fun Material.setMetallicRoughnessMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D
) = setTexture("metallicRoughnessMap", texture, textureSampler)

// Normal Map
fun Material.setNormalIndex(value: Int) =
    filamentMaterialInstance.setParameter("normalIndex", value)

fun Material.setNormalScale(value: Float) =
    filamentMaterialInstance.setParameter("normalScale", value)

fun Material.setNormalMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D
) = setTexture("normalMap", texture, textureSampler)

// Ambient Occlusion
fun Material.setAoIndex(value: Int) = filamentMaterialInstance.setParameter("aoIndex", value)
fun Material.setAoStrength(value: Float) =
    filamentMaterialInstance.setParameter("aoStrength", value)

fun Material.setOcclusionMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D
) = setTexture("occlusionMap", texture, textureSampler)

// Emissive Map
fun Material.setEmissiveIndex(value: Int) =
    filamentMaterialInstance.setParameter("emissiveIndex", value)

fun Material.setEmissiveColor(r: Float = 0.0f, g: Float = 0.0f, b: Float = 0.0f) =
    setParameter("emissiveFactor", Float3(r, g, b))

fun Material.setEmissiveMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D
) = setTexture("emissiveMap", texture, textureSampler)

fun Material.setBaseTexture(
    texture: Texture, textureSampler: TextureSampler = TextureSampler2D
) = setBaseColorMap(texture, textureSampler)