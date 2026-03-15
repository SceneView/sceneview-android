package com.google.ar.sceneform.rendering

import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.Size
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityInstance
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.TransformManager
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import io.github.sceneview.animation.AnimatableModel
import io.github.sceneview.animation.ModelAnimation
import io.github.sceneview.collision.Box
import io.github.sceneview.collision.ChangeId
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.Preconditions
import io.github.sceneview.collision.TransformProvider
import io.github.sceneview.collision.Vector3
import com.google.ar.sceneform.utilities.LoadHelper
import com.google.ar.sceneform.utilities.SceneformBufferUtils
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.concurrent.Callable
import java.util.function.Function

/**
 * Controls how a [Renderable] is displayed. There can be multiple RenderableInstances
 * displaying a single Renderable.
 *
 * @hide
 */
@Suppress("AndroidJdkLibsChecker")
class RenderableInstance(
    private val engine: Engine,
    private val assetLoader: AssetLoader,
    private val resourceLoader: ResourceLoader,
    private val transformProvider: TransformProvider,
    private val renderable: Renderable
) : AnimatableModel {

    /**
     * Interface for modifying the bone transforms for this specific RenderableInstance.
     */
    interface SkinningModifier {
        /**
         * Takes the original boneTransforms and output new boneTransforms used to render the mesh.
         *
         * @param originalBuffer contains the bone transforms from the current animation state of the
         *   skeleton, buffer is read only
         */
        fun modifyMaterialBoneTransformsBuffer(originalBuffer: FloatBuffer): FloatBuffer

        fun isModifiedSinceLastRender(): Boolean
    }

    @Entity
    var entity: Int = 0
    @Entity
    private var childEntity: Int = 0
    @JvmField
    var renderableId: Int = ChangeId.EMPTY_ID

    var filamentAsset: FilamentAsset? = null
    var filamentAnimator: Animator? = null

    private var animations = ArrayList<ModelAnimation>()

    private var skinningModifier: SkinningModifier? = null

    private var renderPriority = Renderable.RENDER_PRIORITY_DEFAULT
    private var isShadowCaster = true
    private var isShadowReceiver = true

    private var materialBindings: ArrayList<MaterialInstance>
    private var materialNames: ArrayList<String>

    private var cachedRelativeTransform: Matrix? = null
    private var cachedRelativeTransformInverse: Matrix? = null

    init {
        Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.")
        Preconditions.checkNotNull(renderable, "Parameter \"renderable\" was null.")
        this.materialBindings = ArrayList(renderable.getMaterialBindings())
        this.materialNames = ArrayList(renderable.getMaterialNames())
        entity = createFilamentEntity(engine)

        val relativeTransform = getRelativeTransform()
        if (relativeTransform != null) {
            childEntity = createFilamentChildEntity(engine, entity, relativeTransform)
        }

        createGltfModelInstance()
        createFilamentAssetModelInstance()
    }

    fun createFilamentAssetModelInstance() {
        if (renderable.getRenderableData() is RenderableInternalFilamentAssetData) {
            val renderableData = renderable.getRenderableData() as RenderableInternalFilamentAssetData

            val createdAsset = assetLoader.createAsset(renderableData.gltfByteBuffer!!)
                ?: throw IllegalStateException("Failed to load gltf")

            if (renderable.collisionShape == null) {
                val box = createdAsset.boundingBox
                val halfExtent = box.halfExtent
                val center = box.center
                renderable.collisionShape = Box(
                    Vector3(halfExtent[0], halfExtent[1], halfExtent[2]).scaled(2.0f),
                    Vector3(center[0], center[1], center[2])
                )
            }

            val urlResolver: Function<String, Uri>? = renderableData.urlResolver
            for (uri in createdAsset.resourceUris) {
                if (urlResolver == null) {
                    Log.e(TAG, "Failed to download uri $uri no url resolver.")
                    continue
                }
                val dataUri = urlResolver.apply(uri)
                try {
                    val callable: Callable<InputStream> = LoadHelper.fromUri(renderableData.context!!, dataUri)
                    resourceLoader.addResourceData(
                        uri, ByteBuffer.wrap(SceneformBufferUtils.inputStreamCallableToByteArray(callable))
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download data uri $dataUri", e)
                }
            }

            if (renderable.asyncLoadEnabled) {
                resourceLoader.asyncBeginLoad(createdAsset)
            } else {
                resourceLoader.loadResources(createdAsset)
            }

            val renderableManager = engine.getRenderableManager()

            this.materialBindings.clear()
            this.materialNames.clear()
            for (entityItem in createdAsset.entities) {
                @EntityInstance val renderableInstance = renderableManager.getInstance(entityItem)
                if (renderableInstance == 0) {
                    continue
                }
                val materialInstance = renderableManager.getMaterialInstanceAt(renderableInstance, 0)
                materialNames.add(materialInstance.name)
                materialBindings.add(materialInstance)
            }

            val transformManager: TransformManager = engine.getTransformManager()

            @EntityInstance val rootInstance = transformManager.getInstance(createdAsset.root)
            @EntityInstance val parentInstance = transformManager.getInstance(if (childEntity == 0) entity else childEntity)

            transformManager.setParent(rootInstance, parentInstance)

            filamentAsset = createdAsset

            setRenderPriority(renderable.getRenderPriority())
            setShadowCaster(renderable.isShadowCaster())
            setShadowReceiver(renderable.isShadowReceiver())

            filamentAnimator = createdAsset.getInstance().getAnimator()
            animations = ArrayList()
            for (i in 0 until filamentAnimator!!.animationCount) {
                animations.add(
                    ModelAnimation(
                        this,
                        filamentAnimator!!.getAnimationName(i),
                        i,
                        filamentAnimator!!.getAnimationDuration(i),
                        getRenderable().getAnimationFrameRate()
                    )
                )
            }
        }
    }

    fun createGltfModelInstance() { return }

    /**
     * Get the [Renderable] to display for this [RenderableInstance].
     *
     * @return [Renderable] asset, usually a 3D model.
     */
    fun getRenderable(): Renderable = renderable

    fun getChildEntities(): IntArray = filamentAsset?.entities ?: IntArray(0)

    @Entity
    fun getRenderedEntity(): Int = if (childEntity == 0) entity else childEntity

    fun setModelMatrix(transformManager: TransformManager, @Size(min = 16) transform: FloatArray) {
        // Use entity, rather than childEntity; setting the latter would slam the local transform which
        // corrects for scaling and offset.
        @EntityInstance val instance = transformManager.getInstance(entity)
        transformManager.setTransform(instance, transform)
    }

    /**
     * Get the render priority that controls the order of rendering.
     */
    fun getRenderPriority(): Int = renderPriority

    /**
     * Set the render priority to control the order of rendering.
     */
    fun setRenderPriority(
        @IntRange(from = Renderable.RENDER_PRIORITY_FIRST.toLong(), to = Renderable.RENDER_PRIORITY_LAST.toLong())
        renderPriority: Int
    ) {
        this.renderPriority = renderPriority.coerceIn(Renderable.RENDER_PRIORITY_FIRST, Renderable.RENDER_PRIORITY_LAST)
        val renderableManager = engine.getRenderableManager()
        val entities = filamentAsset!!.entities
        for (i in entities.indices) {
            @EntityInstance val renderableInstance = renderableManager.getInstance(entities[i])
            if (renderableInstance != 0) {
                renderableManager.setPriority(renderableInstance, this.renderPriority)
            }
        }
    }

    /**
     * Changes whether or not frustum culling is on.
     */
    fun setCulling(isCulling: Boolean) {
        val renderableManager = engine.getRenderableManager()
        @EntityInstance val renderableInstance = renderableManager.getInstance(entity)
        if (renderableInstance != 0 && renderableManager.hasComponent(renderableInstance)) {
            // renderableManager.setCulling(renderableInstance, isCulling)
        }
        val entities = filamentAsset!!.entities
        for (i in entities.indices) {
            val instance = renderableManager.getInstance(entities[i])
            if (instance != 0) {
                // renderableManager.setCulling(instance, isCulling)
            }
        }
    }

    /** Returns true if configured to cast shadows on other renderables. */
    fun isShadowCaster(): Boolean = isShadowCaster

    /** Sets whether the renderable casts shadow on other renderables in the scene. */
    fun setShadowCaster(isShadowCaster: Boolean) {
        this.isShadowCaster = isShadowCaster
        val renderableManager = engine.getRenderableManager()
        @EntityInstance val renderableInstance = renderableManager.getInstance(entity)
        if (renderableInstance != 0) {
            renderableManager.setCastShadows(renderableInstance, isShadowCaster)
        }

        val asset = filamentAsset ?: return
        val entities = asset.entities
        for (i in entities.indices) {
            val instance = renderableManager.getInstance(entities[i])
            if (instance != 0) {
                renderableManager.setCastShadows(instance, isShadowCaster)
            }
        }
    }

    /** Returns true if configured to receive shadows cast by other renderables. */
    fun isShadowReceiver(): Boolean = isShadowReceiver

    /** Sets whether the renderable receives shadows cast by other renderables in the scene. */
    fun setShadowReceiver(isShadowReceiver: Boolean) {
        this.isShadowReceiver = isShadowReceiver
        val renderableManager = engine.getRenderableManager()
        @EntityInstance val renderableInstance = renderableManager.getInstance(entity)
        if (renderableInstance != 0) {
            renderableManager.setReceiveShadows(renderableInstance, isShadowReceiver)
        }

        val asset = filamentAsset ?: return
        val entities = asset.entities
        for (i in entities.indices) {
            val instance = renderableManager.getInstance(entities[i])
            if (instance != 0) {
                renderableManager.setReceiveShadows(instance, isShadowReceiver)
            }
        }
    }

    fun getMaterialBindings(): ArrayList<MaterialInstance> = materialBindings

    fun getMaterialNames(): ArrayList<String> = materialNames

    /** Returns the material bound to the first submesh. */
    fun getMaterialInstance(): MaterialInstance = getMaterialInstance(0)!!

    /** Returns the number of materials. */
    fun getMaterialsCount(): Int = materialBindings.size

    /** Returns the material bound to the specified index. */
    fun getMaterialInstance(index: Int): MaterialInstance? {
        if (index < materialBindings.size) {
            return materialBindings[index]
        }
        return null
    }

    /** Returns the material bound to the specified name. */
    fun getMaterialInstance(name: String): MaterialInstance? {
        for (i in materialBindings.indices) {
            if (TextUtils.equals(materialNames[i], name)) {
                return materialBindings[i]
            }
        }
        return null
    }

    /** Sets the material bound to the first index. */
    fun setMaterialInstance(materialInstance: MaterialInstance) {
        setMaterialInstance(0, materialInstance)
    }

    /** Sets the material bound to the specified index. */
    fun setMaterial(@IntRange(from = 0) primitiveIndex: Int, material: Material) {
        setMaterialInstance(primitiveIndex, material.getFilamentMaterialInstance())
    }

    /** Sets the material bound to the specified index. */
    fun setMaterialInstance(@IntRange(from = 0) primitiveIndex: Int, materialInstance: MaterialInstance) {
        for (i in filamentAsset!!.entities.indices) {
            setMaterialInstance(i, primitiveIndex, materialInstance)
        }
    }

    /** Sets the material bound to the specified index and entityIndex */
    fun setMaterial(entityIndex: Int, @IntRange(from = 0) primitiveIndex: Int, material: Material) {
        setMaterialInstance(entityIndex, primitiveIndex, material.getFilamentMaterialInstance())
    }

    /** Sets the material bound to the specified index and entityIndex */
    fun setMaterialInstance(entityIndex: Int, @IntRange(from = 0) primitiveIndex: Int, materialInstance: MaterialInstance) {
        val entities = filamentAsset!!.entities
        Preconditions.checkElementIndex(entityIndex, entities.size, "No entity found at the given index")
        materialBindings[entityIndex] = materialInstance
        val renderableManager = engine.getRenderableManager()
        @EntityInstance val renderableInstance = renderableManager.getInstance(entities[entityIndex])
        if (renderableInstance != 0) {
            renderableManager.setMaterialInstanceAt(renderableInstance, primitiveIndex, materialInstance)
        }
    }

    /** Returns the name associated with the specified index. */
    fun getMaterialName(index: Int): String? {
        Preconditions.checkState(materialNames.size == materialBindings.size)
        if (index >= 0 && index < materialNames.size) {
            return materialNames[index]
        }
        return null
    }

    /** @hide */
    fun getWorldModelMatrix(): Matrix {
        return renderable.getFinalModelMatrix(transformProvider.getTransformationMatrix())
    }

    fun setSkinningModifier(skinningModifier: SkinningModifier?) {
        this.skinningModifier = skinningModifier
    }

    /**
     * Get the associated [ModelAnimation] at the given index or throw
     * an [IndexOutOfBoundsException].
     *
     * @param animationIndex Zero-based index for the animation of interest.
     */
    override fun getAnimation(animationIndex: Int): ModelAnimation {
        Preconditions.checkElementIndex(animationIndex, getAnimationCount(), "No animation found at the given index")
        return animations[animationIndex]
    }

    /** Returns the number of [ModelAnimation] definitions in the model. */
    override fun getAnimationCount(): Int = animations.size

    // We use our own Choreographer to update the animations so just return false (not applied)
    override fun applyAnimationChange(animation: ModelAnimation): Boolean = false

    private fun setupSkeleton(renderableInternalData: IRenderableInternalData) { return }

    /** @hide */
    fun prepareForDraw(engine: Engine) {
        renderable.prepareForDraw(engine)

        val changeId = renderable.getId()
        if (changeId.checkChanged(renderableId)) {
            val renderableInternalData = renderable.getRenderableData()
            setupSkeleton(renderableInternalData)
            renderableInternalData.buildInstanceData(engine, this, getRenderedEntity())
            renderableId = changeId.get()
            // First time we're rendering, so always update the skinning even if we aren't animating
            updateSkinning()
        } else {
            // Will only update the skinning if the renderable is animating or there is a skinModifier
            // that has been changed since the last draw.
            if (updateAnimations(false)) {
                updateSkinning()
            }
        }
    }

    /** Detach and destroy the instance */
    fun destroy() {
        if (filamentAsset != null) {
            try {
                assetLoader.destroyAsset(filamentAsset!!)
            } catch (e: Exception) { }
            filamentAsset = null
        }

        val renderableManager = engine.getRenderableManager()
        if (childEntity != 0) {
            try {
                renderableManager.destroy(childEntity)
            } catch (e: Exception) { }
            childEntity = 0
        }
        if (entity != 0) {
            try {
                renderableManager.destroy(entity)
            } catch (e: Exception) { }
            entity = 0
        }
    }

    /**
     * Returns the transform of this renderable relative to it's node.
     *
     * @hide
     */
    fun getRelativeTransform(): Matrix? {
        cachedRelativeTransform?.let { return it }

        val renderableData = renderable.getRenderableData()
        val scale = renderableData.getTransformScale()
        val offset = renderableData.getTransformOffset()
        if (scale == 1f && Vector3.equals(offset, Vector3.zero())) {
            return null
        }

        val matrix = Matrix()
        matrix.makeScale(scale)
        matrix.setTranslation(offset)
        cachedRelativeTransform = matrix
        return matrix
    }

    /**
     * Returns the inverse transform of this renderable relative to it's node.
     *
     * @hide
     */
    fun getRelativeTransformInverse(): Matrix? {
        cachedRelativeTransformInverse?.let { return it }

        val relativeTransform = getRelativeTransform() ?: return null

        val inverse = Matrix()
        Matrix.invert(relativeTransform, inverse)
        cachedRelativeTransformInverse = inverse
        return inverse
    }

    /**
     * Apply animations changes if force==true or the animation has dirty values.
     *
     * @param force Update even if the animation time position didn't changed.
     * @return true if any animation update has been made.
     */
    fun updateAnimations(force: Boolean): Boolean {
        var hasUpdate = false
        for (i in 0 until getAnimationCount()) {
            val animation = getAnimation(i)
            if (force || animation.isDirty) {
                filamentAnimator?.applyAnimation(i, animation.timePosition)
                animation.setDirty(false)
                hasUpdate = true
            }
        }
        return hasUpdate
    }

    /**
     * Computes root-to-node transforms for all bone nodes.
     */
    private fun updateSkinning() {
        filamentAnimator?.updateBoneMatrices()
    }

    fun setBlendOrderAt(index: Int, blendOrder: Int) {
        val renderableManager = engine.getRenderableManager()
        @EntityInstance val renderableInstance = renderableManager.getInstance(getRenderedEntity())
        renderableManager.setBlendOrderAt(renderableInstance, index, blendOrder)
    }

    companion object {
        private val TAG = RenderableInstance::class.java.simpleName

        @Entity
        private fun createFilamentEntity(engine: Engine): Int {
            @Entity val entity = EntityManager.get().create()
            engine.getTransformManager().create(entity)
            return entity
        }

        @Entity
        private fun createFilamentChildEntity(engine: Engine, @Entity entity: Int, relativeTransform: Matrix): Int {
            @Entity val childEntity = EntityManager.get().create()
            val transformManager = engine.getTransformManager()
            transformManager.create(
                childEntity,
                transformManager.getInstance(entity),
                relativeTransform.data
            )
            return childEntity
        }
    }
}
