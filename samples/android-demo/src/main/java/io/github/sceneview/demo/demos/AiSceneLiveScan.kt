package io.github.sceneview.demo.demos

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.filament.Engine
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARCoreAvailability
import io.github.sceneview.ar.ARCoreAvailabilityOverlay
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.demo.rememberArPlaybackDataset
import io.github.sceneview.demo.common.ArDemoPermissionGate
import io.github.sceneview.demo.common.QaCameraBackdrop
import io.github.sceneview.demo.common.qaCameraBackdropEnabled
import io.github.sceneview.demo.common.qaCameraBackdropSurfaceType
import io.github.sceneview.demo.common.rememberQaCameraBackdropActive
import io.github.sceneview.demo.common.trackingFailureMessage
import io.github.sceneview.demo.demos.internal.ArDebugOrbitCamera
import io.github.sceneview.demo.demos.internal.ArDebugSession
import io.github.sceneview.demo.demos.internal.ArDebugTrace
import io.github.sceneview.demo.demos.internal.ScanCopy
import io.github.sceneview.demo.demos.internal.OrbitBand
import io.github.sceneview.demo.demos.internal.ScanFigures
import io.github.sceneview.demo.demos.internal.ScanLimits
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Scan this room" for the `ai-scene` demo: a real ARCore session that records the same data as
 * the Rerun scan — ARCore's planes, the feature points, the camera's path, its keyframe photos
 * with their intrinsics, and the dense depth map where the phone has raw depth — through the same
 * recorder ([ArDebugRecorder], [ScanCapture]) and the same file format. Stop builds that file and
 * reads it back ([loadRerunSession]); [onScanned] gets the session the AI's tools then read, the
 * way they read the bundled recording.
 *
 * Nothing is saved: the scan lives as long as the demo screen. Where ARCore cannot run (the
 * emulator, a phone without Google Play Services for AR), the SDK says why and the screen offers
 * the way back to the sample scan.
 */
@Composable
internal fun AiSceneLiveScan(
    onBack: () -> Unit,
    onScanned: (RerunReplayMedia) -> Unit,
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
) {
    BackHandler(onBack = onBack)
    ArDemoPermissionGate(title = LIVE_SCAN_TITLE, onBack = onBack) {
        LiveScanScreen(onBack, onScanned, engine, modelLoader, materialLoader)
    }
}

