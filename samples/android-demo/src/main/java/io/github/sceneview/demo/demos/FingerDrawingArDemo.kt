package io.github.sceneview.demo.demos

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARCoreAvailability
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.demo.ARCameraInitScrim
import io.github.sceneview.demo.DemoPreviewPlaceholder
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.demo.DockItem
import io.github.sceneview.demo.R
import io.github.sceneview.demo.common.ArDemoPermissionGate
import io.github.sceneview.demo.common.DemoStatusBanner
import io.github.sceneview.demo.common.DemoStatusTone
import io.github.sceneview.demo.demos.internal.Drawing
import io.github.sceneview.demo.demos.internal.FingerDrawing
import io.github.sceneview.demo.demos.internal.FingerDrawingPlacement
import io.github.sceneview.demo.demos.internal.SteadyPen
import io.github.sceneview.demo.isDemoCompactHeight
import io.github.sceneview.demo.rememberArPlaybackDataset
import io.github.sceneview.math.Position
import io.github.sceneview.rememberARView
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.utils.screenToRay

/**
 * Finger Drawing, AR mode — the same paint as [FingerDrawingDemo], left hanging in the room
 * (#4082).
 *
 * The finger paints on a plane [FingerDrawingPlacement.AR_PAINT_DEPTH_METERS] in front of
 * the phone, an arm's reach, and the stroke stays where it was painted: walk around it, look
 * at it from behind. No surface has to be found first, so there is nothing to scan before
 * the first stroke.
 *
 * Two things differ from the 3D mode, and both are in this file:
 *
 * - **The pen follows the camera, not the touch events.** The touch handler only records
 *   where the finger is; the stroke is extended in `onSessionUpdated`, once per camera
 *   frame. A finger held still on a phone that moves draws a line through the room — the
 *   gesture AR drawing is made of — and no touch event fires for it.
 * - **A stroke lives in the space of an ARCore anchor**, not in world space. ARCore moves
 *   its world as it learns the room; an anchor moves with what it was attached to. A new
 *   anchor is only created when the pen starts further than
 *   [FingerDrawingPlacement.AR_ANCHOR_REACH_METERS] from every anchor the drawing already
 *   has, so a whole sketch hangs on one anchor and stays rigid.
 *
 * An anchor is released by leaving the composition: `AnchorNode` detaches it when the last
 * stroke that hung on it is undone.
 */
@Composable
fun FingerDrawingArDemo(onBack: () -> Unit) {
    // Inspection mode (Android Studio @Preview pane, Roborazzi snapshot tests): bypass the
    // Filament-backed body BEFORE any rememberEngine() call — LayoutLib ships no .so files.
    if (LocalInspectionMode.current) {
        DemoPreviewPlaceholder(title = "Finger Drawing", onBack = onBack)
        return
    }
    // Only this mode opens the camera, so only this mode asks for it: the 3D mode stays
    // one tap away for someone who refuses.
    ArDemoPermissionGate(
        title = stringResource(R.string.demo_finger_drawing_title),
        onBack = onBack,
    ) {
        FingerDrawingArScene(onBack)
    }
}

