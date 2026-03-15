package com.google.ar.sceneform.rendering

import io.github.sceneview.collision.Vector3

/**
 * Represents a Vertex for a [RenderableDefinition]. Used for constructing renderables
 * dynamically.
 *
 * @see ModelRenderable.Builder
 * @see ViewRenderable.Builder
 */
class Vertex private constructor(builder: Builder) {
    /** Represents a texture Coordinate for a Vertex. Values should be between 0 and 1. */
    class UvCoordinate(
        @JvmField var x: Float,
        @JvmField var y: Float
    )

    // Required.
    private val position: Vector3 = Vector3.zero()

    // Optional.
    private var normal: Vector3? = null
    private var uvCoordinate: UvCoordinate? = null
    private var color: Color? = null

    init {
        position.set(builder.position)
        normal = builder.normal
        uvCoordinate = builder.uvCoordinate
        color = builder.color
    }

    fun setPosition(position: Vector3) {
        this.position.set(position)
    }

    fun getPosition(): Vector3 = position

    fun setNormal(normal: Vector3?) {
        this.normal = normal
    }

    fun getNormal(): Vector3? = normal

    fun setUvCoordinate(uvCoordinate: UvCoordinate?) {
        this.uvCoordinate = uvCoordinate
    }

    fun getUvCoordinate(): UvCoordinate? = uvCoordinate

    fun setColor(color: Color?) {
        this.color = color
    }

    fun getColor(): Color? = color

    /** Factory class for [Vertex]. */
    class Builder {
        // Required.
        val position: Vector3 = Vector3.zero()

        // Optional.
        var normal: Vector3? = null
        var uvCoordinate: UvCoordinate? = null
        var color: Color? = null

        fun setPosition(position: Vector3): Builder {
            this.position.set(position)
            return this
        }

        fun setNormal(normal: Vector3?): Builder {
            this.normal = normal
            return this
        }

        fun setUvCoordinate(uvCoordinate: UvCoordinate?): Builder {
            this.uvCoordinate = uvCoordinate
            return this
        }

        fun setColor(color: Color?): Builder {
            this.color = color
            return this
        }

        fun build(): Vertex = Vertex(this)
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
