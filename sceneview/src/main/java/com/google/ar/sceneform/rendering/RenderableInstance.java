package com.google.ar.sceneform.rendering;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.google.android.filament.Engine;
import com.google.android.filament.Entity;
import com.google.android.filament.EntityInstance;
import com.google.android.filament.EntityManager;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.TransformManager;
import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.ar.sceneform.animation.AnimatableModel;
import com.google.ar.sceneform.animation.ModelAnimation;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.common.TransformProvider;
import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.AndroidPreconditions;
import com.google.ar.sceneform.utilities.ChangeId;
import com.google.ar.sceneform.utilities.LoadHelper;
import com.google.ar.sceneform.utilities.Preconditions;
import com.google.ar.sceneform.utilities.SceneformBufferUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Controls how a {@link Renderable} is displayed. There can be multiple RenderableInstances
 * displaying a single Renderable.
 *
 * @hide
 */
@SuppressWarnings("AndroidJdkLibsChecker")
public class RenderableInstance implements AnimatableModel {

    /**
     * Interface for modifying the bone transforms for this specific RenderableInstance. Used by
     * SkeletonNode to make it possible to control a bone by moving a
     * node.
     */
    public interface SkinningModifier {

        /**
         * Takes the original boneTransforms and output new boneTransforms used to render the mesh.
         *
         * @param originalBuffer contains the bone transforms from the current animation state of the
         *                       skeleton, buffer is read only
         */
        FloatBuffer modifyMaterialBoneTransformsBuffer(FloatBuffer originalBuffer);

        boolean isModifiedSinceLastRender();
    }

    private static final String TAG = RenderableInstance.class.getSimpleName();

    private final TransformProvider transformProvider;
    private final Renderable renderable;
    @Nullable
    Renderer renderer;
    @Entity
    int entity = 0;
    @Entity
    private int childEntity = 0;
    public int renderableId = ChangeId.EMPTY_ID;

    @Nullable
    FilamentAsset filamentAsset;
    @Nullable
    Animator filamentAnimator;

    private ArrayList<ModelAnimation> animations = new ArrayList<>();

    @Nullable
    private SkinningModifier skinningModifier;

    private int renderPriority = Renderable.RENDER_PRIORITY_DEFAULT;
    private boolean isShadowCaster = true;
    private boolean isShadowReceiver = true;

    private ArrayList<Material> materialBindings;
    private ArrayList<String> materialNames;

    @Nullable
    private Matrix cachedRelativeTransform;
    @Nullable
    private Matrix cachedRelativeTransformInverse;

    @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
    public RenderableInstance(TransformProvider transformProvider, Renderable renderable) {
        Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.");
        Preconditions.checkNotNull(renderable, "Parameter \"renderable\" was null.");
        this.transformProvider = transformProvider;
        this.renderable = renderable;
        this.materialBindings = new ArrayList<>(renderable.getMaterialBindings());
        this.materialNames = new ArrayList<>(renderable.getMaterialNames());
        entity = createFilamentEntity(EngineInstance.getEngine());

        // SFB's can be imported with re-centering or scaling; rather than perform those operations to
        // the vertices (and bones, &c) at import time, we keep vertex data in the same unit as the
        // source asset and apply at runtime to a child entity via this relative transform.  If we get
        // back null, the relative transform is identity and the child entity path can be skipped.
        @Nullable Matrix relativeTransform = getRelativeTransform();
        if (relativeTransform != null) {
            childEntity =
                    createFilamentChildEntity(EngineInstance.getEngine(), entity, relativeTransform);
        }

        createGltfModelInstance();

        createFilamentAssetModelInstance();

        ResourceManager.getInstance()
                .getRenderableInstanceCleanupRegistry()
                .register(this, new CleanupCallback(entity, childEntity));
    }

