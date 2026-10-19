package io.github.sceneview.demo.demos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded._3dRotation
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARCoreAvailability
import io.github.sceneview.ar.ARCoreAvailabilityOverlay
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.ar.rerun.RerunBridge
import io.github.sceneview.ar.rerun.rememberRerunBridge
import io.github.sceneview.demo.BuildConfig
import io.github.sceneview.demo.DemoBottomOverlayScope
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.demo.DemoSettings
import io.github.sceneview.demo.DockItem
import io.github.sceneview.demo.R
import io.github.sceneview.demo.common.DemoStatusBanner
import io.github.sceneview.demo.common.DemoStatusTone
import io.github.sceneview.demo.common.ForceTrackingFailureMenu
import io.github.sceneview.demo.common.ForcedTrackingFailure
import io.github.sceneview.demo.common.QaCameraBackdrop
import io.github.sceneview.demo.common.SceneAction
import io.github.sceneview.demo.common.SceneActionBar
import io.github.sceneview.demo.common.qaCameraBackdropEnabled
import io.github.sceneview.demo.common.qaCameraBackdropSurfaceType
import io.github.sceneview.demo.common.ArDemoPermissionGate
import io.github.sceneview.demo.common.qaStateOverridesAllowed
import io.github.sceneview.demo.common.rememberQaCameraBackdropActive
import io.github.sceneview.demo.common.trackingFailureMessage
import io.github.sceneview.demo.demos.internal.ArDebugFormat
import io.github.sceneview.demo.demos.internal.ArDebugLogPlayer
import io.github.sceneview.demo.demos.internal.ArDebugOrbitCamera
import io.github.sceneview.demo.demos.internal.OrbitBand
import io.github.sceneview.demo.demos.internal.ArDebugSession
import io.github.sceneview.demo.demos.internal.ArDebugTrace
import io.github.sceneview.demo.demos.internal.RERUN_INTRO
import io.github.sceneview.demo.demos.internal.RERUN_REPLAY_INTRO
import io.github.sceneview.demo.demos.internal.RERUN_SETUP_STEPS
import io.github.sceneview.demo.demos.internal.RERUN_SETUP_TITLE
import io.github.sceneview.demo.demos.internal.ReplayGeometry
import io.github.sceneview.demo.demos.internal.RerunReplayAssets
import io.github.sceneview.demo.demos.internal.RerunScanModel
import io.github.sceneview.demo.demos.internal.ScanDevice
import io.github.sceneview.demo.demos.internal.RerunCapturePack
import io.github.sceneview.demo.demos.internal.RerunExportFormat
import io.github.sceneview.demo.demos.internal.RerunExportSource
import io.github.sceneview.demo.demos.internal.RerunImportFailure
import io.github.sceneview.demo.demos.internal.RerunSessionSource
import io.github.sceneview.demo.demos.internal.RerunSessionStore
import io.github.sceneview.demo.demos.internal.RerunSetupStep
import io.github.sceneview.demo.demos.internal.RerunStatusUx
import io.github.sceneview.demo.demos.internal.DollhouseCopy
import io.github.sceneview.demo.demos.internal.RoomDollhouse
import io.github.sceneview.demo.demos.internal.ScanClock
import io.github.sceneview.demo.demos.internal.ScanCopy
import io.github.sceneview.demo.demos.internal.ScanDetail
import io.github.sceneview.demo.demos.internal.ScanFollow
import io.github.sceneview.demo.demos.internal.ScanPhase
import io.github.sceneview.demo.demos.internal.of
import io.github.sceneview.demo.demos.internal.parseArDebugLog
import io.github.sceneview.demo.demos.internal.rerunSaveActionUx
import io.github.sceneview.demo.demos.internal.rerunSaveFailureMessage
import io.github.sceneview.demo.demos.internal.rerunShowsSaveAction
import io.github.sceneview.demo.demos.internal.rerunStatusUx
import io.github.sceneview.demo.rememberArPlaybackDataset
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.theme.SceneViewTokens.ArOverlay
import io.github.sceneview.demo.theme.SceneViewTokens.Space
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 3D Scan (the Rerun demo, id `ar-rerun`): a space scanned with ARCore, rebuilt in 3D.
 *
 * It opens on a **landing** laid out as the iOS demo's (#4068): "Start scanning", "See an
 * example", "Open scan", and the scans kept on this phone (#4627).
 *
 * The **sample** is a real ARCore session bundled with the app — the camera's path, its photos in
 * their frustums, the planes it found textured with the room, the coloured point cloud and the
 * models placed on them — orbitable, with a filmstrip of the recorded frames that scrubs it. It
 * needs no ARCore, so it works on every device and on the emulator (#2754).
 *
 * **Start scanning** captures a space live with ARCore — the path, the coloured points, the
 * planes and a photo every few steps, growing in a 3D card while you walk. Pause freezes it;
 * Finish opens a review that Keep scanning leaves for the same capture; Save scan writes it on
 * the phone, as the same three files the sample ships in the iOS demo's session layout, and
 * opens it in the same replay; it stays under "Your scans" until deleted, and shares as a
 * `.svscan` scan file the iOS demo opens too. Nothing leaves the phone unless you share it.
 *
 * **Place** (#4075), at the end of a replay of your own room or from a session's menu, stands
 * that room on a table as a miniature — a dollhouse — through [RoomDollhouseScreen]. With
 * [startInDollhouse] the demo opens there directly: the `ar-splat-room` card of the home screen.
 *
 * Advanced, from the sheet: the live screen also streams the session to the Rerun viewer on a
 * computer. The bridge auto-connects to the Python recorder at `127.0.0.1:9876` — for USB,
 * `adb reverse tcp:9876 tcp:9876` — and "Save & Share recording" flushes a `.rrd` file.
 */
@Composable
fun ARRerunDemo(onBack: () -> Unit, startInDollhouse: Boolean = false) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    // The replay's shaders compile while the landing is read, not behind its loading cover.
    LaunchedEffect(engine, materialLoader) { warmUpReplay(engine, materialLoader) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Replay a recorded ARCore dataset when the device-QA harness deep-links this demo
    // with `--es ar_playback_file <path>` (#1576): that harness drives the live path.
    val arPlaybackDataset = rememberArPlaybackDataset()

    val qaState = remember { DemoSettings.qaDemoState?.takeIf { qaStateOverridesAllowed() } }
    val qaReplay = remember { RerunReplayQaState.of(qaState) }
    var screen by remember {
        mutableStateOf(
            when {
                startInDollhouse || qaState in DOLLHOUSE_QA_STATES -> RerunScreen.Dollhouse
                qaState in MODEL_QA_STATES -> RerunScreen.Model
                qaReplay != null -> RerunScreen.Replay
                arPlaybackDataset != null || qaState in LIVE_QA_STATES -> RerunScreen.Live
                else -> RerunScreen.Landing
            },
        )
    }
    var mode by remember { mutableStateOf(qaReplay?.mode ?: RerunMode.Scene) }

    // The sample lives here, above every screen: the landing turns it, the replay plays it, and
    // the QA record take is fed from it.
    val sample by produceState<RerunReplayMedia?>(null) {
        value = runCatching { loadRerunReplay(context) }.getOrNull()
    }

    // The sessions kept on this phone, re-read whenever one is saved, opened from a file or deleted.
    val store = remember { rerunSessionStore(context.applicationContext) }
    var sessionsVersion by remember { mutableIntStateOf(0) }
    val sessions by produceState<List<LandingSession>?>(null, sessionsVersion) { value = store.landingSessions() }
    // What the landing says about the last file opened: why it did not open, or that it is opening.
    var notice by remember { mutableStateOf<String?>(null) }
    var openingFile by remember { mutableStateOf(false) }

    // What the replay shows: the sample, or one of your scans — the same view either way.
    var showingScan by remember { mutableStateOf(false) }
    var scanMedia by remember { mutableStateOf<RerunReplayMedia?>(null) }
    // The replay is titled after the session it shows, as the card that opened it is.
    var scanTitle by remember { mutableStateOf(ScanCopy.REPLAY_TITLE) }
    // The scan's own files, for the export sheet; the sample's are read from the assets there.
    var scanPack by remember { mutableStateOf<RerunCapturePack?>(null) }
    // The kept session the replay shows, when it was opened from the list: Place stands that one.
    var scanId by remember { mutableStateOf<String?>(null) }
    var exporting by remember {
        mutableStateOf(qaReplay == RerunReplayQaState.ReplayExport || qaState == QA_STATE_MODEL_EXPORT)
    }
    var opening by remember { mutableStateOf<Job?>(null) }
    // Bumped on every open, so reopening the same replay frames and plays it afresh.
    var openCount by remember { mutableIntStateOf(0) }
    val media = if (showingScan) scanMedia else sample
    // The scan's result (RerunModelUi.kt): your room as one model, kept with the scan. Picked once
    // when a scan opens, so the build is not restarted by a recomposition. The sample has none.
    var modelSource by remember {
        mutableStateOf<RerunModelSource?>(
            when (qaState) {
                QA_STATE_MODEL_SYNTHETIC, QA_STATE_MODEL_EXPORT -> RerunModelSource.Synthetic()
                QA_STATE_MODEL_POINTS -> RerunModelSource.Synthetic(meshFails = true)
                else -> null
            },
        )
    }
    // The model on screen, once built or read back: what the export sheet writes first.
    var resultModel by remember { mutableStateOf<RerunScanModel?>(null) }
    // Whether the scan shown has a result screen: the replay's Back returns to it.
    var hasResult by remember { mutableStateOf(modelSource != null) }

    val replaySession = remember { ArDebugSession().apply { loops = true } }
    // The band of the stage the chrome leaves free, per orientation: the room is fitted into it.
    val stageBand = LocalConfiguration.current.let { OrbitBand.stage(it.screenWidthDp.toFloat() / it.screenHeightDp) }
    // QA captures hold still: no intro fly-in, no idle sway. Each opening is framed afresh.
    val replayOrbit = remember(media, openCount) { ArDebugOrbitCamera(drift = qaState == null, band = stageBand) }
    LaunchedEffect(media, openCount) {
        val replay = media ?: return@LaunchedEffect
        replaySession.trace = replay.trace
        // Held until the stage is on screen: the sample then plays from its first frame; your own
        // scan opens whole, on its last frame, ready to be turned with a finger.
        val at = if (showingScan) 1f else qaReplay?.pauseAt ?: 0f
        replaySession.scrubTo(replay.trace.duration * at)
    }
    // The replay opens when its first frames are on screen, not when its files are read: the
    // cover, the chrome and the playback all wait for the stage, so nothing shows up empty.
    var revealed by remember(media, openCount) { mutableStateOf(false) }
    LaunchedEffect(revealed, media) {
        val held = qaReplay?.pauseAt != null || qaState == QA_STATE_MODEL_SYNTHETIC
        if (revealed && !showingScan && !held) replaySession.playFromStart()
    }
    LaunchedEffect(mode, replayOrbit) { replayOrbit.overhead = mode == RerunMode.Map }

    // The dollhouse (#4075): one of your sessions, stood on a table in AR. [dollhouseRequest] is
    // the session asked for (null: the newest), [dollhouseMedia] the one read, and Back returns to
    // [dollhouseReturn] — or leaves the demo when it opened straight on the dollhouse.
    val dollhouseReturn = if (startInDollhouse) null else RerunScreen.Landing
    var dollhouseRequest by remember { mutableStateOf<String?>(null) }
    var dollhouseMedia by remember { mutableStateOf<RerunReplayMedia?>(null) }
    var dollhouseTitle by remember { mutableStateOf(ScanCopy.REPLAY_TITLE) }
    var dollhouseFailed by remember { mutableStateOf(false) }
    // Record, opened from the dollhouse's empty state, comes back to it with the new room.
    var recordingForDollhouse by remember { mutableStateOf(false) }
    val dollhouseSession = if (qaState == QA_STATE_DOLLHOUSE_EMPTY) {
        null
    } else {
        sessions?.let { kept -> RoomDollhouse.pickSession(kept.map { it.info }, dollhouseRequest) }
    }
    // The kept session on the table, said by name and date. A recording handed over already read
    // (just recorded, or from the replay) without its id is found by its title.
    val dollhouseShown = when {
        dollhouseRequest != null || dollhouseMedia == null -> dollhouseSession
        else -> sessions?.firstOrNull { it.info.title == dollhouseTitle }?.info
    }
    LaunchedEffect(screen, dollhouseSession?.id) {
        if (screen != RerunScreen.Dollhouse || dollhouseMedia != null || dollhouseFailed) return@LaunchedEffect
        val session = dollhouseSession ?: return@LaunchedEffect
        dollhouseTitle = session.title
        val capture = withContext(Dispatchers.IO) { store.capture(session.id) }
        val opened = capture?.let { runCatching { loadRerunSession(it) }.getOrNull() }
        // Another recording picked meanwhile: this one no longer stands.
        if (dollhouseRequest != null && dollhouseRequest != session.id) return@LaunchedEffect
        if (opened == null) dollhouseFailed = true else dollhouseMedia = opened
    }
    val openDollhouse = { id: String?, title: String, media: RerunReplayMedia? ->
        opening?.cancel()
        opening = null
        dollhouseRequest = id
        dollhouseTitle = title
        dollhouseMedia = media
        dollhouseFailed = false
        screen = RerunScreen.Dollhouse
    }

    val toLanding = {
        opening?.cancel()
        opening = null
        screen = RerunScreen.Landing
    }
    val leaveDollhouse = { dollhouseReturn?.let { screen = it } ?: onBack() }
    val leaveLive = {
        if (recordingForDollhouse) {
            recordingForDollhouse = false
            screen = RerunScreen.Dollhouse
        } else {
            toLanding()
        }
    }
    BackHandler(enabled = screen != RerunScreen.Landing) {
        when (screen) {
            RerunScreen.Dollhouse -> leaveDollhouse()
            RerunScreen.Live -> leaveLive()
            RerunScreen.Replay -> if (showingScan && hasResult) screen = RerunScreen.Model else toLanding()
            else -> toLanding()
        }
    }

    val openSample = {
        showingScan = false
        mode = RerunMode.Scene
        openCount++
        screen = RerunScreen.Replay
    }
    // A scan, just recorded or reopened: its result when it has one (a model kept with it, or the
    // depth to build one), else its replay. [kept]: the store already holds its model.
    val openResult = { media: RerunReplayMedia, title: String, pack: RerunCapturePack, id: String?, kept: Boolean ->
        scanId = id
        scanMedia = media
        scanTitle = title
        scanPack = pack
        showingScan = true
        mode = RerunMode.Scene
        openCount++
        // A kept model is read back as it is: no source, so nothing is built again.
        modelSource = if (kept) null else RerunModelSource.of(pack, media)
        hasResult = kept || modelSource != null
        resultModel = null
        screen = if (hasResult) RerunScreen.Model else RerunScreen.Replay
    }
    val openStored = { id: String, title: String ->
        scanMedia = null
        scanTitle = title
        scanPack = null
        scanId = id
        showingScan = true
        resultModel = null
        mode = RerunMode.Scene
        opening?.cancel()
        opening = scope.launch {
            // A scan with its model opens on it at once; the recording is read meanwhile. Without
            // one — or with one that no longer reads — the replay's cover says "Opening your scan…"
            // while its files are read, then the model is built again from them.
            val keptModel = withContext(Dispatchers.IO) { store.model(id) }
            val kept = keptModel != null
            resultModel = keptModel
            modelSource = null
            hasResult = kept
            screen = if (kept) RerunScreen.Model else RerunScreen.Replay
            val capture = withContext(Dispatchers.IO) { store.capture(id) }
            val opened = capture?.let { runCatching { loadRerunSession(it) }.getOrNull() }
            when {
                capture != null && opened != null -> if (screen == RerunScreen.Model && kept) {
                    // Already on the model: the recording only completes the scan behind it.
                    scanMedia = opened
                    scanPack = capture
                    openCount++
                } else {
                    openResult(opened, title, capture, id, kept)
                }
                kept -> Unit
                else -> {
                    notice = ScanCopy.OPEN_FAILED
                    screen = RerunScreen.Landing
                }
            }
        }
    }
    // A file opened here ("Open file") or handed over by another app: kept as a session, then
    // replayed — or, when it is not one this app reads, the landing says why.
    val importFile = { uri: Uri ->
        notice = null
        openingFile = true
        scope.launch {
            val result = importSession(context, store, uri)
            openingFile = false
            sessionsVersion++
            result
                .onSuccess { openStored(it.id, it.title) }
                .onFailure { failure ->
                    notice = (failure as? RerunImportFailure)?.let { ScanCopy.importFailure(it, it.message.orEmpty()) }
                        ?: ScanCopy.OPEN_FAILED
                }
        }
        Unit
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(importFile)
    }
    val inbox = DemoSettings.rerunInbox
    LaunchedEffect(inbox) {
        val uri = inbox ?: return@LaunchedEffect
        DemoSettings.rerunInbox = null
        opening?.cancel()
        screen = RerunScreen.Landing
        importFile(uri)
    }

    when (screen) {
        RerunScreen.Landing -> RerunLandingScreen(
            onBack = onBack,
            state = RerunLandingState(sessions = sessions, notice = notice, openingFile = openingFile),
            actions = RerunLandingActions(
                onRecord = {
                    notice = null
                    screen = RerunScreen.Live
                },
                onWatchSample = {
                    notice = null
                    openSample()
                },
                onOpenFile = { pickFile.launch(arrayOf("*/*")) },
                onOpen = {
                    notice = null
                    openStored(it.id, it.info.title)
                },
                onViewInAr = { session ->
                    notice = null
                    openDollhouse(session.id, session.info.title, null)
                },
                onDelete = { session ->
                    scope.launch {
                        withContext(Dispatchers.IO) { store.delete(session.id) }
                        sessionsVersion++
                    }
                },
                onDismissNotice = { notice = null },
            ),
        )
        // Capture is the camera, and so is ARCore's MP4 playback as the library runs it:
        // `ARCore.checkPermissionAndInstall` holds every session back without the permission.
        // The landing page, the replays and the shared files around it never ask (#4139).
        // A QA state stages this screen without a session, so it stages it without the gate.
        RerunScreen.Live -> ArDemoPermissionGate(
            title = stringResource(R.string.demo_ar_rerun_title),
            onBack = leaveLive,
            enabled = qaState == null,
        ) {
            RerunLiveScreen(
                onBack = leaveLive,
                onScanned = { scan, title, pack, id ->
                    sessionsVersion++
                    if (recordingForDollhouse) {
                        recordingForDollhouse = false
                        openDollhouse(null, title, scan)
                    } else {
                        // Save scan lands on the model, built here and kept with the scan.
                        openResult(scan, title, pack, id, false)
                    }
                },
                store = store,
                sample = sample,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                arPlaybackDataset = arPlaybackDataset,
                qaState = qaState,
            )
        }
        RerunScreen.Replay -> RerunReplayScreen(
            onBack = { if (showingScan && hasResult) screen = RerunScreen.Model else toLanding() },
            mode = mode,
            onMode = { mode = it },
            media = media,
            isScan = showingScan,
            scanTitle = scanTitle,
            session = replaySession,
            orbit = replayOrbit,
            stageBand = stageBand,
            revealed = revealed,
            onRevealed = { revealed = true },
            onExport = { exporting = true },
            // Your own room only: the sample is not a room of yours to stand on a table.
            onViewInAr = scanMedia?.takeIf { showingScan }?.let { scan ->
                { openDollhouse(scanId, scanTitle, scan) }
            },
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
        )
        RerunScreen.Dollhouse -> RoomDollhouseScreen(
            onBack = leaveDollhouse,
            title = dollhouseTitle,
            media = dollhouseMedia,
            session = dollhouseShown,
            sessions = sessions.takeUnless { qaState == QA_STATE_DOLLHOUSE_EMPTY },
            onPickSession = { picked -> openDollhouse(picked.id, picked.title, null) },
            sessionsKnown = sessions != null || dollhouseMedia != null,
            hasSession = dollhouseMedia != null || dollhouseSession != null,
            openFailed = dollhouseFailed,
            onRecord = {
                recordingForDollhouse = true
                screen = RerunScreen.Live
            },
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            arPlaybackDataset = arPlaybackDataset,
            startIn3d = qaState == QA_STATE_DOLLHOUSE_3D,
        )
        RerunScreen.Model -> RerunModelScreen(
            source = modelSource,
            title = if (showingScan) scanTitle else ScanCopy.REPLAY_TITLE,
            store = store.takeIf { scanId != null },
            id = scanId,
            onModel = { resultModel = it },
            onBack = toLanding,
            onRename = scanId?.let { id ->
                { name: String ->
                    scanTitle = name.trim().ifEmpty { scanTitle }
                    scope.launch {
                        withContext(Dispatchers.IO) { runCatching { store.rename(id, name) } }
                        sessionsVersion++
                    }
                }
            },
            // The recording behind the model, once it is read: a scan reopened shows its model first.
            onReplay = scanMedia?.takeIf { showingScan }?.let { { screen = RerunScreen.Replay } },
            onViewInAr = scanMedia?.takeIf { showingScan }?.let { scan ->
                { openDollhouse(scanId, scanTitle, scan) }
            },
            onExport = { exporting = true },
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            hintStays = qaState != null,
            opened = resultModel,
        )
    }
    val exportable = when (screen) {
        RerunScreen.Replay -> media != null
        RerunScreen.Model -> true
        else -> false
    }
    if (exporting && exportable) {
        val onModel = screen == RerunScreen.Model
        val source = remember(showingScan, scanTitle, scanPack, onModel, resultModel) {
            val pack = scanPack
            val model = resultModel
            when {
                showingScan -> RerunExportSource(
                    title = scanTitle,
                    capture = pack?.let { { it } },
                    model = model?.let { { it } },
                )
                // QA: the synthetic room's model, and no recording to go with it.
                onModel -> RerunExportSource(title = ScanCopy.REPLAY_TITLE, model = model?.let { { it } })
                else -> {
                    val appContext = context.applicationContext
                    RerunExportSource(ScanCopy.SAMPLE_TITLE, capture = { sampleCapturePack(appContext) })
                }
            }
        }
        RerunExportSheet(source = source, onDismiss = { exporting = false })
    }
}

/**
 * The demo's screens: the landing, the live AR session (Record), the replay, and the dollhouse —
 * a session stood on a table in AR (#4075).
 */
private enum class RerunScreen { Landing, Live, Replay, Dollhouse, Model }

/** The replay's three views. */
private enum class RerunMode { Scene, Map, Camera }

/**
 * The landing, laid out as the iOS demo's: "3D Scan", "Start scanning", the example and "Open
 * scan", then "Your scans". A themed stage like the replay it opens (#4080): the
 * light stage in light theme, the dark one in dark theme.
 */
@Composable
private fun RerunLandingScreen(onBack: () -> Unit, state: RerunLandingState, actions: RerunLandingActions) {
    DemoScaffold(
        title = stringResource(R.string.demo_ar_rerun_title),
        onBack = onBack,
        controls = { RerunSheet() },
        dock = emptyList(),
        themedStage = true,
    ) {
        RerunLanding(state, actions)
    }
}

/**
 * The replay: the 3D view (orbit, or the overhead map) or the camera's frames, and one thin
 * timeline bar over the dock (#4379). A themed stage (#4080): it follows the app theme, where the
 * live camera screen keeps the media chrome. A tap on the stage hides the header, the bar and
 * the dock; another brings them back.
 *
 * What is read once lives in the settings sheet: the layers and their figures, and — for a scan
 * that can be meshed — Points | Surface (#4306), the surface being drawn by this same 3D view,
 * under the same camera, instead of a second screen.
 */
@Composable
@Suppress("LongParameterList") // the demo's shared engine and replay state, handed down once
private fun RerunReplayScreen(
    onBack: () -> Unit,
    mode: RerunMode,
    onMode: (RerunMode) -> Unit,
    media: RerunReplayMedia?,
    isScan: Boolean,
    scanTitle: String,
    revealed: Boolean,
    onRevealed: () -> Unit,
    session: ArDebugSession,
    orbit: ArDebugOrbitCamera,
    stageBand: OrbitBand,
    onExport: () -> Unit,
    onViewInAr: (() -> Unit)?,
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
) {
    val thumbnails = remember(media) { media?.thumbnails?.mapValues { it.value.asImageBitmap() }.orEmpty() }
    val title = if (isScan) scanTitle else ScanCopy.SAMPLE_TITLE
    // The stage the chrome really leaves, measured on screen: the room is fitted between the
    // header above and the timeline bar below, whatever the phone or the font scale. A phone on
    // its side has no height to give a row under the room: the bar stands in the top corner,
    // under the header's line, and the room keeps the middle of the window.
    val compact = compactStage()
    var stage by remember { mutableStateOf(Rect.Zero) }
    // Kept while a tap has the chrome hidden: the room does not move when the bar leaves.
    var timeline by remember(compact) { mutableStateOf<Rect?>(null) }
    var headerBottom by remember(compact) { mutableFloatStateOf(Float.NaN) }
    // On its side: where the dock starts, under the room.
    var pillTop by remember(compact) { mutableFloatStateOf(Float.NaN) }
    val statusBottom = WindowInsets.safeDrawing.getTop(LocalDensity.current).toFloat()
    // The room's dimensions are written under its floor, outside the box the band fits: they
    // keep this much air over the dock.
    val pillClearance = with(LocalDensity.current) { Space.lg.toPx() }
    val measured = when {
        compact -> sideBand(stage, timeline, statusBottom, pillTop - pillClearance)
        else -> stackedBand(stage, headerBottom, timeline)
    }
    val band = measured ?: stageBand
    SideEffect { orbit.band = band }
    // What the recording covers at the replay's time (#4631): the map card in the top corner, in
    // the 3D and Map views, which feed it.
    val coverage = remember(media) { ScanCoverageState() }
    val showsCoverage = media != null && mode != RerunMode.Camera
    // The camera frames are pictures, ready with the files; the 3D view says when it has drawn.
    val ready = media != null && (revealed || mode == RerunMode.Camera)
    LaunchedEffect(ready) { if (ready) onRevealed() }
    val readyState = rememberUpdatedState(ready)
    val filmstripIn = rememberReveal(revealed, delayMillis = REVEAL_STAGGER_MS)
    // The 3D view runs the session's clock while it draws. Over the camera's frames there is
    // none on screen (the corner picture-in-picture that ran it is gone, #4379): the screen does.
    // It counts for the view too: the sheet's layer rows and the timeline read those figures.
    if (media != null && mode == RerunMode.Camera) {
        LaunchedEffect(session, media) {
            // A finished recording stands on the floor of the whole recording, as in the 3D view.
            val whole = if (media.growing) null else session.trace.let { it.frameAt(it.duration) }
            var last = withFrameNanos { it }
            var countedAt = last - STATS_INTERVAL_NS
            while (true) {
                val now = withFrameNanos { it }
                session.tick((now - last) / NANOS_PER_SECOND)
                last = now
                if (now - countedAt >= STATS_INTERVAL_NS) {
                    countedAt = now
                    val frame = session.trace.frameAt(session.time)
                    session.count(frame, media.pointCountAt(frame.time), stageFloorY(whole ?: frame))
                }
            }
        }
    }
    // The session on screen as open files: .rrd, .glb and .ply, written on the phone.
    val export = DockItem(
        icon = Icons.Rounded.IosShare,
        label = RerunExportFormat.DOCK_LABEL,
        caption = RerunExportFormat.DOCK_CAPTION,
        onClick = onExport,
        enabled = media != null,
    )
    DemoScaffold(
        title = stringResource(R.string.demo_ar_rerun_title),
        onBack = onBack,
        controls = {
            // What the 3D view draws, layer by layer, and its figures.
            RerunSheet { RerunLayersSection(session) }
        },
        firstFrameRendered = readyState,
        loadingLabel = if (isScan) ScanCopy.LOADING else RERUN_REPLAY_LOADING,
        themedStage = true,
        // The room has the screen (#4379): a tap on the stage hides the header, the timeline and
        // the dock, and another brings them back.
        chromeToggleOnTap = true,
        overlaysFollowChrome = true,
        // 3D Scan shows no mode switch on any of its screens (#4397).
        modeSwitch = null,
        topOverlay = {
            if (media != null && compact) {
                // On its side: what is scanned in the start corner, the timeline in the end one,
                // the room centred between them (the band keeps clear of the wider of the two).
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    if (showsCoverage) {
                        ScanCoverageMap(
                            state = coverage,
                            modifier = Modifier
                                .padding(start = Space.md)
                                .reveal(filmstripIn, rise = -Space.md),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    RerunTimelineBar(
                        media = media,
                        thumbnails = thumbnails,
                        session = session,
                        modifier = Modifier
                            .padding(end = Space.md)
                            .width(SceneViewTokens.DebugView.compactCardWidth)
                            // Measured outside the reveal: where the bar rests, not where it rises from.
                            .onGloballyPositioned { timeline = it.boundsInRoot() }
                            .reveal(filmstripIn, rise = -Space.md),
                    )
                }
            } else if (showsCoverage) {
                // What is scanned and what is not, in the corner under the header: the room is
                // fitted below the card, as below the header, so the card never stands on it.
                ScanCoverageMap(
                    state = coverage,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = Space.md)
                        // Measured outside the reveal: where the card rests, not where it rises from.
                        .onGloballyPositioned { headerBottom = it.boundsInRoot().bottom }
                        .reveal(filmstripIn, rise = -Space.md),
                )
            } else {
                // Nothing to draw: this marks where the header ends over the room — by its
                // position: an empty box has no bounds.
                Spacer(Modifier.fillMaxWidth().onGloballyPositioned { headerBottom = it.positionInRoot().y })
            }
        },
        bottomOverlay = {
            if (media != null && compact) {
                // Nothing to draw: the timeline stands in the corner. This marks where the dock
                // starts under the room.
                Spacer(Modifier.fillMaxWidth().onGloballyPositioned { pillTop = it.positionInRoot().y })
            } else if (media != null) {
                RerunTimelineBar(
                    media = media,
                    thumbnails = thumbnails,
                    session = session,
                    modifier = Modifier
                        // The bar goes with the dock: one small gutter above it, not a card
                        // stacked over it.
                        .offset(y = (dockGap - Space.sm).coerceAtLeast(0.dp))
                        .padding(horizontal = Space.md)
                        .widthIn(max = ArOverlay.maxWidth)
                        .fillMaxWidth()
                        .onGloballyPositioned { timeline = it.boundsInRoot() }
                        .reveal(filmstripIn, rise = Space.lg),
                )
            }
        },
        dock = listOfNotNull(
            DockItem(
                icon = Icons.Rounded._3dRotation,
                label = "3D view",
                caption = "3D",
                onClick = { if (mode == RerunMode.Scene) orbit.recenter() else onMode(RerunMode.Scene) },
                selected = mode == RerunMode.Scene,
            ),
            DockItem(
                icon = Icons.Rounded.Map,
                label = "Map view",
                caption = "Map",
                onClick = { onMode(RerunMode.Map) },
                selected = mode == RerunMode.Map,
            ),
            DockItem(
                icon = Icons.Rounded.Movie,
                label = "Camera frames",
                caption = "Camera",
                onClick = { onMode(RerunMode.Camera) },
                selected = mode == RerunMode.Camera,
            ),
            // With Place as the accent, Export stays one tap away in the dock.
            export.takeIf { onViewInAr != null },
        ),
        // Your own room stands on a table in AR (#4075); the sample offers its files instead.
        dockAccent = onViewInAr?.let {
            DockItem(
                icon = Icons.Rounded.ViewInAr,
                label = stringResource(R.string.scan_place),
                onClick = it,
                enabled = media != null,
            )
        } ?: export,
    ) {
        when {
            media == null -> Unit // the scaffold's cover says it is loading
            mode == RerunMode.Camera -> RerunCameraView(media, thumbnails, session, Modifier.fillMaxSize())
            else -> ArDebugSceneView(
                session = session,
                orbit = orbit,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                modifier = Modifier.fillMaxSize().onGloballyPositioned { stage = it.boundsInRoot() },
                replay = media,
                onShown = onRevealed,
                observe = coverage::observe,
            )
        }
    }
}

/**
 * A phone on its side: too short for the stacked cards, and wide enough for the two side cards to
 * leave the room a stage between them — by the rule the band itself is measured with
 * ([OrbitBand.halfWidthBeside]). A window that is short and narrow (split screen) keeps the
 * stacked layout: beside the room, the cards would stand on it.
 */
@Composable
private fun compactStage(): Boolean {
    if (LocalConfiguration.current.screenHeightDp.dp >= SceneViewTokens.DebugView.compactStageHeight) return false
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    // The room stays centred: the side a cutout pushes further in decides for both.
    val inset = maxOf(insets.getLeft(density, direction), insets.getRight(density, direction))
    val card = with(density) { (Space.md + SceneViewTokens.DebugView.compactCardWidth).toPx() }
    val width = LocalWindowInfo.current.containerSize.width.toFloat()
    return OrbitBand.halfWidthBeside(cardEnd = inset + card, viewWidth = width) != null
}

/**
 * The band the replay's chrome leaves the room on [stage] when the phone is upright, measured on
 * screen: from [headerBottom], where the header ends, down to the timeline bar. `null` until both
 * are laid out.
 */
private fun stackedBand(stage: Rect, headerBottom: Float, timeline: Rect?): OrbitBand? {
    if (timeline == null) return null
    return OrbitBand.between(headerBottom - stage.top, timeline.top - stage.top, stage.height)
}

/**
 * The same band for a phone on its side: the timeline bar stands in a top corner, so the room —
 * which stays centred — keeps clear of it on both sides, from [statusBottom] under the status bar
 * down to [pillTop], where the dock starts. `null` until the bar is laid out.
 */
private fun sideBand(stage: Rect, timeline: Rect?, statusBottom: Float, pillTop: Float): OrbitBand? {
    if (timeline == null) return null
    // How far in the bar reaches from the edge it stands against, whichever that is (RTL).
    val reach = minOf(stage.right - timeline.left, timeline.right - stage.left)
    return OrbitBand.betweenSides(
        startCardEnd = reach,
        endCardStart = stage.width - reach,
        top = (statusBottom - stage.top).coerceAtLeast(0f),
        bottom = pillTop - stage.top,
        viewWidth = stage.width,
        viewHeight = stage.height,
    )
}

/**
 * The settings sheet: [head], what the screen it is opened from keeps off its stage (#4379) — the
 * replay's layers and figures, a scan's counts — then what the demo is and, advanced, how to
 * stream a live session to a computer, with [stream], that stream's status, once it is connected.
 */
@Composable
private fun RerunSheet(head: (@Composable () -> Unit)? = null) {
    if (head != null) {
        head()
        Spacer(Modifier.size(Space.md))
    }
    Text(
        text = RERUN_REPLAY_INTRO,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (BuildConfig.DEBUG) {
        RerunSetupSection()
        // Developer-only debug toggle — visible when QA mode is on. Lets QA force-emit each
        // TrackingFailureReason so the actionable-message overlay can be validated without staging a
        // real failure. See io.github.sceneview.demo.common.ForcedTrackingFailure / #1881.
        ForceTrackingFailureMenu()
    }
}

/**
 * Live AR — the capture: the camera, with taps placing the shiba on planes, recorded into the
 * in-app 3D view (#3950). The scan starts by itself once ARCore has found the space; Pause,
 * Finish and its review, Keep scanning and Save scan follow [ScanPhase] (#4627), and a saved
 * scan is handed to [onScanned], opened, with its id in the store, for its result (#4597).
 * Back during a capture asks first.
 *
 * #3831: the screen used to open on a banner about a "recording service" and "Settings" for
 * every Play Store user without a computer attached. Streaming to a computer is now an advanced
 * option: its steps live in the settings sheet, and its status card shows only once connected.
 */
@Composable
@Suppress("LongParameterList", "LongMethod") // the live AR screen, moved as-is behind the replay
private fun RerunLiveScreen(
    onBack: () -> Unit,
    onScanned: (RerunReplayMedia, String, RerunCapturePack, String?) -> Unit,
    store: RerunSessionStore,
    sample: RerunReplayMedia?,
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    arPlaybackDataset: File?,
    qaState: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // QA only (`--es qa_state connected|saved`): draw the connected status, or the saved
    // dialog, on the emulator, which can reach neither ARCore nor a computer (#2754).
    val qaConnected = qaState == QA_STATE_CONNECTED
    // QA only (`--es qa_state pip|pip-stream|3d|3d-scrub|3d-stream`, #3950): feed the in-app 3D
    // view from a recorded session, since the emulator cannot track. Never shown to a user.
    val qaDebug = remember { ArDebugQaState.of(qaState) }

    // The in-app 3D debug view (#3950): what ARCore understood of the room, drawn by a second
    // SceneView from a free camera. The picture-in-picture over the camera opens it full-screen.
    val debugSession = remember { ArDebugSession() }
    val debugRecorder = remember { ArDebugRecorder() }
    val debugOrbit = remember { ArDebugOrbitCamera(drift = qaState == null) }
    val pipOrbit = remember { ArDebugOrbitCamera(drift = qaState == null) }
    var debugFullScreen by remember { mutableStateOf(qaDebug?.fullScreen == true) }

    LaunchedEffect(qaDebug) {
        val fixture = qaDebug ?: return@LaunchedEffect
        val events = withContext(Dispatchers.IO) {
            context.assets.open(AR_DEBUG_FIXTURE).bufferedReader().useLines { parseArDebugLog(it) }
        }
        if (fixture.streams) {
            // The trail grows as the recorded walk plays, the way a live session fills in.
            val player = ArDebugLogPlayer(events)
            debugSession.trace = player.trace
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else (now - last) / 1e9f
                    last = now
                    if (player.advance(dt)) debugSession.trace = player.trace
                }
            }
        } else {
            debugSession.trace = ArDebugTrace.of(events)
            if (fixture.scrubbed) debugSession.scrubTo(debugSession.trace.duration * QA_SCRUB_FRACTION)
        }
    }

    // Capture mode: the scan in progress, and its 3D card's camera. A scan records into the
    // debug session's trace, so the 3D view and the scan are the same data.
    var scan by remember { mutableStateOf<ScanCapture?>(null) }
    // #4627: where the capture stands. Pause and Finish freeze it; Finish opens a review that
    // Keep scanning leaves for the same capture. Only Save scan builds it — `finish()` is final.
    var phase by remember { mutableStateOf(ScanPhase.Scanning) }
    // The built scan, held across a failed write so Try again writes the same one.
    var builtPack by remember { mutableStateOf<RerunCapturePack?>(null) }
    var builtTitle by remember { mutableStateOf("") }
    var leaveAsked by remember { mutableStateOf(false) }
    // Discard on the "Couldn't save" card: the scan was never written, so it asks first.
    var discardAsked by remember { mutableStateOf(false) }
    // Rerun v2: whether this phone runs ARCore's raw depth, set when the session is configured.
    // With it a scan also fuses a dense surfel map (tier "depth"); without it, the sparse v1 scan.
    var rawDepth by remember { mutableStateOf(false) }
    // QA only (`--es qa_state record`): the capture screen mid-scan, fed by the sample room's
    // log and photos, since the emulator cannot track. Its Save scan saves that take like a real
    // scan — the same builder, bake and file — and opens it. `record-tracking-lost` holds it on
    // a lost track; `record-save-failed` makes its write fail.
    var qaScan by remember { mutableStateOf<RerunReplayMedia?>(null) }
    val qaRecord = qaState in RECORD_QA_STATES
    val qaTrackingLost = qaState == QA_STATE_RECORD_TRACKING_LOST
    val qaSaveFails = qaState == QA_STATE_RECORD_SAVE_FAILED
    // #4629: `record-long` feeds the dense map a whole flat; `-fixed` runs it with the old cap.
    var qaLong by remember { mutableStateOf<QaLongScan?>(null) }
    LaunchedEffect(qaRecord, sample) {
        val source = sample?.takeIf { qaRecord } ?: return@LaunchedEffect
        val events = withContext(Dispatchers.IO) {
            context.assets.open(RerunReplayAssets.LOG).bufferedReader().useLines { parseArDebugLog(it) }
        }
        val player = ArDebugLogPlayer(events, loopPauseSeconds = QA_RECORD_NEVER_LOOP_S)
        player.trace.keyframeSpacing = ReplayGeometry.KEYFRAME_SPACING_M
        player.trace.journal = ArrayList()
        debugSession.trace = player.trace
        val long = qaState.takeIf { it in LONG_QA_STATES }
            ?.let { QaLongScan.over(source.trace, fixed = it == QA_STATE_RECORD_LONG_FIXED) }
        qaLong = long
        qaScan = RerunReplayMedia(
            player.trace, source.manifest, emptyMap(), source.thumbnails, ByteArray(0), growing = true,
            liveDense = long?.let { { it.dense.live } },
        )
        long?.let { launch { it.run { phase.integrates } } }
        var last = 0L
        var played = 0f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                // The tracking-loss state scans a few seconds first, so it loses a real capture.
                val tracking = !qaTrackingLost || played < QA_TRACKING_LOST_AFTER_S
                if (phase.integrates && tracking) played += dt.also { player.advance(it) }
            }
        }
    }
    val scanMedia = scan?.live ?: qaScan
    val recording = scanMedia != null
    // #4628: the live view follows the phone heading-up, like a navigation app. A drag or a
    // pinch takes it over and shows Recenter, which hands it back to the phone.
    var scanFollowing by remember(scanMedia) { mutableStateOf(true) }
    val scanFollow = remember(scanMedia) { ScanFollow() }
    // What the capture has covered (#4631): the live map and the coach read it, frame by frame.
    val scanCoverage = remember(scanMedia) { ScanCoverageState() }
    val scanOrbit = remember(scanMedia) {
        ArDebugOrbitCamera(
            drift = false,
            band = ScanFollow.BAND,
            approachRate = ScanFollow.APPROACH_RATE,
            maxTurnDegreesPerSecond = ScanFollow.MAX_TURN_DEGREES_PER_SECOND,
            degreesPerViewWidth = ScanFollow.DRAG_DEGREES_PER_VIEW_WIDTH,
            elevationRange = ScanFollow.DRAG_ELEVATION_RANGE,
            maxCoastDegrees = ScanFollow.MAX_COAST_DEGREES,
        )
            .apply { onFollowingChange = { scanFollowing = it } }
    }

    var isTracking by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }

    // #3341: non-null once ARCore has ruled this device out. The flag the scanning
    // banner waits on never flips then, so that banner has to read the verdict or
    // it promises a scan under the SDK's "AR unavailable" card, forever.
    var arCoreAvailability by remember { mutableStateOf<ARCoreAvailability?>(null) }
    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var eventsPerSec by remember { mutableStateOf(0f) }
    var latestFrame by remember { mutableStateOf<Frame?>(null) }
    val anchors = remember { mutableStateListOf<Anchor>() }

    var sharing by remember { mutableStateOf(false) }
    var shareResult by remember {
        mutableStateOf(if (qaState == QA_STATE_SAVED) QA_SHARE_RESULT else null)
    }

    // Bridge auto-connects on first composition, auto-disconnects on
    // dispose — no Connect/Disconnect UI to confuse first-time users who
    // came in from the QR code on /rerun/.
    val bridge = rememberRerunBridge(rateHz = 10, enabled = true)
    // Read the bridge's actually-shipped count, not a local frame counter — a
    // local counter ticks even when the recorder is unreachable, which would
    // mislead the user into thinking events are being sent.
    val isConnected = bridge.isConnected || qaConnected
    val eventCount = if (qaConnected) QA_EVENTS_SENT else bridge.eventsSent

    // Sample events/sec once per second. Reads the bridge inside the loop: the previous
    // version read a value captured at first composition, so the rate never left zero.
    LaunchedEffect(bridge) {
        var lastSampleCount = bridge.eventsSent
        while (true) {
            delay(1000)
            val current = bridge.eventsSent
            eventsPerSec = (current - lastSampleCount).toFloat()
            lastSampleCount = current
        }
    }
    val status = rerunStatusUx(
        isConnected = isConnected,
        eventsSent = eventCount,
        eventsPerSecond = if (qaConnected) QA_EVENTS_PER_SECOND else eventsPerSec,
    )

    // Save & Share is the demo's primary action. Hoisted so the on-screen
    // SceneActionBar can invoke it — primary actions belong on-screen, not in
    // the Settings sheet (#1964).
    val onSaveAndShare = {
        if (!sharing) {
            sharing = true
            bridge.requestSaveAndShare { result ->
                scope.launch {
                    withContext(Dispatchers.Main) {
                        sharing = false
                        shareResult = result
                    }
                }
            }
        }
    }

    // Record starts on the frame on screen, whose camera gives the scan its lens.
    val onStartScan = start@{
        val frame = latestFrame ?: return@start
        val capture = ScanCapture.start(frame, scope, rawDepth) ?: return@start
        debugFullScreen = false
        debugSession.trace = capture.trace
        debugSession.goLive()
        phase = ScanPhase.Scanning
        scan = capture
    }
    // The capture starts by itself, once, as soon as ARCore has found the space. After a
    // capture that caught nothing, or a discarded one, the shutter starts the next one.
    var autoStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (qaDebug != null || qaRecord) return@LaunchedEffect
        while (!autoStarted) {
            if (isTracking && scan == null) {
                onStartScan()
                autoStarted = scan != null
            }
            delay(AUTO_START_POLL_MS)
        }
    }
    // Pause, Finish and Keep scanning move the phase; the capture's clock follows it (below).
    val moveTo = { next: ScanPhase -> phase = next }
    // Drops the capture, built or not: the camera goes back to waiting for the next one.
    val dropCapture = {
        if (scan != null) {
            debugSession.trace = ArDebugTrace()
            scan = null
        }
        qaScan = null
        builtPack = null
        leaveAsked = false
        discardAsked = false
        phase = ScanPhase.Scanning
    }
    // Save scan builds the capture — no more photos, the last ones encoded, its planes painted
    // from its photos — writes it on the phone and opens it, read back from what was saved. A
    // failed write keeps the built scan on screen for Try again; nothing claims it was saved.
    val onSaveScan = save@{
        val qaTrace = qaScan?.trace
        val capture = scan
        val nothingToSave = builtPack == null && qaTrace == null && capture == null
        if (phase == ScanPhase.Saving || nothingToSave) return@save
        leaveAsked = false
        moveTo(ScanPhase.Saving)
        scope.launch {
            val now = System.currentTimeMillis()
            // The build can throw too — out of memory on a long scan, painting its planes. That
            // lands on the same "Couldn't save" card as a failed write, and Try again rebuilds.
            val built = runCatching {
                builtPack ?: when {
                    qaTrace != null && sample != null -> qaSessionOf(qaTrace, sample)
                    capture != null -> capture.finish()
                    else -> null
                }
            }.onFailure { if (it is CancellationException) throw it }
            if (built.isFailure) {
                Log.w(SCAN_LOG_TAG, "3D Scan: building the scan failed", built.exceptionOrNull())
                phase = ScanPhase.SaveFailed
                return@launch
            }
            val pack = built.getOrNull()?.also {
                if (builtPack == null) builtTitle = recordingTitle(now)
                builtPack = it
            }
            if (pack == null) {
                // Nothing to build after all: the next capture starts from the shutter.
                dropCapture()
                return@launch
            }
            val title = builtTitle
            // The saved entry's id opens its result screen, where the name is edited (#4597).
            val savedId = withContext(Dispatchers.IO) {
                runCatching {
                    check(!qaSaveFails) { "QA: forced save failure" }
                    store.save(pack, title, RerunSessionSource.Recorded, now).id
                }.getOrNull()
            }
            if (savedId == null) {
                phase = ScanPhase.SaveFailed
                return@launch
            }
            val opened = runCatching { loadRerunSession(pack) }.getOrNull()
            dropCapture()
            if (opened != null) onScanned(opened, title, pack, savedId)
        }
    }
    val onDiscardScan = {
        dropCapture()
        onBack()
    }
    // Back during a capture asks first: keep scanning, save, or discard. The capture pauses
    // while it asks, so nothing is recorded behind the dialog. While it saves, Back waits.
    val onBackFromLive: () -> Unit = {
        if (!recording) {
            onBack()
        } else if (phase != ScanPhase.Saving) {
            moveTo(phase.leaveAsked())
            leaveAsked = true
        }
    }
    BackHandler(enabled = recording, onBack = onBackFromLive)

    // What the capture holds so far, counted off the scan itself: dense points on a depth scan,
    // ARCore's feature points otherwise. Zero at the start, never shown against a cap.
    val stats = debugSession.stats
    val capturedPoints = when {
        scan?.rawDepth == true -> scan?.denseCount ?: 0
        qaLong != null -> qaLong?.shownCount ?: 0
        else -> stats.mapPoints
    }
    val capturedPhotos = debugSession.trace.imageCount
    val captureEmpty = capturedPoints == 0 && capturedPhotos == 0 && stats.planes == 0
    val capturedClock = ArDebugFormat.clock(scan?.elapsedSeconds() ?: stats.duration)
    val trackingLost = recording && (
        ForcedTrackingFailure.override != null || if (qaRecord) qaTrackingLost else !isTracking
        )
    // The capture's clock runs only while it scans with tracking held: Pause, review, the Back
    // dialog and a lost track all stop it, and it runs on from there.
    val clockRuns = phase.clockRuns(trackingLost)
    LaunchedEffect(scan, clockRuns) { scan?.followClock(clockRuns) }

    DemoScaffold(
        title = stringResource(R.string.demo_ar_rerun_title),
        onBack = onBackFromLive,
        // The sheet holds what the screen must not: the connection steps a developer types
        // once. The screen itself only says what the demo does and whether it is live. A
        // capture in progress owns the screen: no Settings over it.
        controls = if (recording) null else { { RerunSheet() } },
        topOverlay = {
            if (recording && scanMedia != null) {
                // The HUD runs on its own clock: the trace records nothing while tracking is
                // lost ("Not enough detail"), and a HUD read off it froze there for seconds.
                val now by produceState(SystemClock.elapsedRealtimeNanos(), scan) {
                    while (true) {
                        delay(HUD_TICK_MS)
                        value = SystemClock.elapsedRealtimeNanos()
                    }
                }
                // Tracking loss freezes the capture, so the HUD says so: the coaching line
                // reads "Scan paused", and a red "Scanning" dot above it would contradict it.
                ScanHud(
                    phase = if (trackingLost && phase.integrates) ScanPhase.Paused else phase,
                    seconds = scan?.elapsedSeconds(now) ?: qaLong?.shownSeconds ?: stats.duration,
                    points = capturedPoints,
                )
            } else if (debugFullScreen) {
                ArDebugLegend(debugSession)
            } else {
                // While the SDK's "Couldn't start AR" card explains why there is no session,
                // the preview has nothing to mirror and sat on top of that card's title,
                // under the "No computer connected" card (#3989). One card at a time
                // (DESIGN.md "AR Overlay Card"): both come back as soon as a session
                // starts. A QA fixture hides the SDK card and owns the 3D view, so it
                // keeps them. Streaming to a computer is advanced: its card shows only
                // once one is connected.
                val availabilityCardShown = arCoreAvailability != null && qaState == null
                if (!availabilityCardShown) {
                    if (isConnected) RerunStatusCard(status)
                    ArDebugPip(
                        session = debugSession,
                        orbit = pipOrbit,
                        engine = engine,
                        modelLoader = modelLoader,
                        materialLoader = materialLoader,
                        onExpand = { debugFullScreen = true },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = Space.md),
                    )
                }
            }
        },
        // Status banner + primary action are both bottom-anchored, so both live in the
        // scaffold slot: a bottom-aligned Column that stacks them instead of letting
        // them share the band with each other and with the Settings FAB (#2779).
        bottomOverlay = {
            if (debugFullScreen) {
                ArDebugTimelineCard(debugSession)
            } else {
                // During a capture its one coaching line speaks instead: one message at a time.
                RerunCameraBottomOverlay(
                    visible = !recording && (
                        (!isTracking && arCoreAvailability == null && qaDebug == null) ||
                            ForcedTrackingFailure.override != null
                        ),
                    trackingFailureReason = trackingFailureReason,
                    // Before a scan, the wait is for Record; during one, ARCore's own guidance.
                    searching = if (recording) null else ScanCopy.WAITING,
                    isConnected = isConnected,
                    sharing = sharing,
                    onSaveAndShare = onSaveAndShare,
                )
                if (recording && scanMedia != null && phase in SCAN_MAP_PHASES) {
                    ScanCoverageMap(
                        state = scanCoverage,
                        modifier = Modifier.align(Alignment.Start).padding(start = Space.md),
                    )
                }
                if (recording && !scanFollowing && phase != ScanPhase.Saving) {
                    ScanRecenterPill(onRecenter = { scanOrbit.recenter() })
                }
                if (arCoreAvailability == null || qaState != null) {
                    when {
                        !recording -> ScanShutter(startEnabled = isTracking, onStart = onStartScan)
                        phase == ScanPhase.Reviewing -> ScanReviewCard(
                            empty = captureEmpty,
                            figures = ScanCopy.reviewFigures(capturedPoints, capturedPhotos, capturedClock),
                            onKeepScanning = { moveTo(ScanPhase.Scanning) },
                            onSave = onSaveScan,
                        )
                        phase == ScanPhase.Saving -> ScanSavingCard()
                        phase == ScanPhase.SaveFailed -> ScanSaveFailedCard(
                            // The scan was never written: discarding it asks first.
                            onDiscard = { discardAsked = true },
                            onRetry = onSaveScan,
                        )
                        else -> ScanControls(
                            phase = phase,
                            coaching = ScanCopy.coaching(
                                phase = phase,
                                trackingLost = trackingLost,
                                photoLimitReached = scan?.isPhotoLimitReached == true,
                                detail = scan?.denseDetail ?: qaLong?.shownDetail ?: ScanDetail.Fine,
                                coach = scanCoverage.coachLine,
                            ),
                            onTogglePause = { moveTo(phase.togglePause()) },
                            onFinish = { moveTo(phase.finish()) },
                        )
                    }
                }
            }
        },
        // A scan in progress owns the screen: nothing in the dock may leave it half-taken.
        dock = if (recording) emptyList() else listOf(
            DockItem(
                icon = Icons.Rounded.Videocam,
                label = "Camera view",
                caption = "Camera",
                onClick = { debugFullScreen = false },
                selected = !debugFullScreen,
            ),
            DockItem(
                icon = Icons.Rounded.ViewInAr,
                label = "3D debug view",
                caption = "3D view",
                onClick = { debugFullScreen = true },
                selected = debugFullScreen,
            ),
            DockItem(
                icon = Icons.Outlined.RestartAlt,
                label = "Recenter the 3D view",
                caption = "Recenter",
                onClick = { debugOrbit.recenter() },
                enabled = debugFullScreen,
            ),
        ),
    ) {
        val cameraStream = rememberARCameraStream(materialLoader)
        // QA camera backdrop (#3308): the emulator delivers no camera frame.
        val qaBackdrop = rememberQaCameraBackdropActive(cameraReady)
        Box(modifier = Modifier.fillMaxSize()) {
            if (qaBackdrop) QaCameraBackdrop(seed = QA_SEED)
            ARSceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                isOpaque = !qaCameraBackdropEnabled(),
                surfaceType = qaCameraBackdropSurfaceType(),
                cameraStream = if (qaBackdrop) null else cameraStream,
                playbackDataset = arPlaybackDataset,
                planeRenderer = true,
                sessionConfiguration = { session: Session, config: Config ->
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    // Rerun v2 tier B: ARCore's raw depth where the phone has it, never where it
                    // does not — that phone keeps the sparse v1 scan, and says so.
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
                    // Bridge gates on its own enabled + connection state, so this
                    // is safe whether or not the recorder is reachable.
                    bridge.logFrame(session, frame)
                    // A QA fixture owns the 3D view; otherwise it mirrors this session.
                    // A scan rides on the same pass while it is scanning.
                    // Paused, in review or saving, a capture takes nothing: no path, no points.
                    val capturing = scan == null || phase.integrates
                    if (qaDebug == null && !qaRecord && capturing) {
                        debugRecorder.record(debugSession.trace, session, frame, anchors, scan)
                    }
                },
                // A forced QA state hides the SDK's "Couldn't start AR" card so the screen
                // can be captured on the emulator, which never starts AR (#2754).
                arCoreAvailabilityOverlay = if (qaState == null) {
                    { ARCoreAvailabilityOverlay(it) }
                } else {
                    null
                },
                onARCoreAvailability = { arCoreAvailability = it },
                onTrackingFailureChanged = { reason ->
                    trackingFailureReason = reason
                },
                onGestureListener = rememberOnGestureListener(
                    onSingleTapConfirmed = { event: MotionEvent, _ ->
                        val frame = latestFrame ?: return@rememberOnGestureListener
                        if (frame.camera.trackingState != TrackingState.TRACKING) {
                            return@rememberOnGestureListener
                        }
                        val hit = frame.hitTest(event).firstOrNull { result ->
                            val trackable = result.trackable
                            trackable is Plane &&
                                trackable.isPoseInPolygon(result.hitPose) &&
                                result.distance <= MAX_PLACEMENT_DISTANCE_METERS
                        }
                        if (hit != null) {
                            anchors.add(hit.createAnchor())
                        }
                    }
                )
            ) {
                anchors.forEach { anchor ->
                    // One model instance per placement: a Filament instance can only hang
                    // off one node, so a shared one showed a single dog however many taps.
                    key(anchor) {
                        val dog = rememberModelInstance(modelLoader, "models/shiba.glb")
                        AnchorNode(anchor = anchor) {
                            dog?.let { ModelNode(modelInstance = it, scaleToUnits = 0.3f) }
                        }
                    }
                }
            }

            // A capture owns the screen (#4628): its cloud full screen over the dimmed camera,
            // following the phone. The camera keeps tracking, and recording, under it.
            if (recording && scanMedia != null) {
                ScanLiveView(
                    session = debugSession,
                    orbit = scanOrbit,
                    follow = scanFollow,
                    coverage = scanCoverage,
                    following = scanFollowing,
                    media = scanMedia,
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                )
            }

            // Full-screen 3D view over the camera, which keeps tracking (and recording) under it.
            if (debugFullScreen) {
                ArDebugSceneView(
                    session = debugSession,
                    orbit = debugOrbit,
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (leaveAsked) {
                ScanLeaveDialog(
                    saveFailed = phase == ScanPhase.SaveFailed,
                    // A capture that caught nothing has nothing to save: Keep scanning or Discard.
                    empty = captureEmpty && builtPack == null,
                    onKeepScanning = {
                        leaveAsked = false
                        // Back paused the capture, or it was paused or in review already: Keep
                        // scanning means scanning again, from any of them.
                        moveTo(phase.keepScanning())
                    },
                    onSave = onSaveScan,
                    onDiscard = onDiscardScan,
                )
            }
            if (discardAsked) {
                ScanDiscardDialog(
                    onCancel = { discardAsked = false },
                    onDiscard = {
                        discardAsked = false
                        dropCapture()
                    },
                )
            }

            // Share result dialog
            shareResult?.let { result ->
                ShareResultDialog(
                    result = result,
                    onDismiss = { shareResult = null },
                    onCopyPath = { path ->
                        copyToClipboard(context, "Path", path)
                        Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                    },
                    onCopyUrl = { url ->
                        copyToClipboard(context, "Viewer URL", url)
                        Toast.makeText(context, "Viewer URL copied", Toast.LENGTH_SHORT).show()
                    },
                    onShare = onShare@{ url ->
                        if (url.isNullOrBlank()) return@onShare
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                            putExtra(Intent.EXTRA_SUBJECT, "AR session — SceneView")
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Share AR session")
                        )
                    },
                )
            }
        }
    }
}

