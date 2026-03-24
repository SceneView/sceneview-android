package io.github.sceneview.ar

import com.google.android.filament.Engine
import io.github.sceneview.ar.camera.ARCameraStream
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.createEnvironment
import io.github.sceneview.loaders.MaterialLoader

/**
 * Creates an [ARCameraNode] with default AR-appropriate exposure settings.
 *
 * The default exposure (aperture 16, shutter speed 1/125s, ISO 100) is tuned to match
 * ARCore's light estimation output so that virtual objects blend naturally with the
 * real-world camera feed.
 */
fun createARCameraNode(engine: Engine): ARCameraNode = ARDefaultCameraNode(engine)

/**
 * Creates an [ARCameraStream] that renders the device camera feed as the scene background.
 *
 * @param materialLoader The [MaterialLoader] used to build the camera background material.
 */
fun createARCameraStream(materialLoader: MaterialLoader) = ARCameraStream(materialLoader)

/**
 * Creates an AR-optimised [io.github.sceneview.environment.Environment] with no skybox
 * (transparent background so the camera feed shows through) and a neutral IBL.
 */
fun createAREnvironment(engine: Engine) = createEnvironment(engine, isOpaque = true, skybox = null)

/**
 * Default AR camera node with exposure tuned for ARCore light estimation.
 */
class ARDefaultCameraNode(engine: Engine) : ARCameraNode(engine) {
    init {
        setExposure(16.0f, 1.0f / 125.0f, 100.0f)
    }
}
