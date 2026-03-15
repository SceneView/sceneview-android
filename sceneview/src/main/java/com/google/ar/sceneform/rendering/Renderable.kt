package com.google.ar.sceneform.rendering

import android.content.Context
import android.net.Uri
import androidx.annotation.IntRange
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.ResourceLoader
import io.github.sceneview.collision.Box
import io.github.sceneview.collision.ChangeId
import io.github.sceneview.collision.CollisionShape
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.TransformProvider
import com.google.ar.sceneform.resources.ResourceRegistry
import com.google.ar.sceneform.utilities.AndroidPreconditions
import com.google.ar.sceneform.utilities.LoadHelper
import io.github.sceneview.collision.Preconditions
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Function

/*
 * ###########!!!!!!!!!!!!!!!!################
 * ###########!!!!!!!!!!!!!!!!################
 *          PLEASE KILL ME !!!!!!
 * ###########!!!!!!!!!!!!!!!!################
 *
 * I had a great life and I even changed
 * from sfb to filament but I'm too old now.
 * Someone please move me to only Filament
 * in order for me to start a new life.
 *
 * Maybe by directly adding the FilamentAsset
 * to the ModelNode class and exclude the
 * renderableData nightmare from my variables
 *
 * ###########!!!!!!!!!!!!!!!!################
 * ;-)
 * ###########!!!!!!!!!!!!!!!!################
 * ###########!!!!!!!!!!!!!!!!################
 */

/**
 * Base class for rendering in 3D space by attaching to a [io.github.sceneview.node.Node].
 */
@Suppress("AndroidApiChecker")
abstract class Renderable {
    // Data that can be shared between Renderables with makeCopy()
    private val renderableData: IRenderableInternalData

    internal var asyncLoadEnabled: Boolean = false

    // Data that is unique per-Renderable.
    private val materialBindings = ArrayList<MaterialInstance>()
    private val materialNames = ArrayList<String>()
    private var renderPriority = RENDER_PRIORITY_DEFAULT
    private var isShadowCaster = true
    private var isShadowReceiver = true
    // The number of frames per seconds defined in the asset
    private var animationFrameRate: Int = 0
    var collisionShape: CollisionShape? = null

    private val changeId = ChangeId()

    protected constructor(builder: Builder<out Renderable, out Builder<*, *>>) {
        Preconditions.checkNotNull(builder, "Parameter \"builder\" was null.")
        renderableData = when {
            builder.isFilamentAsset -> RenderableInternalFilamentAssetData()
            builder.isGltf -> createRenderableInternalGltfData()!!
            else -> RenderableInternalData()
        }
        if (builder.definition != null) {
            updateFromDefinition(builder.definition!!)
        }
        asyncLoadEnabled = builder.asyncLoadEnabled
        animationFrameRate = builder.animationFrameRate
    }

    protected constructor(other: Renderable) {
        if (other.getId().isEmpty()) {
            throw AssertionError("Cannot copy uninitialized Renderable.")
        }

        // Share renderableData with the original Renderable.
        renderableData = other.renderableData

        // Copy materials.
        Preconditions.checkState(other.materialNames.size == other.materialBindings.size)
        for (i in other.materialBindings.indices) {
            val otherMaterial = other.materialBindings[i]
            materialBindings.add(otherMaterial.material.createInstance())
            materialNames.add(other.materialNames[i])
        }

        renderPriority = other.renderPriority
        isShadowCaster = other.isShadowCaster
        isShadowReceiver = other.isShadowReceiver

        // Copy collision shape.
        collisionShape = other.collisionShape?.makeCopy()

        asyncLoadEnabled = other.asyncLoadEnabled
        animationFrameRate = other.animationFrameRate

        changeId.update()
    }

    /** Returns the material bound to the first submesh. */
    fun getMaterial(): MaterialInstance = getMaterial(0)

    /** Returns the material bound to the specified submesh. */
    fun getMaterial(submeshIndex: Int): MaterialInstance {
        if (submeshIndex < materialBindings.size) {
            return materialBindings[submeshIndex]
        }
        throw makeSubmeshOutOfRangeException(submeshIndex)
    }

