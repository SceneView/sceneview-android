package io.github.sceneview.ar

import androidx.annotation.Nullable
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Scene
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.rendering.RenderableDefinition.Submesh
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.rendering.Vertex
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.TransformProvider
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.ModelLoader
import java.util.concurrent.ExecutionException

/**
 * Renders a single ARCore Plane.
 */
class PlaneVisualizer(
    private val engine: Engine,
    private val modelLoader: ModelLoader,
    private val scene: Scene,
    private val plane: Plane
) : TransformProvider {

    companion object {
        private val TAG = PlaneVisualizer::class.java.simpleName

        private const val VERTS_PER_BOUNDARY_VERT = 2

        // Feather distance 0.2 meters.
        private const val FEATHER_LENGTH = 0.2f

        // Feather scale over the distance between plane center and vertices.
        private const val FEATHER_SCALE = 0.2f
    }

    private val planeMatrix = Matrix()

    private var isPlaneAddedToScene = false
    private var isEnabled = true
    private var isShadowReceiver = false
    private var isVisible = false

    private var planeRenderable: ModelRenderable? = null
    private var planeRenderableInstance: RenderableInstance? = null

    private val vertices = ArrayList<Vertex>()
    private val triangleIndices = ArrayList<Int>()
    private val renderableDefinition: RenderableDefinition = RenderableDefinition
        .builder()
        .setVertices(vertices)
        .build(engine)

    private var planeSubmesh: Submesh? = null
    private var shadowSubmesh: Submesh? = null

    fun setEnabled(enabled: Boolean) {
        if (isEnabled != enabled) {
            isEnabled = enabled
            updatePlane()
        }
    }

    fun setShadowReceiver(shadowReceiver: Boolean) {
        if (isShadowReceiver != shadowReceiver) {
            isShadowReceiver = shadowReceiver
            updatePlane()
        }
    }

    fun setVisible(visible: Boolean) {
        if (isVisible != visible) {
            isVisible = visible
            updatePlane()
        }
    }

    fun setPlaneMaterial(materialInstance: MaterialInstance) {
        if (planeSubmesh == null) {
            planeSubmesh = Submesh.builder()
                .setTriangleIndices(triangleIndices)
                .setMaterial(materialInstance)
                .build(engine)
        } else {
            planeSubmesh!!.material = materialInstance
        }

        if (planeRenderable != null) {
            updateRenderable()
        }
    }

    fun setShadowMaterial(materialInstance: MaterialInstance) {
        if (shadowSubmesh == null) {
            shadowSubmesh = Submesh.builder()
                .setTriangleIndices(triangleIndices)
                .setMaterial(materialInstance)
                .build(engine)
        } else {
            shadowSubmesh!!.material = materialInstance
        }

        if (planeRenderable != null) {
            updateRenderable()
        }
    }

    override fun getTransformationMatrix(): Matrix {
        return planeMatrix
    }

    fun updatePlane() {
        if (!isEnabled || (!isVisible && !isShadowReceiver)) {
            removePlaneFromScene()
            return
        }

        if (plane.trackingState != TrackingState.TRACKING) {
            removePlaneFromScene()
            return
        }

        // Set the transformation matrix to the pose of the plane.
        plane.centerPose.toMatrix(planeMatrix.data, 0)

        // Calculate the mesh for the plane.
        val success = updateRenderableDefinitionForPlane()
        if (!success) {
            removePlaneFromScene()
            return
        }

        updateRenderable()
        addPlaneToScene()
    }

    @Suppress("AndroidApiChecker", "FutureReturnValueIgnored")
    internal fun updateRenderable() {
        // The order of the meshes is important: plane before shadow, so blend order indices match.
        val submeshes = buildList {
            if (isVisible && planeSubmesh != null) add(planeSubmesh!!)
            if (isShadowReceiver && shadowSubmesh != null) add(shadowSubmesh!!)
        }

        if (submeshes.isEmpty()) {
            removePlaneFromScene()
            return
        }

        renderableDefinition.setSubmeshes(submeshes)

        if (planeRenderable == null) {
            try {
                planeRenderable = ModelRenderable.builder()
                    .setSource(renderableDefinition)
                    .build(engine)
                    .get()
                planeRenderable!!.setShadowCaster(false)
                planeRenderable!!.setShadowReceiver(true)
                // Creating a Renderable is immediate when using RenderableDefinition.
            } catch (ex: InterruptedException) {
                throw AssertionError("Unable to create plane renderable.")
            } catch (ex: ExecutionException) {
                throw AssertionError("Unable to create plane renderable.")
            }
            planeRenderableInstance = planeRenderable!!.createInstance(
                engine,
                modelLoader.assetLoader,
                modelLoader.resourceLoader,
                this
            )
        } else {
            planeRenderable!!.updateFromDefinition(renderableDefinition)
        }

        // The plane must always be drawn before the shadow, we use the blendOrder to enforce that.
        // this works because both sub-meshes will be sorted at the same distance from the camera
        // since they're part of the same renderable. The blendOrder, determines the sorting in
        // that situation.
        if (planeRenderableInstance != null && submeshes.size > 1) {
            planeRenderableInstance!!.setBlendOrderAt(0, 0) // plane
            planeRenderableInstance!!.setBlendOrderAt(1, 1) // shadow
        }
        planeRenderableInstance!!.prepareForDraw(engine)

        val transformManager = engine.transformManager
        val instance = transformManager.getInstance(planeRenderableInstance!!.entity)
        transformManager.setTransform(instance, planeRenderableInstance!!.getWorldModelMatrix().data)
    }

    fun destroy() {
        removePlaneFromScene()

        planeRenderableInstance?.destroy()
        planeRenderable = null
    }

    private fun addPlaneToScene() {
        if (isPlaneAddedToScene || planeRenderableInstance == null) {
            return
        }
        scene.addEntity(planeRenderableInstance!!.getRenderedEntity())

        isPlaneAddedToScene = true
    }

    private fun removePlaneFromScene() {
        if (!isPlaneAddedToScene || planeRenderableInstance == null) {
            return
        }

        scene.removeEntity(planeRenderableInstance!!.getRenderedEntity())

        isPlaneAddedToScene = false
    }

    private fun updateRenderableDefinitionForPlane(): Boolean {
        val boundary = plane.polygon ?: return false

        boundary.rewind()
        val boundaryVertices = boundary.limit() / 2

        if (boundaryVertices == 0) {
            return false
        }

        val numVertices = boundaryVertices * VERTS_PER_BOUNDARY_VERT
        vertices.clear()
        vertices.ensureCapacity(numVertices)

        val numIndices = (boundaryVertices * 6) + ((boundaryVertices - 2) * 3)
        triangleIndices.clear()
        triangleIndices.ensureCapacity(numIndices)

        val normal = Vector3.up()

        // Copy the perimeter vertices into the vertex buffer and add in the y-coordinate.
        while (boundary.hasRemaining()) {
            val x = boundary.get()
            val z = boundary.get()
            vertices.add(Vertex.builder().setPosition(Vector3(x, 0.0f, z)).setNormal(normal).build())
        }

        // Generate the interior vertices.
        boundary.rewind()
        while (boundary.hasRemaining()) {
            val x = boundary.get()
            val z = boundary.get()

            val magnitude = Math.hypot(x.toDouble(), z.toDouble()).toFloat()
            var scale = 1.0f - FEATHER_SCALE
            if (magnitude != 0.0f) {
                scale = 1.0f - Math.min(FEATHER_LENGTH / magnitude, FEATHER_SCALE)
            }

            vertices.add(
                Vertex.builder()
                    .setPosition(Vector3(x * scale, 1.0f, z * scale))
                    .setNormal(normal)
                    .build()
            )
        }

        // Generate triangle (4, 5, 6) and (4, 6, 7).
        val firstInnerVertex = boundaryVertices.toShort().toInt()
        for (i in 0 until boundaryVertices - 2) {
            triangleIndices.add(firstInnerVertex)
            triangleIndices.add(firstInnerVertex + i + 1)
            triangleIndices.add(firstInnerVertex + i + 2)
        }

        // Generate triangle (0, 1, 4), (4, 1, 5), (5, 1, 2), (5, 2, 6), (6, 2, 3), (6, 3, 7)
        // (7, 3, 0), (7, 0, 4)
        val firstOuterVertex = 0
        for (i in 0 until boundaryVertices) {
            val outerVertex1 = firstOuterVertex + i
            val outerVertex2 = firstOuterVertex + ((i + 1) % boundaryVertices)
            val innerVertex1 = firstInnerVertex + i
            val innerVertex2 = firstInnerVertex + ((i + 1) % boundaryVertices)

            triangleIndices.add(outerVertex1)
            triangleIndices.add(outerVertex2)
            triangleIndices.add(innerVertex1)

            triangleIndices.add(innerVertex1)
            triangleIndices.add(outerVertex2)
            triangleIndices.add(innerVertex2)
        }

        return true
    }
}
