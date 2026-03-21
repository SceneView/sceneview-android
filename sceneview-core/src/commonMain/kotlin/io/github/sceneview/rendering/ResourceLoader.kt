package io.github.sceneview.rendering

/**
 * Cross-platform resource loading interface.
 *
 * Abstracts model, material, and environment loading across platforms.
 * Android uses Filament loaders + Android assets/resources.
 * iOS uses SceneKit/RealityKit loaders + iOS bundle resources.
 */
interface ModelLoader {
    /**
     * Loads a 3D model from a file path or asset URI.
     *
     * @param path relative path to the model file (e.g., "models/car.glb")
     * @return platform-specific model instance, or null on failure
     */
    suspend fun loadModel(path: String): Any?

    /**
     * Loads a 3D model asynchronously with progress callback.
     *
     * @param path relative path to the model file
     * @param onProgress progress callback (0.0 to 1.0)
     * @return platform-specific model instance, or null on failure
     */
    suspend fun loadModel(path: String, onProgress: ((Float) -> Unit)? = null): Any?

    /**
     * Creates a model instance from a previously loaded model.
     */
    fun createInstance(model: Any): Any?
}

interface MaterialLoader {
    /**
     * Loads a material from a file path or asset URI.
     *
     * @param path relative path to the material file
     * @return platform-specific material instance, or null on failure
     */
    suspend fun loadMaterial(path: String): Any?
}

interface EnvironmentLoader {
    /**
     * Loads an HDR environment (IBL + skybox) from a file.
     *
     * @param path relative path to the HDR/KTX file
     * @return platform-specific environment, or null on failure
     */
    suspend fun loadEnvironment(path: String): Any?
}
