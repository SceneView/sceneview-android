package io.github.sceneview.demo.demos

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import com.google.android.filament.Box as FilamentBox
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARCoachingOverlay
import io.github.sceneview.ar.arcore.hitTestDepth
import io.github.sceneview.ar.ARCoreAvailability
import io.github.sceneview.ar.ArGuidanceCue
import io.github.sceneview.ar.ARSceneScope
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.ar.rememberArGuidanceState
import io.github.sceneview.ar.rememberHostLifecycle
import io.github.sceneview.demo.ARCameraInitScrim
import io.github.sceneview.demo.BuildConfig
import io.github.sceneview.demo.common.ForcedTrackingFailure
import io.github.sceneview.demo.common.QaCameraBackdrop
import io.github.sceneview.demo.common.qaCameraBackdropEnabled
import io.github.sceneview.demo.common.qaCameraBackdropSurfaceType
import io.github.sceneview.demo.common.rememberQaCameraBackdropActive
import io.github.sceneview.demo.common.shareText
import io.github.sceneview.demo.demos.internal.formatMeasureArea
import io.github.sceneview.demo.demos.internal.formatMeasureLength
import io.github.sceneview.demo.demos.internal.liveKind
import io.github.sceneview.demo.demos.internal.LivePlane
import io.github.sceneview.demo.demos.internal.LivePlaneIds
import io.github.sceneview.demo.demos.internal.livePlanes
import io.github.sceneview.demo.demos.internal.livePlanesFloorY
import io.github.sceneview.demo.demos.internal.LiveWall
import io.github.sceneview.demo.demos.internal.LiveWallMotion
import io.github.sceneview.demo.demos.internal.LiveWallSnapshot
import io.github.sceneview.demo.demos.internal.MeasureAction
import io.github.sceneview.demo.demos.internal.measureAction
import io.github.sceneview.demo.demos.internal.MeasureBoard
import io.github.sceneview.demo.demos.internal.MeasureCandidateControl
import io.github.sceneview.demo.demos.internal.measureClosesOutline
import io.github.sceneview.demo.demos.internal.measureDistanceMeters
import io.github.sceneview.demo.demos.internal.MeasureFigure
import io.github.sceneview.demo.demos.internal.MeasureFigureNumbers
import io.github.sceneview.demo.demos.internal.measureFigureNumbers
import io.github.sceneview.demo.demos.internal.measureFigureOutline
import io.github.sceneview.demo.demos.internal.measureFirstFloorPoint
import io.github.sceneview.demo.demos.internal.measureFloorLevel
import io.github.sceneview.demo.demos.internal.measureLabelQuaternion
import io.github.sceneview.demo.demos.internal.measureLabelScale
import io.github.sceneview.demo.demos.internal.measureLerp
import io.github.sceneview.demo.demos.internal.measureMidpoint
import io.github.sceneview.demo.demos.internal.MeasureMode
import io.github.sceneview.demo.demos.internal.measureOnLevel
import io.github.sceneview.demo.demos.internal.MeasurePhase
import io.github.sceneview.demo.demos.internal.MeasurePlane
import io.github.sceneview.demo.demos.internal.MeasurePlaneKind
import io.github.sceneview.demo.demos.internal.measurePlaneSnapTargets
import io.github.sceneview.demo.demos.internal.measurePointsMoved
import io.github.sceneview.demo.demos.internal.MeasureQaPoint
import io.github.sceneview.demo.demos.internal.measureQaLines
import io.github.sceneview.demo.demos.internal.measureRaisingHeight
import io.github.sceneview.demo.demos.internal.measureRayFloorHit
import io.github.sceneview.demo.demos.internal.MeasureRefusal
import io.github.sceneview.demo.demos.internal.measureRefusal
import io.github.sceneview.demo.demos.internal.measureRoomHeightFromRay
import io.github.sceneview.demo.demos.internal.measureShareText
import io.github.sceneview.demo.demos.internal.MeasureSnap
import io.github.sceneview.demo.demos.internal.MeasureSnapKind
import io.github.sceneview.demo.demos.internal.measureSnapTolerance
import io.github.sceneview.demo.demos.internal.measureTriangulateXZ
import io.github.sceneview.demo.demos.internal.MeasureUnitSystem
import io.github.sceneview.demo.demos.internal.measureWallFootAt
import io.github.sceneview.demo.demos.internal.measureWallHeightFromRay
import io.github.sceneview.demo.demos.internal.measureWallIndices
import io.github.sceneview.demo.demos.internal.measureWallVertices
import io.github.sceneview.demo.demos.internal.PlaneWallSource
import io.github.sceneview.demo.demos.internal.snapMeasureTarget
import io.github.sceneview.demo.demos.internal.snapSquareCorner
import io.github.sceneview.demo.demos.internal.WALL_MAX_HEIGHT
import io.github.sceneview.demo.demos.internal.WALL_MIN_HEIGHT
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.demo.DockItem
import io.github.sceneview.demo.LocalDemoChromeBottomInset
import io.github.sceneview.demo.R
import io.github.sceneview.demo.rememberArPlaybackDataset
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.ui.ConnectedChoiceRow
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.haptic.rememberHapticFeedback
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.Node as SceneNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.safeDestroyGeometry
import io.github.sceneview.sample.rememberUnlitMaterialInstance
import kotlinx.coroutines.delay
import java.util.Locale

/** Measured points: small white beads, like the system Measure apps. */
private const val POINT_RADIUS = 0.007f

/** Segment and wall-edge thickness, metres — a line you can see across a room. */
private const val LINE_RADIUS = 0.0028f
private const val LIVE_LINE_RADIUS = 0.0022f
private const val GUIDE_RADIUS = 0.0015f

/** The reticle's 3D dot, where the snapped point really is. */
private const val AIM_DOT_RADIUS = 0.009f

/** World size of a label at scale 1 — 4:1, the label bitmap's ratio. */
private const val LABEL_WIDTH = 0.12f
private const val LABEL_HEIGHT = 0.03f
private const val LABEL_LIFT = 0.025f
private const val LABEL_FONT_SIZE = 60f

/** Alpha of the translucent wall and floor fills over the camera feed. */
private const val WALL_FILL_ALPHA = 0.32f
private const val FLOOR_FILL_ALPHA = 0.2f

/** Floor fill sits a hair above the floor line so it never fights the detected plane. */
private const val FLOOR_FILL_LIFT = 0.002f

/** Smallest change pushed into Compose state from the 60 Hz camera loop, metres. */
private const val AIM_EPSILON = 0.003f
private const val HEIGHT_EPSILON = 0.004f

/** Bounding-box margin around rising walls: the box is built once, the walls move. */
private const val WALL_BOX_MARGIN = 0.5f

/**
 * Where a measured point came from. Shown in the settings sheet's accuracy notice and kept
 * on each aim because the sources do not carry the same accuracy (`AR_MEASURE.md`).
 */
internal enum class MeasureHitSource { Plane, Floor, Depth, FeaturePoint }

/**
 * What the reticle is on this frame. Only [MeasureAimView] reaches Compose state; this
 * holder carries the anchor factory, which changes every frame.
 */
private class MeasureAim(
    val view: MeasureAimView,
    val createAnchor: () -> Anchor?,
    val reuse: Anchor?,
    /**
     * What the debug QA log records for a point committed from this aim. Built only in debug
     * builds and read only on a commit, so release does no extra work per frame.
     */
    val qa: (() -> MeasureQaPoint)?,
)

/** The reticle as the screen draws it — compared with [AIM_EPSILON] before each state write. */
internal data class MeasureAimView(
    val snap: MeasureSnap,
    val closes: Boolean,
    val source: MeasureHitSource,
    /** The foot of the recognised wall the reticle is on (#4732), drawn as a bright seam. */
    val wallFoot: Pair<Position, Position>? = null,
)

