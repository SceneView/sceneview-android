package com.google.ar.sceneform.rendering;

import android.media.Image;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.filament.EntityManager;
import com.google.android.filament.IndexBuffer;
import com.google.android.filament.IndexBuffer.Builder.IndexType;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.Scene;
import com.google.android.filament.VertexBuffer;
import com.google.android.filament.VertexBuffer.Builder;
import com.google.android.filament.VertexBuffer.VertexAttribute;
import com.google.android.filament.utils.Mat4;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraIntrinsics;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.sceneform.utilities.AndroidPreconditions;
import com.google.ar.sceneform.utilities.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Displays the Camera stream using Filament.
 *
 * @hide Note: The class is hidden because it should only be used by the Filament Renderer and does
 * not expose a user facing API.
 */
@SuppressWarnings("AndroidApiChecker") // CompletableFuture
public class CameraStream {
    public static final String MATERIAL_CAMERA_TEXTURE = "cameraTexture";
    public static final String MATERIAL_DEPTH_TEXTURE = "depthTexture";

    private static final String TAG = CameraStream.class.getSimpleName();

    private static final int VERTEX_COUNT = 3;
    private static final int POSITION_BUFFER_INDEX = 0;
    private static final float[] CAMERA_VERTICES =
            new float[]{
                    -1.0f, 1.0f,
                    1.0f, -1.0f,
                    -3.0f, 1.0f,
                    3.0f, 1.0f,
                    1.0f};
    private static final int UV_BUFFER_INDEX = 1;
    private static final float[] CAMERA_UVS = new float[]{
            0.0f, 0.0f,
            0.0f, 2.0f,
            2.0f, 0.0f};
    private static final short[] INDICES = new short[]{0, 1, 2};

    private static final int FLOAT_SIZE_IN_BYTES = Float.SIZE / 8;
    private static final int UNINITIALIZED_FILAMENT_RENDERABLE = -1;

    private final Scene scene;
    private final int cameraTextureId;
    private final IndexBuffer cameraIndexBuffer;
    private final VertexBuffer cameraVertexBuffer;
    private final FloatBuffer cameraUvCoords;
    private final FloatBuffer transformedCameraUvCoords;
    private final IEngine engine;
    public int cameraStreamRenderable = UNINITIALIZED_FILAMENT_RENDERABLE;

    /**
     * By default the depthMode is set to {@link DepthMode#NO_DEPTH}
     */
    private DepthMode depthMode = DepthMode.NO_DEPTH;
    /**
     * By default the depthOcclusionMode ist set to {@link DepthOcclusionMode#DEPTH_OCCLUSION_DISABLED}
     */
    private DepthOcclusionMode depthOcclusionMode = DepthOcclusionMode.DEPTH_OCCLUSION_DISABLED;

    @Nullable
    private ExternalTexture cameraTexture;
    @Nullable
    private DepthTexture depthTexture;

    @Nullable
    private Material cameraMaterial = null;
    @Nullable
    private Material occlusionCameraMaterial = null;

    private int renderablePriority = Renderable.RENDER_PRIORITY_LAST;

    private boolean isTextureInitialized = false;

    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored", "initialization"})
    public CameraStream(int cameraTextureId, Renderer renderer) {
        scene = renderer.getFilamentScene();
        this.cameraTextureId = cameraTextureId;

        engine = EngineInstance.getEngine();

        // INDEXBUFFER
        // create screen quad geometry to camera stream to
        ShortBuffer indexBufferData = ShortBuffer.allocate(INDICES.length);
        indexBufferData.put(INDICES);

        final int indexCount = indexBufferData.capacity();
        cameraIndexBuffer = createIndexBuffer(indexCount);

        indexBufferData.rewind();
        Preconditions.checkNotNull(cameraIndexBuffer)
                .setBuffer(engine.getFilamentEngine(), indexBufferData);


        // Note: ARCore expects the UV buffers to be direct or will assert in transformDisplayUvCoords.
        cameraUvCoords = createCameraUVBuffer();
        transformedCameraUvCoords = createCameraUVBuffer();


        // VERTEXTBUFFER
        FloatBuffer vertexBufferData = FloatBuffer.allocate(CAMERA_VERTICES.length);
        vertexBufferData.put(CAMERA_VERTICES);

        cameraVertexBuffer = createVertexBuffer();

        vertexBufferData.rewind();
        Preconditions.checkNotNull(cameraVertexBuffer)
                .setBufferAt(engine.getFilamentEngine(), POSITION_BUFFER_INDEX, vertexBufferData);

        adjustCameraUvsForOpenGL();
        cameraVertexBuffer.setBufferAt(
                engine.getFilamentEngine(), UV_BUFFER_INDEX, transformedCameraUvCoords);

        setupStandardCameraMaterial(renderer);
        setupOcclusionCameraMaterial(renderer);
    }

