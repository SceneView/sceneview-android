package com.google.ar.sceneform.rendering

import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.IndexBuffer.Builder.IndexType
import com.google.android.filament.MaterialInstance
import com.google.android.filament.VertexBuffer
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.ar.sceneform.rendering.Vertex.UvCoordinate
import com.google.ar.sceneform.utilities.AndroidPreconditions
import io.github.sceneview.safeDestroyIndexBuffer
import io.github.sceneview.collision.MathHelper
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.Preconditions
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Vector3
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.EnumSet

/**
 * Represents the visual information of a [Renderable]. Can be used to construct and modify
 * renderables dynamically.
 *
 * @see ModelRenderable.Builder
 * @see ViewRenderable.Builder
 */
class RenderableDefinition private constructor(private val engine: Engine, builder: Builder) {

    /**
     * Represents a Submesh for a RenderableDefinition. Each RenderableDefinition may have multiple
     * Submeshes.
     */
    class Submesh private constructor(engine: Engine, builder: Builder) {
        var triangleIndices: List<Int> = Preconditions.checkNotNull(builder.triangleIndices)
        var material: MaterialInstance = Preconditions.checkNotNull(builder.material)
        var name: String? = builder.name

        /** Factory class for [Submesh]. */
        class Builder {
            var triangleIndices: List<Int>? = null
            var material: MaterialInstance? = null
            var name: String? = null

            fun setTriangleIndices(triangleIndices: List<Int>): Builder {
                this.triangleIndices = triangleIndices
                return this
            }

            fun setName(name: String): Builder {
                this.name = name
                return this
            }

            fun setMaterial(material: MaterialInstance): Builder {
                this.material = material
                return this
            }

            fun build(engine: Engine): Submesh = Submesh(engine, this)
        }

        companion object {
            @JvmStatic
            fun builder(): Builder = Builder()
        }
    }

    private var vertices: List<Vertex> = Preconditions.checkNotNull(builder.vertices)
    private var submeshes: List<Submesh> = Preconditions.checkNotNull(builder.submeshes)

    fun setVertices(vertices: List<Vertex>) {
        this.vertices = vertices
    }

    fun getVertices(): List<Vertex> = vertices

    fun setSubmeshes(submeshes: List<Submesh>) {
        this.submeshes = submeshes
    }

    fun getSubmeshes(): List<Submesh> = submeshes

    fun applyDefinitionToData(
        // TODO: Split into RenderableInternalSfbData & RenderableInternalDefinitionData
        data: IRenderableInternalData,
        materialBindings: ArrayList<MaterialInstance>,
        materialNames: ArrayList<String>
    ) {
        AndroidPreconditions.checkUiThread()

        applyDefinitionToDataIndexBuffer(data)
        applyDefinitionToDataVertexBuffer(data)

        // Update/Add mesh data.
        var indexStart = 0
        materialBindings.clear()
        materialNames.clear()
        for (i in submeshes.indices) {
            val submesh = submeshes[i]

            val meshData: MeshData
            if (i < data.getMeshes().size) {
                meshData = data.getMeshes()[i]
            } else {
                meshData = MeshData()
                data.getMeshes().add(meshData)
            }

            meshData.indexStart = indexStart
            meshData.indexEnd = indexStart + submesh.triangleIndices.size
            indexStart = meshData.indexEnd
            materialBindings.add(submesh.material)
            val name = submesh.name
            materialNames.add(name ?: "")
        }

        // Remove old mesh data.
        while (data.getMeshes().size > submeshes.size) {
            data.getMeshes().removeAt(data.getMeshes().size - 1)
        }
    }