/**
 * AR measuring tape, rebuilt from scratch (#3509) against the system Measure apps and
 * magicplan: three modes behind one mode pill.
 *
 * - **Distance** — a reticle that snaps to points, plane corners and edges and to vertical
 *   and level guides; tap to drop points, every segment gets a camera-facing length label.
 *   Tap the first point again to close the outline and read its area.
 * - **Wall** — tap both ends of a wall at the floor; the wall then **rises with the aim**,
 *   a translucent surface whose top follows where the phone points. Tap to set its height
 *   and read width, height and area.
 * - **Room** — trace the floor corner by corner (corners square themselves to 90°), tap the
 *   first corner to close it; every wall rises together to the ceiling. Floor area,
 *   perimeter, ceiling height and wall area; Share sends the dimensions as text.
 *
 * In wall and room modes every point after the first is the camera ray's hit on the
 * **floor plane** of the first point, not the raw hit test: aiming at the foot of a wall,
 * the hit test lands on the wall itself a few centimetres up, the floor plane gives the
 * corner.
 *
 * The geometry, the snapping, the board's undo flow, units and the share text are pure and
 * unit-tested (`MeasureGeometry.kt`, `MeasureBoard.kt`, `MeasureReport.kt`). Accuracy is
 * the honest limit: a few centimetres on a phone without a depth sensor (`AR_MEASURE.md`).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ARMeasureDemo(onBack: () -> Unit) {
    var arSessionFailed by remember { mutableStateOf(false) }
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val arPlaybackDataset = rememberArPlaybackDataset()
    val haptic = rememberHapticFeedback()
    val context = LocalContext.current
    val locale: Locale = LocalLocale.current.platformLocale

    var board by remember { mutableStateOf(MeasureBoard<Anchor>()) }
    // World positions of every anchor on the board, refreshed only when one actually moves
    // (anchors drift as ARCore refines its map; pushing every frame would recompose at 60 Hz).
    var positions by remember { mutableStateOf<Map<Anchor, Position>>(emptyMap()) }
    var units by remember { mutableStateOf(MeasureUnitSystem.forLocale(locale)) }

    var session by remember { mutableStateOf<Session?>(null) }
    val aimControl = remember { MeasureCandidateControl<MeasureAim>() }
    var aim by remember { mutableStateOf<MeasureAimView?>(null) }
    // Height the walls follow while raising; `null` until the aim crosses a wall.
    var liveHeight by remember { mutableStateOf<Float?>(null) }
    // The last height read on the wall: the rising walls keep it while the aim is off.
    var shownHeight by remember { mutableStateOf<Float?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var isTracking by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var arCoreAvailability by remember { mutableStateOf<ARCoreAvailability?>(null) }
    val cameraStream = rememberARCameraStream(materialLoader)
    val qaBackdrop = rememberQaCameraBackdropActive(cameraReady)
    var depthSupported by remember { mutableStateOf(false) }
    var useDepthFallback by remember { mutableStateOf(true) }

    // Walls recognised live from ARCore's vertical planes (#4732), the same helper and look as
    // 3D Scan's live view (#4540): fed once a frame, drawn by a LiveWallLayer.
    val liveWalls = remember { MeasureLiveWalls() }
    val wallLayer = remember(engine, materialLoader) { LiveWallLayer(engine, materialLoader) }
    DisposableEffect(wallLayer) { onDispose { wallLayer.destroy() } }
    var liveWallCount by remember { mutableIntStateOf(0) }
    // A tap the button refused, said for a moment even once the state has moved on.
    var tapRefusal by remember { mutableStateOf<MeasureRefusal?>(null) }
    var tapRefusalAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(tapRefusalAt) {
        if (tapRefusal != null) {
            delay(REFUSAL_SHOWN_MS)
            tapRefusal = null
        }
    }

    // Anchors are native ARCore handles: detach every one the board drops (undo, clear) and
    // all of them when the screen goes.
    val latestBoard = remember { arrayOf(board) }
    SideEffect { latestBoard[0] = board }
    DisposableEffect(Unit) {
        onDispose { latestBoard[0].allPoints.distinct().forEach { it.detach() } }
    }
    // Debug-only accuracy log (MeasureQaRecorder): what each point was committed from, and the
    // last camera tracking state. Release builds never create the recorder.
    val qaRecorder = remember { if (BuildConfig.DEBUG) MeasureQaRecorder(context) else null }
    DisposableEffect(qaRecorder) { onDispose { qaRecorder?.close() } }
    val qaPoints = remember { HashMap<Anchor, MeasureQaPoint>() }
    val qaTracking = remember { arrayOfNulls<String>(1) }
    fun recordQa(reason: String, logged: MeasureBoard<Anchor>) {
        val recorder = qaRecorder ?: return
        val lines = measureQaLines(
            board = logged,
            point = { anchor ->
                val now = positions[anchor] ?: anchor.pose.toPosition()
                qaPoints[anchor]?.copy(position = now) ?: MeasureQaPoint(now)
            },
            tracking = qaTracking[0],
            liveHeight = shownHeight,
            device = Build.MODEL,
            app = BuildConfig.VERSION_NAME,
        )
        recorder.record(reason, logged.isEmpty, lines)
    }
    fun commit(next: MeasureBoard<Anchor>, qaReason: String) {
        val kept = next.allPoints.toSet()
        board.allPoints.distinct().filterNot { it in kept }.forEach { it.detach() }
        qaPoints.keys.retainAll(kept)
        board = next
        positions = next.allPoints.distinct().associateWith { positions[it] ?: it.pose.toPosition() }
        aimControl.update(null)
        recordQa(qaReason, next)
    }

    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var surfaceSeen by remember { mutableStateOf(false) }
    LaunchedEffect(aim != null) { if (aim != null) surfaceSeen = true }
    val failure = ForcedTrackingFailure.override ?: trackingFailureReason
    val guidance = rememberArGuidanceState(
        cameraReady = cameraReady,
        isTracking = isTracking && failure == null,
        surfaceFound = surfaceSeen || !board.isEmpty,
        trackingFailureReason = failure,
    )
    val coaching = arCoreAvailability == null && !arSessionFailed && guidance.isCoaching

    // Snap haptics: one tick each time the reticle locks onto something new.
    LaunchedEffect(aim?.snap?.kind, aim?.closes) {
        if (aim?.closes == true || (aim?.snap?.kind ?: MeasureSnapKind.None) != MeasureSnapKind.None) {
            haptic.selection()
        }
    }

    val phase = board.phase
    val active = board.active

    // Numbers of every figure, from the anchors' current positions.
    val figureOutlines: List<List<Position>> = board.figures.map { figure ->
        measureFigureOutline(figure.mode, figure.points.mapNotNull { positions[it] })
    }
    val figureNumbers: List<MeasureFigureNumbers> = board.figures.mapIndexed { i, figure ->
        measureFigureNumbers(figure, figureOutlines[i], shownHeight)
    }
    fun len(m: Float) = formatMeasureLength(m, units, locale)
    fun area(m: Float) = formatMeasureArea(m, units, locale)

    val activeOutline = if (active != null) figureOutlines.last() else emptyList()
    // The live segment from the last point to the reticle.
    val liveSegment: Float? = aim?.takeIf { phase == MeasurePhase.Next && !it.closes }?.let {
        activeOutline.lastOrNull()?.let { last -> measureDistanceMeters(last, it.snap.position) }
    }

    val shareTitle = stringResource(R.string.demo_ar_measure_action_share)
    val action = measureAction(board, closes = aim?.closes == true)
    // Why a tap would do nothing now; null when it would act (#4732: never refuse in silence).
    val refusal = measureRefusal(
        tracking = isTracking,
        phase = phase,
        aimFound = aim != null,
        heightFound = liveHeight != null,
    )
    fun refuse(reason: MeasureRefusal) {
        tapRefusal = reason
        tapRefusalAt = System.nanoTime()
        haptic.warning()
    }

    val onPrimary: () -> Unit = {
        if (refusal != null) {
            refuse(refusal)
        } else if (phase == MeasurePhase.Raising) {
            liveHeight?.let {
                commit(board.lockHeight(it), "height")
                haptic.success()
            }
        } else {
            val current = aimControl.current?.takeIf { isTracking }
            val next = tapAt(board, current)
            if (next == null || next == board) {
                refuse(if (current == null) MeasureRefusal.NoSurface else MeasureRefusal.AnchorFailed)
            } else {
                val qa = current?.qa?.takeIf { qaRecorder != null }?.invoke()
                if (qa != null) next.allPoints.filterNot { it in qaPoints }.forEach { qaPoints[it] = qa }
                commit(next, if (current?.view?.closes == true) "close" else "point")
                tapRefusal = null
                if (next.phase == MeasurePhase.Raising) {
                    liveHeight = null
                    shownHeight = null
                }
                if (current?.view?.closes == true) haptic.success() else haptic.light()
            }
        }
    }

    DemoScaffold(
        arSessionFailed = arSessionFailed,
        arOverlaysEnabled = arCoreAvailability == null,
        title = stringResource(R.string.demo_ar_measure_title),
        chromeToggleOnTap = false,
        onBack = onBack,
        peekHeader = if (coaching && board.isEmpty) null else measureHint(board, aim, action, liveWallCount),
        onReset = {
            commit(board.clear(), "reset")
            liveWalls.reset()
        },
        controls = {
            MeasureSettings(
                units = units,
                onUnits = { units = it },
                depthSupported = depthSupported,
                useDepth = useDepthFallback,
                onUseDepth = {
                    useDepthFallback = it
                    aimControl.update(null)
                    aim = null
                },
            )
        },
        dock = listOfNotNull(
            // Done ends the open chain; walls and rooms end themselves, so only Distance has it.
            DockItem(
                icon = Icons.Rounded.Done,
                label = stringResource(R.string.demo_ar_measure_action_done),
                onClick = {
                    commit(board.finish(), "done")
                    haptic.success()
                },
                enabled = board.canFinish,
            ).takeIf { board.mode == MeasureMode.Distance },
            DockItem(
                icon = Icons.AutoMirrored.Rounded.Undo,
                label = stringResource(R.string.demo_ar_measure_action_undo),
                onClick = { commit(board.undo(), "undo") },
                enabled = !board.isEmpty,
            ),
            DockItem(
                icon = Icons.Rounded.DeleteSweep,
                label = stringResource(R.string.demo_ar_measure_action_clear),
                onClick = { commit(board.clear(), "clear") },
                enabled = !board.isEmpty,
            ),
            DockItem(
                icon = Icons.Rounded.IosShare,
                label = shareTitle,
                onClick = {
                    recordQa("share", board)
                    shareText(
                        context = context,
                        text = measureShareText(figureNumbers, units, locale),
                        chooserTitle = shareTitle,
                    )
                },
                enabled = figureNumbers.any { it.segments.isNotEmpty() },
            ),
        ),
        topOverlay = {
            val readout = measureReadout(
                board = board,
                numbers = figureNumbers,
                liveSegment = liveSegment,
                len = ::len,
                area = ::area,
            )
            AnimatedVisibility(
                visible = readout != null,
                enter = fadeIn(SceneViewTokens.Motion.fade()),
                exit = fadeOut(SceneViewTokens.Motion.fade()),
            ) {
                // Keep the last readout while fading out.
                val shown = remember { arrayOf<MeasureReadout?>(null) }
                if (readout != null) shown[0] = readout
                shown[0]?.let { MeasureReadoutCard(it) }
            }
        },
        bottomOverlay = {
            // The camera-app order: the modes, then the one big button that measures (#4732).
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MeasureModePill(
                    mode = board.mode,
                    onSelect = { mode ->
                        if (mode != board.mode) {
                            commit(board.withMode(mode), "mode")
                            haptic.selection()
                        }
                    },
                )
                MeasureAddButton(
                    action = action,
                    refused = refusal != null,
                    onClick = onPrimary,
                    modifier = Modifier.padding(top = SceneViewTokens.Space.md),
                )
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (qaBackdrop) QaCameraBackdrop(seed = "ar-measure")
            ARSceneView(
                lifecycle = rememberHostLifecycle(),
                onSessionFailure = { arSessionFailed = true },
                modifier = Modifier.fillMaxSize().onSizeChanged {
                    viewport = it
                    aimControl.update(null)
                    aim = null
                },
                engine = engine,
                materialLoader = materialLoader,
                isOpaque = !qaCameraBackdropEnabled(),
                surfaceType = qaCameraBackdropSurfaceType(),
                cameraStream = if (qaBackdrop) null else cameraStream,
                cameraNode = cameraNode,
                playbackDataset = arPlaybackDataset,
                // The same surface marks as every other AR demo, while the first point is sought.
                planeRenderer = board.isEmpty,
                sessionConfiguration = { configuredSession: Session, config: Config ->
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    val supported = configuredSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                    depthSupported = supported
                    config.depthMode = if (supported) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                },
                onSessionCreated = { created: Session -> session = created },
                onARCoreAvailability = { arCoreAvailability = it },
                onTrackingFailureChanged = { reason -> trackingFailureReason = reason },
                onSessionUpdated = { _: Session, frame: Frame ->
                    cameraReady = true
                    isTracking = frame.camera.trackingState == TrackingState.TRACKING
                    if (qaRecorder != null) qaTracking[0] = frame.camera.trackingState.name
                    val cameraPose = frame.camera.pose
                    val origin = cameraPose.toPosition()
                    val z = cameraPose.zAxis
                    val direction = Position(-z[0], -z[1], -z[2])

                    // Anchors drift as tracking refines: follow them, but only on real moves.
                    if (isTracking) movedPositions(board, positions)?.let { positions = it }

                    // One read of ARCore's planes a frame: the live walls and the snapper share it.
                    val livePlanes = if (isTracking) session?.livePlanes(liveWalls.planeIds).orEmpty() else emptyList()
                    // Walls matter to Wall and Room; while a figure raises its own, it is the one shown.
                    // Hidden, they are not recognised at all: the snapper still reads the planes, as on main.
                    val wallsShown = board.mode != MeasureMode.Distance && board.phase != MeasurePhase.Raising
                    val walls = if (wallsShown) {
                        liveWalls.update(livePlanes, origin.y, frame.timestamp)
                    } else {
                        LiveWallSnapshot.Empty
                    }
                    if (wallsShown && walls.walls.size != liveWallCount) liveWallCount = walls.walls.size
                    wallLayer.sync(liveWalls.motion, walls, liveWalls.dt, shown = wallsShown)

                    if (board.phase == MeasurePhase.Raising) {
                        aimControl.update(null)
                        if (aim != null) aim = null
                        val height = if (isTracking) {
                            raisingHeight(board.active, figureOutlines.lastOrNull().orEmpty(), origin, direction)
                        } else {
                            null
                        }
                        // Off the wall there is no height to lock: the shutter says why (#4751).
                        val read = measureRaisingHeight(height, liveHeight, HEIGHT_EPSILON)
                        if (read != liveHeight) liveHeight = read
                        if (read != null && read != shownHeight) shownHeight = read
                    } else {
                        val next = if (isTracking && viewport.width > 0 && viewport.height > 0) {
                            findMeasureAim(
                                frame = frame,
                                session = session,
                                planes = livePlanes.map { it.toMeasurePlane() },
                                walls = walls.takeIf { wallsShown },
                                board = board,
                                outlines = figureOutlines,
                                positions = positions,
                                origin = origin,
                                direction = direction,
                                x = viewport.width / 2f,
                                y = viewport.height / 2f,
                                allowDepth = useDepthFallback && depthSupported,
                            )
                        } else {
                            null
                        }
                        aimControl.update(next)
                        if (aimChanged(next?.view, aim)) aim = next?.view
                    }
                },
            ) {
                val cameraPosition = cameraNode.worldPosition
                board.figures.forEachIndexed { index, figure ->
                    key(index, figure.mode) {
                        MeasureFigureNodes(
                            figure = figure,
                            outline = figureOutlines[index],
                            liveHeight = shownHeight,
                            cameraPosition = cameraPosition,
                            eye = cameraNode,
                            len = ::len,
                            area = ::area,
                        )
                    }
                }
                aim?.let { view ->
                    MeasureAimNodes(
                        view = view,
                        from = activeOutline.lastOrNull()?.takeIf { phase == MeasurePhase.Next },
                        liveLabel = liveSegment?.let(::len),
                        cameraPosition = cameraPosition,
                        eye = cameraNode,
                    )
                }
                key(wallLayer) { Node(apply = { wallLayer.nodes.forEach { addChildNode(it) } }) }
            }

            if (cameraReady && arCoreAvailability == null && !arSessionFailed) {
                MeasureReticle(
                    aim = aim,
                    raising = phase == MeasurePhase.Raising,
                    wallFoot = board.mode != MeasureMode.Distance && phase != MeasurePhase.Raising,
                    // A refused tap says why for a moment; aiming off a rising wall says it all along.
                    refusal = tapRefusal ?: refusal?.takeIf { it == MeasureRefusal.OffWall && !coaching },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            ARCoachingOverlay(
                cue = if (arCoreAvailability == null && !arSessionFailed) guidance.cue else ArGuidanceCue.NONE,
                surface = guidance.surface,
                hint = guidance.hint,
                scanLingering = guidance.scanLingering,
                contentPadding = PaddingValues(bottom = LocalDemoChromeBottomInset.current),
            )

            ARCameraInitScrim(
                initializing = !cameraReady,
                arCoreAvailability = arCoreAvailability,
            )
        }
    }
}

// ── Hint, readout and settings ──────────────────────────────────────────────────────────────

@Composable
private fun measureHint(
    board: MeasureBoard<Anchor>,
    aim: MeasureAimView?,
    action: MeasureAction,
    liveWalls: Int,
): String {
    val count = board.active?.points?.size ?: 0
    return stringResource(
        // A finished measure stays on screen: say the next point starts another one (#4732).
        if (action == MeasureAction.StartNew) {
            R.string.demo_ar_measure_hint_new
        } else when (board.mode) {
            MeasureMode.Distance -> when {
                board.phase == MeasurePhase.First -> R.string.demo_ar_measure_hint_first
                aim?.closes == true -> R.string.demo_ar_measure_hint_close
                board.canFinish -> R.string.demo_ar_measure_hint_chain
                else -> R.string.demo_ar_measure_hint_second
            }
            MeasureMode.Wall -> when (board.phase) {
                MeasurePhase.First -> if (liveWalls == 0) {
                    R.string.demo_ar_measure_hint_wall_find
                } else {
                    R.string.demo_ar_measure_hint_wall_first
                }
                MeasurePhase.Next -> R.string.demo_ar_measure_hint_wall_second
                MeasurePhase.Raising -> R.string.demo_ar_measure_hint_wall_raise
            }
            MeasureMode.Room -> when {
                board.phase == MeasurePhase.First -> R.string.demo_ar_measure_hint_room_first
                board.phase == MeasurePhase.Raising -> R.string.demo_ar_measure_hint_room_raise
                aim?.closes == true -> R.string.demo_ar_measure_hint_close
                count >= MeasureBoard.MIN_OUTLINE_POINTS -> R.string.demo_ar_measure_hint_room_close
                else -> R.string.demo_ar_measure_hint_room_next
            }
        },
    )
}

/** The readout card: one big number, what it is, and the figure's other numbers. */
internal data class MeasureReadout(val value: String, val label: String, val details: String?)

