package com.google.ar.sceneform.rendering;

import android.os.Build;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

/**
 * A rendering context.
 *
 * <p>Contains everything that will be drawn on a surface.
 *
 * @hide Not a public facing API for version 1.0
 */
public class ArRenderer extends Renderer {

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ArRenderer(SurfaceView view, CameraProvider cameraProvider) {
        super(view, cameraProvider);
    }

    /**
     * Releases rendering resources ready for garbage collection
     *
     * @return Count of resources currently in use
     */
    @Override
    public long reclaimReleasedResources() {
        return super.reclaimReleasedResources() + ArResourceManager.getInstance().reclaimReleasedResources();
    }

    /**
     * Immediately releases all rendering resources, even if in use.
     */
    @Override
    public void destroyAllResources() {
        ArResourceManager.getInstance().destroyAllResources();
        super.destroyAllResources();
    }
}
