package io.github.sceneview.ar

import android.util.Size
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.View
import com.google.ar.core.CameraConfig
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import io.github.sceneview.ar.arcore.getUpdatedTrackables
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.Node

@Composable
fun ARScene(
    modifier: Modifier = Modifier,
    childNodes: Set<Node> = setOf(),
    sharedEngine: Engine? = null,
    sharedScene: Scene? = null,
    sharedView: View? = null,
    sharedRenderer: Renderer? = null,
    sharedModelLoader: ModelLoader? = null,
    sharedMaterialLoader: MaterialLoader? = null,
    sessionFeatures: Set<Session.Feature> = setOf(),
    cameraConfig: ((Session) -> CameraConfig)? = null,
    cameraNode: (engine: Engine, viewSize: Size) -> ARCameraNode = { engine, viewSize ->
        ARCameraNode(engine, viewSize)
    },
    planeRenderer: Boolean = true,
    onSessionConfiguration: ((session: Session, Config) -> Unit)? = null,
    onSessionCreated: ((session: Session) -> Unit)? = null,
    /**
     * Updates of the state of the ARCore system.
     *
     * Callback for [onSessionUpdated].
     *
     * This includes: receiving a new camera frame, updating the location of the device, updating
     * the location of tracking anchors, updating detected planes, etc.
     *
     * This call may update the pose of all created anchors and detected planes. The set of updated
     * objects is accessible through [Frame.getUpdatedTrackables].
     *
     * Invoked once per [Frame] immediately before the Scene is updated.
     */
    onSessionUpdated: ((session: Session, frame: Frame) -> Unit)? = null,
    onSessionResumed: ((session: Session) -> Unit)? = null,
    /**
     * Invoked when an ARCore error occurred.
     *
     * Registers a callback to be invoked when the ARCore Session cannot be initialized because
     * ARCore is not available on the device or the camera permission has been denied.
     */
    onSessionFailed: ((exception: Exception) -> Unit)? = null,
    onSessionConfigChanged: ((session: Session, config: Config) -> Unit)? = null,
    onTap: ((
        /** The motion event that caused the tap. **/
        motionEvent: MotionEvent,
        /** The node that was tapped or `null`. **/
        node: Node?
    ) -> Unit)? = null,
    /**
     * Invoked when an ARCore trackable is tapped.
     *
     * Depending on the session configuration the [HitResult.getTrackable] can be:
     * - A [Plane] if [Config.setPlaneFindingMode] is enable.
     * - An [InstantPlacementPoint] if [Config.setInstantPlacementMode] is enable.
     * - A [DepthPoint] and [Point] if [Config.setDepthMode] is enable.
     */
    onTapAR: ((
        /** The motion event that caused the tap. */
        motionEvent: MotionEvent,
        /** The ARCore hit result for the trackable that was tapped. */
        hitResult: HitResult
    ) -> Unit)? = null,
    onCreate: ((ARSceneView) -> Unit)? = null
) {
    if (LocalInspectionMode.current) {
        ArScenePreview(modifier)
    } else {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                ARSceneView(
                    context, null, 0, 0,
                    sharedEngine,
                    sharedScene,
                    sharedView,
                    sharedRenderer,
                    sharedModelLoader,
                    sharedMaterialLoader,
                    sessionFeatures,
                    cameraConfig,
                    cameraNode,
                    onSessionConfiguration,
                    onSessionCreated,
                    onSessionUpdated,
                    onSessionResumed,
                    onSessionFailed,
                    onSessionConfigChanged,
                    onTap,
                    onTapAR
                ).apply {
                    onCreate?.invoke(this)
                }
            },
            update = { sceneView ->
                sceneView.childNodes = childNodes
                sceneView.planeRenderer.isEnabled = planeRenderer
            }
        )
    }
}

@Composable
private fun ArScenePreview(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.DarkGray)
    )
}