@Composable
private fun measureReadout(
    board: MeasureBoard<Anchor>,
    numbers: List<MeasureFigureNumbers>,
    liveSegment: Float?,
    len: (Float) -> String,
    area: (Float) -> String,
): MeasureReadout? {
    val figure = board.active ?: board.figures.lastOrNull() ?: return null
    val n = numbers.last()
    val raising = figure.raising
    val sep = " · "
    // With more than one measure of a kind on screen, the card names which one it reads (#4732).
    val sameKind = board.figures.count { it.mode == figure.mode && it.points.isNotEmpty() }
    val prefix = if (sameKind > 1) stringResource(R.string.demo_ar_measure_number, sameKind) + sep else ""
    val readout = when (figure.mode) {
        MeasureMode.Distance -> {
            val value = liveSegment ?: n.segments.lastOrNull() ?: return null
            val details = buildList {
                if (n.segments.size > 1 || (liveSegment != null && n.segments.isNotEmpty())) {
                    val total = n.length + (liveSegment ?: 0f)
                    val res = if (n.closed) R.string.demo_ar_measure_perimeter else R.string.demo_ar_measure_total
                    add(stringResource(res, len(total)))
                }
                n.outlineArea?.let { add(stringResource(R.string.demo_ar_measure_area, area(it))) }
            }.joinToString(sep).ifEmpty { null }
            MeasureReadout(
                value = len(value),
                label = stringResource(
                    if (liveSegment != null) {
                        R.string.demo_ar_measure_label_live
                    } else {
                        R.string.demo_ar_measure_label_segment
                    },
                ),
                details = details,
            )
        }
        MeasureMode.Wall -> {
            val width = n.segments.firstOrNull() ?: liveSegment ?: return null
            val height = n.height
            if (height == null) {
                MeasureReadout(len(width), stringResource(R.string.demo_ar_measure_label_width), null)
            } else {
                MeasureReadout(
                    value = len(height),
                    label = stringResource(
                        if (raising) {
                            R.string.demo_ar_measure_label_height_live
                        } else {
                            R.string.demo_ar_measure_label_height
                        },
                    ),
                    details = listOfNotNull(
                        stringResource(R.string.demo_ar_measure_width, len(width)),
                        n.wallArea?.let { stringResource(R.string.demo_ar_measure_area, area(it)) },
                    ).joinToString(sep),
                )
            }
        }
        MeasureMode.Room -> {
            val height = n.height
            when {
                height != null -> MeasureReadout(
                    value = len(height),
                    label = stringResource(
                        if (raising) {
                            R.string.demo_ar_measure_label_ceiling_live
                        } else {
                            R.string.demo_ar_measure_label_ceiling
                        },
                    ),
                    details = listOfNotNull(
                        n.outlineArea?.let { stringResource(R.string.demo_ar_measure_floor, area(it)) },
                        stringResource(R.string.demo_ar_measure_perimeter, len(n.length)),
                        n.wallArea?.let { stringResource(R.string.demo_ar_measure_walls, area(it)) },
                    ).joinToString(sep),
                )
                else -> {
                    val value = liveSegment ?: n.segments.lastOrNull() ?: return null
                    MeasureReadout(
                        value = len(value),
                        label = stringResource(
                            R.string.demo_ar_measure_label_wall,
                            n.segments.size + if (liveSegment != null) 1 else 0,
                        ),
                        details = if (n.segments.isNotEmpty()) {
                            stringResource(R.string.demo_ar_measure_total, len(n.length + (liveSegment ?: 0f)))
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
    return readout.copy(label = prefix + readout.label)
}

/** `DESIGN.md` AR Overlay Card: scrim ground, hairline, `radius-lg`, `space-md` padding. */
@Composable
internal fun MeasureReadoutCard(readout: MeasureReadout) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val overlay = SceneViewTokens.ArOverlay
    Surface(
        modifier = Modifier.widthIn(max = overlay.maxWidth).semantics(mergeDescendants = true) {},
        color = if (dark) overlay.scrimDark else overlay.scrimLight,
        contentColor = overlay.onScrim,
        shape = RoundedCornerShape(SceneViewTokens.Radius.lg),
        border = BorderStroke(SceneViewTokens.Glass.edgeWidth, SceneViewTokens.Glass.edgeRing),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = SceneViewTokens.Space.md, vertical = SceneViewTokens.Space.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = readout.label, style = SceneViewTokens.Type.caption, color = overlay.onScrimMuted)
            Text(text = readout.value, style = SceneViewTokens.Type.display, color = overlay.onScrim)
            readout.details?.let {
                Text(text = it, style = SceneViewTokens.Type.caption, color = overlay.onScrimMuted)
            }
        }
    }
}

/** Distance · Wall · Room — the over-media mode pill of Cosmos and AR Depth. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MeasureModePill(mode: MeasureMode, onSelect: (MeasureMode) -> Unit) {
    val pill = SceneViewTokens.ModePill
    val labels = mapOf(
        MeasureMode.Distance to stringResource(R.string.demo_ar_measure_mode_distance),
        MeasureMode.Wall to stringResource(R.string.demo_ar_measure_mode_wall),
        MeasureMode.Room to stringResource(R.string.demo_ar_measure_mode_room),
    )
    Surface(
        shape = CircleShape,
        color = pill.container,
        contentColor = pill.onContainer,
        border = BorderStroke(pill.outlineWidth, pill.outline),
    ) {
        ConnectedChoiceRow(
            options = MeasureMode.entries,
            selected = mode,
            onSelect = onSelect,
            label = { labels.getValue(it) },
            modifier = Modifier.padding(horizontal = SceneViewTokens.Space.xs),
            optionTestTag = { "measure_mode_${it.name.lowercase(Locale.ROOT)}" },
            colors = ToggleButtonDefaults.colors(
                containerColor = pill.container,
                contentColor = pill.onContainer,
                checkedContainerColor = pill.selectedContainer,
                checkedContentColor = pill.onSelected,
            ),
            fillWidth = false,
        )
    }
}

/**
 * The one control that measures (#4732): a camera-app shutter (72 dp ring, 56 dp accent disc),
 * one tap per point, with what the tap does next written under it. A tap that cannot act is
 * still taken — dimmed, it answers with the reason under the reticle instead of doing nothing.
 */
@Composable
internal fun MeasureAddButton(
    action: MeasureAction,
    refused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overlay = SceneViewTokens.ArOverlay
    val label = stringResource(
        when (action) {
            MeasureAction.Start, MeasureAction.Continue -> R.string.demo_ar_measure_action_add
            MeasureAction.Close -> R.string.demo_ar_measure_action_close
            MeasureAction.SetHeight -> R.string.demo_ar_measure_action_set_height
            MeasureAction.StartNew -> R.string.demo_ar_measure_action_new
        },
    )
    val alpha = if (refused) ADD_REFUSED_ALPHA else 1f
    // An opaque scrim behind the disc: the shutter stays readable in front of a light wall.
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scrim = if (dark) overlay.scrimDark else overlay.scrimLight
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = scrim,
            border = BorderStroke(AddRingWidth, overlay.onScrim.copy(alpha = alpha)),
            modifier = Modifier
                .size(AddButtonSize)
                .testTag(MEASURE_ADD_TAG)
                .semantics(mergeDescendants = true) { contentDescription = label },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(AddDiscSize)
                        .background(overlay.accentProgress.copy(alpha = alpha), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (action) {
                            MeasureAction.SetHeight -> Icons.Rounded.Height
                            MeasureAction.Close -> Icons.Rounded.Done
                            else -> Icons.Rounded.Add
                        },
                        contentDescription = null,
                        tint = overlay.onAccentProgress,
                        modifier = Modifier.size(SceneViewTokens.Space.xl),
                    )
                }
            }
        }
        Surface(
            shape = CircleShape,
            color = SceneViewTokens.ModePill.container,
            contentColor = overlay.onScrim,
            border = BorderStroke(SceneViewTokens.Glass.edgeWidth, SceneViewTokens.Glass.edgeRing),
            modifier = Modifier.padding(top = SceneViewTokens.Space.sm),
        ) {
            Text(
                text = label,
                style = SceneViewTokens.Type.caption,
                modifier = Modifier.padding(horizontal = SceneViewTokens.Space.md, vertical = SceneViewTokens.Space.xs),
            )
        }
    }
}

