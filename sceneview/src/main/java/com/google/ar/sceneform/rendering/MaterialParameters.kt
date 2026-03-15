package com.google.ar.sceneform.rendering

import com.google.android.filament.MaterialInstance
import com.google.android.filament.TextureSampler
import com.google.android.filament.proguard.UsedByNative
import io.github.sceneview.collision.Vector3

/** Material property store. */
@UsedByNative("material_java_wrappers.h")
internal class MaterialParameters {
    private val namedParameters = HashMap<String, Parameter>()

    @UsedByNative("material_java_wrappers.h")
    fun setBoolean(name: String, x: Boolean) {
        namedParameters[name] = BooleanParameter(name, x)
    }

    fun getBoolean(name: String): Boolean {
        val param = namedParameters[name]
        if (param is BooleanParameter) {
            return param.x
        }
        return false
    }

    @UsedByNative("material_java_wrappers.h")
    fun setBoolean2(name: String, x: Boolean, y: Boolean) {
        namedParameters[name] = Boolean2Parameter(name, x, y)
    }

    fun getBoolean2(name: String): BooleanArray? {
        val param = namedParameters[name]
        if (param is Boolean2Parameter) {
            return booleanArrayOf(param.x, param.y)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setBoolean3(name: String, x: Boolean, y: Boolean, z: Boolean) {
        namedParameters[name] = Boolean3Parameter(name, x, y, z)
    }

    fun getBoolean3(name: String): BooleanArray? {
        val param = namedParameters[name]
        if (param is Boolean3Parameter) {
            return booleanArrayOf(param.x, param.y, param.z)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setBoolean4(name: String, x: Boolean, y: Boolean, z: Boolean, w: Boolean) {
        namedParameters[name] = Boolean4Parameter(name, x, y, z, w)
    }

    fun getBoolean4(name: String): BooleanArray? {
        val param = namedParameters[name]
        if (param is Boolean4Parameter) {
            return booleanArrayOf(param.x, param.y, param.z, param.w)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setFloat(name: String, x: Float) {
        namedParameters[name] = FloatParameter(name, x)
    }

    fun getFloat(name: String): Float {
        val param = namedParameters[name]
        if (param is FloatParameter) {
            return param.x
        }
        return 0.0f
    }

    @UsedByNative("material_java_wrappers.h")
    fun setFloat2(name: String, x: Float, y: Float) {
        namedParameters[name] = Float2Parameter(name, x, y)
    }

    fun getFloat2(name: String): FloatArray? {
        val param = namedParameters[name]
        if (param is Float2Parameter) {
            return floatArrayOf(param.x, param.y)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setFloat3(name: String, x: Float, y: Float, z: Float) {
        namedParameters[name] = Float3Parameter(name, x, y, z)
    }

    fun setFloat3(name: String, value: Vector3) {
        namedParameters[name] = Float3Parameter(name, value.x, value.y, value.z)
    }

    fun getFloat3(name: String): FloatArray? {
        val param = namedParameters[name]
        if (param is Float3Parameter) {
            return floatArrayOf(param.x, param.y, param.z)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) {
        namedParameters[name] = Float4Parameter(name, x, y, z, w)
    }

    fun getFloat4(name: String): FloatArray? {
        val param = namedParameters[name]
        if (param is Float4Parameter) {
            return floatArrayOf(param.x, param.y, param.z, param.w)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setInt(name: String, x: Int) {
        namedParameters[name] = IntParameter(name, x)
    }

    fun getInt(name: String): Int {
        val param = namedParameters[name]
        if (param is IntParameter) {
            return param.x
        }
        return 0
    }

    @UsedByNative("material_java_wrappers.h")
    fun setInt2(name: String, x: Int, y: Int) {
        namedParameters[name] = Int2Parameter(name, x, y)
    }

    fun getInt2(name: String): IntArray? {
        val param = namedParameters[name]
        if (param is Int2Parameter) {
            return intArrayOf(param.x, param.y)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setInt3(name: String, x: Int, y: Int, z: Int) {
        namedParameters[name] = Int3Parameter(name, x, y, z)
    }

    fun getInt3(name: String): IntArray? {
        val param = namedParameters[name]
        if (param is Int3Parameter) {
            return intArrayOf(param.x, param.y, param.z)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setInt4(name: String, x: Int, y: Int, z: Int, w: Int) {
        namedParameters[name] = Int4Parameter(name, x, y, z, w)
    }

    fun getInt4(name: String): IntArray? {
        val param = namedParameters[name]
        if (param is Int4Parameter) {
            return intArrayOf(param.x, param.y, param.z, param.w)
        }
        return null
    }

    @UsedByNative("material_java_wrappers.h")
    fun setTexture(name: String, texture: Texture) {
        namedParameters[name] = TextureParameter(name, texture)
    }

    fun getTexture(name: String): Texture? {
        val param = namedParameters[name]
        if (param is TextureParameter) {
            return param.texture
        }
        return null
    }

    fun setDepthTexture(name: String, depthTexture: com.google.android.filament.Texture) {
        namedParameters[name] = DepthTextureParameter(name, depthTexture)
    }

    fun setExternalTexture(name: String, externalTexture: ExternalTexture) {
        namedParameters[name] = ExternalTextureParameter(name, externalTexture)
    }

    fun getExternalTexture(name: String): ExternalTexture? {
        val param = namedParameters[name]
        if (param is ExternalTextureParameter) {
            return param.externalTexture
        }
        return null
    }

    fun applyTo(materialInstance: MaterialInstance) {
        val material = materialInstance.material
        for (value in namedParameters.values) {
            if (material.hasParameter(value.name)) {
                value.applyTo(materialInstance)
            }
        }
    }

    fun applyParameterTo(materialInstance: MaterialInstance, name: String) {
        val material = materialInstance.material
        if (material.hasParameter(name)) {
            namedParameters[name]?.applyTo(materialInstance)
        }
    }

    fun copyFrom(other: MaterialParameters) {
        namedParameters.clear()
        merge(other)
    }

    fun merge(other: MaterialParameters) {
        for (value in other.namedParameters.values) {
            val clonedValue = value.clone()
            namedParameters[clonedValue.name] = clonedValue
        }
    }

    fun mergeIfAbsent(other: MaterialParameters) {
        for (value in other.namedParameters.values) {
            if (!namedParameters.containsKey(value.name)) {
                val clonedValue = value.clone()
                namedParameters[clonedValue.name] = clonedValue
            }
        }
    }

    abstract class Parameter : Cloneable {
        var name: String = ""

        abstract fun applyTo(materialInstance: MaterialInstance)

        public override fun clone(): Parameter {
            return try {
                super.clone() as Parameter
            } catch (e: CloneNotSupportedException) {
                throw AssertionError(e)
            }
        }
    }

    class BooleanParameter(name: String, var x: Boolean) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x)
        }
    }

    class Boolean2Parameter(name: String, var x: Boolean, var y: Boolean) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y)
        }
    }

    class Boolean3Parameter(name: String, var x: Boolean, var y: Boolean, var z: Boolean) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y, z)
        }
    }

    class Boolean4Parameter(name: String, var x: Boolean, var y: Boolean, var z: Boolean, var w: Boolean) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y, z, w)
        }
    }

    class FloatParameter(name: String, var x: Float) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x)
        }
    }

