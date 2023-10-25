package com.google.ar.sceneform.rendering;

import androidx.annotation.Nullable;

import com.google.android.filament.Engine;
import com.google.android.filament.IndexBuffer;
import com.google.android.filament.IndexBuffer.Builder.IndexType;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.VertexBuffer;
import com.google.android.filament.VertexBuffer.VertexAttribute;
import com.google.ar.sceneform.rendering.Vertex.UvCoordinate;
import com.google.ar.sceneform.utilities.AndroidPreconditions;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import io.github.sceneview.EngineKt;
import io.github.sceneview.collision.MathHelper;
import io.github.sceneview.collision.Matrix;
import io.github.sceneview.collision.Preconditions;
import io.github.sceneview.collision.Quaternion;
import io.github.sceneview.collision.Vector3;

/**
 * Represents the visual information of a {@link Renderable}. Can be used to construct and modify
 * renderables dynamically.
 *
 * @see ModelRenderable.Builder
 * @see ViewRenderable.Builder
 */
public class RenderableDefinition {

    private static final Matrix scratchMatrix = new Matrix();

    /**
     * Represents a Submesh for a RenderableDefinition. Each RenderableDefinition may have multiple
     * Submeshes.
     */
    public static class Submesh {
        private List<Integer> triangleIndices;
        private MaterialInstance material;
        @Nullable
        private String name;

        public void setTriangleIndices(List<Integer> triangleIndices) {
            this.triangleIndices = triangleIndices;
        }

        public List<Integer> getTriangleIndices() {
            return triangleIndices;
        }

        public void setMaterial(MaterialInstance material) {
            this.material = material;
        }