internal const val MEASURE_ADD_TAG = "measure_add"
// DESIGN.md has no disabled-opacity token: M3's 0.38 disables a control on a surface, but this
// shutter sits on the camera feed, where 0.38 lost the ring on bright floors. 0.45 keeps it seen
// while it plainly reads as off; the refusal line under the reticle says why.
private const val ADD_REFUSED_ALPHA = 0.45f
private val AddButtonSize = SceneViewTokens.Space.x3l + SceneViewTokens.Space.sm // 72 dp, a camera shutter
private val AddDiscSize = SceneViewTokens.Space.x2l + SceneViewTokens.Space.sm // 56 dp
private val AddRingWidth = SceneViewTokens.Space.xs - SceneViewTokens.Space.xs / 4 // 3 dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MeasureSettings(
    units: MeasureUnitSystem,
    onUnits: (MeasureUnitSystem) -> Unit,
    depthSupported: Boolean,
    useDepth: Boolean,
    onUseDepth: (Boolean) -> Unit,
) {
    Text(text = stringResource(R.string.demo_ar_measure_description), style = MaterialTheme.typography.bodyMedium)
    Text(
        text = stringResource(R.string.demo_ar_measure_units),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = SceneViewTokens.Space.md, bottom = SceneViewTokens.Space.sm),
    )
    val metric = stringResource(R.string.demo_ar_measure_units_metric)
    val imperial = stringResource(R.string.demo_ar_measure_units_imperial)
    ConnectedChoiceRow(
        options = MeasureUnitSystem.entries,
        selected = units,
        onSelect = onUnits,
        label = { if (it == MeasureUnitSystem.Metric) metric else imperial },
        optionTestTag = { "measure_units_${it.name.lowercase(Locale.ROOT)}" },
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = SceneViewTokens.Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.demo_ar_measure_depth_toggle),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    if (depthSupported) {
                        R.string.demo_ar_measure_depth_supported
                    } else {
                        R.string.demo_ar_measure_depth_unsupported
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = useDepth && depthSupported, onCheckedChange = onUseDepth, enabled = depthSupported)
    }
    Text(
        text = stringResource(R.string.demo_ar_measure_accuracy_notice),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = SceneViewTokens.Space.md),
    )
}

