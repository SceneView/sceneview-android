package io.github.sceneview.node

import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.LightInstance
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a light for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class LightNode : Node {

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
                field?.renderer = null
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

    /**
     * ### Construct a [LightNode] with it Position, Rotation and Scale
     *
     * @param position See [Node.position]
     * @param rotation See [Node.rotation]
     * @param scale See [Node.scale]
     */
    constructor(
        position: Position = DEFAULT_POSITION,
        rotation: Rotation = DEFAULT_ROTATION,
        scale: Scale = DEFAULT_SCALE
    ) : super(position, rotation, scale)

    /**
     * TODO : Doc
     */
    constructor(light: Light) : this() {
        setLight(light)
    }

    /**
     * TODO : Doc
     */
    constructor(lightInstance: LightInstance) : this() {
        this.lightInstance = lightInstance
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

    override fun clone() = copy(LightNode())

    fun copy(toNode: LightNode = LightNode()): LightNode = toNode.apply {
        super.copy(toNode)
        setLight(this@LightNode.light)
    }
}