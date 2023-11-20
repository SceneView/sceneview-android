package io.github.sceneview.material

import com.google.android.filament.Colors
import com.google.android.filament.MaterialInstance
import com.google.android.filament.MaterialInstance.FloatElement
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Mat4
import io.github.sceneview.math.Color
import io.github.sceneview.math.colorOf
import io.github.sceneview.math.toColumnsFloatArray
import io.github.sceneview.texture.TextureSampler2D
import io.github.sceneview.texture.TextureSamplerExternal

const val kMaterialDefaultMetallic = 0.0f
const val kMaterialDefaultRoughness = 0.4f
const val kMaterialDefaultReflectance = 0.5f

fun MaterialInstance.setParameter(name: String, value: Float2) =
    setParameter(name, value.x, value.y)

fun MaterialInstance.setParameter(name: String, value: Float4) =
    setParameter(name, value.x, value.y, value.z, value.w)

fun MaterialInstance.setParameter(name: String, value: Mat4) =
    setParameter(name, FloatElement.FLOAT4, value.toColumnsFloatArray(), 0, 4)

fun MaterialInstance.setParameter(name: String, value: Float3) =
    setParameter(name, value.x, value.y, value.z)

fun MaterialInstance.setTexture(
    name: String,
    texture: Texture,
    sampler: TextureSampler = TextureSampler2D()
) = setParameter(name, texture, sampler)

fun MaterialInstance.setExternalTexture(name: String, texture: Texture) =
    setParameter(name, texture, TextureSamplerExternal())

// **********
// PBR
// **********


//fun MaterialInstance.setColor(name: String, type: Colors.RgbaType, value: Float4) =
//    setParameter(name, type, value.x, value.y, value.z, value.w)
fun MaterialInstance.setColor(
    color: Color,
    parameterName: String = "color",
    type: Colors.RgbaType = Colors.RgbaType.SRGB
) = setParameter(parameterName, type, color.r, color.g, color.b, color.a)

fun MaterialInstance.setColor(
    color: Int,
    parameterName: String = "color",
    type: Colors.RgbaType = Colors.RgbaType.SRGB
) = setColor(colorOf(color), parameterName, type)

fun MaterialInstance.setColor(
    color: androidx.compose.ui.graphics.Color,
    parameterName: String = "color",
    type: Colors.RgbaType = Colors.RgbaType.SRGB
) = setColor(colorOf(color), parameterName, type)

/**
 * The metallic property defines whether the surface is a metallic (conductor) or a non-metallic
 * (dielectric) surface.
 *
 * This property should be used as a binary value, set to either 0 or 1.
 * Intermediate values are only truly useful to create transitions between different types of
 * surfaces when using textures.
 *
 * The default value is 0.
 */
fun MaterialInstance.setMetallic(factor: Float) = setParameter("metallic", factor)

/**
 * The roughness property controls the perceived smoothness of the surface.
 *
 * When roughness is set to 0, the surface is perfectly smooth and highly glossy.
 * The rougher a surface is, the "blurrier" the reflections are.
 *
 * The default value is 0.4.
 */
fun MaterialInstance.setRoughness(factor: Float) = setParameter("roughness", factor)

/**
 * The reflectance property only affects non-metallic surfaces.
 *
 * This property can be used to control the specular intensity. This value is defined between 0 and
 * 1 and represents a remapping of a percentage of reflectance.
 *
 * The default value is 0.5.
 */
fun MaterialInstance.setReflectance(factor: Float) = setParameter("reflectance", factor)

// **********
// Texture
// **********

fun MaterialInstance.setTexture(texture: Texture) = setTexture("texture", texture)
fun MaterialInstance.setExternalTexture(texture: Texture) = setExternalTexture("texture", texture)

// **********
// Base Color
// **********
fun MaterialInstance.setBaseColorIndex(value: Int) = setParameter("baseColorIndex", value)
fun MaterialInstance.setBaseColor(value: Color) = setParameter("baseColorFactor", value)

fun MaterialInstance.setBaseColorMap(
    texture: Texture, sampler: TextureSampler = TextureSampler2D()
) = setParameter("baseColorMap", texture, sampler)

// **********************
// Metallic-Roughness Map
// **********************
fun MaterialInstance.setMetallicRoughnessIndex(value: Int) =
    setParameter("metallicRoughnessIndex", value)

fun MaterialInstance.setMetallicFactor(value: Float) = setParameter("metallicFactor", value)
fun MaterialInstance.setRoughnessFactor(value: Float) = setParameter("roughnessFactor", value)
fun MaterialInstance.setMetallicRoughnessMap(
    texture: Texture, sampler: TextureSampler = TextureSampler2D()
) = setParameter("metallicRoughnessMap", texture, sampler)

// **********
// Normal Map
// **********
fun MaterialInstance.setNormalIndex(value: Int) = setParameter("normalIndex", value)
fun MaterialInstance.setNormalScale(value: Float) = setParameter("normalScale", value)
fun MaterialInstance.setNormalMap(
    texture: Texture, textureSampler: TextureSampler = TextureSampler2D()
) = setParameter("normalMap", texture, textureSampler)

// *****************
// Ambient Occlusion
// *****************
fun MaterialInstance.setAoIndex(value: Int) = setParameter("aoIndex", value)
fun MaterialInstance.setAoStrength(value: Float) = setParameter("aoStrength", value)
fun MaterialInstance.setOcclusionMap(
    texture: Texture, textureSampler: TextureSampler = TextureSampler2D()
) = setParameter("occlusionMap", texture, textureSampler)

// ************
// Emissive Map
// ************
fun MaterialInstance.setEmissiveIndex(value: Int) = setParameter("emissiveIndex", value)
fun MaterialInstance.setEmissiveColor(value: Color) = setParameter("emissiveFactor", value)

fun MaterialInstance.setEmissiveMap(
    texture: Texture, textureSampler: TextureSampler = TextureSampler2D()
) = setParameter("emissiveMap", texture, textureSampler)

fun MaterialInstance.setBaseTexture(
    texture: Texture, textureSampler: TextureSampler = TextureSampler2D()
) = setBaseColorMap(texture, textureSampler)