// ── Reticle ─────────────────────────────────────────────────────────────────────────────────

/**
 * Screen-centre reticle. A ring with a centre dot; it tightens and fills with the accent when
 * it locks onto something, turns amber on a guide, and a chip under it names the lock.
 */
@Composable
internal fun MeasureReticle(
    aim: MeasureAimView?,
    raising: Boolean,
    modifier: Modifier = Modifier,
    wallFoot: Boolean = false,
    refusal: MeasureRefusal? = null,
) {
    val kind = aim?.snap?.kind ?: MeasureSnapKind.None
    val onFoot = aim?.wallFoot != null
    val locked = aim != null && (kind != MeasureSnapKind.None || aim.closes)
    val guide = kind == MeasureSnapKind.Vertical || kind == MeasureSnapKind.Level || kind == MeasureSnapKind.Square
    val ringScale by animateFloatAsState(
        targetValue = when {
            raising -> RETICLE_SCALE_RAISING
            locked -> RETICLE_SCALE_LOCKED
            else -> 1f
        },
        animationSpec = SceneViewTokens.Motion.spring(),
        label = "reticle-scale",
    )
    val overlay = SceneViewTokens.ArOverlay
    val accent = when {
        refusal != null -> overlay.accentGuidance
        aim == null && !raising -> SceneViewTokens.Glass.onGlassMuted
        guide -> overlay.accentGuidance
        locked -> overlay.accentProgress
        else -> SceneViewTokens.Glass.onGlass
    }
    val description = stringResource(
        if (aim != null || raising) {
            R.string.demo_ar_measure_target_ready
        } else {
            R.string.demo_ar_measure_target_searching
        },
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(SceneViewTokens.Space.x3l).scale(ringScale).semantics { contentDescription = description },
        ) {
            val radius = size.minDimension / 2f - SceneViewTokens.Glass.edgeWidth.toPx() * 3
            val stroke = SceneViewTokens.Glass.edgeWidth.toPx()
            drawCircle(SceneViewTokens.Glass.edgeHalo, radius = radius, style = Stroke(stroke * 4))
            drawCircle(accent, radius = radius, style = Stroke(stroke * 2))
            if (raising) {
                // Raising: a crosshair, the aim is a line on the wall, not a point.
                val arm = radius * RETICLE_ARM
                drawLine(accent, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), stroke * 2)
            } else {
                if (wallFoot) {
                    // Wall and Room: a seam glyph, the floor line and the wall rising from it —
                    // points go where a wall meets the floor.
                    val arm = radius * RETICLE_ARM
                    val seam = if (onFoot) stroke * 3 else stroke * 2
                    drawLine(SceneViewTokens.Glass.edgeHalo, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), seam * 2)
                    drawLine(SceneViewTokens.Glass.edgeHalo, center, Offset(center.x, center.y - radius), stroke * 4)
                    drawLine(accent, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), seam)
                    drawLine(accent, center, Offset(center.x, center.y - radius), stroke * 2)
                }
                drawCircle(SceneViewTokens.Glass.edgeHalo, radius = stroke * 4, center = center)
                drawCircle(accent, radius = stroke * 3, center = center)
            }
        }
        val chip = when {
            refusal == MeasureRefusal.NotTracking -> R.string.demo_ar_measure_refusal_tracking
            refusal == MeasureRefusal.NoSurface -> R.string.demo_ar_measure_refusal_no_surface
            refusal == MeasureRefusal.OffWall -> R.string.demo_ar_measure_refusal_off_wall
            refusal == MeasureRefusal.AnchorFailed -> R.string.demo_ar_measure_refusal_anchor
            aim?.closes == true -> R.string.demo_ar_measure_snap_close
            kind == MeasureSnapKind.Point -> R.string.demo_ar_measure_snap_point
            kind == MeasureSnapKind.Corner -> R.string.demo_ar_measure_snap_corner
            kind == MeasureSnapKind.Square -> R.string.demo_ar_measure_snap_square
            onFoot -> R.string.demo_ar_measure_snap_wall
            kind == MeasureSnapKind.Edge -> R.string.demo_ar_measure_snap_edge
            kind == MeasureSnapKind.Vertical -> R.string.demo_ar_measure_snap_vertical
            kind == MeasureSnapKind.Level -> R.string.demo_ar_measure_snap_level
            else -> null
        }
        val lastChip = remember { intArrayOf(R.string.demo_ar_measure_snap_point) }
        if (chip != null) lastChip[0] = chip
        AnimatedVisibility(
            visible = chip != null,
            enter = fadeIn(SceneViewTokens.Motion.fade()) + scaleIn(SceneViewTokens.Motion.spring()),
            exit = fadeOut(SceneViewTokens.Motion.fade()) + scaleOut(SceneViewTokens.Motion.spring()),
            modifier = Modifier.offset(y = SceneViewTokens.Space.x2l),
        ) {
            Surface(
                shape = CircleShape,
                color = SceneViewTokens.ModePill.container,
                contentColor = accent,
                border = BorderStroke(SceneViewTokens.Glass.edgeWidth, SceneViewTokens.Glass.edgeRing),
            ) {
                Text(
                    text = stringResource(lastChip[0]),
                    style = SceneViewTokens.Type.caption,
                    modifier = Modifier.padding(
                        horizontal = SceneViewTokens.Space.sm,
                        vertical = SceneViewTokens.Space.xs,
                    ),
                )
            }
        }
    }
}