    void createFilamentAssetModelInstance() {
        if (renderable.getRenderableData() instanceof RenderableInternalFilamentAssetData) {
            RenderableInternalFilamentAssetData renderableData =
                    (RenderableInternalFilamentAssetData) renderable.getRenderableData();

            Engine engine = EngineInstance.getEngine().getFilamentEngine();

            AssetLoader loader =
                    new AssetLoader(
                            engine,
                            RenderableInternalFilamentAssetData.getUberShaderLoader(),
                            EntityManager.get());

            FilamentAsset createdAsset = renderableData.isGltfBinary ? loader.createAssetFromBinary(renderableData.gltfByteBuffer)
                    : loader.createAssetFromJson(renderableData.gltfByteBuffer);

            if (createdAsset == null) {
                throw new IllegalStateException("Failed to load gltf");
            }

            if (renderable.collisionShape == null) {
                com.google.android.filament.Box box = createdAsset.getBoundingBox();
                float[] halfExtent = box.getHalfExtent();
                float[] center = box.getCenter();
                renderable.collisionShape =
                        new Box(
                                new Vector3(halfExtent[0], halfExtent[1], halfExtent[2]).scaled(2.0f),
                                new Vector3(center[0], center[1], center[2]));
            }

            Function<String, Uri> urlResolver = renderableData.urlResolver;
            for (String uri : createdAsset.getResourceUris()) {
                if (urlResolver == null) {
                    Log.e(TAG, "Failed to download uri " + uri + " no url resolver.");
                    continue;
                }
                Uri dataUri = urlResolver.apply(uri);
                try {
                    Callable<InputStream> callable = LoadHelper.fromUri(renderableData.context, dataUri);
                    renderableData.resourceLoader.addResourceData(
                            uri, ByteBuffer.wrap(SceneformBufferUtils.inputStreamCallableToByteArray(callable)));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to download data uri " + dataUri, e);
                }
            }

            if(renderable.asyncLoadEnabled) {
                renderableData.resourceLoader.asyncBeginLoad(createdAsset);
            } else {
                renderableData.resourceLoader.loadResources(createdAsset);
            }

            RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();

            this.materialBindings.clear();
            this.materialNames.clear();
            for (int entity : createdAsset.getEntities()) {
                @EntityInstance int renderableInstance = renderableManager.getInstance(entity);
                if (renderableInstance == 0) {
                    continue;
                }
                MaterialInstance materialInstance = renderableManager.getMaterialInstanceAt(renderableInstance, 0);
                materialNames.add(materialInstance.getName());

                MaterialInternalDataGltfImpl materialData = new MaterialInternalDataGltfImpl(materialInstance.getMaterial());
                Material material = new Material(materialData, true);
                material.updateGltfMaterialInstance(materialInstance);
                materialBindings.add(material);
            }

            TransformManager transformManager = EngineInstance.getEngine().getTransformManager();

            @EntityInstance int rootInstance = transformManager.getInstance(createdAsset.getRoot());
            @EntityInstance
            int parentInstance = transformManager.getInstance(childEntity == 0 ? entity : childEntity);

            transformManager.setParent(rootInstance, parentInstance);

            filamentAsset = createdAsset;

            setRenderPriority(renderable.getRenderPriority());
            setShadowCaster(renderable.isShadowCaster());
            setShadowReceiver(renderable.isShadowReceiver());

            filamentAnimator = createdAsset != null ? createdAsset.getAnimator() : null;
            animations = new ArrayList<>();
            for (int i = 0; i < filamentAnimator.getAnimationCount(); i++) {
                animations.add(new ModelAnimation(this, filamentAnimator.getAnimationName(i), i,
                        filamentAnimator.getAnimationDuration(i),
                        getRenderable().getAnimationFrameRate()));
            }
        }
    }

    void createGltfModelInstance() {
        return;
    }

    @Nullable
    public FilamentAsset getFilamentAsset() {
        return filamentAsset;
    }

