package com.google.ar.sceneform.rendering

/** Interface callbacks for events that occur when loading a gltf file into a renderable. */
interface LoadGltfListener {
    /** Defines the current stage of the load operation, each value supersedes the previous. */
    enum class GltfLoadStage {
        LOAD_STAGE_NONE,
        FETCH_MATERIALS,
        DOWNLOAD_MODEL,
        CREATE_LOADER,
        ADD_MISSING_FILES,
        FINISHED_READING_FILES,
        CREATE_RENDERABLE
    }

    fun setLoadingStage(stage: GltfLoadStage)

    fun reportBytesDownloaded(bytes: Long)

    fun onFinishedFetchingMaterials()

    fun onFinishedLoadingModel(durationMs: Long)

    fun onFinishedReadingFiles(durationMs: Long)

    fun setModelSize(modelSizeMeters: Float)

    fun onReadingFilesFailed(exception: Exception)
}