    private fun applyDefinitionToDataIndexBuffer(data: IRenderableInternalData) {
        // Determine how many indices there are.
        var numIndices = 0
        for (submesh in submeshes) {
            numIndices += submesh.triangleIndices.size
        }

        // Create the raw index buffer if needed.
        var rawIndexBuffer = data.getRawIndexBuffer()
        if (rawIndexBuffer == null || rawIndexBuffer.capacity() < numIndices) {
            rawIndexBuffer = IntBuffer.allocate(numIndices)
            data.setRawIndexBuffer(rawIndexBuffer)
        } else {
            rawIndexBuffer.rewind()
        }

        // Fill the index buffer with the data.
        for (submesh in submeshes) {
            val triangleIndices = submesh.triangleIndices
            for (index in triangleIndices) {
                rawIndexBuffer.put(index)
            }
        }
        rawIndexBuffer.rewind()

        // Create the filament index buffer if needed.
        var indexBuffer = data.getIndexBuffer()
        if (indexBuffer == null || indexBuffer.indexCount < numIndices) {
            if (indexBuffer != null) {
                engine.safeDestroyIndexBuffer(indexBuffer)
            }

            indexBuffer = IndexBuffer.Builder()
                .indexCount(numIndices)
                .bufferType(IndexType.UINT)
                .build(engine)
            data.setIndexBuffer(indexBuffer)
        }

        indexBuffer.setBuffer(engine, rawIndexBuffer, 0, numIndices)
    }