private const val RETICLE_SCALE_LOCKED = 0.72f
private const val RETICLE_SCALE_RAISING = 0.85f
private const val RETICLE_ARM = 1.6f

// ── 3D nodes ────────────────────────────────────────────────────────────────────────────────

/** Points, growing segments with labels, rising walls and the floor of one figure. */
@Composable
private fun ARSceneScope.MeasureFigureNodes(
    figure: MeasureFigure<Anchor>,
    outline: List<Position>,
    liveHeight: Float?,
    cameraPosition: Position,
    eye: SceneNode,
    len: (Float) -> String,
    area: (Float) -> String,
) {
    if (outline.isEmpty()) return
    val overlay = SceneViewTokens.ArOverlay
    val walls = figure.mode != MeasureMode.Distance
    val pointMaterial = rememberUnlitMaterialInstance(materialLoader, overlay.onScrim)
    val lineColor = if (walls) overlay.accentProgress else overlay.onScrim
    val lineMaterial = rememberUnlitMaterialInstance(materialLoader, lineColor)

    outline.forEachIndexed { i, p ->
        key("point-$i") { SphereNode(radius = POINT_RADIUS, materialInstance = pointMaterial, position = p) }
    }
    val segmentCount = when {
        figure.closed && outline.size > 2 -> outline.size
        else -> outline.size - 1
    }
    for (i in 0 until segmentCount) {
        val a = outline[i]
        val b = outline[(i + 1) % outline.size]
        key("segment-$i") {
            MeasureSegmentNode(
                start = a,
                end = b,
                label = len(measureDistanceMeters(a, b)),
                material = lineMaterial,
                cameraPosition = cameraPosition,
                eye = eye,
            )
        }
    }

    if (figure.hasWalls && outline.size >= 2) {
        val target = figure.height ?: liveHeight ?: WALL_MIN_HEIGHT
        // Walls rise from the floor the moment they appear, then follow the aim on a spring.
        val rising = remember { Animatable(WALL_MIN_HEIGHT) }
        LaunchedEffect(target) { rising.animateTo(target, SceneViewTokens.Motion.spring()) }
        val height = rising.value
        val closed = figure.closed
        MeasureWalls(outline = outline, closed = closed, height = height)
        // The top edge and each corner's upright, in the accent.
        val top = outline.map { Position(it.x, it.y + height, it.z) }
        TubeNode(
            points = if (closed) top + top.first() else top,
            radius = LINE_RADIUS,
            materialInstance = lineMaterial,
        )
        outline.forEachIndexed { i, p ->
            key("upright-$i") {
                TubeNode(
                    points = listOf(p, Position(p.x, p.y + height, p.z)),
                    radius = LINE_RADIUS,
                    materialInstance = lineMaterial,
                )
            }
        }
        // Height on the first upright, wall area on the first wall.
        val first = outline.first()
        MeasureLabel(
            text = len(height),
            position = Position(first.x, first.y + height / 2f, first.z),
            accent = true,
            cameraPosition = cameraPosition,
            eye = eye,
        )
        if (figure.mode == MeasureMode.Wall) {
            val mid = measureMidpoint(outline[0], outline[1])
            val wallArea = measureDistanceMeters(outline[0], outline[1]) * height
            MeasureLabel(
                text = area(wallArea),
                position = Position(mid.x, mid.y + height / 2f, mid.z),
                cameraPosition = cameraPosition,
                eye = eye,
            )
        }
    }

    if (figure.mode == MeasureMode.Room && figure.closed && outline.size >= MeasureBoard.MIN_OUTLINE_POINTS) {
        MeasureFloor(outline = outline)
        val numbers = measureFigureNumbers(figure, outline)
        numbers.outlineArea?.let {
            val cx = outline.map { p -> p.x }.average().toFloat()
            val cz = outline.map { p -> p.z }.average().toFloat()
            MeasureLabel(
                text = area(it),
                position = Position(cx, outline.first().y + LABEL_LIFT, cz),
                accent = true,
                cameraPosition = cameraPosition,
                eye = eye,
            )
        }
    }
}

/** A segment that draws itself from its start, then its length label pops in. */
@Composable
private fun ARSceneScope.MeasureSegmentNode(
    start: Position,
    end: Position,
    label: String,
    material: MaterialInstance,
    cameraPosition: Position,
    eye: SceneNode,
) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, tween(SceneViewTokens.Duration.mediumMillis, easing = SceneViewTokens.Ease.expressive))
    }
    TubeNode(
        points = listOf(start, measureLerp(start, end, grow.value)),
        radius = LINE_RADIUS,
        materialInstance = material,
    )
    if (grow.value > LABEL_APPEAR_AT) {
        MeasureLabel(
            text = label,
            position = measureMidpoint(start, end),
            cameraPosition = cameraPosition,
            eye = eye,
            appear = ((grow.value - LABEL_APPEAR_AT) / (1f - LABEL_APPEAR_AT)).coerceIn(0f, 1f),
        )
    }
}

private const val LABEL_APPEAR_AT = 0.6f

/** The reticle's 3D side: the snapped dot, the live segment and its label, the guide line. */
@Composable
private fun ARSceneScope.MeasureAimNodes(
    view: MeasureAimView,
    from: Position?,
    liveLabel: String?,
    cameraPosition: Position,
    eye: SceneNode,
) {
    val overlay = SceneViewTokens.ArOverlay
    val guide = view.snap.kind == MeasureSnapKind.Vertical ||
        view.snap.kind == MeasureSnapKind.Level ||
        view.snap.kind == MeasureSnapKind.Square
    val dotMaterial = rememberUnlitMaterialInstance(materialLoader, overlay.accentProgress)
    val liveMaterial = rememberUnlitMaterialInstance(materialLoader, overlay.accentProgress)
    val guideMaterial = rememberUnlitMaterialInstance(materialLoader, overlay.accentGuidance)
    val target = view.snap.position
    view.wallFoot?.let { (a, b) ->
        // The recognised wall's foot the point lands on, lit along its length.
        TubeNode(points = listOf(a, b), radius = LINE_RADIUS, materialInstance = liveMaterial)
    }
    SphereNode(radius = AIM_DOT_RADIUS, materialInstance = if (guide) guideMaterial else dotMaterial, position = target)
    if (from != null && !view.closes) {
        TubeNode(points = listOf(from, target), radius = LIVE_LINE_RADIUS, materialInstance = liveMaterial)
        if (liveLabel != null) {
            MeasureLabel(
                text = liveLabel,
                position = measureMidpoint(from, target),
                accent = true,
                cameraPosition = cameraPosition,
                eye = eye,
            )
        }
    }
    val guideFrom = view.snap.guideFrom
    if (guide && guideFrom != null) {
        // The guide runs past the target so it reads as a line, not a segment.
        val extended = measureLerp(guideFrom, target, GUIDE_OVERSHOOT)
        TubeNode(points = listOf(guideFrom, extended), radius = GUIDE_RADIUS, materialInstance = guideMaterial)
    }
}