    /** Sets the material bound to the first submesh. */
    fun setMaterial(material: MaterialInstance) {
        setMaterial(0, material)
    }

    /** Sets the material bound to the specified submesh. */
    fun setMaterial(submeshIndex: Int, material: MaterialInstance) {
        if (submeshIndex < materialBindings.size) {
            materialBindings[submeshIndex] = material
            changeId.update()
        } else {
            throw makeSubmeshOutOfRangeException(submeshIndex)
        }
    }

    /**
     * Returns the name associated with the specified submesh.
     *
     * @throws IllegalArgumentException if the index is out of range
     */
    fun getSubmeshName(submeshIndex: Int): String {
        Preconditions.checkState(materialNames.size == materialBindings.size)
        if (submeshIndex >= 0 && submeshIndex < materialNames.size) {
            return materialNames[submeshIndex]
        }
        throw makeSubmeshOutOfRangeException(submeshIndex)
    }

    /**
     * Get the render priority that controls the order of rendering. The priority is between a range
     * of 0 (rendered first) and 7 (rendered last). The default value is 4.
     */
    fun getRenderPriority(): Int = renderPriority

    /**
     * Set the render priority to control the order of rendering. The priority is between a range of 0
     * (rendered first) and 7 (rendered last). The default value is 4.
     */
    fun setRenderPriority(
        @IntRange(from = RENDER_PRIORITY_FIRST.toLong(), to = RENDER_PRIORITY_LAST.toLong()) renderPriority: Int
    ) {
        this.renderPriority = renderPriority.coerceIn(RENDER_PRIORITY_FIRST, RENDER_PRIORITY_LAST)
        changeId.update()
    }

    /** Returns true if configured to cast shadows on other renderables. */
    fun isShadowCaster(): Boolean = isShadowCaster

    /** Sets whether the renderable casts shadow on other renderables in the scene. */
    fun setShadowCaster(isShadowCaster: Boolean) {
        this.isShadowCaster = isShadowCaster
        changeId.update()
    }

    /** Returns true if configured to receive shadows cast by other renderables. */
    fun isShadowReceiver(): Boolean = isShadowReceiver

    /** Sets whether the renderable receives shadows cast by other renderables in the scene. */
    fun setShadowReceiver(isShadowReceiver: Boolean) {
        this.isShadowReceiver = isShadowReceiver
        changeId.update()
    }

    /** Gets the number of frames per seconds defined in the asset animation. */
    fun getAnimationFrameRate(): Int = animationFrameRate

    /** Returns the number of submeshes that this renderable has. All Renderables have at least one. */
    fun getSubmeshCount(): Int = renderableData.getMeshes().size

    /** @hide */
    fun getId(): ChangeId = changeId

    /** @hide */
    fun createInstance(
        engine: Engine,
        assetLoader: AssetLoader,
        resourceLoader: ResourceLoader,
        transformProvider: TransformProvider
    ): RenderableInstance {
        return RenderableInstance(engine, assetLoader, resourceLoader, transformProvider, this)
    }

    fun updateFromDefinition(definition: RenderableDefinition) {
        Preconditions.checkState(!definition.getSubmeshes().isEmpty())

        changeId.update()

        definition.applyDefinitionToData(renderableData, materialBindings, materialNames)

        collisionShape = Box(renderableData.getSizeAabb(), renderableData.getCenterAabb())
    }

    /**
     * Creates a new instance of this Renderable.
     *
     * The new renderable will have unique copy of all mutable state. All materials referenced by
     * the Renderable will also be instanced. Immutable data will be shared between the instances.
     */
    abstract fun makeCopy(): Renderable

    fun getRenderableData(): IRenderableInternalData = renderableData

    fun getMaterialBindings(): ArrayList<MaterialInstance> = materialBindings

    fun getMaterialNames(): ArrayList<String> = materialNames

    /**
     * Optionally override in subclasses for work that must be done each frame for specific types of
     * Renderables.
     */
    open fun prepareForDraw(engine: Engine) {}

    /**
     * Gets the final model matrix to use for rendering this [Renderable] based on the matrix
     * passed in. Default implementation simply passes through the original matrix.
     *
     * @hide
     */
    open fun getFinalModelMatrix(originalMatrix: Matrix): Matrix {
        Preconditions.checkNotNull(originalMatrix, "Parameter \"originalMatrix\" was null.")
        return originalMatrix
    }