    /**
     * <p>Animator is owned by <code>FilamentAsset</code> and can be used for two things:
     * <ul>
     * <li>Updating matrices in <code>TransformManager</code> components according to glTF <code>animation</code> definitions.</li>
     * <li>Updating bone matrices in <code>RenderableManager</code> components according to glTF <code>skin</code> definitions.</li>
     * </ul>
     * </p>
     */
    @Nullable
    Animator getFilamentAnimator() {
        return filamentAnimator;
    }

    /**
     * Get the {@link Renderable} to display for this {@link RenderableInstance}.
     *
     * @return {@link Renderable} asset, usually a 3D model.
     */
    public Renderable getRenderable() {
        return renderable;
    }

    public @Entity
    int getEntity() {
        return entity;
    }

    public @Entity int[] getChildEntities() {
        return filamentAsset!=null ? filamentAsset.getEntities() : new int[0];
    }

    public @Entity
    int getRenderedEntity() {
        return (childEntity == 0) ? entity : childEntity;
    }

    void setModelMatrix(TransformManager transformManager, @Size(min = 16) float[] transform) {
        // Use entity, rather than childEntity; setting the latter would slam the local transform which
        // corrects for scaling and offset.
        @EntityInstance int instance = transformManager.getInstance(entity);
        transformManager.setTransform(instance, transform);
    }

    /**
     * Get the render priority that controls the order of rendering. The priority is between a range
     * of 0 (rendered first) and 7 (rendered last). The default value is 4.
     */
    public int getRenderPriority() {
        return renderPriority;
    }

    /**
     * Set the render priority to control the order of rendering. The priority is between a range of 0
     * (rendered first) and 7 (rendered last). The default value is 4.
     */
    public void setRenderPriority(@IntRange(from = Renderable.RENDER_PRIORITY_FIRST, to = Renderable.RENDER_PRIORITY_LAST) int renderPriority) {
        this.renderPriority = Math.min(Renderable.RENDER_PRIORITY_LAST, Math.max(Renderable.RENDER_PRIORITY_FIRST, renderPriority));
        RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();
        int[] entities = getFilamentAsset().getEntities();
        for (int i = 0; i < entities.length; i++) {
            @EntityInstance int renderableInstance = renderableManager.getInstance(entities[i]);
            if (renderableInstance != 0) {
                renderableManager.setPriority(renderableInstance, this.renderPriority);
            }
        }
    }

    /**
     * ### Changes whether or not frustum culling is on
     *
     * The view frustum is the region of space in the modeled world that may appear on the screen.
     * Viewing-frustum culling is the process of removing objects that lie completely outside the
     * viewing frustum from the rendering process.
     * In other words, `true` = your model won't be visible/rendered when not any part of its
     * bounding box is visible/inside the camera view.
     *
     * Rendering these object would be a waste of time since they are not directly visible.
     * To make culling fast, it is usually done using bounding box surrounding the objects rather
     * than the objects themselves.
     * Instead of sending all information to your GPU, you will sort visible and invisible elements
     * and render only visible elements.
     * Thanks to this technique, you will earn GPU compute time.
     *
     * Do not confuse frustum culling with backface culling. The latter is controlled via the
     * material
     *
     * true by default
     */
    public void setCulling(boolean isCulling) {
        RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();
        @EntityInstance int renderableInstance = renderableManager.getInstance(getEntity());
        if (renderableInstance != 0 && renderableManager.hasComponent(renderableInstance)) {
            renderableManager.setCulling(renderableInstance, isShadowCaster);
        }
        int[] entities = getFilamentAsset().getEntities();
        for (int i = 0; i < entities.length; i++) {
            renderableInstance = renderableManager.getInstance(entities[i]);
            if (renderableInstance != 0) {
                renderableManager.setCulling(renderableInstance, isCulling);
            }
        }
    }

    /**
     * Returns true if configured to cast shadows on other renderables.
     */
    public boolean isShadowCaster() {
        return isShadowCaster;
    }