private const val GUIDE_OVERSHOOT = 1.6f

/** A length label facing [eye] the right way up, sized by its distance so it reads the same near and far. */
@Composable
private fun ARSceneScope.MeasureLabel(
    text: String,
    position: Position,
    cameraPosition: Position,
    eye: SceneNode,
    accent: Boolean = false,
    appear: Float = 1f,
) {
    val overlay = SceneViewTokens.ArOverlay
    val scale = measureLabelScale(measureDistanceMeters(cameraPosition, position)) * appear
    val lifted = Position(position.x, position.y + LABEL_LIFT * scale, position.z)
    // Recreated per accent, since a label never changes tone.
    key(accent) {
        TextNode(
            text = text,
            fontSize = LABEL_FONT_SIZE,
            textColor = (if (accent) overlay.onAccentProgress else overlay.onScrim).toArgb(),
            backgroundColor = (if (accent) overlay.accentProgress else overlay.scrimDark).toArgb(),
            widthMeters = LABEL_WIDTH,
            heightMeters = LABEL_HEIGHT,
            position = lifted,
            scale = Scale(scale.coerceAtLeast(MIN_LABEL_SCALE)),
            // Faces the camera with the screen's up as its top, so a label on the floor reads the
            // right way up seen from above too (#4732): SceneView's billboard flips there.
            apply = {
                onFrame = {
                    measureLabelQuaternion(worldPosition, eye.worldPosition, eye.worldTransform.up)
                        ?.let { worldQuaternion = it }
                }
            },
        )
    }
}

private const val MIN_LABEL_SCALE = 0.001f

/** Translucent, two-sided walls on [outline], rebuilt only when the number of walls changes. */
@Composable
private fun ARSceneScope.MeasureWalls(outline: List<Position>, closed: Boolean, height: Float) {
    val wallCount = if (closed && outline.size > 2) outline.size else outline.size - 1
    val fill = rememberTwoSidedMaterial(SceneViewTokens.ArOverlay.accentProgress.copy(alpha = WALL_FILL_ALPHA))
    val vertices = measureWallVertices(outline, closed, height).map { Geometry.Vertex(position = it) }
    MeasureMesh(
        key = "walls-$wallCount",
        vertices = vertices,
        indices = measureWallIndices(wallCount).toList(),
        bounds = outline,
        material = fill,
    )
}

/** The room's floor, ear-clipped so L- and T-shaped rooms fill correctly. */
@Composable
private fun ARSceneScope.MeasureFloor(outline: List<Position>) {
    val indices = measureTriangulateXZ(outline)
    if (indices.isEmpty()) return
    val fill = rememberTwoSidedMaterial(SceneViewTokens.ArOverlay.accentProgress.copy(alpha = FLOOR_FILL_ALPHA))
    val vertices = outline.map { Geometry.Vertex(position = Position(it.x, it.y + FLOOR_FILL_LIFT, it.z)) }
    MeasureMesh(
        key = "floor-${outline.size}",
        vertices = vertices,
        indices = indices.toList(),
        bounds = outline,
        material = fill,
    )
}

@Composable
private fun ARSceneScope.MeasureMesh(
    key: String,
    vertices: List<Geometry.Vertex>,
    indices: List<Int>,
    bounds: List<Position>,
    material: MaterialInstance,
) {
    val geometry = remember(engine, key) {
        Geometry.Builder(PrimitiveType.TRIANGLES).vertices(vertices).indices(indices).build(engine)
    }
    SideEffect { geometry.update(engine, vertices, listOf(indices)) }
    DisposableEffect(geometry) { onDispose { engine.safeDestroyGeometry(geometry) } }
    // Built once with room to rise: Filament culls on this box, the walls move inside it.
    val box = remember(geometry) { wallBounds(bounds) }
    MeshNode(
        primitiveType = PrimitiveType.TRIANGLES,
        vertexBuffer = geometry.vertexBuffer,
        indexBuffer = geometry.indexBuffer,
        boundingBox = box,
        materialInstance = material,
    )
}

