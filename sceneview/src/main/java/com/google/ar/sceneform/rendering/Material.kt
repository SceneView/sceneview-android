package com.google.ar.sceneform.rendering

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.ar.sceneform.resources.ResourceRegistry
import com.google.ar.sceneform.utilities.AndroidPreconditions
import com.google.ar.sceneform.utilities.LoadHelper
import io.github.sceneview.collision.Preconditions
import com.google.ar.sceneform.utilities.SceneformBufferUtils
import io.github.sceneview.collision.Vector3
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * Represents a reference to a material.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class Material {
    private val materialParameters = MaterialParameters()

    protected var lifecycle: Lifecycle? = null

    private val filamentMaterial: com.google.android.filament.Material?
    @JvmField val filamentMaterialInstance: MaterialInstance

    /**
     * Creates a new instance of this Material.
     *
     * The new material will have a unique copy of the material parameters that can be changed
     * independently. The getFilamentEngine material resource is immutable and will be shared between
     * instances.
     */
    fun makeCopy(): Material = Material(this)

    fun setBoolean(name: String, x: Boolean) {
        materialParameters.setBoolean(name, x)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setBoolean2(name: String, x: Boolean, y: Boolean) {
        materialParameters.setBoolean2(name, x, y)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setBoolean3(name: String, x: Boolean, y: Boolean, z: Boolean) {
        materialParameters.setBoolean3(name, x, y, z)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setBoolean4(name: String, x: Boolean, y: Boolean, z: Boolean, w: Boolean) {
        materialParameters.setBoolean4(name, x, y, z, w)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setFloat(name: String, x: Float) {
        materialParameters.setFloat(name, x)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setFloat2(name: String, x: Float, y: Float) {
        materialParameters.setFloat2(name, x, y)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setFloat3(name: String, x: Float, y: Float, z: Float) {
        materialParameters.setFloat3(name, x, y, z)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setFloat3(name: String, value: Vector3) {
        materialParameters.setFloat3(name, value)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setFloat3(name: String, color: Color) {
        materialParameters.setFloat3(name, color.r, color.g, color.b)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) {
        materialParameters.setFloat4(name, x, y, z, w)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setFloat4(name: String, color: Color) {
        materialParameters.setFloat4(name, color.r, color.g, color.b, color.a)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setInt(name: String, x: Int) {
        materialParameters.setInt(name, x)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setInt2(name: String, x: Int, y: Int) {
        materialParameters.setInt2(name, x, y)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setInt3(name: String, x: Int, y: Int, z: Int) {
        materialParameters.setInt3(name, x, y, z)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setInt4(name: String, x: Int, y: Int, z: Int, w: Int) {
        materialParameters.setInt4(name, x, y, z, w)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setTexture(name: String, texture: Texture) {
        materialParameters.setTexture(name, texture)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun setBaseColorTexture(texture: Texture) {
        // Set the baseColorIndex to 0 if no existing texture was set
        setInt("baseColorIndex", 0)
        setTexture("baseColorMap", texture)
    }

    /**
     * Sets a [Texture] to a parameter of the type 'sampler2d' on this material.
     *
     * @param name the name of the parameter in the material
     * @param depthTexture the texture to set
     */
    fun setDepthTexture(name: String, depthTexture: com.google.android.filament.Texture) {
        materialParameters.setDepthTexture(name, depthTexture)
        materialParameters.applyParameterTo(filamentMaterialInstance, name)
    }

    /**
     * Sets an [ExternalTexture] to a parameter of type 'samplerExternal' on this material.
     *
     * @param name the name of the parameter in the material
     * @param externalTexture the texture to set
     */
    fun setExternalTexture(name: String, externalTexture: ExternalTexture) {
        materialParameters.setExternalTexture(name, externalTexture)
        materialParameters.applyTo(filamentMaterialInstance)
    }

    fun getExternalTexture(name: String): ExternalTexture? = materialParameters.getExternalTexture(name)

    internal fun copyMaterialParameters(materialParameters: MaterialParameters) {
        this.materialParameters.copyFrom(materialParameters)
        this.materialParameters.applyTo(filamentMaterialInstance)
    }

    fun getFilamentMaterial(): com.google.android.filament.Material =
        filamentMaterial ?: filamentMaterialInstance.material

    fun getFilamentMaterialInstance(): MaterialInstance {
        // Filament Material Instance is only set to null when it is disposed or destroyed, so any
        // usage after that point is an internal error.
        return filamentMaterialInstance
    }

    constructor(filamentMaterial: com.google.android.filament.Material) : this(filamentMaterial, false)

    constructor(filamentMaterial: com.google.android.filament.Material, useDefaultInstance: Boolean) {
        this.lifecycle = lifecycle
        this.filamentMaterial = filamentMaterial
        this.filamentMaterialInstance = if (useDefaultInstance) filamentMaterial.defaultInstance else filamentMaterial.createInstance()
    }

    constructor(lifecycle: Lifecycle?, materialInstance: MaterialInstance) {
        this.lifecycle = lifecycle
        this.filamentMaterial = materialInstance.material
        this.filamentMaterialInstance = materialInstance
    }

    private constructor(other: Material) : this(other.filamentMaterial!!) {
        copyMaterialParameters(other.materialParameters)
    }

    /**
     * Builder for constructing a [Material]
     *
     * @hide We do not support custom materials in version 1.0 and use a Material Factory to create
     * new materials, so there is no need to expose a builder.
     */
    class Builder internal constructor() {
        /**
         * The [Material] will be constructed from the contents of this buffer
         */
        var sourceBuffer: ByteBuffer? = null
        /**
         * The [Material] will be constructed from the contents of this callable
         */
        private var inputStreamCreator: Callable<InputStream>? = null
        /**
         * The [Material] will be constructed from an existing filament material.
         */
        var existingMaterial: com.google.android.filament.Material? = null

        private var registryId: Any? = null

        /**
         * Allows a [Material] to be created with data.
         *
         * Construction will be immediate. Please use [setRegistryId] to register this
         * material for reuse.
         *
         * @param materialBuffer Sets the material data.
         * @return [Builder] for chaining setup calls
         */
        fun setSource(materialBuffer: ByteBuffer): Builder {
            Preconditions.checkNotNull(materialBuffer, "Parameter \"materialBuffer\" was null.")
            inputStreamCreator = null
            sourceBuffer = materialBuffer
            return this
        }

        /**
         * Allows a [Material] to be constructed from [Uri]. Construction will be asynchronous.
         *
         * @param context Sets the [Context] used for loading the resource
         * @param sourceUri Sets a remote Uri or android resource Uri.
         * @return [Builder] for chaining setup calls
         */
        fun setSource(context: Context, sourceUri: Uri): Builder {
            Preconditions.checkNotNull(sourceUri, "Parameter \"sourceUri\" was null.")
            registryId = sourceUri
            inputStreamCreator = LoadHelper.fromUri(context, sourceUri)
            sourceBuffer = null
            return this
        }

        /**
         * Allows a [Material] to be constructed from resource.
         *
         * @param context Sets the [Context] used for loading the resource
         * @param resource an android resource with raw type.
         * @return [Builder] for chaining setup calls
         */
        fun setSource(context: Context, resource: Int): Builder {
            registryId = context.resources.getResourceName(resource)
            inputStreamCreator = LoadHelper.fromResource(context, resource)
            sourceBuffer = null
            return this
        }

        /**
         * Allows a [Material] to be constructed via callable function.
         *
         * @param inputStreamCreator Supplies an [InputStream] with the [Material] data
         * @return [Builder] for chaining setup calls
         */
        fun setSource(inputStreamCreator: Callable<InputStream>): Builder {
            Preconditions.checkNotNull(inputStreamCreator, "Parameter \"sourceInputStreamCallable\" was null.")
            this.inputStreamCreator = inputStreamCreator
            sourceBuffer = null
            return this
        }

        /**
         * Allows a [Material] to be reused.
         *
         * @param registryId allows the function to be skipped and a previous material to be re-used
         * @return [Builder] for chaining setup calls
         */
        fun setRegistryId(registryId: Any): Builder {
            this.registryId = registryId
            return this
        }

        /**
         * Creates a new [Material] based on the parameters set previously.
         */
        @Suppress("AndroidApiChecker")
        fun build(engine: Engine): CompletableFuture<Material> {
            try {
                checkPreconditions()
            } catch (failedPrecondition: Throwable) {
                val result = CompletableFuture<Material>()
                result.completeExceptionally(failedPrecondition)
                FutureHelper.logOnException(
                    TAG, result, "Unable to load Material registryId='$registryId'"
                )
                return result
            }

            // For static-analysis check.
            val registryId = this.registryId
            if (registryId != null) {
                val registry: ResourceRegistry<Material> = ResourceManager.getInstance().getMaterialRegistry()
                val materialFuture: CompletableFuture<Material>? = registry.get(registryId)
                if (materialFuture != null) {
                    return materialFuture.thenApply { material -> material.makeCopy() }
                }
            }

            val sourceBuffer = this.sourceBuffer
            if (sourceBuffer != null) {
                val filamentMaterial = createFilamentMaterial(engine, sourceBuffer)
                val material = Material(filamentMaterial)

                if (registryId != null) {
                    val registry: ResourceRegistry<Material> = ResourceManager.getInstance().getMaterialRegistry()
                    registry.register(registryId, CompletableFuture.completedFuture(material))
                }

                val result = CompletableFuture.completedFuture(material.makeCopy())
                FutureHelper.logOnException(TAG, result, "Unable to load Material registryId='$registryId'")
                return result
            }

            val existingMaterial = this.existingMaterial
            if (existingMaterial != null) {
                val material = Material(existingMaterial)

                if (registryId != null) {
                    val registry: ResourceRegistry<Material> = ResourceManager.getInstance().getMaterialRegistry()
                    registry.register(registryId, CompletableFuture.completedFuture(material.makeCopy()))
                }

                val result = CompletableFuture.completedFuture(material)
                FutureHelper.logOnException(TAG, result, "Unable to load Material registryId='$registryId'")
                return result
            }

            // For static-analysis check.
            val inputStreamCallable = this.inputStreamCreator
            if (inputStreamCallable == null) {
                val result = CompletableFuture<Material>()
                result.completeExceptionally(AssertionError("Input Stream Creator is null."))
                return result
            }

            val result = CompletableFuture.supplyAsync(
                {
                    val byteBuffer: ByteBuffer?
                    try {
                        inputStreamCallable.call().use { inputStream ->
                            byteBuffer = SceneformBufferUtils.readStream(inputStream)
                        }
                    } catch (e: Exception) {
                        throw CompletionException(e)
                    }

                    if (byteBuffer == null) {
                        throw IllegalStateException("Unable to read data from input stream.")
                    }

                    byteBuffer
                },
                ThreadPools.getThreadPoolExecutor()
            ).thenApplyAsync(
                { byteBuffer -> Material(createFilamentMaterial(engine, byteBuffer)) },
                ThreadPools.getMainExecutor()
            )

            if (registryId != null) {
                val registry: ResourceRegistry<Material> = ResourceManager.getInstance().getMaterialRegistry()
                registry.register(registryId, result)
            }

            return result.thenApply { material -> material.makeCopy() }
        }

        private fun checkPreconditions() {
            AndroidPreconditions.checkUiThread()
            if (!hasSource()) {
                throw AssertionError("Material must have a source.")
            }
        }

        private fun hasSource(): Boolean =
            inputStreamCreator != null || sourceBuffer != null || existingMaterial != null

        private fun createFilamentMaterial(engine: Engine, sourceBuffer: ByteBuffer): com.google.android.filament.Material {
            return try {
                com.google.android.filament.Material.Builder()
                    .payload(sourceBuffer, sourceBuffer.limit())
                    .build(engine)
            } catch (e: Exception) {
                throw IllegalArgumentException("Unable to create material from source byte buffer.", e)
            }
        }
    }

    companion object {
        private val TAG = Material::class.java.simpleName

        /**
         * Constructs a [Material]
         *
         * @hide
         */
        @JvmStatic
        fun builder(): Builder {
            AndroidPreconditions.checkMinAndroidApiLevel()
            return Builder()
        }
    }
}