    private fun makeSubmeshOutOfRangeException(submeshIndex: Int): IllegalArgumentException {
        return IllegalArgumentException(
            "submeshIndex ($submeshIndex) is out of range. It must be less than the submeshCount (${getSubmeshCount()})."
        )
    }

    private fun createRenderableInternalGltfData(): IRenderableInternalData? = null

    /**
     * Used to programmatically construct a [Renderable]. Builder data is stored, not copied.
     */
    @Suppress("AndroidApiChecker")
    abstract class Builder<T : Renderable, B : Builder<T, B>> {
        /** @hide */
        var registryId: Any? = null
        /** @hide */
        var context: Context? = null

        private var sourceUri: Uri? = null
        var inputStreamCreator: Callable<InputStream>? = null
        var definition: RenderableDefinition? = null
        var isGltf = false
        var isFilamentAsset = false
        var asyncLoadEnabled = false
        private var loadGltfListener: LoadGltfListener? = null
        private var uriResolver: Function<String, Uri>? = null
        private var materialsBytes: ByteArray? = null

        var animationFrameRate: Int = DEFAULT_ANIMATION_FRAME_RATE

        protected constructor()

        fun setSource(context: Context, inputStreamCreator: Callable<InputStream>): B {
            Preconditions.checkNotNull(inputStreamCreator)
            this.sourceUri = null
            this.inputStreamCreator = inputStreamCreator
            this.context = context
            return getSelf()
        }

        fun setSource(context: Context, sourceUri: Uri): B {
            return setSource(context, sourceUri) { source ->
                val url = sourceUri.toString().let { if (it.endsWith("/")) it.dropLast(1) else it }
                val lastSeparatorIndex = url.lastIndexOf('/')
                Uri.parse(
                    (if (lastSeparatorIndex != -1) url.substring(0, lastSeparatorIndex + 1) else "") + source
                )
            }
        }

        fun setSource(context: Context, sourceUri: Uri, uriResolver: Function<String, Uri>): B {
            this.uriResolver = uriResolver
            return setRemoteSourceHelper(context, sourceUri, true)
        }

        fun setSource(context: Context, resource: Int): B {
            this.inputStreamCreator = LoadHelper.fromResource(context, resource)
            this.context = context

            val uri = LoadHelper.resourceToUri(context, resource)
            this.sourceUri = uri
            this.registryId = uri
            return getSelf()
        }

        /** Build a [Renderable] from a [RenderableDefinition]. */
        fun setSource(definition: RenderableDefinition): B {
            this.definition = definition
            registryId = null
            sourceUri = null
            return getSelf()
        }

        fun setRegistryId(registryId: Any?): B {
            this.registryId = registryId
            return getSelf()
        }

        fun setIsFilamentGltf(isFilamentGltf: Boolean): B {
            this.isFilamentAsset = isFilamentGltf
            return getSelf()
        }

        /**
         * Enable textures async loading after first rendering.
         * Default is false.
         */
        fun setAsyncLoadEnabled(asyncLoadEnabled: Boolean): B {
            this.asyncLoadEnabled = asyncLoadEnabled
            return getSelf()
        }

        /**
         * Sets the number of frames per seconds defined in the asset.
         *
         * @param frameRate The number of frames during one second
         */
        fun setAnimationFrameRate(frameRate: Int): B {
            this.animationFrameRate = frameRate
            return getSelf()
        }

        /**
         * True if a source function will be called during build
         *
         * @hide
         */
        fun hasSource(): Boolean = sourceUri != null || inputStreamCreator != null || definition != null

