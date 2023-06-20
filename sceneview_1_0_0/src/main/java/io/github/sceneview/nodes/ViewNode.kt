package io.github.sceneview.nodes

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Texture
import com.google.android.filament.View.PickingQueryResult
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.components.RenderableComponent
import io.github.sceneview.geometries.Plane
import io.github.sceneview.geometries.destroyGeometry
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.managers.WindowViewManager
import io.github.sceneview.managers.geometry
import io.github.sceneview.managers.setGeometry
import io.github.sceneview.math.Direction
import io.github.sceneview.math.size
import io.github.sceneview.texture.ViewStream
import io.github.sceneview.texture.ViewTexture
import io.github.sceneview.texture.destroyViewStream

/**
 * A Node that can display an Android [View]
 *
 * This node contains a View for the rendering engine to render.
 *
 * @param view The 2D Android [View] that is rendered by this [ViewNode]
 * @param unlit True to disable all lights influences on the rendered view
 * @param invertFrontFaceWinding Inverts the winding order of front faces.
 * Inverting the winding order of front faces is useful when rendering mirrored reflections
 * (water, mirror surfaces, front camera in AR, etc.).
 * True to invert front faces, false otherwise
 */
class ViewNode constructor(
    engine: Engine,
    nodeManager: NodeManager,
    viewWindowViewManager: WindowViewManager,
    materialLoader: MaterialLoader,
    view: View,
    unlit: Boolean = false,
    invertFrontFaceWinding: Boolean = false
) : RenderableNode(engine, nodeManager, EntityManager.get().create()),
    RenderableComponent {

    val viewStream: ViewStream
    val geometry: Plane
    val texture: Texture
    var material: MaterialInstance

    val view get() = viewStream.view

    init {
        viewStream = ViewStream.Builder()
            .view(view)
            .build(engine, viewWindowViewManager)

        geometry = Plane.Builder(size = viewStream.worldSize, normal = Direction(z = 1.0f))
            .build(engine)

        texture = ViewTexture.Builder()
            .viewStream(viewStream)
            .build(engine)

        material = materialLoader.createViewMaterial(texture, unlit, invertFrontFaceWinding)

        val renderView: () -> Unit = {
            if (geometry.boundingBox.size != Float3(0f)) {
                RenderableManager.Builder(geometry.submeshes.size)
                    .geometry(geometry)
                    .material(0, material)
                    .build(engine, entity)
            }
        }

        viewStream.onSizeChanged = { size ->
            geometry.update(engine, size = size)
            if (engine.renderableManager.hasComponent(entity)) {
                engine.renderableManager.setGeometry(entity, geometry)
            } else {
                renderView()
            }
        }

        renderView()
    }

    constructor(
        sceneView: SceneView,
        view: View,
        unlit: Boolean = false,
        invertFrontFaceWinding: Boolean = false
    ) : this(
        sceneView.engine,
        sceneView.nodeManager,
        sceneView.windowViewManager,
        sceneView.materialLoader,
        view,
        unlit,
        invertFrontFaceWinding
    )

    constructor(
        sceneView: SceneView,
        viewResourceId: Int,
        unlit: Boolean = false,
        invertFrontFaceWinding: Boolean = false
    ) : this(
        sceneView,
        LayoutInflater.from(sceneView.context).inflate(viewResourceId, null, false),
        unlit,
        invertFrontFaceWinding
    )

    override fun destroy() {
        engine.destroyViewStream(viewStream)
        engine.destroyGeometry(geometry)
        engine.destroyTexture(texture)
        engine.destroyMaterialInstance(material)
        engine.renderableManager.destroy(entity)

        super.destroy()
    }

    override fun getBoundingBox() = axisAlignedBoundingBox

    fun onTouchEvent(motionEvent: MotionEvent, pickingResult: PickingQueryResult): Boolean {
        //TODO: Use PickingQueryResult.getWorldSpacePosition()
//        val scene: SceneView = node.getSceneViewInternal() ?: return false
//        val pointerCount = motionEvent.pointerCount
//        val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(pointerCount)
//        val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(pointerCount)
//        val nodeTransformMatrix: Matrix = node.getTransformationMatrix()
//        val nodePosition = Vector3()
//        nodeTransformMatrix.decomposeTranslation(nodePosition)
//        val nodeScale = Vector3()
//        nodeTransformMatrix.decomposeScale(nodeScale)
//        val nodeRotation = Quaternion()
//        nodeTransformMatrix.decomposeRotation(nodeScale, nodeRotation)
//        val nodeForward = Quaternion.rotateVector(nodeRotation, Vector3.forward())
//        val nodeBack = Quaternion.rotateVector(nodeRotation, Vector3.back())
//
//        /*
//     * Cast a ray against a plane that extends to infinity located where the view is in 3D space
//     * instead of casting against the node's collision shape. This is important for the UX of touch
//     * events after the initial ACTION_DOWN event. i.e. If a user is dragging a slider and their
//     * finger moves beyond the view the position of their finger relative to the slider should still
//     * be respected.
//     */
//        val plane = Plane(nodePosition, nodeForward)
//        val rayHit = RayHit()
//
//        // Also cast a ray against a back-facing plane because we render the view as double-sided.
//        val backPlane = Plane(nodePosition, nodeBack)
//
//        // Convert the pointer coordinates for each pointer into the view's local coordinate space.
//        for (i in 0 until pointerCount) {
//            val props = MotionEvent.PointerProperties()
//            val coords = MotionEvent.PointerCoords()
//            motionEvent.getPointerProperties(i, props)
//            motionEvent.getPointerCoords(i, coords)
//            val camera: CameraNode? = scene.cameraNode
//            val ray = camera!!.screenPointToRay(coords.x, coords.y)
//            if (plane.rayIntersection(ray, rayHit)) {
//                val viewPosition = convertWorldPositionToLocalView(node, rayHit.point)
//                coords.x = viewPosition.x
//                coords.y = viewPosition.y
//            } else if (backPlane.rayIntersection(ray, rayHit)) {
//                val viewPosition = convertWorldPositionToLocalView(node, rayHit.point)
//
//                // Flip the x coordinate for the back-facing plane.
//                coords.x = getView().width - viewPosition.x
//                coords.y = viewPosition.y
//            } else {
//                coords.clear()
//                props.clear()
//            }
//            pointerProperties[i] = props
//            pointerCoords[i] = coords
//        }
//
//        // We must copy the touch event with the new coordinates and dispatch it to the view.
//        val me = MotionEvent.obtain(
//            motionEvent.downTime,
//            motionEvent.eventTime,
//            motionEvent.action,
//            pointerCount,
//            pointerProperties,
//            pointerCoords,
//            motionEvent.metaState,
//            motionEvent.buttonState,
//            motionEvent.xPrecision,
//            motionEvent.yPrecision,
//            motionEvent.deviceId,
//            motionEvent.edgeFlags,
//            motionEvent.source,
//            motionEvent.flags
//        )
//        return getView().dispatchTouchEvent(me)

        return true
    }

//    fun convertWorldPositionToLocalView(node: Node, worldPos: Vector3?): Vector3 {
//        Preconditions.checkNotNull(node, "Parameter \"node\" was null.")
//        Preconditions.checkNotNull(worldPos, "Parameter \"worldPos\" was null.")
//
//        // Find where the view renderable is being touched in local space.
//        // this will be in meters relative to the bottom-middle of the view.
//        val localPos: Vector3 = node.getTransformationMatrixInverted().transformPoint(worldPos)
//
//        // Calculate the pixels to meters ratio.
//        val view = getView()
//        val width = view.width
//        val height = view.height
//        val pixelsToMetersRatio = getPixelsToMetersRatio()
//
//        // We must convert the position to pixels
//        var xPixels = (localPos.x * pixelsToMetersRatio).toInt()
//        var yPixels = (localPos.y * pixelsToMetersRatio).toInt()
//
//        // We must convert the coordinates from the renderable's alignment origin to top-left origin.
//        val halfWidth = width / 2
//        val halfHeight = height / 2
//        val verticalAlignment = getVerticalAlignment()
//        yPixels = when (verticalAlignment) {
//            VerticalAlignment.BOTTOM -> height - yPixels
//            VerticalAlignment.CENTER -> height - (yPixels + halfHeight)
//            VerticalAlignment.TOP -> height - (yPixels + height)
//        }
//        val horizontalAlignment = getHorizontalAlignment()
//        when (horizontalAlignment) {
//            HorizontalAlignment.LEFT -> {}
//            HorizontalAlignment.CENTER -> xPixels = xPixels + halfWidth
//            HorizontalAlignment.RIGHT -> xPixels = xPixels + width
//        }
//        return Vector3(xPixels.toFloat(), yPixels.toFloat(), 0.0f)
//    }
}
