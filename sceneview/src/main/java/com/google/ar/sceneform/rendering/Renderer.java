package com.google.ar.sceneform.rendering;

import android.content.Context;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.google.android.filament.Camera;
import com.google.android.filament.ColorGrading;
import com.google.android.filament.Engine;
import com.google.android.filament.Entity;
import com.google.android.filament.IndirectLight;
import com.google.android.filament.Scene;
import com.google.android.filament.Skybox;
import com.google.android.filament.SwapChain;
import com.google.android.filament.TransformManager;
import com.google.android.filament.View.DynamicResolutionOptions;
import com.google.android.filament.Viewport;
import com.google.android.filament.android.UiHelper;
import com.google.ar.sceneform.utilities.AndroidPreconditions;
import com.google.ar.sceneform.utilities.Preconditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dev.romainguy.kotlin.math.Float4;
import io.github.sceneview.Filament;
import io.github.sceneview.FilamentKt;
import io.github.sceneview.environment.Environment;
import io.github.sceneview.scene.CameraKt;
import io.github.sceneview.scene.RendererKt;
import io.github.sceneview.scene.SceneKt;
import io.github.sceneview.scene.ViewKt;

/**
 * A rendering context.
 *
 * <p>Contains everything that will be drawn on a surface.
 *
 * @hide Not a public facing API for version 1.0
 */
public class Renderer implements UiHelper.RendererCallback {

    private static final Float4 DEFAULT_CLEAR_COLOR = new Float4(0.0f, 0.0f, 0.0f, 1.0f);

    // Limit resolution to 1080p for the minor edge. This is enough for Filament.
    private static final int MAXIMUM_RESOLUTION = 1080;
    private final SurfaceView surfaceView;
    private final ViewAttachmentManager viewAttachmentManager;
    private final ArrayList<RenderableInstance> renderableInstances = new ArrayList<>();
    private final ArrayList<LightInstance> lightInstances = new ArrayList<>();
    private final double[] cameraProjectionMatrix = new double[16];
    private final List<Mirror> mirrors = new ArrayList<>();
    public Environment environment = null;
    @Entity
    public Integer mainLight = null;
    @Nullable
    private CameraProvider cameraProvider;
    private Surface surface;
    @Nullable
    private SwapChain swapChain;
    @Nullable
    private com.google.android.filament.View view;
    @Nullable
    private com.google.android.filament.View emptyView;
    private com.google.android.filament.Renderer renderer;
    private Camera camera;
    private Scene scene;
    private boolean recreateSwapChain;

    private UiHelper filamentHelper;

    @Nullable
    private Runnable onFrameRenderDebugCallback = null;
    @Nullable
    private PreRenderCallback preRenderCallback;

    /**
     * @hide
     */
    @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Renderer(SurfaceView view, CameraProvider cameraProvider) {
        Preconditions.checkNotNull(view, "Parameter \"view\" was null.");
        // Enforce api level 24
        AndroidPreconditions.checkMinAndroidApiLevel();

        this.surfaceView = view;
        this.cameraProvider = cameraProvider;
        viewAttachmentManager = new ViewAttachmentManager(getContext(), view);
        initialize();
    }

    @Nullable
    public CameraProvider getCameraProvider() {
        return cameraProvider;
    }

    public void setCameraProvider(@Nullable CameraProvider cameraProvider) {
        this.cameraProvider = cameraProvider;
    }

    /**
     * Starts mirroring to the specified {@link Surface}.
     *
     * @hide
     */
    public void startMirroring(Surface surface, int left, int bottom, int width, int height) {
        Mirror mirror = new Mirror();
        mirror.surface = surface;
        mirror.viewport = new Viewport(left, bottom, width, height);
        mirror.swapChain = null;
        synchronized (mirrors) {
            mirrors.add(mirror);
        }
    }

    /**
     * Stops mirroring to the specified {@link Surface}.
     *
     * @hide
     */
    public void stopMirroring(Surface surface) {
        synchronized (mirrors) {
            for (Mirror mirror : mirrors) {
                if (mirror.surface == surface) {
                    mirror.surface = null;
                }
            }
        }
    }

    /**
     * Access to the underlying Filament renderer.
     *
     * @hide
     */
    public com.google.android.filament.Renderer getFilamentRenderer() {
        return renderer;
    }