    /**
     * Sets whether the renderable casts shadow on other renderables in the scene.
     */
    public void setShadowCaster(boolean isShadowCaster) {
        this.isShadowCaster = isShadowCaster;
        RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();
        @EntityInstance int renderableInstance = renderableManager.getInstance(getEntity());
        if (renderableInstance != 0) {
            renderableManager.setCastShadows(renderableInstance, isShadowCaster);
        }
        //TODO : Verify if we don't need to apply the parameter to child entities
//        int[] entities = getFilamentAsset().getEntities();
//        for (int i = 0; i < entities.length; i++) {
//            renderableInstance = renderableManager.getInstance(entities[i]);
//            if (renderableInstance != 0) {
//                renderableManager.setCastShadows(renderableInstance, isShadowCaster);
//            }
//        }
    }

    /**
     * Returns true if configured to receive shadows cast by other renderables.
     */
    public boolean isShadowReceiver() {
        return isShadowReceiver;
    }

    /**
     * Sets whether the renderable receives shadows cast by other renderables in the scene.
     */
    public void setShadowReceiver(boolean isShadowReceiver) {
        this.isShadowReceiver = isShadowReceiver;
        RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();
        @EntityInstance int renderableInstance = renderableManager.getInstance(getEntity());
        if (renderableInstance != 0) {
            renderableManager.setReceiveShadows(renderableInstance, isShadowReceiver);
        }
        //TODO : Verify if we don't need to apply the parameter to child entities
//        int[] entities = getFilamentAsset().getEntities();
//        for (int i = 0; i < entities.length; i++) {
//            @EntityInstance int renderableInstance = renderableManager.getInstance(entities[i]);
//            if (renderableInstance != 0) {
//                renderableManager.setReceiveShadows(renderableInstance, isShadowReceiver);
//            }
//        }
    }

    ArrayList<Material> getMaterialBindings() {
        return materialBindings;
    }

    ArrayList<String> getMaterialNames() {
        return materialNames;
    }

    /**
     * Returns the material bound to the first submesh.
     */
    public Material getMaterial() {
        return getMaterial(0);
    }

    /**
     * Returns the number of materials.
     */
    public int getMaterialsCount() {
        return materialBindings.size();
    }

    /**
     * Returns the material bound to the specified index.
     */
    public Material getMaterial(int index) {
        if (index < materialBindings.size()) {
            return materialBindings.get(index);
        }
        return null;
    }

    /**
     * Returns the material bound to the specified name.
     */
    public Material getMaterial(String name) {
        for(int i=0;i<materialBindings.size();i++) {
            if(TextUtils.equals(materialNames.get(i), name)) {
                return materialBindings.get(i);
            }
        }
        return null;
    }

    /**
     * Sets the material bound to the first index.
     */
    public void setMaterial(Material material) {
        setMaterial(0, material);
    }

    /**
     * Sets the material bound to the specified index.
     */
    public void setMaterial(@IntRange(from = 0) int primitiveIndex, Material material) {
        for (int i = 0; i < getFilamentAsset().getEntities().length; i++) {
            setMaterial(i, primitiveIndex, material);
        }
    }

    /**
     * Sets the material bound to the specified index and entityIndex
     */
    public void setMaterial(int entityIndex, @IntRange(from = 0) int primitiveIndex, Material material) {
        int[] entities = getFilamentAsset().getEntities();
        Preconditions.checkElementIndex(entityIndex, entities.length, "No entity found at the given index");
        materialBindings.set(entityIndex, material);
        RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();
        @EntityInstance int renderableInstance = renderableManager.getInstance(entities[entityIndex]);
        if (renderableInstance != 0) {
            renderableManager.setMaterialInstanceAt(renderableInstance, primitiveIndex,
                    material.getFilamentMaterialInstance());
        }
    }

    /**
     * Returns the name associated with the specified index.
     */
    public String getMaterialName(int index) {
        Preconditions.checkState(materialNames.size() == materialBindings.size());
        if (index >= 0 && index < materialNames.size()) {
            return materialNames.get(index);
        }
        return null;
    }

