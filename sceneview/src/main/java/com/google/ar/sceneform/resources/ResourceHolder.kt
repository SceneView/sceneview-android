package com.google.ar.sceneform.resources

/** Pool or cache for resources */
interface ResourceHolder {
    /**
     * Polls for garbage collected objects and disposes associated data.
     *
     * @return Count of resources in use.
     */
    fun reclaimReleasedResources(): Long

    /** Ignores reference count and disposes any associated resources. */
    fun destroyAllResources()
}