/**
 * The stream's status, a line of the settings sheet's "Connect your computer" section: a dot that
 * turns green while events reach the computer, a title, and how much has been sent. It was a
 * card over the camera (#4379); Save & Share on the screen already says a computer is there.
 */
@Composable
private fun RerunStatusCard(status: RerunStatusUx) {
    OverlayCard(testTag = RERUN_STATUS_CARD_TAG) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(StatusDotSize)
                    .background(
                        color = if (status.live) ArOverlay.accentSuccess else ArOverlay.onScrimMuted,
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.width(Space.sm))
            Text(text = status.title, style = OnScrimTitle)
        }
        Text(text = status.detail, style = OnScrimBody)
    }
}

/**
 * The camera view's bottom stack: the tracking banner, then Save & Share once it can work.
 * [visible] gates the banner only; [searching] replaces its generic "scanning" line.
 */
@Composable
private fun DemoBottomOverlayScope.RerunCameraBottomOverlay(
    visible: Boolean,
    trackingFailureReason: TrackingFailureReason?,
    searching: String?,
    isConnected: Boolean,
    sharing: Boolean,
    onSaveAndShare: () -> Unit,
) {
    // ForcedTrackingFailure.override shadows the real ARCore-reported reason
    // when a developer has picked one in the debug menu (#1881). Read it here
    // so flipping the override re-renders the overlay immediately.
    val effectiveReason = ForcedTrackingFailure.override ?: trackingFailureReason
    AnimatedVisibility(
        // #3341: on a device ARCore has ruled out, the flag this banner waits on never
        // flips, so the banner would promise a scan under the SDK's "AR unavailable" card.
        // The caller drops it then, and lets the card carry reason and retry.
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val trackingHint = trackingFailureMessage(effectiveReason)
        DemoStatusBanner(
            text = trackingHint ?: searching ?: stringResource(R.string.ar_status_scanning),
            // Tone comes from the same reason that picks the sentence: a bad
            // session state or a camera taken by another app needs the user to
            // act outside this demo, the light / motion / texture reasons ask
            // for a physical move, and no reason at all is plain scanning.
            tone = when {
                trackingHint == null -> DemoStatusTone.Progress
                effectiveReason == TrackingFailureReason.BAD_STATE ||
                    effectiveReason == TrackingFailureReason.CAMERA_UNAVAILABLE ->
                    DemoStatusTone.Blocked
                else -> DemoStatusTone.Guidance
            },
        )
    }

    // Primary action on-screen (#1964), offered only when it can work (#2658,
    // #3831): with no computer attached a save can only fail, and the status card
    // already says so without an error-toned banner.
    if (BuildConfig.DEBUG && rerunShowsSaveAction(isConnected = isConnected, sharing = sharing)) {
        val saveUx = rerunSaveActionUx(sharing = sharing, isConnected = isConnected)
        SceneActionBar(
            SceneAction(
                label = saveUx.label,
                onClick = onSaveAndShare,
                enabled = saveUx.enabled,
            ),
        )
    }
}