        public MaterialInstance getMaterial() {
            return material;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Nullable
        public String getName() {
            return name;
        }

        private Submesh(Engine engine, Builder builder) {
            triangleIndices = Preconditions.checkNotNull(builder.triangleIndices);
            material = Preconditions.checkNotNull(builder.material);
            name = builder.name;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Factory class for {@link Submesh}.
         */
        public static final class Builder {
            @Nullable
            private List<Integer> triangleIndices;
            @Nullable
            private MaterialInstance material;
            @Nullable
            private String name;

            public Builder setTriangleIndices(List<Integer> triangleIndices) {
                this.triangleIndices = triangleIndices;
                return this;
            }

            public Builder setName(String name) {
                this.name = name;
                return this;
            }

            public Builder setMaterial(MaterialInstance material) {
                this.material = material;
                return this;
            }

            public Submesh build(Engine engine) {
                return new Submesh(engine, this);
            }
        }
    }

    private Engine engine;
    private List<Vertex> vertices;
    private List<Submesh> submeshes;

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int POSITION_SIZE = 3; // x, y, z
    private static final int UV_SIZE = 2;
    private static final int TANGENTS_SIZE = 4; // quaternion
    private static final int COLOR_SIZE = 4; // RGBA

    private RenderableDefinition(Engine engine, Builder builder) {
        this.engine = engine;
        vertices = Preconditions.checkNotNull(builder.vertices);
        submeshes = Preconditions.checkNotNull(builder.submeshes);
    }

    public void setVertices(List<Vertex> vertices) {
        this.vertices = vertices;
    }

    List<Vertex> getVertices() {
        return vertices;
    }

    public void setSubmeshes(List<Submesh> submeshes) {
        this.submeshes = submeshes;
    }

    public List<Submesh> getSubmeshes() {
        return submeshes;
    }

    void applyDefinitionToData(
            // TODO: Split into RenderableInternalSfbData & RenderableInternalDefinitionData
            IRenderableInternalData data,
            ArrayList<MaterialInstance> materialBindings,
            ArrayList<String> materialNames) {
        AndroidPreconditions.checkUiThread();

        applyDefinitionToDataIndexBuffer(data);
        applyDefinitionToDataVertexBuffer(data);

        // Update/Add mesh data.
        int indexStart = 0;
        materialBindings.clear();
        materialNames.clear();
        for (int i = 0; i < submeshes.size(); i++) {
            Submesh submesh = submeshes.get(i);

            RenderableInternalData.MeshData meshData;
            if (i < data.getMeshes().size()) {
                meshData = data.getMeshes().get(i);
            } else {
                meshData = new RenderableInternalData.MeshData();
                data.getMeshes().add(meshData);
            }

            meshData.indexStart = indexStart;
            meshData.indexEnd = indexStart + submesh.getTriangleIndices().size();
            indexStart = meshData.indexEnd;
            materialBindings.add(submesh.getMaterial());
            final String name = submesh.getName();
            materialNames.add(name != null ? name : "");
        }

        // Remove old mesh data.
        while (data.getMeshes().size() > submeshes.size()) {
            data.getMeshes().remove(data.getMeshes().size() - 1);
        }
    }

    private void applyDefinitionToDataIndexBuffer(IRenderableInternalData data) {
        // Determine how many indices there are.
        int numIndices = 0;
        for (int i = 0; i < submeshes.size(); i++) {
            Submesh submesh = submeshes.get(i);
            numIndices += submesh.getTriangleIndices().size();
        }

        // Create the raw index buffer if needed.
        IntBuffer rawIndexBuffer = data.getRawIndexBuffer();
        if (rawIndexBuffer == null || rawIndexBuffer.capacity() < numIndices) {
            rawIndexBuffer = IntBuffer.allocate(numIndices);
            data.setRawIndexBuffer(rawIndexBuffer);
        } else {
            rawIndexBuffer.rewind();
        }

        // Fill the index buffer with the data.
        for (int i = 0; i < submeshes.size(); i++) {
            Submesh submesh = submeshes.get(i);
            List<Integer> triangleIndices = submesh.getTriangleIndices();
            for (int j = 0; j < triangleIndices.size(); j++) {
                rawIndexBuffer.put(triangleIndices.get(j));
            }
        }
        rawIndexBuffer.rewind();

        // Create the filament index buffer if needed.
        IndexBuffer indexBuffer = data.getIndexBuffer();
        if (indexBuffer == null || indexBuffer.getIndexCount() < numIndices) {
            if (indexBuffer != null) {
                EngineKt.safeDestroyIndexBuffer(engine, indexBuffer);
            }

            indexBuffer = new IndexBuffer.Builder()
                    .indexCount(numIndices)
                    .bufferType(IndexType.UINT)
                    .build(engine);
            data.setIndexBuffer(indexBuffer);
        }

        indexBuffer.setBuffer(engine, rawIndexBuffer, 0, numIndices);
    }

    private void applyDefinitionToDataVertexBuffer(IRenderableInternalData data) {
        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("RenderableDescription must have at least one vertex.");
        }

        int numVertices = vertices.size();
        Vertex firstVertex = vertices.get(0);

        // Determine which attributes this VertexBuffer needs.
        EnumSet<VertexAttribute> descriptionAttributes = EnumSet.of(VertexAttribute.POSITION);
        if (firstVertex.getNormal() != null) {
            descriptionAttributes.add(VertexAttribute.TANGENTS);
        }
        if (firstVertex.getUvCoordinate() != null) {
            descriptionAttributes.add(VertexAttribute.UV0);
        }
        if (firstVertex.getColor() != null) {
            descriptionAttributes.add(VertexAttribute.COLOR);
        }

        // Determine if the filament vertex buffer needs to be re-created.
        VertexBuffer vertexBuffer = data.getVertexBuffer();
        boolean createVertexBuffer = true;
        if (vertexBuffer != null) {
            EnumSet<VertexAttribute> oldAttributes = EnumSet.of(VertexAttribute.POSITION);
            if (data.getRawTangentsBuffer() != null) {
                oldAttributes.add(VertexAttribute.TANGENTS);
            }
            if (data.getRawUvBuffer() != null) {
                oldAttributes.add(VertexAttribute.UV0);
            }
            if (data.getRawColorBuffer() != null) {
                oldAttributes.add(VertexAttribute.COLOR);
            }

            createVertexBuffer =
                    !oldAttributes.equals(descriptionAttributes)
                            || vertexBuffer.getVertexCount() < numVertices;

            if (createVertexBuffer) {
                engine.destroyVertexBuffer(vertexBuffer);
            }
        }

        if (createVertexBuffer) {
            vertexBuffer = createVertexBuffer(engine, numVertices, descriptionAttributes);
            data.setVertexBuffer(vertexBuffer);
        }

        // Create position Buffer if needed.
        FloatBuffer positionBuffer = data.getRawPositionBuffer();
        if (positionBuffer == null || positionBuffer.capacity() < numVertices * POSITION_SIZE) {
            positionBuffer = FloatBuffer.allocate(numVertices * POSITION_SIZE);
            data.setRawPositionBuffer(positionBuffer);
        } else {
            positionBuffer.rewind();
        }

        // Create tangents Buffer if needed.
        FloatBuffer tangentsBuffer = data.getRawTangentsBuffer();
        if (descriptionAttributes.contains(VertexAttribute.TANGENTS)
                && (tangentsBuffer == null || tangentsBuffer.capacity() < numVertices * TANGENTS_SIZE)) {
            tangentsBuffer = FloatBuffer.allocate(numVertices * TANGENTS_SIZE);
            data.setRawTangentsBuffer(tangentsBuffer);
        } else if (tangentsBuffer != null) {
            tangentsBuffer.rewind();
        }

        // Create uv Buffer if needed.
        FloatBuffer uvBuffer = data.getRawUvBuffer();
        if (descriptionAttributes.contains(VertexAttribute.UV0)
                && (uvBuffer == null || uvBuffer.capacity() < numVertices * UV_SIZE)) {
            uvBuffer = FloatBuffer.allocate(numVertices * UV_SIZE);
            data.setRawUvBuffer(uvBuffer);
        } else if (uvBuffer != null) {
            uvBuffer.rewind();
        }

        // Create color Buffer if needed.
        FloatBuffer colorBuffer = data.getRawColorBuffer();
        if (descriptionAttributes.contains(VertexAttribute.COLOR)
                && (colorBuffer == null || colorBuffer.capacity() < numVertices * COLOR_SIZE)) {
            colorBuffer = FloatBuffer.allocate(numVertices * COLOR_SIZE);
            data.setRawColorBuffer(colorBuffer);
        } else if (colorBuffer != null) {
            colorBuffer.rewind();
        }

        // Variables for calculating the Aabb of the renderable.
        Vector3 minAabb = new Vector3();
        Vector3 maxAabb = new Vector3();
        Vector3 firstPosition = firstVertex.getPosition();
        minAabb.set(firstPosition);
        maxAabb.set(firstPosition);

        // Update the raw buffers and calculate the Aabb in one pass through the vertices.
        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);

