package io.github.sceneview.geometries

import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.max
import dev.romainguy.kotlin.math.min
import io.github.sceneview.SceneView
import io.github.sceneview.math.Box
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.normalToTangent
import io.github.sceneview.utils.Color
import java.nio.FloatBuffer
import java.nio.IntBuffer

typealias UvCoordinate = Float2

private const val kPositionSize = 3 // x, y, z
private const val kTangentSize = 4 // Quaternion: x, y, z, w
private const val kUVSize = 2 // x, y
private const val kColorSize = 4 // r, g, b, a

/**
 * Geometry parameters for building and updating a Renderable
 *
 * A renderable is made of several primitives.
 * You can ever declare only 1 if you want each parts of your Geometry to have the same material
 * or one for each triangle indices with a different material.
 * We could declare n primitives (n per face) and give each of them a different material
 * instance, setup with different parameters
 *
 * @see Cube
 * @see Cylinder
 * @see Plane
 * @see Sphere
 */
open class Geometry(
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer
) {

    /**
     * Used for constructing renderables dynamically
     *
     * @param uvCoordinate Represents a texture Coordinate for a Vertex.
     * Values should be between 0 and 1.
     */
    data class Vertex(
        val position: Position = Position(),
        val normal: Direction? = null,
        val uvCoordinate: UvCoordinate? = null,
        val color: Color? = null
    )

    /**
     * Represents a Submesh for a Geometry.
     *
     * Each Geometry may have multiple Submeshes.
     */
    data class Submesh(val triangleIndices: List<Int>) {
        constructor(vararg triangleIndices: Int) : this(triangleIndices.toList())
    }

    lateinit var vertices: List<Vertex>
    lateinit var submeshes: List<Submesh>

    lateinit var boundingBox: Box
        private set
    lateinit var offsetsCounts: List<Pair<Int, Int>>
        private set

    open class Builder(
        vertices: List<Vertex> = listOf(),
        submeshes: List<Submesh> = listOf()
    ) : BaseGeometryBuilder<Geometry>(vertices, submeshes) {
        override fun build(
            vertexBuffer: VertexBuffer,
            indexBuffer: IndexBuffer
        ) = Geometry(vertexBuffer, indexBuffer)
    }

    fun setBufferVertices(engine: Engine, vertices: List<Vertex>) {
        this.vertices = vertices

        var bufferIndex = 0

        // Create position Buffer
        vertexBuffer.setBufferAt(
            engine, bufferIndex,
            FloatBuffer.allocate(vertices.size * kPositionSize).apply {
                vertices.forEach { put(it.position.toFloatArray()) }
                // Make sure the cursor is pointing in the right place in the byte buffer
                flip()
            }, 0,
            vertices.size * kPositionSize
        )

        // Create tangents Buffer
        if (vertices.hasNormals) {
            bufferIndex++
            vertexBuffer.setBufferAt(
                engine, bufferIndex,
                FloatBuffer.allocate(vertices.size * kTangentSize).apply {
                    vertices.forEach { put(normalToTangent(it.normal!!).toFloatArray()) }
                    flip()
                }, 0,
                vertices.size * kTangentSize
            )
        }

        // Create UV Buffer
        if (vertices.hasUvCoordinates) {
            bufferIndex++
            vertexBuffer.setBufferAt(
                engine, bufferIndex,
                FloatBuffer.allocate(vertices.size * kUVSize).apply {
                    vertices.forEach { put(it.uvCoordinate!!.toFloatArray()) }
                    rewind()
                }, 0,
                vertices.size * kUVSize
            )
        }

        // Create color Buffer
        if (vertices.hasColors) {
            bufferIndex++
            vertexBuffer.setBufferAt(
                engine, bufferIndex,
                FloatBuffer.allocate(vertices.size * kColorSize).apply {
                    vertices.forEach { put(it.color!!.toFloatArray()) }
                    rewind()
                }, 0,
                vertices.size * kColorSize
            )
        }

        // Calculate the Aabb in one pass through the vertices.
        var minPosition = Position(vertices.first().position)
        var maxPosition = Position(vertices.first().position)
        vertices.forEach { vertex ->
            minPosition = min(minPosition, vertex.position)
            maxPosition = max(maxPosition, vertex.position)
        }
        val halfExtent = (maxPosition - minPosition) * 0.5f
        val center = minPosition + halfExtent
        boundingBox = Box(center, halfExtent)
    }

    fun setBufferIndices(engine: Engine, submeshes: List<Submesh>) {
        this.submeshes = submeshes

        // Fill the index buffer with the data
        indexBuffer.setBuffer(engine,
            IntBuffer.allocate(submeshes.sumOf { it.triangleIndices.size }).apply {
                submeshes.flatMap { it.triangleIndices }.forEach { put(it) }
                flip()
            })

        var indexStart = 0
        offsetsCounts = submeshes.map { submesh ->
            (indexStart to submesh.triangleIndices.size).also {
                indexStart += submesh.triangleIndices.size
            }
        }
    }
}