/** "Connect your computer", numbered, commands in mono blocks. Theme colours: it is the sheet. */
@Composable
private fun RerunSetupSection() {
    // Space.md above: the sheet's intro paragraph sits right before this heading.
    Column(
        modifier = Modifier.padding(top = Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Text(
            text = RERUN_SETUP_TITLE,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        // What streaming does, under its own heading: it is the advanced path, not the demo.
        Text(
            text = RERUN_INTRO,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RERUN_SETUP_STEPS.forEachIndexed { index, step -> RerunSetupStepRow(index + 1, step) }
    }
}

@Composable
private fun RerunSetupStepRow(number: Int, step: RerunSetupStep) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(Space.md + Space.xs),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Text(text = step.text, style = MaterialTheme.typography.bodyMedium)
            step.command?.let { command ->
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(SceneViewTokens.Radius.xs),
                        )
                        .padding(horizontal = Space.sm, vertical = Space.xs + Space.xs / 2),
                )
            }
        }
    }
}

@Composable
private fun ShareResultDialog(
    result: RerunBridge.ShareResult,
    onDismiss: () -> Unit,
    onCopyPath: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
    onShare: (String?) -> Unit,
) {
    val viewerUrl = result.viewerUrl
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (result.success) "Recording saved" else "Couldn't save")
        },
        text = {
            if (result.success) {
                ShareResultBody(result, onCopyPath, onCopyUrl)
            } else {
                // Never surface the bridge's raw internal reason (e.g. "call
                // connect() first") — map it to actionable setup copy (#2658).
                Text(
                    text = rerunSaveFailureMessage(result.reason),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            if (result.success && viewerUrl != null) {
                TextButton(onClick = { onShare(viewerUrl) }) { Text("Share link") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = if (result.success) {
            { TextButton(onClick = onDismiss) { Text("Done") } }
        } else null,
    )
}

@Composable
private fun ShareResultBody(
    result: RerunBridge.ShareResult,
    onCopyPath: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            "${result.events} events recorded.",
            style = MaterialTheme.typography.bodyMedium,
        )
        result.path?.let { path ->
            Text("Saved on your computer:", style = MaterialTheme.typography.labelMedium)
            Text(
                path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { onCopyPath(path) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Copy path") }
        }
        result.viewerUrl?.let { url ->
            Text(
                "Drop the file onto sceneview.github.io/rerun to scrub through it. To share " +
                    "it, upload the file somewhere public and send this link:",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(
                onClick = { onCopyUrl(url) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Copy viewer URL") }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

private val StatusDotSize = Space.sm + Space.xs / 2 // 10 dp, same as the record dot

private const val MAX_PLACEMENT_DISTANCE_METERS = 5f
private const val QA_SEED = "ar-rerun"

private const val QA_STATE_CONNECTED = "connected"
private const val QA_STATE_SAVED = "saved"

/** QA only: the Record screen mid-scan, fed by the sample room (#2754: no AR on the emulator). */
private const val QA_STATE_RECORD = "record"

/** QA only (#4627): the capture screen held on a lost track, which the emulator never has. */
private const val QA_STATE_RECORD_TRACKING_LOST = "record-tracking-lost"

/** QA only (#4627): the capture screen whose write fails, to show the failure that stays. */
private const val QA_STATE_RECORD_SAVE_FAILED = "record-save-failed"

/** QA only (#4629): the capture screen fed a whole flat's depth, and the same with the old fixed cap. */
private const val QA_STATE_RECORD_LONG = "record-long"
private const val QA_STATE_RECORD_LONG_FIXED = "record-long-fixed"
private val LONG_QA_STATES = setOf(QA_STATE_RECORD_LONG, QA_STATE_RECORD_LONG_FIXED)

private val RECORD_QA_STATES =
    setOf(QA_STATE_RECORD, QA_STATE_RECORD_TRACKING_LOST, QA_STATE_RECORD_SAVE_FAILED) + LONG_QA_STATES

/** The QA scan plays the sample once and holds on its end, as a scan in progress would. */
private const val QA_RECORD_NEVER_LOOP_S = 3_600f

/** Seconds the tracking-loss QA scan captures before it loses tracking. */
private const val QA_TRACKING_LOST_AFTER_S = 3f

/** `adb logcat -s ARRerunDemo`: a scan build that threw. */
private const val SCAN_LOG_TAG = "ARRerunDemo"

/** How often the live screen checks whether ARCore has found the room, to start recording. */
private const val AUTO_START_POLL_MS = 100L

/** The scan HUD's clock ticks ten times a second, tracking or not. */
private const val HUD_TICK_MS = 100L
private const val NANOS_PER_SECOND = 1e9f

/**
 * QA only: the take the Record screen played from the sample, built into a session like a real
 * scan — its journal, and the sample's photos taken from where the trace's camera stood.
 */
private suspend fun qaSessionOf(trace: ArDebugTrace, sample: RerunReplayMedia): RerunCapturePack? {
    val events = trace.journal?.toList().orEmpty()
    trace.journal = null
    if (events.isEmpty()) return null
    val photos = withContext(Dispatchers.Default) {
        (0 until trace.imageCount).mapNotNull { index ->
            val path = trace.imagePath(index)
            val jpeg = sample.bytesOf(path) ?: return@mapNotNull null
            val pose = trace.frameAt(trace.imageTime(index)).camera ?: return@mapNotNull null
            ScanPhoto(path, jpeg, pose)
        }
    }
    // The emulator has no depth: the ray-cast room stands in for a scan's fused points, so the
    // result the QA take opens on is built, kept and reopened as a real scan's is.
    val dense = withContext(Dispatchers.Default) { syntheticCloud() }
    val device = ScanDevice("android", Build.MODEL, ScanDevice.TIER_DEPTH, ScanDevice.SOURCE_RAW_DEPTH)
    return RerunCaptureBuilder.build(events, sample.manifest.lens, photos, device = device, dense = dense)
}
private const val QA_SCRUB_FRACTION = 0.45f
private const val AR_DEBUG_FIXTURE = "rerun/sample-session.jsonl"

/** The QA states that feed the 3D view from [AR_DEBUG_FIXTURE] (#3950). */
private enum class ArDebugQaState(
    val key: String,
    val fullScreen: Boolean,
    val streams: Boolean,
    val scrubbed: Boolean = false,
) {
    Pip("pip", fullScreen = false, streams = false),
    PipStream("pip-stream", fullScreen = false, streams = true),
    Full("3d", fullScreen = true, streams = false),
    Scrub("3d-scrub", fullScreen = true, streams = false, scrubbed = true),
    FullStream("3d-stream", fullScreen = true, streams = true),
    ;

    companion object {
        fun of(state: String?): ArDebugQaState? = entries.firstOrNull { it.key == state }
    }
}

/** The QA states that open straight on the dollhouse (#4075). */
private val DOLLHOUSE_QA_STATES = setOf(QA_STATE_DOLLHOUSE_EMPTY, QA_STATE_DOLLHOUSE_3D)

/** The dollhouse with no session kept, whatever the phone holds (#4075). */
private const val QA_STATE_DOLLHOUSE_EMPTY = "dollhouse-empty"

/** The dollhouse of the newest session, in the 3D view: the emulator cannot run AR (#2754). */
private const val QA_STATE_DOLLHOUSE_3D = "dollhouse-3d"

/** The result built from the ray-cast room: the emulator's view of the result screen, which it cannot scan. */
private const val QA_STATE_MODEL_SYNTHETIC = "model-synthetic"

/** The same room when meshing finds no surface: the coloured-points fallback. */
private const val QA_STATE_MODEL_POINTS = "model-synthetic-points"

/** The same result with its export sheet open. */
private const val QA_STATE_MODEL_EXPORT = "model-synthetic-export"

/** The QA states that open straight on the result screen. */
private val MODEL_QA_STATES = setOf(QA_STATE_MODEL_SYNTHETIC, QA_STATE_MODEL_POINTS, QA_STATE_MODEL_EXPORT)

/** The QA states that open straight on the live AR screen. */
private val LIVE_QA_STATES =
    ArDebugQaState.entries.map { it.key } + QA_STATE_CONNECTED + QA_STATE_SAVED + RECORD_QA_STATES

/**
 * The QA states of the bundled replay: a view, and where the replay stands — paused at a fixed
 * fraction for a stable capture, or playing (`null`). With any QA state the camera neither flies
 * in nor drifts.
 */
private enum class RerunReplayQaState(val key: String, val mode: RerunMode, val pauseAt: Float?) {
    Replay("replay", RerunMode.Scene, QA_REPLAY_FRACTION),
    ReplayPlay("replay-play", RerunMode.Scene, null),
    ReplayMap("replay-map", RerunMode.Map, QA_REPLAY_FRACTION),
    ReplayCamera("replay-camera", RerunMode.Camera, QA_REPLAY_FRACTION),
    /** The sample's replay with its export sheet open. */
    ReplayExport("replay-export", RerunMode.Scene, QA_REPLAY_FRACTION),
    ;

    companion object {
        fun of(state: String?): RerunReplayQaState? = entries.firstOrNull { it.key == state }
    }
}

/** 62 % in: both models placed, all three planes found, the camera mid-turn. */
private const val QA_REPLAY_FRACTION = 0.62f

private const val QA_EVENTS_SENT = 1_204L
private const val QA_EVENTS_PER_SECOND = 10f
private val QA_SHARE_RESULT = RerunBridge.ShareResult(
    success = true,
    path = "~/sceneview/recording.rrd",
    viewerUrl = "https://sceneview.github.io/rerun/?url=https://example.com/recording.rrd",
    events = 1_204,
    reason = null,
)

internal const val RERUN_STATUS_CARD_TAG = "ar_rerun_status_card"

/** The capture phases that show its coverage map (#4631): while it is open, not under a card. */
private val SCAN_MAP_PHASES = setOf(ScanPhase.Scanning, ScanPhase.Paused)