    private FloatBuffer createCameraUVBuffer() {
        FloatBuffer buffer =
                ByteBuffer.allocateDirect(CAMERA_UVS.length * FLOAT_SIZE_IN_BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();
        buffer.put(CAMERA_UVS);
        buffer.rewind();

        return buffer;
    }

    private IndexBuffer createIndexBuffer(int indexCount) {
        return new IndexBuffer.Builder()
                .indexCount(indexCount)
                .bufferType(IndexType.USHORT)
                .build(engine.getFilamentEngine());
    }

    private VertexBuffer createVertexBuffer() {
        return new Builder()
                .vertexCount(VERTEX_COUNT)
                .bufferCount(2)
                .attribute(
                        VertexAttribute.POSITION,
                        POSITION_BUFFER_INDEX,
                        VertexBuffer.AttributeType.FLOAT3,
                        0,
                        (CAMERA_VERTICES.length / VERTEX_COUNT) * FLOAT_SIZE_IN_BYTES)
                .attribute(
                        VertexAttribute.UV0,
                        UV_BUFFER_INDEX,
                        VertexBuffer.AttributeType.FLOAT2,
                        0,
                        (CAMERA_UVS.length / VERTEX_COUNT) * FLOAT_SIZE_IN_BYTES)
                .build(engine.getFilamentEngine());
    }

    void setupStandardCameraMaterial(Renderer renderer) {
        CompletableFuture<Material> materialFuture =
                Material.builder()
                        .setSource(
                                renderer.getContext(),
                                Uri.parse("sceneview/materials/camera_stream_flat.filamat"))
                        .build();

        materialFuture
                .thenAccept(
                        material -> {
                            float[] uvTransform = Mat4.Companion.identity().toFloatArray();
                            material.getFilamentMaterialInstance()
                                    .setParameter(
                                            "uvTransform",
                                            MaterialInstance.FloatElement.FLOAT4,
                                            uvTransform,
                                            0,
                                            4);

                            // Only set the camera material if it hasn't already been set to a custom material.
                            if (cameraMaterial == null) {
                                cameraMaterial = material;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Unable to load camera stream materials.", throwable);
                            return null;
                        });
    }

    void setupOcclusionCameraMaterial(Renderer renderer) {
        CompletableFuture<Material> materialFuture =
                Material.builder()
                        .setSource(
                                renderer.getContext(),
                                Uri.parse("sceneview/materials/camera_stream_depth.filamat"))
                        .build();
        materialFuture
                .thenAccept(
                        material -> {
                            float[] uvTransform = Mat4.Companion.identity().toFloatArray();
                            material.getFilamentMaterialInstance()
                                    .setParameter(
                                            "uvTransform",
                                            MaterialInstance.FloatElement.FLOAT4,
                                            uvTransform,
                                            0,
                                            4);

                            // Only set the occlusion material if it hasn't already been set to a custom material.
                            if (occlusionCameraMaterial == null) {
                                occlusionCameraMaterial = material;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Unable to load camera stream materials.", throwable);
                            return null;
                        });
    }

    private void setCameraMaterial(Material material) {
        cameraMaterial = material;
        if (cameraMaterial == null)
            return;

        // The ExternalTexture can't be created until we receive the first AR Core Frame so that we
        // can access the width and height of the camera texture. Return early if the ExternalTexture
        // hasn't been created yet so we don't start rendering until we have a valid texture. This will
        // be called again when the ExternalTexture is created.
        if (!isTextureInitialized()) {
            return;
        }

        cameraMaterial.setExternalTexture(
                MATERIAL_CAMERA_TEXTURE,
                Preconditions.checkNotNull(cameraTexture));
    }

    private void setOcclusionMaterial(Material material) {
        occlusionCameraMaterial = material;
        if (occlusionCameraMaterial == null)
            return;

        // The ExternalTexture can't be created until we receive the first AR Core Frame so that we
        // can access the width and height of the camera texture. Return early if the ExternalTexture
        // hasn't been created yet so we don't start rendering until we have a valid texture. This will
        // be called again when the ExternalTexture is created.
        if (!isTextureInitialized()) {
            return;
        }

        occlusionCameraMaterial.setExternalTexture(
                MATERIAL_CAMERA_TEXTURE,
                Preconditions.checkNotNull(cameraTexture));
    }


    private void initOrUpdateRenderableMaterial(Material material) {
        if (!isTextureInitialized()) {
            return;
        }

        if (cameraStreamRenderable == UNINITIALIZED_FILAMENT_RENDERABLE) {
            initializeFilamentRenderable(material);
        } else {
            RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();
            int renderableInstance = renderableManager.getInstance(cameraStreamRenderable);
            renderableManager.setMaterialInstanceAt(
                    renderableInstance, 0, material.getFilamentMaterialInstance());
        }
    }


    private void initializeFilamentRenderable(Material material) {
        // create entity id
        cameraStreamRenderable = EntityManager.get().create();

        // create the quad renderable (leave off the aabb)
        RenderableManager.Builder builder = new RenderableManager.Builder(1);
        builder
                .castShadows(false)
                .receiveShadows(false)
                .culling(false)
                // Always draw the camera feed last to avoid overdraw
                .priority(renderablePriority)
                .geometry(
                        0, RenderableManager.PrimitiveType.TRIANGLES, cameraVertexBuffer, cameraIndexBuffer)
                .material(0, Preconditions.checkNotNull(material).getFilamentMaterialInstance())
                .build(EngineInstance.getEngine().getFilamentEngine(), cameraStreamRenderable);

        // add to the scene
        scene.addEntity(cameraStreamRenderable);

        ArResourceManager.getInstance()
                .getCameraStreamCleanupRegistry()
                .register(
                        this,
                        new CleanupCallback(
                                scene, cameraStreamRenderable, cameraIndexBuffer, cameraVertexBuffer));
    }


    /**
     * The {@link Session} holds the information if the DepthMode is configured or not. Based on
     * that result different materials and textures are used for the camera.
     *
     * @param session {@link Session}
     * @param config  {@link Config}
     */
    public void checkIfDepthIsEnabled(Session session, Config config) {
        depthMode = DepthMode.NO_DEPTH;

        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
            if (config.getDepthMode() == Config.DepthMode.AUTOMATIC) {
                depthMode = DepthMode.DEPTH;
            }

        if (session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY))
            if (config.getDepthMode() == Config.DepthMode.RAW_DEPTH_ONLY) {
                depthMode = DepthMode.RAW_DEPTH;
            }
    }

    public boolean isTextureInitialized() {
        return isTextureInitialized;
    }

    public void initializeTexture(Camera arCamera) {
        if (isTextureInitialized()) {
            return;
        }

        // External Camera Texture
        if (cameraTexture == null) {
            CameraIntrinsics intrinsics = arCamera.getTextureIntrinsics();
            int[] dimensions = intrinsics.getImageDimensions();

            cameraTexture = new ExternalTexture(
                    cameraTextureId,
                    dimensions[0],
                    dimensions[1]);
        }

        if (depthOcclusionMode == DepthOcclusionMode.DEPTH_OCCLUSION_ENABLED && (
                depthMode == DepthMode.DEPTH ||
                        depthMode == DepthMode.RAW_DEPTH)) {
            if (occlusionCameraMaterial != null) {
                isTextureInitialized = true;
                setOcclusionMaterial(occlusionCameraMaterial);
                initOrUpdateRenderableMaterial(occlusionCameraMaterial);
            }
        } else {
            if (cameraMaterial != null) {
                isTextureInitialized = true;
                setCameraMaterial(cameraMaterial);
                initOrUpdateRenderableMaterial(cameraMaterial);
            }
        }
    }

    /**
     * <pre>
     *      Update the DepthTexture.
     * </pre>
     *
     * @param depthImage {@link Image}
     */
    public void recalculateOcclusion(Image depthImage) {
        if (occlusionCameraMaterial != null &&
                depthTexture == null) {
            depthTexture = new DepthTexture(
                    depthImage.getWidth(),
                    depthImage.getHeight());

            occlusionCameraMaterial.setDepthTexture(
                    MATERIAL_DEPTH_TEXTURE,
                    depthTexture.getFilamentTexture());
        }

        if (occlusionCameraMaterial == null ||
                !isTextureInitialized ||
                depthImage == null) {
            return;
        }

        depthTexture.updateDepthTexture(depthImage);
    }


    public void recalculateCameraUvs(Frame frame) {
        FloatBuffer cameraUvCoords = this.cameraUvCoords;
        FloatBuffer transformedCameraUvCoords = this.transformedCameraUvCoords;
        VertexBuffer cameraVertexBuffer = this.cameraVertexBuffer;
        frame.transformDisplayUvCoords(cameraUvCoords, transformedCameraUvCoords);
        adjustCameraUvsForOpenGL();
        cameraVertexBuffer.setBufferAt(
                engine.getFilamentEngine(), UV_BUFFER_INDEX, transformedCameraUvCoords);
    }


    private void adjustCameraUvsForOpenGL() {
        // Correct for vertical coordinates to match OpenGL
        for (int i = 1; i < VERTEX_COUNT * 2; i += 2) {
            transformedCameraUvCoords.put(i, 1.0f - transformedCameraUvCoords.get(i));
        }
    }


    public int getRenderPriority() {
        return renderablePriority;
    }

    public void setRenderPriority(int priority) {
        renderablePriority = priority;
        if (cameraStreamRenderable != UNINITIALIZED_FILAMENT_RENDERABLE) {
            RenderableManager renderableManager = EngineInstance.getEngine().getRenderableManager();
            int renderableInstance = renderableManager.getInstance(cameraStreamRenderable);
            renderableManager.setPriority(renderableInstance, renderablePriority);
        }
    }

    /**
     * Gets the currently applied depth mode depending on the device supported modes.
     */
    public DepthMode getDepthMode() {
        return depthMode;
    }

    /**
     * Checks whether the provided DepthOcclusionMode is supported on this device with the selected camera configuration and AR config.
     * The current list of supported devices is documented on the ARCore supported devices page.
     *
     * @param depthOcclusionMode The desired depth mode to check.
     * @return True if the depth mode has been activated on the AR session config
     * and the provided depth occlusion mode is supported on this device.
     */
    public boolean isDepthOcclusionModeSupported(DepthOcclusionMode depthOcclusionMode) {
        switch (depthOcclusionMode) {
            case DEPTH_OCCLUSION_ENABLED:
                return depthMode == DepthMode.DEPTH || depthMode == DepthMode.RAW_DEPTH;
            default:
                return true;
        }
    }

    /**
     * Gets the current Depth Occlusion Mode
     *
     * @return the occlusion mode currently defined for the CarmeraStream
     * @see #setDepthOcclusionMode
     * @see DepthOcclusionMode
     */
    public DepthOcclusionMode getDepthOcclusionMode() {
        return depthOcclusionMode;
    }


    /**
     * <pre>
     *     Set the DepthModeUsage to {@link DepthOcclusionMode#DEPTH_OCCLUSION_ENABLED} to set the
     *     occlusion {@link com.google.android.filament.Material}. This will process the incoming DepthImage to
     *     occlude virtual objects behind real world objects. If the {@link Session} configuration
     *     for the {@link Config.DepthMode} is set to {@link Config.DepthMode#DISABLED},
     *     the standard camera {@link Material} is used.
     *
     *     Set the DepthModeUsage to {@link DepthOcclusionMode#DEPTH_OCCLUSION_DISABLED} to set the
     *     standard camera {@link com.google.android.filament.Material}.
     *
     *     A good place to set the DepthModeUsage is inside of the onViewCreated() function call.
     *     To make sure that this function is called in your code set the correct listener on
     *     your Ar Fragment
     *
     *     <code>public void onAttachFragment(
     *         FragmentManager fragmentManager,
     *         Fragment fragment
     *     ) {
     *         if (fragment.getId() == R.id.arFragment) {
     *             arFragment = (ArFragment) fragment;
     *             arFragment.setOnViewCreatedListener(this);
     *             arFragment.setOnSessionConfigurationListener(this);
     *         }
     *     }
     *
     *     public void onViewCreated(
     *         ArFragment arFragment,
     *         ArSceneView arSceneView
     *     ) {
     *         arSceneView
     *            .getCameraStream()
     *            .setDepthModeUsage(CameraStream
     *               .setDepthOcclusionMode
     *               .DEPTH_OCCLUSION_DISABLED);
     *     }
     *     </code>
     *
     *     The default value for {@link DepthOcclusionMode} is {@link DepthOcclusionMode#DEPTH_OCCLUSION_DISABLED}.
     * </pre>
     *
     * @param depthOcclusionMode {@link DepthOcclusionMode}
     */
    public void setDepthOcclusionMode(DepthOcclusionMode depthOcclusionMode) {
        // Only set the occlusion material if the session config
        // has set the DepthMode to AUTOMATIC or RAW_DEPTH_ONLY,
        // otherwise set the standard camera material.
        if (isDepthOcclusionModeSupported(depthOcclusionMode)) {
            if (occlusionCameraMaterial != null) {
                setOcclusionMaterial(occlusionCameraMaterial);
                initOrUpdateRenderableMaterial(occlusionCameraMaterial);
            }
        } else {
            if (cameraMaterial != null) {
                setCameraMaterial(cameraMaterial);
                initOrUpdateRenderableMaterial(cameraMaterial);
            }
        }

        this.depthOcclusionMode = depthOcclusionMode;
    }


    /**
     * The DepthMode Enum is used to reflect the {@link Session} configuration
     * for the DepthMode to decide if the occlusion material should be set and if
     * frame.acquireDepthImage() or frame.acquireRawDepthImage() should be called to get
     * the input data for the depth texture.
     */
    public enum DepthMode {
        /**
         * <pre>
         * The {@link Session} is not configured to use the Depth-API
         *
         * This is the default value
         * </pre>
         */
        NO_DEPTH,
        /**
         * The {@link Session} is configured to use the DepthMode AUTOMATIC
         */
        DEPTH,
        /**
         * The {@link Session} is configured to use the DepthMode RAW_DEPTH_ONLY
         */
        RAW_DEPTH
    }


    /**
     * Independent from the {@link Session} configuration, the user can decide with the
     * DeptModeUsage which {@link com.google.android.filament.Material} should be set to the
     * CameraStream renderable.
     */
    public enum DepthOcclusionMode {
        /**
         * Set the occlusion material. If the {@link Session} is not
         * configured properly the standard camera material is used.
         * Valid {@link Session} configuration for the DepthMode are
         * {@link Config.DepthMode#AUTOMATIC} and {@link Config.DepthMode#RAW_DEPTH_ONLY}.
         */
        DEPTH_OCCLUSION_ENABLED,
        /**
         * <pre>
         * Use this value if the standard camera material should be applied to
         * the CameraStream Renderable even if the {@link Session} configuration has set
         * the DepthMode to {@link Config.DepthMode#AUTOMATIC} or
         * {@link Config.DepthMode#RAW_DEPTH_ONLY}. This Option is useful, if you
         * want to use the DepthImage or RawDepthImage or just the DepthPoints without the
         * occlusion effect.
         *
         * This is the default value
         * </pre>
         */
        DEPTH_OCCLUSION_DISABLED
    }

    /**
     * Cleanup filament objects after garbage collection
     */
    private static final class CleanupCallback implements Runnable {
        private final Scene scene;
        private final int cameraStreamRenderable;
        private final IndexBuffer cameraIndexBuffer;
        private final VertexBuffer cameraVertexBuffer;

        CleanupCallback(
                Scene scene,
                int cameraStreamRenderable,
                IndexBuffer cameraIndexBuffer,
                VertexBuffer cameraVertexBuffer) {
            this.scene = scene;
            this.cameraStreamRenderable = cameraStreamRenderable;
            this.cameraIndexBuffer = cameraIndexBuffer;
            this.cameraVertexBuffer = cameraVertexBuffer;
        }

        @Override
        public void run() {
            AndroidPreconditions.checkUiThread();

            IEngine engine = EngineInstance.getEngine();
            if (engine == null && !engine.isValid()) {
                return;
            }

            if (cameraStreamRenderable != UNINITIALIZED_FILAMENT_RENDERABLE) {
                scene.removeEntity(cameraStreamRenderable);
            }

            engine.destroyIndexBuffer(cameraIndexBuffer);
            engine.destroyVertexBuffer(cameraVertexBuffer);
        }
    }
}
