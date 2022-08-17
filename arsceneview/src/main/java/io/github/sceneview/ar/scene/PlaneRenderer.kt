package io.github.sceneview.ar.scene

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.google.android.filament.MaterialInstance
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.PlaneVisualizer
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneLifecycle
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.arcore.zDirection
import io.github.sceneview.material.MaterialLoader
import io.github.sceneview.material.setParameter
import io.github.sceneview.material.setTexture
import io.github.sceneview.math.Position
import io.github.sceneview.texture.TextureLoader
import io.github.sceneview.texture.TextureLoader.TextureType
import io.github.sceneview.utils.Color

/**
 * Control rendering of ARCore planes.
 *
 *
 * Used to visualize detected planes and to control whether Renderables cast shadows on them.
 */
class PlaneRenderer(private val lifecycle: ArSceneLifecycle) : ArSceneLifecycleObserver {

    private val sceneView get() = lifecycle.sceneView

    private val visualizers: MutableMap<Plane, PlaneVisualizer> = HashMap()

    // TODO: Remove when it isn't used in PlaneVisualizer
    private var planeMaterial: Material? = null

    /**
     * Default material instance used to render the planes.
     */
    var materialInstance: MaterialInstance? = null
        private set

    // TODO: Remove when it isn't used in PlaneVisualizer
    private var shadowMaterial: Material? = null

    var shadowMaterialInstance: MaterialInstance? = null
        private set

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
    var planeRendererMode = PlaneRendererMode.RENDER_TOP_MOST

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

    init {
        lifecycle.addObserver(this)
        lifecycle.coroutineScope.launchWhenCreated {
            loadPlaneMaterial()
            loadShadowMaterial()
        }
    }

    override fun onArFrame(arFrame: ArFrame) {
        if (isEnabled) {
            if (arFrame.fps(this.arFrame) < maxHitTestPerSecond) {
                this.arFrame = arFrame
                try {
                    // Do a hitTest on the current frame. The result is used to calculate a
                    // focusPoint and to render the top most plane Trackable if planeRendererMode is
                    // set to RENDER_TOP_MOST
                    val hitResult = arFrame.hitTest(
                        position = Position(x = 0.0f, y = 0.0f),
                        plane = true,
                        depth = false,
                        instant = false
                    )

                    // Calculate the focusPoint. It is used to determine the position of
                    // the visualized grid.
                    val focusPoint = getFocusPoint(arFrame.frame, hitResult)
                    materialInstance?.setParameter(MATERIAL_SPOTLIGHT_FOCUS_POINT, focusPoint)

                    if (planeRendererMode == PlaneRendererMode.RENDER_ALL) {
                        renderAll(arFrame.updatedPlanes)
                    } else if (planeRendererMode == PlaneRendererMode.RENDER_TOP_MOST && hitResult != null) {
                        renderPlane(hitResult.trackable as Plane)
                    }

                    // Check for not tracking Plane-Trackables and remove them.
                    cleanupOldPlaneVisualizer()
                } catch (exception: DeadlineExceededException) {
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destroy()

        super.onDestroy(owner)
    }

    fun destroy() {
        visualizers.forEach { (_, planeVisualizer) -> planeVisualizer.destroy() }
    }

    /**
     * Render all tracked Planes
     *
     * @param updatedPlanes [Collection]<[Plane]>
     */
    private fun renderAll(updatedPlanes: Collection<Plane>) {
        updatedPlanes.forEach { renderPlane(it) }
    }

    /**
     * This function is responsible to update the rendering
     * of a [PlaneVisualizer]. If for the given [Plane]
     * no [PlaneVisualizer] exists, create a new one and add
     * it to the [visualizers].
     *
     * @param plane [Plane]
     */
    private fun renderPlane(plane: Plane) {
        // Find the plane visualizer if it already exists.
        // If not, create a new plane visualizer for this plane.
        val planeVisualizer = visualizers[plane]
            ?: PlaneVisualizer(sceneView, plane).apply {
                if (planeMaterial != null) {
                    setPlaneMaterial(planeMaterial)
                }
                if (shadowMaterial != null) {
                    setShadowMaterial(shadowMaterial)
                }
                setShadowReceiver(isShadowReceiver)
                setVisible(isVisible)
                setEnabled(isEnabled)
            }.also {
                visualizers[plane] = it
            }
        // Update the plane visualizer.
        planeVisualizer.updatePlane()
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

    private suspend fun loadShadowMaterial() {
        shadowMaterialInstance = MaterialLoader.loadMaterial(
            sceneView.context,
            lifecycle,
            "sceneview/materials/plane_renderer_shadow.filamat"
        ) ?: throw AssertionError("Can't load the plane renderer shadow material")
        shadowMaterial = Material(lifecycle, shadowMaterialInstance)

        visualizers.values.forEach { it.setShadowMaterial(shadowMaterial) }
    }

    private suspend fun loadPlaneMaterial() {
        val texture = TextureLoader.loadImageTexture(
            sceneView.context,
            lifecycle,
            "sceneview/textures/plane_renderer.png",
            TextureType.COLOR
        ) ?: throw AssertionError("Can't load the plane renderer texture")

        materialInstance = MaterialLoader.loadMaterial(
            sceneView.context,
            lifecycle,
            "sceneview/materials/plane_renderer.filamat"
        )?.apply {
            setTexture(MATERIAL_TEXTURE, texture)

            val widthToHeightRatio = texture.getWidth(0).toFloat() / texture.getHeight(0).toFloat()
            val scaleX = BASE_UV_SCALE
            val scaleY = scaleX * widthToHeightRatio

            setParameter(MATERIAL_UV_SCALE, scaleX, scaleY)
            setParameter(MATERIAL_COLOR, Color(1.0f, 1.0f, 1.0f))
            setParameter(MATERIAL_SPOTLIGHT_RADIUS, SPOTLIGHT_RADIUS)
        } ?: throw AssertionError("Can't load the plane renderer material")

        planeMaterial = Material(lifecycle, materialInstance)
        for (planeVisualizer in visualizers.values) {
            planeVisualizer.setPlaneMaterial(planeMaterial)
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
        RENDER_TOP_MOST
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