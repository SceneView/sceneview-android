package io.github.sceneview.ar.scene

import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.sceneform.rendering.PlaneVisualizer
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.arcore.zDirection
import io.github.sceneview.material.MaterialLoader
import io.github.sceneview.material.destroy
import io.github.sceneview.material.setParameter
import io.github.sceneview.material.setTexture
import io.github.sceneview.math.Position
import io.github.sceneview.texture.TextureLoader
import io.github.sceneview.texture.TextureLoader.TextureType
import io.github.sceneview.texture.destroy
import io.github.sceneview.utils.Color

/**
 * Control rendering of ARCore planes.
 *
 *
 * Used to visualize detected planes and to control whether Renderables cast shadows on them.
 */
class PlaneRenderer(val sceneView: ArSceneView) {

    private val visualizers = mutableMapOf<Plane, PlaneVisualizer>()

    val planeTexture = TextureLoader.createImageTexture(
        sceneView.context,
        "sceneview/textures/plane_renderer.png",
        TextureType.COLOR
    )

    /**
     * Default material instance used to render the planes.
     */
    val planeMaterial = MaterialLoader.createMaterial(
        sceneView.context,
        "sceneview/materials/plane_renderer.filamat"
    ).apply {
        defaultInstance.apply {
            setTexture(MATERIAL_TEXTURE, planeTexture)

            val widthToHeightRatio =
                planeTexture.getWidth(0).toFloat() / planeTexture.getHeight(0).toFloat()
            val scaleX = BASE_UV_SCALE
            val scaleY = scaleX * widthToHeightRatio
            setParameter(MATERIAL_UV_SCALE, scaleX, scaleY)

            setParameter(MATERIAL_COLOR, Color(1.0f, 1.0f, 1.0f))
            setParameter(MATERIAL_SPOTLIGHT_RADIUS, SPOTLIGHT_RADIUS)
        }
    }

    // TODO: Remove when it isn't used in PlaneVisualizer
    private var shadowMaterial = MaterialLoader.createMaterial(
        sceneView.context,
        "sceneview/materials/plane_renderer_shadow.filamat"
    )

    /**
     * Determines how tracked planes should be visualized on the screen. Two options are available,
     * `RENDER_ALL` and `RENDER_TOP_MOST`.
     * To see all tracked planes which are visible to the camera set the PlaneRendererMode to
     * `RENDER_ALL`. This mode eats up quite a few resources and should only be set
     * with care. To optimize the rendering set the mode to `RENDER_TOP_MOST`.
     * In that case only the top most plane visible to a camera is rendered on the screen.
     * Especially on weaker smartphone models this improves the overall performance.
     *
     * The default mode is `RENDER_TOP_MOST`
     */
    var planeRendererMode = PlaneRendererMode.RENDER_CENTER

    // Distance from the camera to last plane hit, default value is 4 meters (standing height).
    private var planeHitDistance = 4.0f

    var arFrame: ArFrame? = null

    /**
     * ### Adjust the max screen [ArFrame.hitTest] number per seconds
     *
     * Decrease if you don't need a very precise position update and want to reduce frame
     * consumption.
     * Increase for a more accurate positioning update.
     */
    var maxHitTestPerSecond: Int = 10

    /**
     * ### Enable/disable the plane renderer.
     */
    var isEnabled = true
        set(value) {
            if (field != value) {
                field = value
                visualizers.values.forEach { it.setEnabled(value) }
            }
        }

    /**
     * ### Control visibility of plane visualization.
     *
     * If false - no planes are drawn. Note that shadow visibility is independent of plane
     * visibility.
     */
    var isVisible = true
        set(value) {
            if (field != value) {
                field = value
                visualizers.values.forEach { it.setVisible(value) }
            }
        }

    /**
     * ### Control whether Renderables in the scene should cast shadows onto the planes
     *
     * If false - no planes receive shadows, regardless of the per-plane setting.
     */
    var isShadowReceiver = true
        set(value) {
            if (field != value) {
                field = value
                visualizers.values.forEach { it.setShadowReceiver(value) }
            }
        }