            // Aabb.
            Vector3 position = vertex.getPosition();
            minAabb.set(Vector3.min(minAabb, position));
            maxAabb.set(Vector3.max(maxAabb, position));

            // Position attribute.
            addVector3ToBuffer(position, positionBuffer);

            // Tangents attribute.
            if (tangentsBuffer != null) {
                Vector3 normal = vertex.getNormal();
                if (normal == null) {
                    throw new IllegalArgumentException(
                            "Missing normal: If any Vertex in a "
                                    + "RenderableDescription has a normal, all vertices must have one.");
                }

                Quaternion tangent = normalToTangent(normal);
                addQuaternionToBuffer(tangent, tangentsBuffer);
            }

            // Uv attribute.
            if (uvBuffer != null) {
                UvCoordinate uvCoordinate = vertex.getUvCoordinate();
                if (uvCoordinate == null) {
                    throw new IllegalArgumentException(
                            "Missing UV Coordinate: If any Vertex in a "
                                    + "RenderableDescription has a UV Coordinate, all vertices must have one.");
                }

                addUvToBuffer(uvCoordinate, uvBuffer);
            }

            // Color attribute.
            if (colorBuffer != null) {
                Color color = vertex.getColor();
                if (color == null) {
                    throw new IllegalArgumentException(
                            "Missing Color: If any Vertex in a "
                                    + "RenderableDescription has a Color, all vertices must have one.");
                }

                addColorToBuffer(color, colorBuffer);
            }
        }

        // Set the Aabb in the renderable data.
        Vector3 extentsAabb = Vector3.subtract(maxAabb, minAabb).scaled(0.5f);
        Vector3 centerAabb = Vector3.add(minAabb, extentsAabb);
        data.setExtentsAabb(extentsAabb);
        data.setCenterAabb(centerAabb);

        if (vertexBuffer == null) {
            throw new AssertionError("VertexBuffer is null.");
        }

        positionBuffer.rewind();
        int bufferIndex = 0;
        vertexBuffer.setBufferAt(engine, bufferIndex, positionBuffer, 0, numVertices * POSITION_SIZE);

        if (tangentsBuffer != null) {
            tangentsBuffer.rewind();
            bufferIndex++;
            vertexBuffer.setBufferAt(engine, bufferIndex, tangentsBuffer, 0, numVertices * TANGENTS_SIZE);
        }

        if (uvBuffer != null) {
            uvBuffer.rewind();
            bufferIndex++;
            vertexBuffer.setBufferAt(engine, bufferIndex, uvBuffer, 0, numVertices * UV_SIZE);
        }

