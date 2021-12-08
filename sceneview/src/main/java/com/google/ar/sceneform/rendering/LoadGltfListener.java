package com.google.ar.sceneform.rendering;

/** Interface callbacks for events that occur when loading a gltf file into a renderable. */
public interface LoadGltfListener {
  /** Defines the current stage of the load operation, each value supersedes the previous. */
  public enum GltfLoadStage {
    LOAD_STAGE_NONE,
    FETCH_MATERIALS,
    DOWNLOAD_MODEL,
    CREATE_LOADER,
    ADD_MISSING_FILES,
    FINISHED_READING_FILES,
    CREATE_RENDERABLE
  }

  void setLoadingStage(GltfLoadStage stage);

  void reportBytesDownloaded(long bytes);

  void onFinishedFetchingMaterials();

  void onFinishedLoadingModel(long durationMs);

  void onFinishedReadingFiles(long durationMs);

  void setModelSize(float modelSizeMeters);

  void onReadingFilesFailed(Exception exception);
}
