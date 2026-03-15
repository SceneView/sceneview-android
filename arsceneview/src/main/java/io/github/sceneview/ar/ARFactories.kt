package io.github.sceneview.ar

import com.google.android.filament.Engine
import io.github.sceneview.ar.camera.ARCameraStream
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.createEnvironment
import io.github.sceneview.loaders.MaterialLoader

fun createARCameraNode(engine: Engine): ARCameraNode = ARDefaultCameraNode(engine)

fun createARCameraStream(materialLoader: MaterialLoader) = ARCameraStream(materialLoader)

fun createAREnvironment(engine: Engine) = createEnvironment(engine, isOpaque = true, skybox = null)

class ARDefaultCameraNode(engine: Engine) : ARCameraNode(engine) {
    init {
        setExposure(16.0f, 1.0f / 125.0f, 100.0f)
    }
}
