package io.github.sceneview.nodes

import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.components.CameraComponent
import io.github.sceneview.managers.NodeManager

/**
 * Camera represents the eye through which the scene is viewed.
 *
 * A Camera has a position and orientation and controls the projection and exposure parameters.
 *
 * The camera coordinate system defines the <b>view space</b>. The camera points towards its -z axis
 * and is oriented such that its top side is in the direction of +y, and its right side in the
 * direction of +x.
 *
 * The Camera is also used to set the scene's exposure, just like with a real camera. The lights
 * intensity and the Camera exposure interact to produce the final scene's brightness.
 *
 * @see Camera
 */
open class CameraNode(
    engine: Engine,
    nodeManager: NodeManager,
    entity: Entity
) : Node(engine, nodeManager, entity, isSelectable = false, isEditable = false), CameraComponent {

    constructor(
        engine: Engine,
        nodeManager: NodeManager,
        entity: Entity = EntityManager.get().create(),
        apply: Camera.() -> Unit
    ) : this(engine, nodeManager, entity) {
        engine.createCamera(entity).apply(apply)
    }

    constructor(
        sceneView: SceneView,
        entity: Entity
    ) : this(sceneView.engine, sceneView.nodeManager, entity)

    constructor(
        sceneView: SceneView,
        entity: Entity = EntityManager.get().create(),
        apply: Camera.() -> Unit
    ) : this(sceneView.engine, sceneView.nodeManager, entity, apply)

    override fun destroy() {
        engine.destroyCameraComponent(entity)
        super.destroy()
    }
}