@Composable
private fun FingerDrawingArScene(onBack: () -> Unit) {
    var drawing by remember { mutableStateOf(Drawing()) }
    // Which anchor each stroke hangs on, by stroke id.
    var anchorOf by remember { mutableStateOf(emptyMap<Int, Anchor>()) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var thicknessDp by rememberSaveable { mutableFloatStateOf(FingerDrawing.DEFAULT_THICKNESS_DP) }

    var isTracking by remember { mutableStateOf(false) }
    // Cover the jet-black ARSceneView surface until ARCore delivers its first camera frame.
    var cameraReady by remember { mutableStateOf(false) }
    // Non-null once ARCore has ruled this device out: `cameraReady` never flips then, so
    // the init scrim has to read the verdict or it covers the SDK's own explanation card.
    var arCoreAvailability by remember { mutableStateOf<ARCoreAvailability?>(null) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberARView(engine)
    val cameraNode = rememberARCameraNode(engine)
    val arPlaybackDataset = rememberArPlaybackDataset()
    val paints = rememberPaintMaterials(materialLoader)

    val density = LocalDensity.current.density
    val pen = remember { ArPen() }

    /** Keeps only the anchors a stroke still hangs on; the others leave with their `AnchorNode`. */
    fun commit(next: Drawing) {
        drawing = next
        val alive = next.strokes.mapTo(HashSet()) { it.id }
        next.active?.let { alive += it.id }
        if (anchorOf.keys.any { it !in alive }) anchorOf = anchorOf.filterKeys { it in alive }
    }

    /** The point under the finger on the plane an arm's reach in front of the phone. */
    fun paintPoint(x: Float, y: Float): Position? {
        val forward = cameraNode.forwardDirection
        val ray = view.screenToRay(x, y) ?: return null
        val depth = FingerDrawingPlacement.AR_PAINT_DEPTH_METERS
        return FingerDrawingPlacement.planeHit(
            rayOrigin = ray.origin,
            rayDirection = ray.direction,
            planePoint = cameraNode.worldPosition + forward * depth,
            planeNormal = forward,
        )
    }

    fun startStroke(session: Session, frameNanos: Long) {
        val point = paintPoint(pen.x, pen.y) ?: return
        val aside = paintPoint(pen.x + SCALE_PROBE_DP * density, pen.y) ?: return
        val anchors = anchorOf.values.distinct()
        val reused = FingerDrawingPlacement.anchorToReuse(
            point = point,
            anchorPositions = anchors.map { it.pose.position() },
            reach = FingerDrawingPlacement.AR_ANCHOR_REACH_METERS,
        )
        val anchor = reused?.let { anchors[it] }
            // ARCore refuses an anchor while it is not tracking, and can run out of them.
            ?: runCatching {
                session.createAnchor(Pose(floatArrayOf(point.x, point.y, point.z), IDENTITY_ROTATION))
            }.getOrNull()
            ?: return
        val pose = anchor.pose

        pen.anchor = anchor
        pen.unitsPerDp = FingerDrawingPlacement.unitsPerDp(point, aside, SCALE_PROBE_DP)
        pen.steady = SteadyPen.at(point)
        pen.lastNanos = frameNanos
        pen.speed = 0f
        val next = drawing.penDown(
            point = FingerDrawingPlacement.worldToAnchor(point, pose.position(), pose.rotation()),
            radius = FingerDrawing.radiusFor(thicknessDp, pen.unitsPerDp, 0f),
            colorIndex = colorIndex,
            frameHint = FingerDrawingPlacement.worldDirectionToAnchor(
                direction = cameraNode.forwardDirection,
                anchorRotation = pose.rotation(),
            ),
            spacing = FingerDrawing.RING_SPACING_DP * pen.unitsPerDp,
        )
        next.active?.let { anchorOf = anchorOf + (it.id to anchor) }
        commit(next)
    }

    fun extendStroke(frameNanos: Long) {
        val anchor = pen.anchor ?: return
        val steady = pen.steady ?: return
        val point = paintPoint(pen.x, pen.y) ?: return
        val dtSeconds = (frameNanos - pen.lastNanos) / NANOS_PER_SECOND
        pen.speed = FingerDrawing.smoothedSpeed(
            previous = pen.speed,
            measured = FingerDrawing.speedDpPerSecond(steady.finger, point, pen.unitsPerDp, dtSeconds),
            dtSeconds = dtSeconds,
        )
        // The same steadying as the 3D mode, in the room: it also takes the tremor of a
        // hand holding a phone at arm's length out of the line.
        val followed = steady.follow(point, dtSeconds)
        pen.steady = followed
        pen.lastNanos = frameNanos
        val pose = anchor.pose
        val next = drawing.penMove(
            point = FingerDrawingPlacement.worldToAnchor(followed.paint, pose.position(), pose.rotation()),
            targetRadius = FingerDrawing.radiusFor(thicknessDp, pen.unitsPerDp, pen.speed),
        )
        // A very long stroke is continued in a new one; it hangs on the same anchor.
        next.active?.let { if (it.id !in anchorOf) anchorOf = anchorOf + (it.id to anchor) }
        commit(next)
    }

    fun endStroke() {
        pen.anchor = null
        pen.steady = null
        commit(drawing.penUp())
    }

    val strokeCount = drawing.strokeCount
    val compactHeight = isDemoCompactHeight()

    DemoScaffold(
        title = stringResource(R.string.demo_finger_drawing_title),
        onBack = onBack,
        peekHeader = when {
            compactHeight || strokeCount == 0 -> null
            else -> pluralStringResource(R.plurals.demo_finger_drawing_strokes, strokeCount, strokeCount)
        },
        onResetSettings = {
            colorIndex = 0
            thicknessDp = FingerDrawing.DEFAULT_THICKNESS_DP
        },
        dock = listOf(
            DockItem(
                icon = Icons.AutoMirrored.Outlined.Undo,
                label = stringResource(R.string.demo_finger_drawing_undo),
                onClick = { commit(drawing.undo()) },
                enabled = !drawing.isEmpty,
            ),
            DockItem(
                icon = Icons.Rounded.DeleteSweep,
                label = stringResource(R.string.demo_finger_drawing_clear),
                onClick = { commit(drawing.clear()) },
                enabled = !drawing.isEmpty,
            ),
        ),
        controls = {
            FingerDrawingControls(
                explainer = stringResource(R.string.demo_finger_drawing_ar_explainer),
                colorIndex = colorIndex,
                onColorChange = { colorIndex = it },
                thicknessDp = thicknessDp,
                onThicknessChange = { thicknessDp = it },
                canUndo = !drawing.isEmpty,
                onUndo = { commit(drawing.undo()) },
                onClear = { commit(drawing.clear()) },
            )
        },
        bottomOverlay = {
            DemoStatusBanner(
                text = when {
                    !cameraReady -> null
                    !isTracking -> stringResource(R.string.demo_finger_drawing_ar_tracking_hint)
                    drawing.isEmpty -> stringResource(R.string.demo_finger_drawing_ar_draw_hint)
                    else -> null
                },
                tone = if (isTracking) DemoStatusTone.Guidance else DemoStatusTone.Progress,
                icon = if (isTracking) Icons.Rounded.Gesture else null,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ARSceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                materialLoader = materialLoader,
                view = view,
                cameraNode = cameraNode,
                playbackDataset = arPlaybackDataset,
                // Nothing is placed on a surface here: the plane grid would only be noise
                // behind the paint.
                planeRenderer = false,
                sessionConfiguration = { _: Session, config: Config ->
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                },
                onARCoreAvailability = { arCoreAvailability = it },
                onSessionUpdated = { session: Session, frame: Frame ->
                    cameraReady = true
                    val tracking = frame.camera.trackingState == TrackingState.TRACKING
                    isTracking = tracking
                    // Down, move and up in that order, so a tap shorter than a camera frame
                    // still leaves its dot.
                    if (pen.pendingDown) {
                        pen.pendingDown = false
                        if (pen.anchor != null) endStroke()
                        if (tracking) startStroke(session, frame.timestamp)
                    } else if (tracking && pen.anchor != null) {
                        extendStroke(frame.timestamp)
                    }
                    if (pen.pendingUp) {
                        pen.pendingUp = false
                        endStroke()
                    }
                },
                onTouchEvent = { event, _ ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            pen.x = event.x
                            pen.y = event.y
                            pen.pendingDown = true
                            pen.pendingUp = false
                        }

                        MotionEvent.ACTION_MOVE -> {
                            pen.x = event.x
                            pen.y = event.y
                        }

                        // A second finger ends the stroke where it is.
                        MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            pen.pendingUp = true
                    }
                    true
                },
            ) {
                val strokes = drawing.strokes + listOfNotNull(drawing.active)
                strokes.groupBy { anchorOf[it.id] }.forEach { (anchor, hung) ->
                    if (anchor != null) {
                        key(anchor) {
                            AnchorNode(anchor = anchor) {
                                hung.forEach { stroke ->
                                    key(stroke.id) { StrokeMesh(stroke, paints[stroke.colorIndex]) }
                                }
                            }
                        }
                    }
                }
            }
            ARCameraInitScrim(initializing = !cameraReady, arCoreAvailability = arCoreAvailability)
        }
    }
}

/**
 * What the AR pen remembers between a touch event and the camera frame that acts on it. Not
 * Compose state: nothing reads it to draw.
 */
private class ArPen {
    var x = 0f
    var y = 0f
    var pendingDown = false
    var pendingUp = false

    /** Anchor of the stroke being drawn; `null` when the pen is up. */
    var anchor: Anchor? = null
    var unitsPerDp = 0f
    var steady: SteadyPen? = null
    var lastNanos = 0L
    var speed = 0f
}

private fun Pose.position() = Position(tx(), ty(), tz())

private fun Pose.rotation() = Quaternion(qx(), qy(), qz(), qw())

private val IDENTITY_ROTATION = floatArrayOf(0f, 0f, 0f, 1f)

/** Screen distance between the two rays that measure one dp on the painting plane. */
private const val SCALE_PROBE_DP = 100f

private const val NANOS_PER_SECOND = 1e9f
