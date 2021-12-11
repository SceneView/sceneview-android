package io.github.sceneview.ar.scene

import android.net.Uri
import androidx.lifecycle.coroutineScope
import com.google.ar.core.*
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.PlaneVisualizer
import com.google.ar.sceneform.rendering.Texture
import io.github.sceneview.ar.ArSceneLifecycle
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.ar.arcore.ArFrame
import kotlinx.coroutines.future.await
import java.util.*

/**
 * Control rendering of ARCore planes.
 *
 *
 * Used to visualize detected planes and to control whether Renderables cast shadows on them.
 */
class PlaneRenderer(val lifecycle: ArSceneLifecycle) : ArSceneLifecycleObserver {

    private val renderer get() = lifecycle.renderer

    private val visualizerMap: MutableMap<Plane, PlaneVisualizer> = HashMap()

    // Per-plane overrides
    private val materialOverrides: Map<Plane, Material> = HashMap()

    /**
     * Returns default material instance used to render the planes.
     */
    var material: Material? = null
        private set
    private var shadowMaterial: Material? = null

    /**
     * <pre>
     * Return the used [PlaneRendererMode]. Two options are available,
     * `RENDER_ALL` and `RENDER_TOP_MOST`. See
     * [PlaneRendererMode] and
     * [.setPlaneRendererMode] for more information.
    </pre> *
     *
     * @return [PlaneRendererMode]
     */
    /**
     * <pre>
     * Set here how tracked planes should be visualized on the screen. Two options are available,
     * `RENDER_ALL` and `RENDER_TOP_MOST`.
     * To see all tracked planes which are visible to the camera set the PlaneRendererMode to
     * `RENDER_ALL`. This mode eats up quite a few resources and should only be set
     * with care. To optimize the rendering set the mode to `RENDER_TOP_MOST`.
     * In that case only the top most plane visible to a camera is rendered on the screen.
     * Especially on weaker smartphone models this improves the overall performance.
     *
     * The default mode is `RENDER_TOP_MOST`
    </pre> *
     *
     * @param planeRendererMode [PlaneRendererMode]
     */
    var planeRendererMode = PlaneRendererMode.RENDER_ALL

    // Distance from the camera to last plane hit, default value is 4 meters (standing height).
    private var lastPlaneHitDistance = 4.0f

    init {
        lifecycle.addObserver(this)
        lifecycle.coroutineScope.launchWhenCreated {
            loadPlaneMaterial()
            loadShadowMaterial()
        }
    }

    override fun onArSessionConfigChanged(session: ArSession, config: Config) {
        // Disable the rendering of detected planes if no PlaneFindingMode
        isEnabled = config.planeFindingMode != Config.PlaneFindingMode.DISABLED
    }

    override fun onArFrame(arFrame: ArFrame) {
        if (isEnabled) {
            try {
                // Do a hittest on the current frame. The result is used to calculate
                // a focusPoint and to render the top most plane Trackable if
                // planeRendererMode is set to RENDER_TOP_MOST.
                val hitResult = getHitResult(arFrame.frame, renderer.desiredWidth, renderer.desiredHeight)
                // Calculate the focusPoint. It is used to determine the position of
                // the visualized grid.
                val focusPoint = getFocusPoint(arFrame.frame, hitResult)
                material?.setFloat3(MATERIAL_SPOTLIGHT_FOCUS_POINT, focusPoint)
                material?.setFloat(MATERIAL_SPOTLIGHT_RADIUS, SPOTLIGHT_RADIUS)

                val updatedPlanes = arFrame.updatedPlanes

                if (planeRendererMode == PlaneRendererMode.RENDER_ALL) {
                    renderAll(updatedPlanes)
                } else if (planeRendererMode == PlaneRendererMode.RENDER_TOP_MOST) {
                    updatedPlanes.firstOrNull()?.let { topMostPlane ->
                        renderPlane(topMostPlane)
                    }
                }

                // Check for not tracking Plane-Trackables and remove them.
                cleanupOldPlaneVisualizer()
            } catch (exception: DeadlineExceededException) {
            }
        }
    }

    /**
     * ### Enable/disable the plane renderer.
     */
    var isEnabled = true
        set(value) {
            if (field != value) {
                field = value
                visualizerMap.values.forEach { it.setEnabled(value) }
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
                visualizerMap.values.forEach { it.setVisible(value) }
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
                visualizerMap.values.forEach { it.setShadowReceiver(value) }
            }
        }

    /**
     * <pre>
     * Render all tracked Planes
    </pre> *
     *
     * @param updatedPlanes [Collection]<[Plane]>
     * @param planeMaterial [Material]
     */
    private fun renderAll(updatedPlanes: Collection<Plane>) {
        updatedPlanes.forEach { renderPlane(it) }
    }

