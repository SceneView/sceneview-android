package io.github.sceneview.material

import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Mat3
import io.github.sceneview.math.Color
import io.github.sceneview.texture.TextureSampler2D

//////////////
// UberShader
//////////////

fun MaterialInstance.setSpecularFactor(value: Float3) = setParameter("specularFactor", value)
fun MaterialInstance.setGlossinessFactor(value: Float) = setParameter("glossinessFactor", value)

// Base Color

fun MaterialInstance.setBaseColorIndex(value: Int) = setParameter("baseColorIndex", value)
fun MaterialInstance.setBaseColorFactor(value: Color) = setParameter("baseColorFactor", value)
fun MaterialInstance.setBaseColorMap(
    texture: Texture,
    sampler: TextureSampler = TextureSampler2D()
) = setParameter("baseColorMap", texture, sampler)

fun MaterialInstance.setBaseColorUvMatrix(value: Mat3) = setParameter("baseColorUvMatrix", value)

// Metallic-Roughness Map

fun MaterialInstance.setMetallicRoughnessIndex(value: Int) =
    setParameter("metallicRoughnessIndex", value)

fun MaterialInstance.setMetallicFactor(value: Float) = setParameter("metallicFactor", value)
fun MaterialInstance.setRoughnessFactor(value: Float) = setParameter("roughnessFactor", value)
fun MaterialInstance.setMetallicRoughnessMap(
    texture: Texture,
    sampler: TextureSampler = TextureSampler2D()
) = setParameter("metallicRoughnessMap", texture, sampler)

fun MaterialInstance.setMetallicRoughnessUvMatrix(value: Mat3) =
    setParameter("metallicRoughnessUvMatrix", value)

// Normal Map

fun MaterialInstance.setNormalIndex(value: Int) = setParameter("normalIndex", value)
fun MaterialInstance.setNormalScale(value: Float) = setParameter("normalScale", value)
fun MaterialInstance.setNormalMap(texture: Texture, sampler: TextureSampler = TextureSampler2D()) =
    setParameter("normalMap", texture, sampler)

fun MaterialInstance.setNormalUvMatrix(value: Mat3) = setParameter("normalUvMatrix", value)

// Ambient Occlusion

fun MaterialInstance.setAoIndex(value: Int) = setParameter("aoIndex", value)
fun MaterialInstance.setAoStrength(value: Float) = setParameter("aoStrength", value)
fun MaterialInstance.setOcclusionMap(
    texture: Texture,
    sampler: TextureSampler = TextureSampler2D()
) = setParameter("occlusionMap", texture, sampler)

fun MaterialInstance.setOcclusionUvMatrix(value: Mat3) = setParameter("occlusionUvMatrix", value)

// Emissive Map

fun MaterialInstance.setEmissiveIndex(value: Int) = setParameter("emissiveIndex", value)
fun MaterialInstance.setEmissiveFactor(value: Color) = setParameter("emissiveFactor", value)
fun MaterialInstance.setEmissiveStrength(value: Float) = setParameter("emissiveStrength", value)
fun MaterialInstance.setEmissiveMap(
    texture: Texture,
    sampler: TextureSampler = TextureSampler2D()
) = setParameter("emissiveMap", texture, sampler)

fun MaterialInstance.setemissiveUvMatrix(value: Mat3) = setParameter("emissiveUvMatrix", value)


// Clear coat

fun MaterialInstance.setClearCoatFactor(value: Float) = setParameter("clearCoatFactor", value)
fun MaterialInstance.setClearCoatRoughnessFactor(value: Float) =
    setParameter("clearCoatRoughnessFactor", value)

fun MaterialInstance.setClearCoatIndex(value: Int) = setParameter("clearCoatIndex", value)
fun MaterialInstance.setClearCoatMap(
    texture: Texture,
    sampler: TextureSampler = TextureSampler2D()
) = setParameter("clearCoatMap", texture, sampler)

fun MaterialInstance.setClearCoatUvMatrix(value: Mat3) = setParameter("clearCoatUvMatrix", value)
fun MaterialInstance.setClearCoatRoughnessIndex(value: Int) =
    setParameter("clearCoatRoughnessIndex", value)

fun MaterialInstance.setClearCoatRoughnessMap(
    texture: Texture,
    sampler: TextureSampler = TextureSampler2D()
) = setParameter("clearCoatRoughnessMap", texture, sampler)

fun MaterialInstance.setClearCoatRoughnessUvMatrix(value: Mat3) =
    setParameter("clearCoatRoughnessUvMatrix", value)

fun MaterialInstance.setClearCoatNormalIndex(value: Int) =
    setParameter("clearCoatNormalIndex", value)

fun MaterialInstance.setClearCoatNormalMap(
    texture: Texture,
    sampler: TextureSampler = TextureSampler2D()
) = setParameter("clearCoatNormalMap", texture, sampler)

fun MaterialInstance.setClearCoatNormalUvMatrix(value: Mat3) =
    setParameter("clearCoatNormalUvMatrix", value)

fun MaterialInstance.setClearCoatNormalScale(value: Float) =
    setParameter("clearCoatNormalScale", value)

// Reflectance

//fun MaterialInstance.setReflectance(value: Float) = setParameter("reflectance", value)