private fun wallBounds(outline: List<Position>): FilamentBox {
    val minX = outline.minOf { it.x } - WALL_BOX_MARGIN
    val maxX = outline.maxOf { it.x } + WALL_BOX_MARGIN
    val minY = outline.minOf { it.y } - WALL_BOX_MARGIN
    val maxY = outline.maxOf { it.y } + WALL_MAX_HEIGHT + WALL_BOX_MARGIN
    val minZ = outline.minOf { it.z } - WALL_BOX_MARGIN
    val maxZ = outline.maxOf { it.z } + WALL_BOX_MARGIN
    return FilamentBox(
        (minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f,
        (maxX - minX) / 2f, (maxY - minY) / 2f, (maxZ - minZ) / 2f,
    )
}

/** Unlit, transparent, seen from both sides — a wall is looked at from inside and out. */
@Composable
private fun ARSceneScope.rememberTwoSidedMaterial(color: Color): MaterialInstance {
    val instance = remember(materialLoader, color) {
        materialLoader.createUnlitColorInstance(color).also { it.setCullingMode(Material.CullingMode.NONE) }
    }
    DisposableEffect(instance) { onDispose { materialLoader.destroyMaterialInstance(instance) } }
    return instance
}

// ── Per-frame helpers ───────────────────────────────────────────────────────────────────────

/** The board after a tap with the reticle on [aim]: close the outline, or add an anchored point. */
private fun tapAt(board: MeasureBoard<Anchor>, aim: MeasureAim?): MeasureBoard<Anchor>? {
    if (aim == null) return null
    if (aim.view.closes) return board.tap(null, closesOutline = true)
    val anchor = aim.reuse ?: runCatching { aim.createAnchor() }.getOrNull() ?: return null
    return board.tap(anchor, closesOutline = false)
}

/** New anchor positions when one of them really moved, `null` otherwise. */
private fun movedPositions(board: MeasureBoard<Anchor>, positions: Map<Anchor, Position>): Map<Anchor, Position>? {
    val anchors = board.allPoints.distinct()
    if (anchors.isEmpty()) return null
    val next = anchors.map { it.pose.toPosition() }
    val previous = anchors.map { positions[it] ?: Position() }
    return if (measurePointsMoved(previous, next)) anchors.zip(next).toMap() else null
}

/** Height the rising walls of [figure] reach along the camera ray, `null` off every wall. */
private fun raisingHeight(
    figure: MeasureFigure<Anchor>?,
    outline: List<Position>,
    origin: Position,
    direction: Position,
): Float? = when {
    figure == null -> null
    figure.mode == MeasureMode.Wall && outline.size >= 2 ->
        measureWallHeightFromRay(origin, direction, outline[0], outline[1])
    else -> measureRoomHeightFromRay(origin, direction, outline, closed = true)
}

/** Whether the reticle moved enough (or changed lock) to be worth a recomposition. */
private fun aimChanged(next: MeasureAimView?, shown: MeasureAimView?): Boolean = when {
    next == shown -> false
    next == null || shown == null -> true
    next.snap.kind != shown.snap.kind || next.closes != shown.closes -> true
    (next.wallFoot == null) != (shown.wallFoot == null) -> true
    next.wallFoot != null && shown.wallFoot != null &&
        measureDistanceMeters(next.wallFoot.first, shown.wallFoot.first) > AIM_EPSILON -> true
    else -> measureDistanceMeters(next.snap.position, shown.snap.position) > AIM_EPSILON
}

// ── Hit testing ─────────────────────────────────────────────────────────────────────────────

/**
 * What the screen centre is on this frame, snapped. Wall and room points after the first
 * come from the floor plane of the first point; everything else from the accuracy-ordered
 * hit test (plane, depth point, depth image, feature point).
 */
@Suppress("LongParameterList")
private fun findMeasureAim(
    frame: Frame,
    session: Session?,
    planes: List<MeasurePlane>,
    walls: LiveWallSnapshot?,
    board: MeasureBoard<Anchor>,
    outlines: List<List<Position>>,
    positions: Map<Anchor, Position>,
    origin: Position,
    direction: Position,
    x: Float,
    y: Float,
    allowDepth: Boolean,
): MeasureAim? {
    val mode = board.mode
    val active = board.active
    val activeOutline = if (active != null) outlines.lastOrNull().orEmpty() else emptyList()
    val floorY = activeOutline.firstOrNull()?.y?.takeIf { mode != MeasureMode.Distance }

    var hit: MeasureHit? = null
    if (floorY != null) {
        measureRayFloorHit(origin, direction, floorY)?.let { hit = MeasureHit(it, MeasureHitSource.Floor, null, null) }
    }
    if (hit == null) hit = hitTestCenter(frame, session, x, y, allowDepth)
    val raw = hit ?: return null
    val rawPosition = when {
        floorY != null -> measureOnLevel(raw.position, floorY)
        // The first corner of a wall or a room may be aimed on the wall's foot: drop it to the floor.
        mode != MeasureMode.Distance -> measureFirstFloorPoint(raw.position, raw.planeKind, measureFloorLevel(planes))
        else -> raw.position
    }
    // The level a wall or a room is traced on: its first point's, or the one being placed.
    val level = if (mode == MeasureMode.Distance) null else floorY ?: rawPosition.y

    val pointPositions = board.allPoints.distinct().mapNotNull { anchor ->
        positions[anchor]?.let { if (floorY != null) measureOnLevel(it, floorY) else it }
    }
    val context = measurePlaneSnapTargets(mode, planes, level).copy(
        points = pointPositions,
        previous = activeOutline.lastOrNull().takeIf { mode == MeasureMode.Distance },
    )
    val tolerance = measureSnapTolerance(measureDistanceMeters(origin, rawPosition))
    var snap = snapMeasureTarget(rawPosition, context, tolerance)
    if (snap.kind == MeasureSnapKind.None && mode == MeasureMode.Room && activeOutline.size >= 2) {
        snapSquareCorner(activeOutline[activeOutline.size - 2], activeOutline.last(), rawPosition)?.let { snap = it }
    }
    val closes = active != null && mode != MeasureMode.Wall &&
        measureClosesOutline(activeOutline, snap.position, tolerance)
    val reuse = if (snap.kind == MeasureSnapKind.Point) {
        board.allPoints.distinct().firstOrNull { anchor ->
            positions[anchor]?.let {
                val p = if (floorY != null) measureOnLevel(it, floorY) else it
                measureDistanceMeters(p, snap.position) < REUSE_EPSILON
            } == true
        }
    } else {
        null
    }
    val snapped = snap.position
    val createAnchor: () -> Anchor? = if (snap.kind == MeasureSnapKind.None && snapped == raw.position &&
        raw.createHitAnchor != null
    ) {
        raw.createHitAnchor
    } else {
        { session?.createAnchor(Pose.makeTranslation(snapped.x, snapped.y, snapped.z)) }
    }
    // The live walls only tell the point is on a wall's foot; they never move it (#4750 review).
    val foot = if (walls != null && mode != MeasureMode.Distance) {
        measureWallFootAt(snapped, walls.walls, walls.floorY)?.let { it.a to it.b }
    } else {
        null
    }
    // Debug only, and lazy: release builds allocate nothing here, and no JNI call runs before a commit.
    val qa: (() -> MeasureQaPoint)? = if (BuildConfig.DEBUG) {
        {
            MeasureQaPoint(
                position = snapped,
                source = raw.source.name,
                snap = snap.kind.name,
                plane = raw.plane?.let { plane -> runCatching { plane.type.name }.getOrNull() },
                // The aim is at most a frame old; a frame ARCore already replaced may refuse.
                timestampNs = runCatching { frame.timestamp }.getOrNull(),
                cameraDistance = measureDistanceMeters(origin, snapped),
            )
        }
    } else {
        null
    }
    return MeasureAim(MeasureAimView(snap, closes, raw.source, foot), createAnchor, reuse, qa)
}

private const val REUSE_EPSILON = 0.002f

private class MeasureHit(
    val position: Position,
    val source: MeasureHitSource,
    val createHitAnchor: (() -> Anchor?)?,
    /** How the hit plane faces; `null` off a plane. */
    val planeKind: MeasurePlaneKind?,
    /** The hit plane, for the QA log only (its type is read on a commit); `null` off a plane. */
    val plane: Plane? = null,
)

/** Accuracy-ordered hit at the screen centre, without creating an anchor. */
private fun hitTestCenter(frame: Frame, session: Session?, x: Float, y: Float, allowDepth: Boolean): MeasureHit? {
    val hits = runCatching { frame.hitTest(x, y) }.getOrNull().orEmpty()
        .filter { it.trackable.trackingState == TrackingState.TRACKING }
    hits.firstOrNull { hit ->
        val plane = hit.trackable as? Plane
        plane != null && plane.subsumedBy == null && plane.isPoseInPolygon(hit.hitPose)
    }?.let { hit ->
        val plane = hit.trackable as Plane
        return MeasureHit(
            position = hit.hitPose.toPosition(),
            source = MeasureHitSource.Plane,
            createHitAnchor = { hit.takeIf { it.trackable.trackingState == TrackingState.TRACKING }?.createAnchor() },
            planeKind = plane.liveKind.toMeasureKind(),
            plane = plane,
        )
    }
    if (allowDepth) {
        hits.firstOrNull { it.trackable is DepthPoint }?.let { hit ->
            return MeasureHit(
                hit.hitPose.toPosition(),
                MeasureHitSource.Depth,
                { hit.takeIf { it.trackable.trackingState == TrackingState.TRACKING }?.createAnchor() },
                null,
            )
        }
        if (session != null) {
            frame.hitTestDepth(x, y)?.let { depthHit ->
                return MeasureHit(depthHit.position, MeasureHitSource.Depth, null, null)
            }
        }
    }
    hits.firstOrNull { it.trackable is com.google.ar.core.Point }?.let { hit ->
        return MeasureHit(
            hit.hitPose.toPosition(),
            MeasureHitSource.FeaturePoint,
            { hit.takeIf { it.trackable.trackingState == TrackingState.TRACKING }?.createAnchor() },
            null,
        )
    }
    return null
}

/** How a live plane faces, as Measure's snapper reads it. */
private fun LivePlane.Kind.toMeasureKind(): MeasurePlaneKind = when (this) {
    LivePlane.Kind.Floor -> MeasurePlaneKind.Upward
    LivePlane.Kind.Ceiling -> MeasurePlaneKind.Downward
    else -> MeasurePlaneKind.Vertical
}

/** A live plane as a snap source: its world polygon's corners and edges, table edges included. */
private fun LivePlane.toMeasurePlane(): MeasurePlane = MeasurePlane(
    kind.toMeasureKind(),
    List(vertexCount) { Position(polygon[it * 3], polygon[it * 3 + 1], polygon[it * 3 + 2]) },
)

/**
 * The walls recognised live for Measure: the shared [PlaneWallSource] fed ARCore's planes once a
 * frame, eased by a [LiveWallMotion]. Nothing is shown before a floor is tracked.
 */
private class MeasureLiveWalls {
    val planeIds = LivePlaneIds()
    private val source = PlaneWallSource()
    val motion = LiveWallMotion()
    private var snapshot = LiveWallSnapshot.Empty
    private var lastNanos = 0L

    /** Seconds since the previous [update], for [motion]. */
    var dt = 0f
        private set

    fun update(planes: List<LivePlane>, cameraY: Float, timestampNanos: Long): LiveWallSnapshot {
        dt = if (lastNanos == 0L || timestampNanos <= lastNanos) 0f else (timestampNanos - lastNanos) / NANOS_PER_SECOND
        lastNanos = timestampNanos
        val floorY = livePlanesFloorY(planes, cameraY) ?: return snapshot
        snapshot = source.update(planes, floorY, timestampNanos / NANOS_PER_SECOND)
        return snapshot
    }

    /** Forgets every wall: a reset leaves no ghost wall behind. */
    fun reset() {
        planeIds.clear()
        source.reset()
        motion.reset()
        snapshot = LiveWallSnapshot.Empty
        lastNanos = 0L
        dt = 0f
    }

    private companion object {
        const val NANOS_PER_SECOND = 1e9f
    }
}

/** How long a refused tap's reason stays under the reticle. */
private const val REFUSAL_SHOWN_MS = 2500L

/** ARCore [Pose] translation as a SceneView world [Position]. */
private fun Pose.toPosition(): Position = Position(x = tx(), y = ty(), z = tz())