    class Float2Parameter(name: String, var x: Float, var y: Float) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y)
        }
    }

    class Float3Parameter(name: String, var x: Float, var y: Float, var z: Float) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y, z)
        }
    }

    class Float4Parameter(name: String, var x: Float, var y: Float, var z: Float, var w: Float) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y, z, w)
        }
    }

    class IntParameter(name: String, var x: Int) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x)
        }
    }

    class Int2Parameter(name: String, var x: Int, var y: Int) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y)
        }
    }

    class Int3Parameter(name: String, var x: Int, var y: Int, var z: Int) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y, z)
        }
    }

    class Int4Parameter(name: String, var x: Int, var y: Int, var z: Int, var w: Int) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(name, x, y, z, w)
        }
    }

    class TextureParameter(name: String, val texture: Texture) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            materialInstance.setParameter(
                name, texture.getFilamentTexture(), convertTextureSampler(texture.getSampler())
            )
        }

        override fun clone(): Parameter = TextureParameter(name, texture)
    }

    class DepthTextureParameter(
        name: String,
        private val depthTexture: com.google.android.filament.Texture
    ) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            val depthTextureSampler = TextureSampler(
                TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
                TextureSampler.MagFilter.LINEAR,
                TextureSampler.WrapMode.REPEAT
            )
            materialInstance.setParameter(name, depthTexture, depthTextureSampler)
        }
    }

    class ExternalTextureParameter(
        name: String,
        val externalTexture: ExternalTexture
    ) : Parameter() {
        init { this.name = name }
        override fun applyTo(materialInstance: MaterialInstance) {
            val filamentSampler = getExternalFilamentSampler()
            materialInstance.setParameter(name, externalTexture.getFilamentTexture(), filamentSampler)
        }

        private fun getExternalFilamentSampler(): TextureSampler {
            val filamentSampler = TextureSampler()
            filamentSampler.setMinFilter(TextureSampler.MinFilter.LINEAR)
            filamentSampler.setMagFilter(TextureSampler.MagFilter.LINEAR)
            filamentSampler.setWrapModeS(TextureSampler.WrapMode.CLAMP_TO_EDGE)
            filamentSampler.setWrapModeT(TextureSampler.WrapMode.CLAMP_TO_EDGE)
            filamentSampler.setWrapModeR(TextureSampler.WrapMode.CLAMP_TO_EDGE)
            return filamentSampler
        }

        override fun clone(): Parameter = ExternalTextureParameter(name, externalTexture)
    }

    companion object {
        private fun convertTextureSampler(sampler: Texture.Sampler): TextureSampler {
            val convertedSampler = TextureSampler()

            when (sampler.getMinFilter()) {
                Texture.Sampler.MinFilter.NEAREST ->
                    convertedSampler.setMinFilter(TextureSampler.MinFilter.NEAREST)
                Texture.Sampler.MinFilter.LINEAR ->
                    convertedSampler.setMinFilter(TextureSampler.MinFilter.LINEAR)
                Texture.Sampler.MinFilter.NEAREST_MIPMAP_NEAREST ->
                    convertedSampler.setMinFilter(TextureSampler.MinFilter.NEAREST_MIPMAP_NEAREST)
                Texture.Sampler.MinFilter.LINEAR_MIPMAP_NEAREST ->
                    convertedSampler.setMinFilter(TextureSampler.MinFilter.LINEAR_MIPMAP_NEAREST)
                Texture.Sampler.MinFilter.NEAREST_MIPMAP_LINEAR ->
                    convertedSampler.setMinFilter(TextureSampler.MinFilter.NEAREST_MIPMAP_LINEAR)
                Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR ->
                    convertedSampler.setMinFilter(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR)
                else -> throw IllegalArgumentException("Invalid MinFilter")
            }

            when (sampler.getMagFilter()) {
                Texture.Sampler.MagFilter.NEAREST ->
                    convertedSampler.setMagFilter(TextureSampler.MagFilter.NEAREST)
                Texture.Sampler.MagFilter.LINEAR ->
                    convertedSampler.setMagFilter(TextureSampler.MagFilter.LINEAR)
                else -> throw IllegalArgumentException("Invalid MagFilter")
            }

            convertedSampler.setWrapModeS(convertWrapMode(sampler.getWrapModeS()))
            convertedSampler.setWrapModeT(convertWrapMode(sampler.getWrapModeT()))
            convertedSampler.setWrapModeR(convertWrapMode(sampler.getWrapModeR()))

            return convertedSampler
        }

        private fun convertWrapMode(wrapMode: Texture.Sampler.WrapMode): TextureSampler.WrapMode {
            return when (wrapMode) {
                Texture.Sampler.WrapMode.CLAMP_TO_EDGE -> TextureSampler.WrapMode.CLAMP_TO_EDGE
                Texture.Sampler.WrapMode.REPEAT -> TextureSampler.WrapMode.REPEAT
                Texture.Sampler.WrapMode.MIRRORED_REPEAT -> TextureSampler.WrapMode.MIRRORED_REPEAT
                else -> throw IllegalArgumentException("Invalid WrapMode")
            }
        }
    }
}