    /**
     * Access to the underlying Filament view.
     *
     * @hide
     */
    public com.google.android.filament.View getFilamentView() {
        return view;
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    /**
     * @hide
     */
    public void setClearColor(Float4 color) {
        com.google.android.filament.Renderer.ClearOptions options = new com.google.android.filament.Renderer.ClearOptions();
        options.clear = true;
        if (color.getA() > 0) {
            options.clearColor[0] = color.getR();
            options.clearColor[1] = color.getG();
            options.clearColor[2] = color.getB();
            options.clearColor[3] = color.getA();
        }
        renderer.setClearOptions(options);
    }

    /**
     * @hide
     */
    public void setDefaultClearColor() {
        setClearColor(DEFAULT_CLEAR_COLOR);
    }

    /**
     * Checks whether winding is inverted for front face rendering.
     *
     * @hide Used internally by ViewRenderable
     */

    public boolean isFrontFaceWindingInverted() {
        return view.isFrontFaceWindingInverted();
    }

    /**
     * Inverts winding for front face rendering.
     *
     * @hide Used internally by ArSceneView
     */

    public void setFrontFaceWindingInverted(Boolean inverted) {
        view.setFrontFaceWindingInverted(inverted);
    }

    /**
     * @hide
     */
    public void onPause() {
        viewAttachmentManager.onPause();
    }

    /**
     * @hide
     */
    public void onResume() {
        viewAttachmentManager.onResume();
    }

    /**
     * Sets a callback to happen after each frame is rendered. This can be used to log performance
     * metrics for a given frame.
     */
    public void setFrameRenderDebugCallback(Runnable onFrameRenderDebugCallback) {
        this.onFrameRenderDebugCallback = onFrameRenderDebugCallback;
    }

    private Viewport getLetterboxViewport(Viewport srcViewport, Viewport destViewport) {
        boolean letterBoxSides =
                (destViewport.width / (float) destViewport.height)
                        > (srcViewport.width / (float) srcViewport.height);
        float scale =
                letterBoxSides
                        ? (destViewport.height / (float) srcViewport.height)
                        : (destViewport.width / (float) srcViewport.width);
        int width = (int) (srcViewport.width * scale);
        int height = (int) (srcViewport.height * scale);
        int left = (destViewport.width - width) / 2;
        int bottom = (destViewport.height - height) / 2;
        return new Viewport(left, bottom, width, height);
    }

    /**
     * @hide
     */
    public void setPreRenderCallback(@Nullable PreRenderCallback preRenderCallback) {
        this.preRenderCallback = preRenderCallback;
    }

    private void doRecreationOfSwapChain() {
        synchronized (this) {
            if (recreateSwapChain) {
                if (swapChain != null) {
                    Filament.getEngine().destroySwapChain(swapChain);
                }
                swapChain = Filament.getEngine().createSwapChain(surface);
                recreateSwapChain = false;
            }
        }
    }

    private void updateMirrorConfig() {
        synchronized (mirrors) {
            Iterator<Mirror> mirrorIterator = mirrors.iterator();
            while (mirrorIterator.hasNext()) {
                Mirror mirror = mirrorIterator.next();
                if (mirror.surface == null) {
                    if (mirror.swapChain != null) {
                        Filament.getEngine().destroySwapChain(
                                Preconditions.checkNotNull(mirror.swapChain));
                    }
                    mirrorIterator.remove();
                } else if (mirror.swapChain == null) {
                    mirror.swapChain = Filament.getEngine().createSwapChain(
                            Preconditions.checkNotNull(mirror.surface));
                }
            }
        }
    }

    private void renderToMirror(com.google.android.filament.View currentView) {
        synchronized (mirrors) {
            for (Mirror mirror : mirrors) {
                if (mirror.swapChain != null) {
                    renderer.copyFrame(
                            mirror.swapChain,
                            getLetterboxViewport(currentView.getViewport(), mirror.viewport),
                            currentView.getViewport(),
                            com.google.android.filament.Renderer.MIRROR_FRAME_FLAG_COMMIT
                                    | com.google.android.filament.Renderer
                                    .MIRROR_FRAME_FLAG_SET_PRESENTATION_TIME
                                    | com.google.android.filament.Renderer.MIRROR_FRAME_FLAG_CLEAR);
                }
            }
        }
    }

    /**
     * @hide
     */
    public boolean render(long frameTimeNanos) {
        doRecreationOfSwapChain();
        updateMirrorConfig();

        @Nullable SwapChain swapChainLocal = swapChain;
        if (swapChainLocal == null)
            return false;

        // Render the scene, unless the renderer wants to skip the frame.
        // This means you are sending frames too quickly to the GPU
        if ((filamentHelper.isReadyToRender() &&
                renderer.beginFrame(swapChainLocal, frameTimeNanos))) {

            updateInstances();
            updateLights();

            CameraProvider cameraProvider = this.cameraProvider;
            if (cameraProvider != null) {

                final float[] projectionMatrixData = cameraProvider.getProjectionMatrix().data;
                for (int i = 0; i < 16; ++i) {
                    cameraProjectionMatrix[i] = projectionMatrixData[i];
                }

                camera.setModelMatrix(cameraProvider.getTransformationMatrix().data);
                camera.setCustomProjection(
                        cameraProjectionMatrix,
                        cameraProvider.getNearClipPlane(),
                        cameraProvider.getFarClipPlane());

                if (preRenderCallback != null) {
                    preRenderCallback.preRender(renderer, swapChainLocal, camera);
                }

                // Currently, filament does not provide functionality for disabling cameras, and
                // rendering a view with a null camera doesn't clear the viewport. As a workaround, we
                // render an empty view when the camera is disabled. this is actually similar to what we
                // need to do in the future if we want to add multiple camera support anyways. filament
                // only allows one camera per-view, so for multiple cameras you need to create multiple
                // views pointing to the same scene.
                com.google.android.filament.View currentView =
                        cameraProvider.isRendered() ? view : emptyView;
                renderer.render(currentView);

                renderToMirror(currentView);

                if (onFrameRenderDebugCallback != null) {
                    onFrameRenderDebugCallback.run();
                }
                renderer.endFrame();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @hide
     */
    public void destroy() {
        filamentHelper.detach(); // call this before destroying the Engine (it could call back)

        RendererKt.destroy(renderer);
        ViewKt.destroy(view);
        ViewKt.destroy(emptyView);
        CameraKt.destroy(camera);
        SceneKt.destroy(scene);
    }

    public Context getContext() {
        return getSurfaceView().getContext();
    }

    /**
     * Retrieve the Filament camera used by the renderer
     */
    public Camera getCamera() {
        return camera;
    }


    /**
     * ### Get the lighting environment and the skybox of the scene
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * ### Defines the lighting environment and the skybox of the scene
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        SceneKt.setEnvironment(scene, environment);
    }

    /**
     * ### Get the main directional light of the scene
     * <p>
     * Usually the Sun.
     */
    public @Entity
    Integer getMainLight() {
        return mainLight;
    }

    /**
     * ### Defines the main directional light of the scene
     * <p>
     * Usually the Sun.
     */
    public void setMainLight(@Entity Integer light) {
        if (mainLight != null) {
            removeLight(mainLight);
        }
        this.mainLight = light;
        if (light != null) {
            addLight(light);
        }
    }

    /**
     * Sets the environment light used for reflections and indirect light.
     *
     * @param indirectLight the IndirectLight to use when rendering the Scene or null to unset.
     */
    public void setIndirectLight(IndirectLight indirectLight) {
        scene.setIndirectLight(indirectLight);
    }

    /**
     * Sets the Skybox. The Skybox is drawn last and covers all pixels not touched by geometry.
     *
     * @param skybox – the Skybox to use to fill untouched pixels, or null to unset the Skybox.
     */
    public void setSkybox(Skybox skybox) {
        scene.setSkybox(skybox);
    }

    public void setDesiredSize(int width, int height) {
        int minor = Math.min(width, height);
        int major = Math.max(width, height);
        if (minor > MAXIMUM_RESOLUTION) {
            major = (major * MAXIMUM_RESOLUTION) / minor;
            minor = MAXIMUM_RESOLUTION;
        }
        if (width < height) {
            int t = minor;
            minor = major;
            major = t;
        }

        filamentHelper.setDesiredSize(major, minor);
    }

    /**
     * @hide
     */
    public int getDesiredWidth() {
        return filamentHelper.getDesiredWidth();
    }

    /**
     * @hide
     */
    public int getDesiredHeight() {
        return filamentHelper.getDesiredHeight();
    }

    /**
     * @hide UiHelper.RendererCallback implementation
     */
    @Override
    public void onNativeWindowChanged(Surface surface) {
        synchronized (this) {
            this.surface = surface;
            recreateSwapChain = true;
        }
    }

    /**
     * @hide UiHelper.RendererCallback implementation
     */
    @Override
    public void onDetachedFromSurface() {
        @Nullable SwapChain swapChainLocal = swapChain;
        if (swapChainLocal != null) {
            Filament.getEngine().destroySwapChain(swapChainLocal);
            // Required to ensure we don't return before Filament is done executing the
            // destroySwapChain command, otherwise Android might destroy the Surface
            // too early
            Filament.getEngine().flushAndWait();
            swapChain = null;
        }
    }

    /**
     * @hide Only used for scuba testing for now.
     */
    public void setDynamicResolutionEnabled(boolean isEnabled) {
        // Enable dynamic resolution. By default it will scale down to 25% of the screen area
        // (i.e.: 50% on each axis, e.g.: reducing a 1080p image down to 720p).
        // This can be changed in the options below.
        // TODO: This functionality should probably be exposed to the developer eventually.
        DynamicResolutionOptions options = new DynamicResolutionOptions();
        options.enabled = isEnabled;
        view.setDynamicResolutionOptions(options);
    }

    /**
     * @hide Only used for scuba testing for now.
     */
    @VisibleForTesting
    public void setAntiAliasing(com.google.android.filament.View.AntiAliasing antiAliasing) {
        view.setAntiAliasing(antiAliasing);
    }

    /**
     * @hide Only used for scuba testing for now.
     */
    @VisibleForTesting
    public void setDithering(com.google.android.filament.View.Dithering dithering) {
        view.setDithering(dithering);
    }

    /**
     * @hide Used internally by ArSceneView.
     */

    public void setPostProcessingEnabled(boolean enablePostProcessing) {
        return;
    }


    /**
     * @hide Used internally by ArSceneView
     */

    public void setRenderQuality(com.google.android.filament.View.RenderQuality renderQuality) {
        return;
    }

    /**
     * @hide UiHelper.RendererCallback implementation
     */
    @Override
    public void onResized(int width, int height) {
        if(view != null) {
            view.setViewport(new Viewport(0, 0, width, height));
        }
        if(emptyView != null) {
            emptyView.setViewport(new Viewport(0, 0, width, height));
        }
    }

    public void addEntity(@Entity int entity) {
        scene.addEntity(entity);
    }

    public void removeEntity(@Entity int entity) {
        scene.removeEntity(entity);
    }

    public void addLight(@Entity int entity) {
        addEntity(entity);
    }

    public void removeLight(@Entity int entity) {
        removeEntity(entity);
    }

    /**
     * @hide
     */
    void addLight(LightInstance instance) {
        addEntity(instance.getEntity());
        lightInstances.add(instance);
    }

    /**
     * @hide
     */
    void removeLight(LightInstance instance) {
        removeEntity(instance.getEntity());
        lightInstances.remove(instance);
    }


    private void addModelInstanceInternal(RenderableInstance instance) {
        return;
    }

    /**
     * @hide
     */
    void addInstance(RenderableInstance instance) {
        addEntity(instance.getRenderedEntity());
        addModelInstanceInternal(instance);
        renderableInstances.add(instance);
    }

    /**
     * @hide
     */
    void removeInstance(RenderableInstance instance) {
        removeEntity(instance.getRenderedEntity());
        renderableInstances.remove(instance);
    }

    @NonNull
    public Scene getFilamentScene() {
        return scene;
    }

    ViewAttachmentManager getViewAttachmentManager() {
        return viewAttachmentManager;
    }

    @SuppressWarnings("AndroidApiChecker") // CompletableFuture
    private void initialize() {
        SurfaceView surfaceView = getSurfaceView();

        filamentHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
        filamentHelper.setRenderCallback(this);
        filamentHelper.attachTo(surfaceView);

        Engine engine = Filament.getEngine();
        renderer = engine.createRenderer();
        scene = engine.createScene();
        view = engine.createView();
        // Change the ToneMapper to FILMIC to avoid some over saturated
        // colors, for example material orange 500.
        view.setColorGrading(ViewKt.build(new ColorGrading.Builder()
                .toneMapping(ColorGrading.ToneMapping.FILMIC)));
        emptyView = engine.createView();
        camera = FilamentKt.createCamera(engine);

        setDefaultClearColor();
        view.setCamera(camera);
        view.setScene(scene);

        setDynamicResolutionEnabled(true);

        emptyView.setCamera(FilamentKt.createCamera(engine));
        emptyView.setScene(Filament.getEngine().createScene());
    }

    private void updateInstances() {
        final TransformManager transformManager = Filament.getTransformManager();
        transformManager.openLocalTransformTransaction();

        for (RenderableInstance renderableInstance : renderableInstances) {
            renderableInstance.prepareForDraw();

            renderableInstance.setModelMatrix(transformManager,
                    renderableInstance.getWorldModelMatrix().data);
        }

        transformManager.commitLocalTransformTransaction();
    }

    private void updateLights() {
        for (LightInstance lightInstance : lightInstances) {
            lightInstance.updateTransform();
        }
    }

    /**
     * @hide
     */
    public interface PreRenderCallback {
        void preRender(
                com.google.android.filament.Renderer renderer,
                SwapChain swapChain,
                Camera camera);
    }

    private static class Mirror {
        @Nullable
        SwapChain swapChain;
        @Nullable
        Surface surface;
        Viewport viewport;
    }
}
