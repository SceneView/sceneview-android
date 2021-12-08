package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;

import com.google.ar.sceneform.resources.ResourceHolder;
import com.google.ar.sceneform.resources.ResourceRegistry;

import java.util.ArrayList;

/**
 * Minimal resource manager. Maintains mappings from ids to created resources and a task executor
 * dedicated to loading resources asynchronously.
 *
 * @hide
 */
@SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
public class ArResourceManager {
  @Nullable private static ArResourceManager instance = null;

  private final ArrayList<ResourceHolder> resourceHolders = new ArrayList<>();
  private final CleanupRegistry<CameraStream> cameraStreamCleanupRegistry = new CleanupRegistry<>();
  private final CleanupRegistry<DepthTexture> depthTextureCleanupRegistry = new CleanupRegistry<>();

  CleanupRegistry<CameraStream> getCameraStreamCleanupRegistry() {
    return cameraStreamCleanupRegistry;
  }

  public CleanupRegistry<DepthTexture> getDepthTextureCleanupRegistry() {
    return depthTextureCleanupRegistry;
  }

  public long reclaimReleasedResources() {
    long resourcesInUse = 0;
    for (ResourceHolder registry : resourceHolders) {
      resourcesInUse += registry.reclaimReleasedResources();
    }
    return resourcesInUse;
  }

  /** Forcibly deletes all tracked references */
  public void destroyAllResources() {
    for (ResourceHolder resourceHolder : resourceHolders) {
      resourceHolder.destroyAllResources();
    }
  }

  public void addResourceHolder(ResourceHolder resource) {
    resourceHolders.add(resource);
  }

  public static ArResourceManager getInstance() {
    if (instance == null) {
      instance = new ArResourceManager();
    }

    return instance;
  }

  private ArResourceManager() {
    addResourceHolder(cameraStreamCleanupRegistry);
    addResourceHolder(depthTextureCleanupRegistry);
  }
}