    /**
     * @hide
     */
    public Matrix getWorldModelMatrix() {
        return renderable.getFinalModelMatrix(transformProvider.getTransformationMatrix());
    }

    public void setSkinningModifier(@Nullable SkinningModifier skinningModifier) {
        this.skinningModifier = skinningModifier;
    }

    /**
     * Get the associated {@link ModelAnimation} at the given index or throw
     * an {@link IndexOutOfBoundsException}.
     *
     * @param animationIndex Zero-based index for the animation of interest.
     */
    @Override
    public ModelAnimation getAnimation(int animationIndex) {
        Preconditions.checkElementIndex(animationIndex, getAnimationCount(), "No animation found at the given index");
        return animations.get(animationIndex);
    }

    /**
     * Returns the number of {@link ModelAnimation} definitions in the model.
     */
    @Override
    public int getAnimationCount() {
        return animations.size();
    }

    // We use our own {@link android.view.Choreographer} to update the animations so just return
    // false (not applied)
    @Override
    public boolean applyAnimationChange(ModelAnimation animation) {
        return false;
    }

    private void setupSkeleton(IRenderableInternalData renderableInternalData) {
        return;
    }

    /**
     * @hide
     */
    public void prepareForDraw() {
        renderable.prepareForDraw();

        ChangeId changeId = renderable.getId();
        if (changeId.checkChanged(renderableId)) {
            IRenderableInternalData renderableInternalData = renderable.getRenderableData();
            setupSkeleton(renderableInternalData);
            renderableInternalData.buildInstanceData(this, getRenderedEntity());
            renderableId = changeId.get();
            // First time we're rendering, so always update the skinning even if we aren't animating and
            // there is no skinModifier.
            updateSkinning();
        } else {
            // Will only update the skinning if the renderable is animating or there is a skinModifier
            // that has been changed since the last draw.
            if (updateAnimations(false)) {
                updateSkinning();
            }
        }
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        if(this.renderer != renderer) {
            FilamentAsset currentFilamentAsset = filamentAsset;
            if(this.renderer != null) {
                if (currentFilamentAsset != null) {
                    int[] entities = currentFilamentAsset.getEntities();
                    for (int entity : entities) {
                        this.renderer.getFilamentScene().removeEntity(entity);
                    }
                    int root = currentFilamentAsset.getRoot();
                    this.renderer.getFilamentScene().removeEntity(root);
                }
                this.renderer.removeInstance(this);
                renderable.detatchFromRenderer();
            }
            if(renderer != null) {
                renderer.addInstance(this);
                renderable.attachToRenderer(renderer);
                if (currentFilamentAsset != null) {
                    int[] entities = currentFilamentAsset.getEntities();
                    Preconditions.checkNotNull(renderer)
                            .getFilamentScene()
                            .addEntity(currentFilamentAsset.getRoot());
                    Preconditions.checkNotNull(renderer)
                            .getFilamentScene()
                            .addEntities(currentFilamentAsset.getEntities());
                    Preconditions.checkNotNull(renderer).getFilamentScene().addEntities(entities);
                }
            }
            this.renderer = renderer;
        }
    }

    /**
     * Detach and destroy the instance
     */
    public void destroy() {
        setRenderer(null);

        if (renderable.getRenderableData() instanceof RenderableInternalFilamentAssetData) {
            RenderableInternalFilamentAssetData renderableData =
                    (RenderableInternalFilamentAssetData) renderable.getRenderableData();
            renderableData.resourceLoader.evictResourceData();
        }
    }