@Composable
@Suppress("LongMethod") // one AR screen: its session, its scan, its three overlays
private fun LiveScanScreen(
    onBack: () -> Unit,
    onScanned: (RerunReplayMedia) -> Unit,
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
) {
    val scope = rememberCoroutineScope()
    // The scan records into this session's trace: the card's 3D view and the scan are one data set.
    val debugSession = remember { ArDebugSession() }
    val recorder = remember { ArDebugRecorder() }
    var scan by remember { mutableStateOf<ScanCapture?>(null) }
    var finishing by remember { mutableStateOf(false) }
    var rawDepth by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var latestFrame by remember { mutableStateOf<Frame?>(null) }
    var availability by remember { mutableStateOf<ARCoreAvailability?>(null) }
    var trackingFailure by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var caughtNothing by remember { mutableStateOf(false) }
    val scanMedia = scan?.live
    val scanOrbit = remember(scanMedia) { ArDebugOrbitCamera(drift = false, band = OrbitBand.STAGE_PORTRAIT.copy(lift = LIVE_SCAN_STAGE_LIFT)) }

    val onStart = start@{
        val frame = latestFrame ?: return@start
        val capture = ScanCapture.start(frame, scope, rawDepth) ?: return@start
        caughtNothing = false
        debugSession.trace = capture.trace
        debugSession.goLive()
        scan = capture
    }
    // As in Rerun: the scan starts by itself once ARCore has found the room.
    var autoStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (!autoStarted) {
            if (isTracking && scan == null) {
                onStart()
                autoStarted = scan != null
            }
            delay(LIVE_SCAN_POLL_MS)
        }
    }
    // Stop: no more photos, the file is built and read back, and the AI gets it. A scan that
    // caught nothing hands nothing over and says so.
    val onStop = stop@{
        val capture = scan
        if (finishing || capture == null) return@stop
        finishing = true
        scope.launch {
            val opened = capture.finish()?.let { pack -> runCatching { loadRerunSession(pack) }.getOrNull() }
            debugSession.trace = ArDebugTrace()
            scan = null
            finishing = false
            if (opened != null) onScanned(opened) else caughtNothing = true
        }
    }

    DemoScaffold(
        title = LIVE_SCAN_TITLE,
        onBack = onBack,
        topOverlay = {
            val media = scanMedia
            if (media != null) {
                val stats = debugSession.stats
                val now by produceState(SystemClock.elapsedRealtimeNanos(), scan) {
                    while (true) {
                        delay(LIVE_SCAN_HUD_TICK_MS)
                        value = SystemClock.elapsedRealtimeNanos()
                    }
                }
                val depthScan = scan?.rawDepth == true
                ScanHud(
                    seconds = scan?.elapsedSeconds(now) ?: stats.duration,
                    figures = ScanFigures(
                        points = if (depthScan) scan?.denseCount ?: 0 else stats.mapPoints,
                        pointBudget = ScanLimits.pointBudget(depthScan),
                        surfaceMetres2 = stats.surfaceMetres2,
                        photos = debugSession.trace.imageCount,
                    ),
                )
                ScanStage(
                    session = debugSession,
                    orbit = scanOrbit,
                    media = media,
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                )
            }
        },
        bottomOverlay = {
            when {
                // ARCore ruled this phone out: the SDK's card says why; this one says what is left.
                availability != null -> OverlayCard(testTag = LIVE_SCAN_UNAVAILABLE_TAG) {
                    Text(text = LIVE_SCAN_UNAVAILABLE_TITLE, style = OnScrimTitle)
                    Text(text = LIVE_SCAN_UNAVAILABLE_BODY, style = OnScrimBody)
                    // The card is a scrim over the camera in both themes: the action reads white on it.
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = SceneViewTokens.ArOverlay.onScrim),
                    ) { Text(LIVE_SCAN_BACK_TO_SAMPLE) }
                }
                else -> {
                    val hint = when {
                        scanMedia != null || finishing -> null
                        caughtNothing -> LIVE_SCAN_CAUGHT_NOTHING
                        !isTracking -> trackingFailureMessage(trackingFailure) ?: ScanCopy.WAITING
                        else -> null
                    }
                    if (hint != null) {
                        OverlayCard(testTag = LIVE_SCAN_HINT_TAG) { Text(text = hint, style = OnScrimBody) }
                    }
                    ScanShutter(
                        recording = scanMedia != null,
                        finishing = finishing,
                        startEnabled = isTracking,
                        onStart = onStart,
                        onStop = onStop,
                    )
                }
            }
        },
    ) {
        val cameraStream = rememberARCameraStream(materialLoader)
        // Replays a recorded ARCore dataset when the device-QA harness deep-links the demo with
        // `--es ar_playback_file <path>` (#1576); `null`, so live AR, for every normal launch.
        val playbackDataset = rememberArPlaybackDataset()
        // QA camera backdrop (#3308): the emulator delivers no camera frame.
        val qaBackdrop = rememberQaCameraBackdropActive(cameraReady)
        Box(modifier = Modifier.fillMaxSize()) {
            if (qaBackdrop) QaCameraBackdrop(seed = LIVE_SCAN_QA_SEED)
            ARSceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                isOpaque = !qaCameraBackdropEnabled(),
                surfaceType = qaCameraBackdropSurfaceType(),
                cameraStream = if (qaBackdrop) null else cameraStream,
                playbackDataset = playbackDataset,
                planeRenderer = true,
                sessionConfiguration = { session: Session, config: Config ->
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    // Raw depth where the phone has it (a dense scan); the sparse scan otherwise.
                    val depth = runCatching {
                        session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)
                    }.getOrDefault(false)
                    if (depth) config.depthMode = Config.DepthMode.RAW_DEPTH_ONLY
                    rawDepth = depth
                },
                onSessionUpdated = { session: Session, frame: Frame ->
                    if (frame.timestamp > 0L) cameraReady = true
                    latestFrame = frame
                    isTracking = frame.camera.trackingState == TrackingState.TRACKING
                    recorder.record(debugSession.trace, session, frame, emptyList(), scan.takeUnless { finishing })
                },
                arCoreAvailabilityOverlay = { ARCoreAvailabilityOverlay(it) },
                onARCoreAvailability = { availability = it },
                onTrackingFailureChanged = { trackingFailure = it },
            )
        }
    }
}

private const val LIVE_SCAN_TITLE = "Scan this room"
private const val LIVE_SCAN_UNAVAILABLE_TITLE = "No live scan on this phone"
private const val LIVE_SCAN_UNAVAILABLE_BODY =
    "The AI can still rebuild the sample room, recorded on a phone with the same scan."
private const val LIVE_SCAN_BACK_TO_SAMPLE = "Back to the sample scan"
private const val LIVE_SCAN_CAUGHT_NOTHING = "The scan caught nothing. Move slower, along the walls, and scan again."
private const val LIVE_SCAN_UNAVAILABLE_TAG = "ai-scene-live-unavailable"
private const val LIVE_SCAN_HINT_TAG = "ai-scene-live-hint"
private const val LIVE_SCAN_QA_SEED = "ai-scene"
private const val LIVE_SCAN_STAGE_LIFT = 0.12f
private const val LIVE_SCAN_POLL_MS = 100L
private const val LIVE_SCAN_HUD_TICK_MS = 100L