    private var isCameraTracking = false
        set(value) {
            if (field != value) {
                field = value
                visualizers.values.forEach { it.setEnabled(isEnabled && value) }
            }
        }

    fun update(arFrame: ArFrame) {
        if (isEnabled) {
            if (arFrame.fps(this.arFrame) < maxHitTestPerSecond) {
                this.arFrame = arFrame

                isCameraTracking = arFrame.camera.isTracking

                try {
                    val updatedPlanes = arFrame.updatedPlanes
                    if (planeRendererMode == PlaneRendererMode.RENDER_ALL) {
                        updatedPlanes.forEach { renderPlane(it) }
                    } else if (planeRendererMode == PlaneRendererMode.RENDER_CENTER) {
                        // Do a hitTest on the current frame. The result is used to calculate a
                        // focusPoint and to render the top most plane Trackable if planeRendererMode is
                        // set to RENDER_TOP_MOST

                        val centerPlane = if (isVisible) {
                            // Don't make the hit test if we don't need to know the center plane
                            arFrame.hitTest(
                                position = Position(x = 0.0f, y = 0.0f),
                                plane = true,
                                depth = false,
                                instant = false
                            )?.trackable as? Plane
                        } else null
//                        if (centerPlane != null) {
//                            // Calculate the focusPoint. It is used to determine the position of
//                            // the visualized grid.
//                                val focusPoint = getFocusPoint(arFrame.frame, hitResult)
//                                materialInstance?.setParameter(
//                                    MATERIAL_SPOTLIGHT_FOCUS_POINT,
//                                    focusPoint
//                                )
//                            renderPlane(centerPlane)
//                        }
                        updatedPlanes.forEach { renderPlane(it, visible = it == centerPlane) }
                        visualizers.forEach { (plane, visualizer) ->
                            if (plane !in updatedPlanes) {
                                visualizer.setVisible(isVisible && plane == centerPlane)
                            }
                        }
                    }

                    // Check for not tracking Plane-Trackables and remove them.
                    cleanupOldPlaneVisualizer()
                } catch (exception: DeadlineExceededException) {
                }
            }
        }
    }

    fun destroy() {
        visualizers.forEach { (_, planeVisualizer) -> planeVisualizer.destroy() }

        planeTexture.destroy()
        planeMaterial.destroy()
        shadowMaterial.destroy()
    }

    /**
     * This function is responsible to update the rendering
     * of a [PlaneVisualizer]. If for the given [Plane]
     * no [PlaneVisualizer] exists, create a new one and add
     * it to the [visualizers].
     *
     * @param plane [Plane]
     */
    private fun renderPlane(plane: Plane, visible: Boolean = true) {
        // Find the plane visualizer if it already exists.
        // If not, create a new plane visualizer for this plane.
        if (plane.trackingState == TrackingState.TRACKING || plane.subsumedBy == null) {
            val planeVisualizer = visualizers[plane]
                ?: PlaneVisualizer(sceneView, plane).apply {
                    setPlaneMaterial(planeMaterial.defaultInstance)
                    setShadowMaterial(shadowMaterial.defaultInstance)
                    setShadowReceiver(isShadowReceiver)
                    setVisible(isVisible && visible)
                    setEnabled(isEnabled && isCameraTracking)
                }.also {
                    visualizers[plane] = it
                }
            // Update the plane visualizer.
            planeVisualizer.updatePlane()
        }
    }

    /**
     * ### Remove plane visualizers for old planes that are no longer tracking
     *
     * Update the material parameters for all remaining planes.
     */
    private fun cleanupOldPlaneVisualizer() {
        visualizers.entries.removeAll { (plane, planeVisualizer) ->
            // If this plane was subsumed by another plane or it has permanently stopped tracking,
            // remove it.
            if (plane.subsumedBy != null || plane.trackingState == TrackingState.STOPPED) {
                planeVisualizer.destroy()
                true
            } else {
                false
            }
        }
    }