        /**
         * Constructs a [Renderable] with the parameters of the builder.
         *
         * @return the constructed [Renderable]
         */
        open fun build(engine: Engine): CompletableFuture<T> {
            try {
                checkPreconditions()
            } catch (failedPrecondition: Throwable) {
                val result = CompletableFuture<T>()
                result.completeExceptionally(failedPrecondition)
                FutureHelper.logOnException(
                    getRenderableClass().simpleName,
                    result,
                    "Unable to load Renderable registryId='$registryId'"
                )
                return result
            }

            // For static-analysis check.
            val registryId = this.registryId
            if (registryId != null) {
                val registry: ResourceRegistry<T> = getRenderableRegistry()
                val renderableFuture = registry.get(registryId)
                if (renderableFuture != null) {
                    return renderableFuture.thenApply { renderable ->
                        getRenderableClass().cast(renderable.makeCopy())
                    }
                }
            }

            val renderable = makeRenderable(engine)

            if (definition != null) {
                return CompletableFuture.completedFuture(renderable)
            }

            // For static-analysis check.
            val inputStreamCreator = this.inputStreamCreator
            if (inputStreamCreator == null) {
                val result = CompletableFuture<T>()
                result.completeExceptionally(AssertionError("Input Stream Creator is null."))
                FutureHelper.logOnException(
                    getRenderableClass().simpleName,
                    result,
                    "Unable to load Renderable registryId='$registryId'"
                )
                return result
            }

            var result: CompletableFuture<T>? = null
            if (isFilamentAsset) {
                val ctx = context
                    ?: throw AssertionError("Gltf Renderable.Builder must have a valid context.")
                result = loadRenderableFromFilamentGltf(ctx, renderable)
            } else if (isGltf) {
                val ctx = context
                    ?: throw AssertionError("Gltf Renderable.Builder must have a valid context.")
                result = loadRenderableFromGltf(ctx, renderable, materialsBytes)
            }

            if (registryId != null) {
                val registry: ResourceRegistry<T> = getRenderableRegistry()
                registry.register(registryId, result!!)
            }

            FutureHelper.logOnException(
                getRenderableClass().simpleName,
                result!!,
                "Unable to load Renderable registryId='$registryId'"
            )
            return result.thenApply { resultRenderable ->
                getRenderableClass().cast(resultRenderable.makeCopy())
            }
        }

        protected open fun checkPreconditions() {
            AndroidPreconditions.checkUiThread()

            if (!hasSource()) {
                throw AssertionError("ModelRenderable must have a source.")
            }
        }

        private fun setRemoteSourceHelper(context: Context, sourceUri: Uri, enableCaching: Boolean): B {
            Preconditions.checkNotNull(sourceUri)
            this.sourceUri = sourceUri
            this.context = context
            this.registryId = sourceUri
            // Configure caching.
            if (enableCaching) {
                setCachingEnabled(context)
            }

            val connectionProperties = HashMap<String, String>()
            if (!enableCaching) {
                connectionProperties["Cache-Control"] = "no-cache"
            } else {
                connectionProperties["Cache-Control"] = "max-stale=$DEFAULT_MAX_STALE_CACHE"
            }
            this.inputStreamCreator = LoadHelper.fromUri(
                context, Preconditions.checkNotNull(this.sourceUri), connectionProperties
            )
            return getSelf()
        }

        private fun loadRenderableFromGltf(
            context: Context,
            renderable: T,
            materialsBytes: ByteArray?
        ): CompletableFuture<T>? = null

        private fun loadRenderableFromFilamentGltf(
            context: Context,
            renderable: T
        ): CompletableFuture<T> {
            val loader = LoadRenderableFromFilamentGltfTask(
                renderable, context, Preconditions.checkNotNull(sourceUri), uriResolver
            )
            return loader.downloadAndProcessRenderable(Preconditions.checkNotNull(inputStreamCreator))
        }

        private fun setCachingEnabled(context: Context) = Unit

        protected abstract fun makeRenderable(engine: Engine): T

        protected abstract fun getRenderableClass(): Class<T>

        protected abstract fun getRenderableRegistry(): ResourceRegistry<T>

        protected abstract fun getSelf(): B
    }

    companion object {
        const val RENDER_PRIORITY_DEFAULT = 4
        const val RENDER_PRIORITY_FIRST = 0
        const val RENDER_PRIORITY_LAST = 7
        // Allow stale data two weeks old by default.
        private val DEFAULT_MAX_STALE_CACHE = TimeUnit.DAYS.toSeconds(14)
        // The default number of frames per seconds for this renderable animation
        const val DEFAULT_ANIMATION_FRAME_RATE = 24
    }
}