abstract class BaseGeometryBuilder<T : Geometry> internal constructor(
    var vertices: List<Geometry.Vertex> = listOf(),
    var submeshes: List<Geometry.Submesh> = listOf()
) {

    fun vertices(vertices: List<Geometry.Vertex>) = apply { this.vertices = vertices }
    fun submeshes(submeshes: List<Geometry.Submesh>) = apply { this.submeshes = submeshes }

    open fun build(engine: Engine): T {
        val vertexBuffer = VertexBuffer.Builder().apply {
            bufferCount(
                1 + // Position is never null
                        (if (vertices.hasNormals) 1 else 0) +
                        (if (vertices.hasUvCoordinates) 1 else 0) +
                        (if (vertices.hasColors) 1 else 0)
            )
            vertexCount(vertices.size)

            // Position Attribute
            var bufferIndex = 0
            attribute(
                VertexBuffer.VertexAttribute.POSITION,
                bufferIndex,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                kPositionSize * Float.SIZE_BYTES
            )
            // Tangents Attribute
            if (vertices.hasNormals) {
                bufferIndex++
                attribute(
                    VertexBuffer.VertexAttribute.TANGENTS,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT4,
                    0,
                    kTangentSize * Float.SIZE_BYTES
                )
                normalized(VertexBuffer.VertexAttribute.TANGENTS)
            }
            // Uv Attribute
            if (vertices.hasUvCoordinates) {
                bufferIndex++
                attribute(
                    VertexBuffer.VertexAttribute.UV0,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT2,
                    0,
                    kUVSize * Float.SIZE_BYTES
                )
            }
            // Color Attribute
            if (vertices.hasColors) {
                bufferIndex++
                attribute(
                    VertexBuffer.VertexAttribute.COLOR,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT4,
                    0,
                    kColorSize * Float.SIZE_BYTES
                )
                normalized(VertexBuffer.VertexAttribute.COLOR)
            }
        }.build(engine)

        // Determine how many indices there are
        val indexBuffer = IndexBuffer.Builder().apply {
            indexCount(submeshes.sumOf { it.triangleIndices.size })
                .bufferType(IndexBuffer.Builder.IndexType.UINT)
        }.build(engine)

        return build(vertexBuffer, indexBuffer).apply {
            setBufferVertices(engine, this@BaseGeometryBuilder.vertices)
            setBufferIndices(engine, this@BaseGeometryBuilder.submeshes)
        }
    }

    fun build(sceneView: SceneView) = build(sceneView.engine).also {
        sceneView.geometries += it
    }

    internal abstract fun build(vertexBuffer: VertexBuffer, indexBuffer: IndexBuffer): T
}

val List<Geometry.Vertex>.hasNormals get() = any { it.normal != null }
val List<Geometry.Vertex>.hasUvCoordinates get() = any { it.uvCoordinate != null }
val List<Geometry.Vertex>.hasColors get() = any { it.color != null }

fun Engine.destroyGeometry(geometry: Geometry) {
    destroyVertexBuffer(geometry.vertexBuffer)
    destroyIndexBuffer(geometry.indexBuffer)
}

fun SceneView.destroyGeometry(geometry: Geometry) {
    engine.destroyGeometry(geometry)
    geometries -= geometry
}