    /**
     * ### Calculate the FocusPoint based on a [HitResult] on the current [Frame]
     *
     * The FocusPoint is used to determine the position of the visualized plane.
     * If the [HitResult] is null, we use the last known distance of the camera to the last hit
     * plane.
     */
    private fun getFocusPoint(frame: Frame, hit: HitResult?): Float3 {
        return if (hit != null) {
            planeHitDistance = hit.distance
            hit.hitPose.position
        } else {
            // If we didn't hit anything, project a point in front of the camera so that the spotlight
            // rolls off the edge smoothly.
            val cameraPose = frame.camera.pose
            // -Z is in front of camera
            cameraPose.position + cameraPose.zDirection * -planeHitDistance
        }
    }

    /**
     * Use this enum to configure the Plane Rendering.
     *
     * For performance reasons use `RENDER_TOP_MOST`.
     */
    enum class PlaneRendererMode {
        /**
         * Render all possible [Plane]s which are visible to the camera.
         */
        RENDER_ALL,

        /**
         * Render only the top most [Plane] which is visible to the camera.
         */
        RENDER_CENTER
    }

    companion object {
        /**
         * Material parameter that controls what texture is being used when rendering the planes.
         */
        const val MATERIAL_TEXTURE = "texture"

        /**
         * Float2 material parameter to control the X/Y scaling of the texture's UV coordinates. Can be
         * used to adjust for the texture's aspect ratio and control the frequency of tiling.
         */
        const val MATERIAL_UV_SCALE = "uvScale"

        /**
         * Float3 material parameter to control the RGB tint of the plane.
         */
        const val MATERIAL_COLOR = "color"

        /**
         * Float material parameter to control the radius of the spotlight.
         */
        const val MATERIAL_SPOTLIGHT_RADIUS = "radius"

        /**
         * Float3 material parameter to control the grid visualization point.
         */
        private const val MATERIAL_SPOTLIGHT_FOCUS_POINT = "focusPoint"

        /**
         * Used to control the UV Scale for the default texture.
         */
        private const val BASE_UV_SCALE = 8.0f
        private const val SPOTLIGHT_RADIUS = 0.5f
    }
}

