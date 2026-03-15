package com.google.ar.sceneform.rendering

import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import com.google.ar.sceneform.rendering.MeshData
import io.github.sceneview.collision.Vector3
import java.nio.FloatBuffer
import java.nio.IntBuffer

// TODO: Split IRenderableInternalData into RenderableInternalSfbData and
// RenderableInternalDefinitionData
interface IRenderableInternalData {

    fun setCenterAabb(minAabb: Vector3)

    fun getCenterAabb(): Vector3

    fun setExtentsAabb(maxAabb: Vector3)

    fun getExtentsAabb(): Vector3

    fun getSizeAabb(): Vector3

    fun setTransformScale(scale: Float)

    fun getTransformScale(): Float

    fun setTransformOffset(offset: Vector3)

    fun getTransformOffset(): Vector3

    fun getMeshes(): ArrayList<MeshData>

    fun setIndexBuffer(indexBuffer: IndexBuffer?)

    fun getIndexBuffer(): IndexBuffer?

    fun setVertexBuffer(vertexBuffer: VertexBuffer?)

    fun getVertexBuffer(): VertexBuffer?

    fun setRawIndexBuffer(rawIndexBuffer: IntBuffer?)

    fun getRawIndexBuffer(): IntBuffer?

    fun setRawPositionBuffer(rawPositionBuffer: FloatBuffer?)

    fun getRawPositionBuffer(): FloatBuffer?

    fun setRawTangentsBuffer(rawTangentsBuffer: FloatBuffer?)

    fun getRawTangentsBuffer(): FloatBuffer?

    fun setRawUvBuffer(rawUvBuffer: FloatBuffer?)

    fun getRawUvBuffer(): FloatBuffer?

    fun setRawColorBuffer(rawColorBuffer: FloatBuffer?)

    fun getRawColorBuffer(): FloatBuffer?

    fun setAnimationNames(animationNames: List<String>)

    fun getAnimationNames(): List<String>

    fun buildInstanceData(engine: Engine, instance: RenderableInstance, @Entity renderedEntity: Int)
}
