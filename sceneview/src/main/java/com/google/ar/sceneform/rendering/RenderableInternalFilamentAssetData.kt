package com.google.ar.sceneform.rendering

import android.content.Context
import android.net.Uri
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.VertexBuffer
import com.google.ar.sceneform.rendering.MeshData
import io.github.sceneview.collision.Vector3
import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.function.Function

/** Represents the data used by a [Renderable] for rendering natively loaded glTF data. */
@Suppress("AndroidJdkLibsChecker")
class RenderableInternalFilamentAssetData : IRenderableInternalData {

    var context: Context? = null
    var gltfByteBuffer: Buffer? = null
    var isGltfBinary: Boolean = false
    var urlResolver: Function<String, Uri>? = null

    override fun setCenterAabb(center: Vector3) {
        // Not Implemented
    }

    override fun getCenterAabb(): Vector3 {
        // Not Implemented
        return Vector3.zero()
    }

    override fun setExtentsAabb(halfExtents: Vector3) {
        // Not Implemented
    }

    override fun getExtentsAabb(): Vector3 {
        throw IllegalStateException("Not Implemented")
    }

    override fun getSizeAabb(): Vector3 {
        // Not Implemented
        return Vector3.zero()
    }

    override fun setTransformScale(scale: Float) {
        // Not Implemented
    }

    override fun getTransformScale(): Float {
        // Not Implemented
        return 1.0f
    }

    override fun setTransformOffset(offset: Vector3) {
        // Not Implemented
    }

    override fun getTransformOffset(): Vector3 {
        // Not Implemented
        return Vector3.zero()
    }

    override fun getMeshes(): ArrayList<MeshData> {
        // Not Implemented
        return ArrayList(1)
    }

    fun getMaterialBindingIds(): ArrayList<Int> {
        // Not Implemented
        return ArrayList()
    }

    override fun setIndexBuffer(indexBuffer: IndexBuffer?) {
        // Not Implemented
    }

    override fun getIndexBuffer(): IndexBuffer? {
        // Not Implemented
        return null
    }

    override fun setVertexBuffer(vertexBuffer: VertexBuffer?) {
        // Not Implemented
    }

    override fun getVertexBuffer(): VertexBuffer? {
        // Not Implemented
        return null
    }

    override fun setRawIndexBuffer(rawIndexBuffer: IntBuffer?) {
        // Not Implemented
    }

    override fun getRawIndexBuffer(): IntBuffer? {
        // Not Implemented
        return null
    }

    override fun setRawPositionBuffer(rawPositionBuffer: FloatBuffer?) {
        // Not Implemented
    }

    override fun getRawPositionBuffer(): FloatBuffer? {
        // Not Implemented
        return null
    }

    override fun setRawTangentsBuffer(rawTangentsBuffer: FloatBuffer?) {
        // Not Implemented
    }

    override fun getRawTangentsBuffer(): FloatBuffer? {
        // Not Implemented
        return null
    }

    override fun setRawUvBuffer(rawUvBuffer: FloatBuffer?) {
        // Not Implemented
    }

    override fun getRawUvBuffer(): FloatBuffer? {
        // Not Implemented
        return null
    }

    override fun setRawColorBuffer(rawColorBuffer: FloatBuffer?) {
        // Not Implemented
    }

    override fun getRawColorBuffer(): FloatBuffer? {
        // Not Implemented
        return null
    }

    override fun setAnimationNames(animationNames: List<String>) {
        // Not Implemented
    }

    override fun getAnimationNames(): List<String> {
        // Not Implemented
        return ArrayList()
    }

    override fun buildInstanceData(engine: Engine, instance: RenderableInstance, renderedEntity: Int) {
    }
}