// TODO: Finish move to Kotlin from there
//class PlaneVisualizer(private val sceneView: SceneView, private val plane: Plane) :
//    TransformProvider {
//
//    private val planeMatrix = Matrix()
//    private var isPlaneAddedToScene = false
//    private var isEnabled = false
//    private var isShadowReceiver = false
//    private var isVisible = false
//
//    @Entity
//    private var planeRenderable: Int? = null
//
//    //    private var planeRenderable: ModelRenderable? = null
//    private val planeRenderableInstance
//        @EntityInstance get() = planeRenderable?.let {
//            renderableManager.getInstance(it)
//        }
//
//    //    private var planeRenderableInstance: RenderableInstance? = null
//    private val vertices = ArrayList<Geometry.Vertex>()
//    private val triangleIndices = ArrayList<Int>()
//
//    //    private val renderableDefinition: RenderableDefinition
//    private val geometry: Geometry
//    private var planeSubmesh: RenderableDefinition.Submesh? = null
//    private var shadowSubmesh: RenderableDefinition.Submesh? = null
//    fun setEnabled(enabled: Boolean) {
//        if (isEnabled != enabled) {
//            isEnabled = enabled
//            updatePlane()
//        }
//    }
//
//    fun setShadowReceiver(shadowReceiver: Boolean) {
//        if (isShadowReceiver != shadowReceiver) {
//            isShadowReceiver = shadowReceiver
//            updatePlane()
//        }
//    }
//
//    fun setVisible(visible: Boolean) {
//        if (isVisible != visible) {
//            isVisible = visible
//            updatePlane()
//        }
//    }
//
//    init {
//        geometry = Geometry.Builder(vertices = vertices).build()
////        renderableDefinition = RenderableDefinition
////            .builder()
////            .setVertices(vertices)
////            .build()
//    }
//
//    fun setShadowMaterial(materialInstance: MaterialInstance?) {
//        if (shadowSubmesh == null) {
//            shadowSubmesh = RenderableDefinition.Submesh.builder()
//                .setTriangleIndices(triangleIndices)
//                .setMaterial(materialInstance)
//                .build()
//        } else {
//            shadowSubmesh!!.material = materialInstance
//        }
//        if (planeRenderable != null) {
//            updateRenderable()
//        }
//    }
//
//    fun setPlaneMaterial(materialInstance: MaterialInstance?) {
//        if (planeSubmesh == null) {
//            planeSubmesh = RenderableDefinition.Submesh.builder()
//                .setTriangleIndices(triangleIndices)
//                .setMaterial(materialInstance)
//                .build()
//        } else {
//            planeSubmesh!!.material = materialInstance
//        }
//        if (planeRenderable != null) {
//            updateRenderable()
//        }
//    }
//
//    override fun getTransformationMatrix(): Matrix {
//        return planeMatrix
//    }
//
//    fun updatePlane() {
//        if (!isEnabled || !isVisible && !isShadowReceiver) {
//            removePlaneFromScene()
//            return
//        }
//        if (plane.trackingState != TrackingState.TRACKING) {
//            removePlaneFromScene()
//            return
//        }
//
//        // Set the transformation matrix to the pose of the plane.
//        plane.centerPose.toMatrix(planeMatrix.data, 0)
//
//        // Calculate the mesh for the plane.
//        val success = updateRenderableDefinitionForPlane()
//        if (!success) {
//            removePlaneFromScene()
//            return
//        }
//        updateRenderable()
//        addPlaneToScene()
//    }
//
//    fun updateRenderable() {
//        val submeshes = geometry.submeshes
////        val submeshes = renderableDefinition.getSubmeshes()
//        submeshes.clear()
//
//        // the order of the meshes is important here, because we set the blendOrder based on
//        // the index below.
//        if (isVisible && planeSubmesh != null) {
//            submeshes.add(planeSubmesh!!)
//        }
//        if (isShadowReceiver && shadowSubmesh != null) {
//            submeshes.add(shadowSubmesh!!)
//        }
//        if (submeshes.isEmpty()) {
//            removePlaneFromScene()
//            return
//        }
//        if (planeRenderable == null) {
//            try {
//                planeRenderable = RenderableManager.Builder(geometry.submeshes.size)
//                    .geometry(geometry)
//                    .castShadows(false)
//                    .build()
////                planeRenderable = ModelRenderable.builder()
////                    .setSource(renderableDefinition)
////                    .build()
////                    .get()
////                planeRenderable.setShadowCaster(false)
//                // Creating a Renderable is immediate when using RenderableDefinition.
//            } catch (ex: InterruptedException) {
//                throw AssertionError("Unable to create plane renderable.")
//            } catch (ex: ExecutionException) {
//                throw AssertionError("Unable to create plane renderable.")
//            }
////            planeRenderableInstance = planeRenderable?.renderableInstance
////            planeRenderableInstance = planeRenderable.createInstance(this)
//        } else {
//            renderableManager.setGeometry()
//            planeRenderable!!.updateFromDefinition(renderableDefinition)
//        }
//
//        // The plane must always be drawn before the shadow, we use the blendOrder to enforce that.
//        // this works because both sub-meshes will be sorted at the same distance from the camera
//        // since they're part of the same renderable. The blendOrder, determines the sorting in
//        // that situation.
//        if (planeRenderableInstance != null && submeshes.size > 1) {
//            planeRenderableInstance!!.setBlendOrderAt(0, 0) // plane
//            planeRenderableInstance!!.setBlendOrderAt(1, 1) // shadow
//        }
//        planeRenderableInstance!!.prepareForDraw(sceneView)
//
//        val transformManager = transformManager
//        val instance = transformManager.getInstance(planeRenderable)
//        transformManager.setTransform(instance, planeRenderableInstance!!.worldModelMatrix.data)
//    }
//
//    fun destroy() {
//        removePlaneFromScene()
//        planeRenderable?.destroyRenderable()
////        if (planeRenderableInstance != null) {
////            planeRenderableInstance!!.destroy()
////        }
//        planeRenderable = null
//    }
//
//    private fun addPlaneToScene() {
//        if (isPlaneAddedToScene || planeRenderableInstance == null) {
//            return
//        }
//        planeRenderable?.let { sceneView.addEntity(it) }
////        sceneView.addEntity(planeRenderableInstance!!.renderedEntity)
//        isPlaneAddedToScene = true
//    }
//
//    private fun removePlaneFromScene() {
//        if (!isPlaneAddedToScene || planeRenderableInstance == null) {
//            return
//        }
//        planeRenderable?.let { sceneView.removeEntity(it) }
////        sceneView.removeEntity(planeRenderableInstance!!.renderedEntity)
//        isPlaneAddedToScene = false
//    }
//
//    private fun updateRenderableDefinitionForPlane(): Boolean {
//        val boundary = plane.polygon ?: return false
//        boundary.rewind()
//        val boundaryVertices = boundary.limit() / 2
//        if (boundaryVertices == 0) {
//            return false
//        }
//        val numVertices: Int = boundaryVertices * VERTS_PER_BOUNDARY_VERT
//        vertices.clear()
//        vertices.ensureCapacity(numVertices)
//        val numIndices = boundaryVertices * 6 + (boundaryVertices - 2) * 3
//        triangleIndices.clear()
//        triangleIndices.ensureCapacity(numIndices)
//        // Up normal
//        val normal = Direction(y = 1.0f)//Vector3.up()
//
//        // Copy the perimeter vertices into the vertex buffer and add in the y-coordinate.
//        while (boundary.hasRemaining()) {
//            val x = boundary.get()
//            val z = boundary.get()
//            vertices.add(
//                Geometry.Vertex(position = Position(x, 0.0f, z), normal = normal)
////                Vertex.builder().setPosition(Vector3(x, 0.0f, z)).setNormal(normal).build()
//            )
//        }
//
//        // Generate the interior vertices.
//        boundary.rewind()
//        while (boundary.hasRemaining()) {
//            val x = boundary.get()
//            val z = boundary.get()
//            val magnitude = Math.hypot(x.toDouble(), z.toDouble()).toFloat()
//            var scale: Float = 1.0f - FEATHER_SCALE
//            if (magnitude != 0.0f) {
//                scale = 1.0f - min(FEATHER_LENGTH / magnitude, FEATHER_SCALE)
//            }
//            vertices.add(
//                Geometry.Vertex(
//                    position = Position(x * scale, 1.0f, z * scale),
//                    normal = normal
//                )
////                Vertex.builder()
////                    .setPosition(Vector3(x * scale, 1.0f, z * scale))
////                    .setNormal(normal)
////                    .build()
//            )
//        }
//        val firstOuterVertex = 0
//        val firstInnerVertex = boundaryVertices.toShort().toInt()
//
//        // Generate triangle (4, 5, 6) and (4, 6, 7).
//        for (i in 0 until boundaryVertices - 2) {
//            triangleIndices.add(firstInnerVertex)
//            triangleIndices.add(firstInnerVertex + i + 1)
//            triangleIndices.add(firstInnerVertex + i + 2)
//        }
//
//        // Generate triangle (0, 1, 4), (4, 1, 5), (5, 1, 2), (5, 2, 6), (6, 2, 3), (6, 3, 7)
//        // (7, 3, 0), (7, 0, 4)
//        for (i in 0 until boundaryVertices) {
//            val outerVertex1 = firstOuterVertex + i
//            val outerVertex2 = firstOuterVertex + (i + 1) % boundaryVertices
//            val innerVertex1 = firstInnerVertex + i
//            val innerVertex2 = firstInnerVertex + (i + 1) % boundaryVertices
//            triangleIndices.add(outerVertex1)
//            triangleIndices.add(outerVertex2)
//            triangleIndices.add(innerVertex1)
//            triangleIndices.add(innerVertex1)
//            triangleIndices.add(outerVertex2)
//            triangleIndices.add(innerVertex2)
//        }
//        return true
//    }
//
//    companion object {
//        private const val VERTS_PER_BOUNDARY_VERT = 2
//
//        // Feather distance 0.2 meters.
//        private const val FEATHER_LENGTH = 0.2f
//
//        // Feather scale over the distance between plane center and vertices.
//        private const val FEATHER_SCALE = 0.2f
//    }
//}