        if (colorBuffer != null) {
            colorBuffer.rewind();
            bufferIndex++;
            vertexBuffer.setBufferAt(engine, bufferIndex, colorBuffer, 0, numVertices * COLOR_SIZE);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static VertexBuffer createVertexBuffer(Engine engine, int vertexCount, EnumSet<VertexAttribute> attributes) {
        VertexBuffer.Builder builder = new VertexBuffer.Builder();

        builder.vertexCount(vertexCount).bufferCount(attributes.size());

        // Position Attribute.
        int bufferIndex = 0;
        builder.attribute(
                VertexAttribute.POSITION,
                bufferIndex,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                POSITION_SIZE * BYTES_PER_FLOAT);

        // Tangents Attribute.
        if (attributes.contains(VertexAttribute.TANGENTS)) {
            bufferIndex++;
            builder.attribute(
                    VertexAttribute.TANGENTS,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT4,
                    0,
                    TANGENTS_SIZE * BYTES_PER_FLOAT);
        }

        // Uv Attribute.
        if (attributes.contains(VertexAttribute.UV0)) {
            bufferIndex++;
            builder.attribute(
                    VertexAttribute.UV0,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT2,
                    0,
                    UV_SIZE * BYTES_PER_FLOAT);
        }

        // Color Attribute.
        if (attributes.contains(VertexAttribute.COLOR)) {
            bufferIndex++;
            builder.attribute(
                    VertexAttribute.COLOR,
                    bufferIndex,
                    VertexBuffer.AttributeType.FLOAT4,
                    0,
                    COLOR_SIZE * BYTES_PER_FLOAT);
        }

        // VertexBufferKt destroy manually handled (not lifecycle aware)
        return builder.build(engine);
    }

    private static void addVector3ToBuffer(Vector3 vector3, FloatBuffer buffer) {
        buffer.put(vector3.x);
        buffer.put(vector3.y);
        buffer.put(vector3.z);
    }

    private static void addUvToBuffer(UvCoordinate uvCoordinate, FloatBuffer buffer) {
        buffer.put(uvCoordinate.x);
        buffer.put(uvCoordinate.y);
    }

    private static void addQuaternionToBuffer(Quaternion quaternion, FloatBuffer buffer) {
        buffer.put(quaternion.x);
        buffer.put(quaternion.y);
        buffer.put(quaternion.z);
        buffer.put(quaternion.w);
    }

    private static void addColorToBuffer(Color color, FloatBuffer buffer) {
        buffer.put(color.r);
        buffer.put(color.g);
        buffer.put(color.b);
        buffer.put(color.a);
    }

    private static Quaternion normalToTangent(Vector3 normal) {
        Vector3 tangent;
        Vector3 bitangent;

        // Calculate basis vectors (+x = tangent, +y = bitangent, +z = normal).
        tangent = Vector3.cross(Vector3.up(), normal);

        // Uses almostEqualRelativeAndAbs for equality checks that account for float inaccuracy.
        if (MathHelper.almostEqualRelativeAndAbs(Vector3.dot(tangent, tangent), 0.0f)) {
            bitangent = Vector3.cross(normal, Vector3.right()).normalized();
            tangent = Vector3.cross(bitangent, normal).normalized();
        } else {
            tangent.set(tangent.normalized());
            bitangent = Vector3.cross(normal, tangent).normalized();
        }

        // Rotation of a 4x4 Transformation Matrix is represented by the top-left 3x3 elements.
        final int rowOne = 0;
        scratchMatrix.data[rowOne] = tangent.x;
        scratchMatrix.data[rowOne + 1] = tangent.y;
        scratchMatrix.data[rowOne + 2] = tangent.z;

        final int rowTwo = 4;
        scratchMatrix.data[rowTwo] = bitangent.x;
        scratchMatrix.data[rowTwo + 1] = bitangent.y;
        scratchMatrix.data[rowTwo + 2] = bitangent.z;

        final int rowThree = 8;
        scratchMatrix.data[rowThree] = normal.x;
        scratchMatrix.data[rowThree + 1] = normal.y;
        scratchMatrix.data[rowThree + 2] = normal.z;

        Quaternion orientationQuaternion = new Quaternion();
        scratchMatrix.extractQuaternion(orientationQuaternion);
        return orientationQuaternion;
    }

    /**
     * Factory class for {@link RenderableDefinition}.
     */
    public static final class Builder {
        @Nullable
        private List<Vertex> vertices;
        @Nullable
        private List<Submesh> submeshes = new ArrayList<>();

        public Builder setVertices(List<Vertex> vertices) {
            this.vertices = vertices;
            return this;
        }

        public Builder setSubmeshes(List<Submesh> submeshes) {
            this.submeshes = submeshes;
            return this;
        }

        public RenderableDefinition build(Engine engine) {
            return new RenderableDefinition(engine, this);
        }
    }
}
