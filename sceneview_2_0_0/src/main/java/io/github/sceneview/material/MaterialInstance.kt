package io.github.sceneview.material

import com.google.android.filament.MaterialInstance
import com.google.android.filament.MaterialInstance.FloatElement
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Mat4
import io.github.sceneview.SceneView
import io.github.sceneview.texture.TextureSampler2D
import io.github.sceneview.utils.Color

fun MaterialInstance.setParameter(name: String, value: Float2) =
    setParameter(name, value.x, value.y)

fun MaterialInstance.setParameter(name: String, value: Float4) =
    setParameter(name, value.x, value.y, value.z, value.w)

fun MaterialInstance.setParameter(name: String, value: Mat4) =
    setParameter(name, FloatElement.FLOAT4, value.toFloatArray(), 0, 4)

fun MaterialInstance.setParameter(name: String, value: Float3) =
    setParameter(name, value.x, value.y, value.z)

// Texture

fun MaterialInstance.setTexture(
    name: String,
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler(
        TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.REPEAT
    )
) = setParameter(name, texture, textureSampler)

fun MaterialInstance.setExternalTexture(
    name: String,
    texture: Texture
) = setTexture(
    name, texture, TextureSampler(
        TextureSampler.MinFilter.LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.CLAMP_TO_EDGE
    )
)

// Base Color

fun MaterialInstance.setBaseColorIndex(value: Int) = setParameter("baseColorIndex", value)
fun MaterialInstance.setBaseColor(value: Color) = setParameter("baseColorFactor", value)

fun MaterialInstance.setBaseColorMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("baseColorMap", texture, textureSampler)

// Metallic-Roughness Map

fun MaterialInstance.setMetallicRoughnessIndex(value: Int) =
    setParameter("metallicRoughnessIndex", value)

fun MaterialInstance.setMetallicFactor(value: Float) = setParameter("metallicFactor", value)
fun MaterialInstance.setRoughnessFactor(value: Float) = setParameter("roughnessFactor", value)
fun MaterialInstance.setMetallicRoughnessMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("metallicRoughnessMap", texture, textureSampler)

// Normal Map

fun MaterialInstance.setNormalIndex(value: Int) = setParameter("normalIndex", value)
fun MaterialInstance.setNormalScale(value: Float) = setParameter("normalScale", value)
fun MaterialInstance.setNormalMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("normalMap", texture, textureSampler)

// Ambient Occlusion

fun MaterialInstance.setAoIndex(value: Int) = setParameter("aoIndex", value)
fun MaterialInstance.setAoStrength(value: Float) = setParameter("aoStrength", value)
fun MaterialInstance.setOcclusionMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("occlusionMap", texture, textureSampler)

// Emissive Map

fun MaterialInstance.setEmissiveIndex(value: Int) = setParameter("emissiveIndex", value)
fun MaterialInstance.setEmissiveColor(value: Color) = setParameter("emissiveFactor", value)

fun MaterialInstance.setEmissiveMap(
    texture: Texture,
    textureSampler: TextureSampler = TextureSampler2D()
) = setTexture("emissiveMap", texture, textureSampler)

fun MaterialInstance.setBaseTexture(
    texture: Texture, textureSampler: TextureSampler = TextureSampler2D()
) = setBaseColorMap(texture, textureSampler)

// View

fun MaterialInstance.setViewTexture(
    viewTexture: Texture,
    textureParamName: String = "viewTexture"
) {
    setTexture(
        textureParamName,
        viewTexture,
        TextureSampler(
            TextureSampler.MinFilter.LINEAR,
            TextureSampler.MagFilter.LINEAR,
            TextureSampler.WrapMode.REPEAT
        )
    )
}

fun MaterialInstance.setInvertFrontFaceWinding(
    invert: Boolean,
    uvOffsetParamName: String = "uvOffset"
) {
    setParameter(uvOffsetParamName, if (invert) Float2(1.0f, 0.0f) else Float2(0.0f))
}

fun SceneView.destroyMaterialInstance(materialInstance: MaterialInstance) {
    engine.destroyMaterialInstance(materialInstance)
    materialInstances -= materialInstance
}