    /**
     * <pre>
     * This function is responsible to update the rendering
     * of a [PlaneVisualizer]. If for the given [Plane]
     * no [PlaneVisualizer] exists, create a new one and add
     * it to the `visualizerMap`.
    </pre> *
     *
     * @param plane         [Plane]
     * @param planeMaterial [Material]
     */
    private fun renderPlane(plane: Plane) {
        // Find the plane visualizer if it already exists.
        // If not, create a new plane visualizer for this plane.
        val planeVisualizer = visualizerMap[plane]
            ?: PlaneVisualizer(plane, renderer).apply {
                val overrideMaterial = materialOverrides[plane]
                if (overrideMaterial != null) {
                    setPlaneMaterial(overrideMaterial)
                } else if (material != null) {
                    setPlaneMaterial(material)
                }
                if (shadowMaterial != null) {
                    setShadowMaterial(shadowMaterial)
                }
                setShadowReceiver(isShadowReceiver)
                setVisible(isVisible)
                setEnabled(isEnabled)
            }.also {
                visualizerMap[plane] = it
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
        visualizerMap.entries.removeAll { (plane, planeVisualizer) ->
            // If this plane was subsumed by another plane or it has permanently stopped tracking,
            // remove it.
            if (plane.subsumedBy != null || plane.trackingState == TrackingState.STOPPED) {
                planeVisualizer.release()
                true
            } else {
                false
            }
        }
    }

    private suspend fun loadShadowMaterial() {
        shadowMaterial = Material.builder()
            .setSource(
                renderer.context,
                Uri.parse("sceneview/materials/plane_renderer_shadow.filamat")
            )
            .build()
            .await()
        visualizerMap.values.forEach { it.setShadowMaterial(shadowMaterial) }
    }

    private suspend fun loadPlaneMaterial() {
        val texture = Texture.builder()
            .setSource(
                renderer.context,
                Uri.parse("sceneview/textures/plane_renderer.png")
            )
            .setSampler(
                Texture.Sampler.builder()
                    .setMinMagFilter(Texture.Sampler.MagFilter.LINEAR)
                    .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
                    .build()
            )
            .build()
            .await()
        material = Material.builder()
            .setSource(
                renderer.context,
                Uri.parse("sceneview/materials/plane_renderer.filamat")
            )
            .build()
            .await()
            .apply {
                setTexture(MATERIAL_TEXTURE, texture)
                setFloat3(MATERIAL_COLOR, 1.0f, 1.0f, 1.0f)

                // TODO: Don't use hardcoded width and height... Need api for getting
                // width and
                // height from the Texture class.
                val widthToHeightRatio = DEFAULT_TEXTURE_WIDTH / DEFAULT_TEXTURE_HEIGHT
                val scaleX = BASE_UV_SCALE
                val scaleY = scaleX * widthToHeightRatio
                setFloat2(MATERIAL_UV_SCALE, scaleX, scaleY)
                for ((plane, planeVisualizer) in visualizerMap) {
                    if (!materialOverrides.containsKey(plane)) {
                        planeVisualizer.setPlaneMaterial(material)
                    }
                }
            }
    }

    /**
     * <pre>
     * Cast a ray from the centre of the screen onto the scene and check
     * if any plane is hit. The result is a [HitResult] with information
     * about the hit position as a [Pose] and the trackable which got hit.
    </pre> *
     *
     * @param frame  [Frame]
     * @param width  int
     * @param height int
     * @return [HitResult]
     */
    private fun getHitResult(frame: Frame, width: Int, height: Int): HitResult? {
        // If we hit a plane, return the hit point.
        val hits = frame.hitTest(width / 2f, height / 2f)
        if (hits != null && !hits.isEmpty()) {
            for (hit in hits) {
                val trackable = hit.trackable
                val hitPose = hit.hitPose
                if (trackable is Plane && trackable.isPoseInPolygon(hitPose)) {
                    return hit
                }
            }
        }
        return null
    }

    /**
     * <pre>
     * Calculate the FocusPoint based on a [HitResult] on the current [Frame].
     * The FocusPoint is used to determine the position of the visualized plane.
     * If the [HitResult] is null, we use the last known distance of the camera to
     * the last hit plane.
    </pre> *
     *
     * @param frame [Frame]
     * @param hit   [HitResult]
     * @return [Vector3]
     */
    private fun getFocusPoint(frame: Frame, hit: HitResult?): Vector3 {
        if (hit != null) {
            val hitPose = hit.hitPose
            lastPlaneHitDistance = hit.distance
            return Vector3(hitPose.tx(), hitPose.ty(), hitPose.tz())
        }

        // If we didn't hit anything, project a point in front of the camera so that the spotlight
        // rolls off the edge smoothly.
        val cameraPose = frame.camera.pose
        val cameraPosition = Vector3(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())
        val zAxis = cameraPose.zAxis
        val backwards = Vector3(zAxis[0], zAxis[1], zAxis[2])
        return Vector3.add(cameraPosition, backwards.scaled(-lastPlaneHitDistance))
    }

    /**
     * <pre>
     * Use this enum to configure the Plane Rendering.
     *
     * For performance reasons use `RENDER_TOP_MOST`.
    </pre> *
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
        private const val DEFAULT_TEXTURE_WIDTH = 293f
        private const val DEFAULT_TEXTURE_HEIGHT = 513f
        private const val SPOTLIGHT_RADIUS = .5f
    }
}