package com.google.ar.sceneform.resources;

/** Pool or cachce for resources */
public interface ResourceHolder {
  /**
   * Polls for garbage collected objects and disposes associated data.
   *
   * @return Count of resources in use.
   */
  long reclaimReleasedResources();

  /** Ignores reference count and disposes any associated resources. */
  void destroyAllResources();
}