    private fun applyDefinitionToDataVertexBuffer(data: IRenderableInternalData) {
        if (vertices.isEmpty()) {
            throw IllegalArgumentException("RenderableDescription must have at least one vertex.")
        }

        val numVertices = vertices.size
        val firstVertex = vertices[0]

        // Determine which attributes this VertexBuffer needs.
        val descriptionAttributes = EnumSet.of(VertexAttribute.POSITION)
        if (firstVertex.getNormal() != null) {
            descriptionAttributes.add(VertexAttribute.TANGENTS)
        }
        if (firstVertex.getUvCoordinate() != null) {
            descriptionAttributes.add(VertexAttribute.UV0)
        }
        if (firstVertex.getColor() != null) {
            descriptionAttributes.add(VertexAttribute.COLOR)
        }

        // Determine if the filament vertex buffer needs to be re-created.
        var vertexBuffer = data.getVertexBuffer()
        var createVertexBuffer = true
        if (vertexBuffer != null) {
            val oldAttributes = EnumSet.of(VertexAttribute.POSITION)
            if (data.getRawTangentsBuffer() != null) oldAttributes.add(VertexAttribute.TANGENTS)
            if (data.getRawUvBuffer() != null) oldAttributes.add(VertexAttribute.UV0)
            if (data.getRawColorBuffer() != null) oldAttributes.add(VertexAttribute.COLOR)

            createVertexBuffer = !oldAttributes.equals(descriptionAttributes)
                    || vertexBuffer.vertexCount < numVertices

            if (createVertexBuffer) {
                engine.destroyVertexBuffer(vertexBuffer)
            }
        }

        if (createVertexBuffer) {
            vertexBuffer = createVertexBuffer(engine, numVertices, descriptionAttributes)
            data.setVertexBuffer(vertexBuffer)
        }

        // Create position Buffer if needed.
        var positionBuffer = data.getRawPositionBuffer()
        if (positionBuffer == null || positionBuffer.capacity() < numVertices * POSITION_SIZE) {
            positionBuffer = FloatBuffer.allocate(numVertices * POSITION_SIZE)
            data.setRawPositionBuffer(positionBuffer)
        } else {
            positionBuffer.rewind()
        }

        // Create tangents Buffer if needed.
        var tangentsBuffer = data.getRawTangentsBuffer()
        if (descriptionAttributes.contains(VertexAttribute.TANGENTS)
            && (tangentsBuffer == null || tangentsBuffer.capacity() < numVertices * TANGENTS_SIZE)
        ) {
            tangentsBuffer = FloatBuffer.allocate(numVertices * TANGENTS_SIZE)
            data.setRawTangentsBuffer(tangentsBuffer)
        } else {
            tangentsBuffer?.rewind()
        }

        // Create uv Buffer if needed.
        var uvBuffer = data.getRawUvBuffer()
        if (descriptionAttributes.contains(VertexAttribute.UV0)
            && (uvBuffer == null || uvBuffer.capacity() < numVertices * UV_SIZE)
        ) {
            uvBuffer = FloatBuffer.allocate(numVertices * UV_SIZE)
            data.setRawUvBuffer(uvBuffer)
        } else {
            uvBuffer?.rewind()
        }

        // Create color Buffer if needed.
        var colorBuffer = data.getRawColorBuffer()
        if (descriptionAttributes.contains(VertexAttribute.COLOR)
            && (colorBuffer == null || colorBuffer.capacity() < numVertices * COLOR_SIZE)
        ) {
            colorBuffer = FloatBuffer.allocate(numVertices * COLOR_SIZE)
            data.setRawColorBuffer(colorBuffer)
        } else {
            colorBuffer?.rewind()
        }

        // Variables for calculating the Aabb of the renderable.
        val minAabb = Vector3()
        val maxAabb = Vector3()
        val firstPosition = firstVertex.getPosition()
        minAabb.set(firstPosition)
        maxAabb.set(firstPosition)

        // Update the raw buffers and calculate the Aabb in one pass through the vertices.
        for (vertex in vertices) {
            // Aabb.
            val position = vertex.getPosition()
            minAabb.set(Vector3.min(minAabb, position))
            maxAabb.set(Vector3.max(maxAabb, position))

            // Position attribute.
            addVector3ToBuffer(position, positionBuffer)

            // Tangents attribute.
            if (tangentsBuffer != null) {
                val normal = vertex.getNormal()
                    ?: throw IllegalArgumentException(
                        "Missing normal: If any Vertex in a " +
                                "RenderableDescription has a normal, all vertices must have one."
                    )

                val tangent = normalToTangent(normal)
                addQuaternionToBuffer(tangent, tangentsBuffer)
            }

            // Uv attribute.
            if (uvBuffer != null) {
                val uvCoordinate = vertex.getUvCoordinate()
                    ?: throw IllegalArgumentException(
                        "Missing UV Coordinate: If any Vertex in a " +
                                "RenderableDescription has a UV Coordinate, all vertices must have one."
                    )

                addUvToBuffer(uvCoordinate, uvBuffer)
            }

            // Color attribute.
            if (colorBuffer != null) {
                val color = vertex.getColor()
                    ?: throw IllegalArgumentException(
                        "Missing Color: If any Vertex in a " +
                                "RenderableDescription has a Color, all vertices must have one."
                    )

                addColorToBuffer(color, colorBuffer)
            }
        }

        // Set the Aabb in the renderable data.
        val extentsAabb = Vector3.subtract(maxAabb, minAabb).scaled(0.5f)
        val centerAabb = Vector3.add(minAabb, extentsAabb)
        data.setExtentsAabb(extentsAabb)
        data.setCenterAabb(centerAabb)

        if (vertexBuffer == null) {
            throw AssertionError("VertexBuffer is null.")
        }

        positionBuffer.rewind()
        var bufferIndex = 0
        vertexBuffer.setBufferAt(engine, bufferIndex, positionBuffer, 0, numVertices * POSITION_SIZE)

        if (tangentsBuffer != null) {
            tangentsBuffer.rewind()
            bufferIndex++
            vertexBuffer.setBufferAt(engine, bufferIndex, tangentsBuffer, 0, numVertices * TANGENTS_SIZE)
        }

        if (uvBuffer != null) {
            uvBuffer.rewind()
            bufferIndex++
            vertexBuffer.setBufferAt(engine, bufferIndex, uvBuffer, 0, numVertices * UV_SIZE)
        }

        if (colorBuffer != null) {
            colorBuffer.rewind()
            bufferIndex++
            vertexBuffer.setBufferAt(engine, bufferIndex, colorBuffer, 0, numVertices * COLOR_SIZE)
        }
    }

    /** Factory class for [RenderableDefinition]. */
    class Builder {
        var vertices: List<Vertex>? = null
        var submeshes: List<Submesh>? = ArrayList()

        fun setVertices(vertices: List<Vertex>): Builder {
            this.vertices = vertices
            return this
        }

        fun setSubmeshes(submeshes: List<Submesh>): Builder {
            this.submeshes = submeshes
            return this
        }

        fun build(engine: Engine): RenderableDefinition = RenderableDefinition(engine, this)
    }

