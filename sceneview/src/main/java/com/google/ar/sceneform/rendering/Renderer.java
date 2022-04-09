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
import io.github.sceneview.environment.Environment;
import io.github.sceneview.scene.CameraKt;
import io.github.sceneview.scene.SceneKt;

/**
 * A rendering context.
 *
 * <p>Contains everything that will be drawn on a surface.
 *
 * @hide Not a public facing API for version 1.0
 */
public class Renderer implements UiHelper.RendererCallback {

    // Limit resolution to 1080p for the minor edge. This is enough for Filament.
    private static final int MAXIMUM_RESOLUTION = 1080;
    private final SurfaceView surfaceView;
    private final ArrayList<RenderableInstance> renderableInstances = new ArrayList<>();
    private final ArrayList<LightInstance> lightInstances = new ArrayList<>();
    private final double[] cameraProjectionMatrix = new double[16];
    @Nullable
    private CameraProvider cameraProvider;
    private Surface surface;
    @Nullable
    private SwapChain swapChain;
    private com.google.android.filament.View view;
    private com.google.android.filament.View emptyView;
    private com.google.android.filament.Renderer renderer;
    private Camera camera;
    private Scene scene;

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
        initialize();
    }

    /**
     * Releases rendering resources ready for garbage collection
     *
     * @return Count of resources currently in use
     */
    public long reclaimReleasedResources() {
        return ResourceManager.getInstance().reclaimReleasedResources();
    }

    /**
     * Immediately releases all rendering resources, even if in use.
     */
    public void destroyAllResources() {
        ResourceManager.getInstance().destroyAllResources();
        EngineInstance.destroyEngine();
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

    private void doRecreationOfSwapChain() {
        synchronized (this) {
            if (recreateSwapChain) {
                final IEngine engine = EngineInstance.getEngine();
                if (swapChain != null) {
                    engine.destroySwapChain(swapChain);
                }
                swapChain = engine.createSwapChain(surface);
                recreateSwapChain = false;
            }
        }
    }

    /**
     * @hide
     */
    public boolean render(long frameTimeNanos) {
        doRecreationOfSwapChain();

        @Nullable SwapChain swapChainLocal = swapChain;
        if (swapChainLocal == null)
            return false;

        // Render the scene, unless the renderer wants to skip the frame.
        // This means you are sending frames too quickly to the GPU
        if ((filamentHelper.isReadyToRender() &&
                renderer.beginFrame(swapChainLocal, frameTimeNanos)) ||
                EngineInstance.isHeadlessMode()) {

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

                renderer.endFrame();

                reclaimReleasedResources();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @hide
     */
    public void dispose() {
        filamentHelper.detach(); // call this before destroying the Engine (it could call back)

        final IEngine engine = EngineInstance.getEngine();
        engine.destroyRenderer(renderer);
        engine.destroyView(view);
        engine.destroyView(emptyView);
        CameraKt.destroy(camera);

        reclaimReleasedResources();
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
     * @hide UiHelper.RendererCallback implementation
     */
    @Override
    public void onNativeWindowChanged(Surface surface) {
        synchronized (this) {
            this.surface = surface;
            recreateSwapChain = true;
        }
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

        IEngine engine = EngineInstance.getEngine();

        renderer = engine.createRenderer();
        scene = engine.createScene();
        view = engine.createView();

        emptyView = engine.createView();
        camera = engine.createCamera();

        setDefaultClearColor();
        view.setCamera(camera);
        view.setScene(scene);


        emptyView.setCamera(engine.createCamera());
        emptyView.setScene(engine.createScene());
    }

    private void updateInstances() {
        final IEngine engine = EngineInstance.getEngine();
        final TransformManager transformManager = engine.getTransformManager();
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
