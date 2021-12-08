package io.github.sceneview.node

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.LightInstance

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * It contains a light for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
class LightNode(
    position: Vector3 = defaultPosition,
    rotationQuaternion: Quaternion = defaultRotation,
    scales: Vector3 = defaultScales,
    parent: NodeParent? = null
) : Node(position, rotationQuaternion, scales, parent) {

    /**
     * ### The [Light] to display.
     *
     * To use, first create a [Light].
     * Set the parameters you care about and then attach it to the node using this function.
     * A node may have a renderable and a light or just act as a [Light].
     *
     * Pass null to remove the light.
     */
    var lightInstance: LightInstance? = null
        set(value) {
            if (field != value) {
                field?.destroy()
                field = value
                value?.renderer = if (shouldBeRendered) renderer else null
                onLightChanged()
            }
        }

    val light: Light?
        get() = lightInstance?.light

    override var isRendered: Boolean
        get() = super.isRendered
        set(value) {
            lightInstance?.renderer = if (value) renderer else null
            super.isRendered = value
        }

    constructor(
        lightInstance: LightInstance? = null,
        parent: NodeParent? = null,
        position: Vector3 = Vector3(),
        rotationQuaternion: Quaternion = Quaternion(),
        scales: Vector3 = Vector3(1.0f, 1.0f, 1.0f)
    ) : this(position, rotationQuaternion, scales, parent) {
        this.lightInstance = lightInstance
    }

    constructor(node: LightNode) : this(
        position = node.position,
        rotationQuaternion = node.rotationQuaternion,
        scales = node.scales
    ) {
        setLight(node.light)
    }

    open fun onLightChanged() {

    }

    fun setLight(light: Light?): LightInstance? =
        light?.createInstance(this)?.also {
            lightInstance = it
        }

    /** ### Detach and destroy the node */
    override fun destroy() {
        super.destroy()
        lightInstance?.destroy()
    }

    fun copy(toNode: LightNode = LightNode()): LightNode = toNode.apply {
        super.copy(toNode)
        setLight(this@LightNode.light)
    }
}