    /**
     * Returns the transform of this renderable relative to it's node. This will be non-null if the
     * .sfa file includes a scale other than 1 or has recentering turned on.
     *
     * @hide
     */
    @Nullable
    public Matrix getRelativeTransform() {
        if (cachedRelativeTransform != null) {
            return cachedRelativeTransform;
        }

        IRenderableInternalData renderableData = renderable.getRenderableData();
        float scale = renderableData.getTransformScale();
        Vector3 offset = renderableData.getTransformOffset();
        if (scale == 1f && Vector3.equals(offset, Vector3.zero())) {
            return null;
        }

        cachedRelativeTransform = new Matrix();
        cachedRelativeTransform.makeScale(scale);
        cachedRelativeTransform.setTranslation(offset);
        return cachedRelativeTransform;
    }

    /**
     * Returns the inverse transform of this renderable relative to it's node. This will be non-null
     * if the .sfa file includes a scale other than 1 or has recentering turned on.
     *
     * @hide
     */
    @Nullable
    public Matrix getRelativeTransformInverse() {
        if (cachedRelativeTransformInverse != null) {
            return cachedRelativeTransformInverse;
        }

        Matrix relativeTransform = getRelativeTransform();
        if (relativeTransform == null) {
            return null;
        }

        cachedRelativeTransformInverse = new Matrix();
        Matrix.invert(relativeTransform, cachedRelativeTransformInverse);
        return cachedRelativeTransformInverse;
    }

    /**
     * Apply animations changes <code>if fore==true</code> or the animation has dirty values.
     *
     * @param force Update even if the animation time position didn't changed.
     * @return true if any animation update has been made.
     */
    public boolean updateAnimations(boolean force) {
        boolean hasUpdate = false;
        for (int i = 0; i < getAnimationCount(); i++) {
            ModelAnimation animation = getAnimation(i);
            if (force || animation.isDirty()) {
                if (getFilamentAnimator() != null) {
                    getFilamentAnimator().applyAnimation(i, animation.getTimePosition());
                }
                animation.setDirty(false);
                hasUpdate = true;
            }
        }
        return hasUpdate;
    }


    /**
     * Computes root-to-node transforms for all bone nodes.
     * Uses <code>TransformManager</code> and <code>RenderableManager</code>.
     *
     * <p>NOTE: this operation is independent of <code>animation</code>.</p>
     */
    private void updateSkinning() {
        if (getFilamentAnimator() != null) {
            getFilamentAnimator().updateBoneMatrices();
        }
    }

    void setBlendOrderAt(int index, int blendOrder) {
        RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();
        @EntityInstance int renderableInstance = renderableManager.getInstance(getRenderedEntity());
        renderableManager.setBlendOrderAt(renderableInstance, index, blendOrder);
    }

    @Entity
    private static int createFilamentEntity(IEngine engine) {
        EntityManager entityManager = EntityManager.get();
        @Entity int entity = entityManager.create();
        TransformManager transformManager = engine.getTransformManager();
        transformManager.create(entity);
        return entity;
    }

    @Entity
    private static int createFilamentChildEntity(
            IEngine engine, @Entity int entity, Matrix relativeTransform) {
        EntityManager entityManager = EntityManager.get();
        @Entity int childEntity = entityManager.create();
        TransformManager transformManager = engine.getTransformManager();
        transformManager.create(
                childEntity, transformManager.getInstance(entity), relativeTransform.data);
        return childEntity;
    }

    /**
     * Releases resources held by a {@link RenderableInstance}
     */
    private static final class CleanupCallback implements Runnable {
        private final int childEntity;
        private final int entity;

        CleanupCallback(int childEntity, int entity) {
            this.childEntity = childEntity;
            this.entity = entity;
        }

        @Override
        public void run() {
            AndroidPreconditions.checkUiThread();

            IEngine engine = EngineInstance.getEngine();

            if (engine == null || !engine.isValid()) {
                return;
            }

            RenderableManager renderableManager = engine.getRenderableManager();

            if (childEntity != 0) {
                renderableManager.destroy(childEntity);
            }
            if (entity != 0) {
                renderableManager.destroy(entity);
            }
        }
    }
}