    companion object {
        private val scratchMatrix = Matrix()

        private const val BYTES_PER_FLOAT = Float.SIZE_BYTES
        private const val POSITION_SIZE = 3 // x, y, z
        private const val UV_SIZE = 2
        private const val TANGENTS_SIZE = 4 // quaternion
        private const val COLOR_SIZE = 4 // RGBA

        @JvmStatic
        fun builder(): Builder = Builder()

        private fun createVertexBuffer(
            engine: Engine,
            vertexCount: Int,
            attributes: EnumSet<VertexAttribute>
        ): VertexBuffer {
            val builder = VertexBuffer.Builder()
            builder.vertexCount(vertexCount).bufferCount(attributes.size)

            // Position Attribute.
            var bufferIndex = 0
            builder.attribute(
                VertexAttribute.POSITION,
                bufferIndex,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                POSITION_SIZE * BYTES_PER_FLOAT
            )

            // Tangents Attribute.
            if (attributes.contains(VertexAttribute.TANGENTS)) {
                bufferIndex++
                builder.attribute(
                    VertexAttribute.TANGENTS,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT4,
                    0,
                    TANGENTS_SIZE * BYTES_PER_FLOAT
                )
            }

            // Uv Attribute.
            if (attributes.contains(VertexAttribute.UV0)) {
                bufferIndex++
                builder.attribute(
                    VertexAttribute.UV0,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT2,
                    0,
                    UV_SIZE * BYTES_PER_FLOAT
                )
            }

            // Color Attribute.
            if (attributes.contains(VertexAttribute.COLOR)) {
                bufferIndex++
                builder.attribute(
                    VertexAttribute.COLOR,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT4,
                    0,
                    COLOR_SIZE * BYTES_PER_FLOAT
                )
            }

            // VertexBufferKt destroy manually handled (not lifecycle aware)
            return builder.build(engine)
        }

        private fun addVector3ToBuffer(vector3: Vector3, buffer: FloatBuffer) {
            buffer.put(vector3.x)
            buffer.put(vector3.y)
            buffer.put(vector3.z)
        }

        private fun addUvToBuffer(uvCoordinate: UvCoordinate, buffer: FloatBuffer) {
            buffer.put(uvCoordinate.x)
            buffer.put(uvCoordinate.y)
        }

        private fun addQuaternionToBuffer(quaternion: Quaternion, buffer: FloatBuffer) {
            buffer.put(quaternion.x)
            buffer.put(quaternion.y)
            buffer.put(quaternion.z)
            buffer.put(quaternion.w)
        }

        private fun addColorToBuffer(color: Color, buffer: FloatBuffer) {
            buffer.put(color.r)
            buffer.put(color.g)
            buffer.put(color.b)
            buffer.put(color.a)
        }

        private fun normalToTangent(normal: Vector3): Quaternion {
            val tangent: Vector3
            val bitangent: Vector3

            // Calculate basis vectors (+x = tangent, +y = bitangent, +z = normal).
            var t = Vector3.cross(Vector3.up(), normal)

            // Uses almostEqualRelativeAndAbs for equality checks that account for float inaccuracy.
            if (MathHelper.almostEqualRelativeAndAbs(Vector3.dot(t, t), 0.0f)) {
                bitangent = Vector3.cross(normal, Vector3.right()).normalized()
                tangent = Vector3.cross(bitangent, normal).normalized()
            } else {
                t.set(t.normalized())
                tangent = t
                bitangent = Vector3.cross(normal, tangent).normalized()
            }

            // Rotation of a 4x4 Transformation Matrix is represented by the top-left 3x3 elements.
            val rowOne = 0
            scratchMatrix.data[rowOne] = tangent.x
            scratchMatrix.data[rowOne + 1] = tangent.y
            scratchMatrix.data[rowOne + 2] = tangent.z

            val rowTwo = 4
            scratchMatrix.data[rowTwo] = bitangent.x
            scratchMatrix.data[rowTwo + 1] = bitangent.y
            scratchMatrix.data[rowTwo + 2] = bitangent.z

            val rowThree = 8
            scratchMatrix.data[rowThree] = normal.x
            scratchMatrix.data[rowThree + 1] = normal.y
            scratchMatrix.data[rowThree + 2] = normal.z

            val orientationQuaternion = Quaternion()
            scratchMatrix.extractQuaternion(orientationQuaternion)
            return orientationQuaternion
        }
    }
}
