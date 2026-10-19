package io.github.sceneview.demo.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import io.github.sceneview.demo.R
import io.github.sceneview.demo.demos.internal.DebugGroup
import io.github.sceneview.demo.demos.internal.ScanFollow
import io.github.sceneview.demo.ui.GlassActionPill
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.google.android.filament.Engine
import io.github.sceneview.demo.demos.internal.ArDebugFormat
import io.github.sceneview.demo.demos.internal.ArDebugOrbitCamera
import io.github.sceneview.demo.demos.internal.ArDebugSession
import io.github.sceneview.demo.demos.internal.ScanCopy
import io.github.sceneview.demo.demos.internal.ScanPhase
import io.github.sceneview.demo.demos.internal.ScanScopeCopy
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.theme.SceneViewTokens.ArOverlay
import io.github.sceneview.demo.theme.SceneViewTokens.Space
import io.github.sceneview.demo.ui.overMediaEdge
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader

/*
 * The capture screen of 3D Scan (#4627, #4628): the scan growing full screen over the dimmed
 * camera, following the phone heading-up, one
 * compact line above it — "Scanning · 0:42" and the points captured so far — and, under it,
 * one coaching line with Pause and Finish. Finish opens a review that Keep scanning leaves for
 * the same capture; only Save scan builds and writes it.
 */

/**
 * The capture's status: the red dot and `Scanning · 0:42` (`Paused` while frozen), then the
 * points captured, counted off the scan from zero and never shown against a cap.
 */
@Composable
internal fun ScanHud(phase: ScanPhase, seconds: Float, points: Int, label: String? = null) {
    val clock = ArDebugFormat.clock(seconds)
    OverlayCard(testTag = SCAN_HUD_TAG) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {},
        ) {
            Box(
                modifier = Modifier
                    .size(ScanDotSize)
                    .background(
                        color = if (phase.integrates) ArOverlay.accentRecord else ArOverlay.onScrimMuted,
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.width(Space.sm))
            // What is being scanned leads (#4647): `Room 2 · Scanning · 0:42`.
            val status = ScanCopy.status(phase, clock)
            Text(
                text = label?.let { "$it · $status" } ?: status,
                style = OnScrimTitle,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = ScanCopy.pointsCaptured(points),
            style = OnScrimBody,
            maxLines = 1,
            modifier = Modifier.testTag(SCAN_POINTS_TAG),
        )
    }
}

/**
 * Under a capture still open: the one coaching line, then Pause (Resume while paused) and the
 * prominent Finish. Both are full-height Material buttons, 48 dp targets.
 */
@Composable
internal fun ScanControls(
    phase: ScanPhase,
    coaching: String?,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    resumeLabel: String = ScanCopy.RESUME,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md).testTag(SCAN_CONTROLS_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        if (coaching != null) ScanHint(coaching)
        Row(
            modifier = Modifier.widthIn(max = ArOverlay.maxWidth).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            FilledTonalButton(
                onClick = onTogglePause,
                modifier = Modifier.weight(1f).heightIn(min = SceneViewTokens.Layout.touchTarget),
            ) {
                Text(if (phase == ScanPhase.Paused) resumeLabel else ScanCopy.PAUSE)
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f).heightIn(min = SceneViewTokens.Layout.touchTarget),
            ) {
                Text(ScanCopy.FINISH)
            }
        }
    }
}

/**
 * Finish's review: the capture frozen, nothing built yet. Keep scanning returns to the same
 * capture; Save scan builds and writes it. A capture with nothing in it offers only Keep
 * scanning — there is nothing to save.
 */
@Composable
internal fun ScanReviewCard(
    empty: Boolean,
    figures: String,
    onKeepScanning: () -> Unit,
    onSave: () -> Unit,
) {
    OverlayCard(testTag = SCAN_REVIEW_TAG) {
        Text(text = if (empty) ScanCopy.EMPTY_TITLE else ScanCopy.REVIEW_TITLE, style = OnScrimTitle)
        Text(text = if (empty) ScanCopy.EMPTY_DETAIL else figures, style = OnScrimBody)
        if (!empty) Text(text = ScanCopy.REVIEW_DETAIL, style = OnScrimBody)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (empty) {
                Button(onClick = onKeepScanning, modifier = Modifier.weight(1f)) { Text(ScanCopy.KEEP_SCANNING) }
            } else {
                FilledTonalButton(onClick = onKeepScanning, modifier = Modifier.weight(1f)) {
                    Text(ScanCopy.KEEP_SCANNING)
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text(ScanCopy.SAVE_SCAN) }
            }
        }
    }
}

/**
 * Finish's review for one room of a whole home (#4647): Keep scanning returns to the same room,
 * Next room saves it and waits, paused, for the next one, Save and finish saves it and shows the
 * plan of every room. A room with nothing in it is not saved: Finish shows the rooms already
 * saved, when there are some.
 */
@Composable
@Suppress("LongParameterList") // the review's four answers
internal fun HomeReviewCard(
    roomNumber: Int,
    empty: Boolean,
    figures: String,
    onKeepScanning: () -> Unit,
    onNextRoom: () -> Unit,
    onSaveAndFinish: () -> Unit,
    onFinish: (() -> Unit)?,
) {
    OverlayCard(testTag = SCAN_REVIEW_TAG) {
        Text(
            text = if (empty) ScanCopy.EMPTY_TITLE else ScanScopeCopy.reviewTitle(roomNumber),
            style = OnScrimTitle,
        )
        Text(
            text = when {
                !empty -> figures
                onFinish != null -> ScanScopeCopy.EMPTY_DETAIL
                else -> ScanCopy.EMPTY_DETAIL
            },
            style = OnScrimBody,
        )
        if (!empty) Text(text = ScanScopeCopy.REVIEW_DETAIL, style = OnScrimBody)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            val keepScanning = Modifier.weight(1f).heightIn(min = SceneViewTokens.Layout.touchTarget)
            when {
                !empty -> {
                    FilledTonalButton(onClick = onKeepScanning, modifier = keepScanning) {
                        Text(ScanCopy.KEEP_SCANNING)
                    }
                    Button(onClick = onNextRoom, modifier = keepScanning) { Text(ScanScopeCopy.NEXT_ROOM) }
                }
                onFinish != null -> {
                    FilledTonalButton(onClick = onKeepScanning, modifier = keepScanning) {
                        Text(ScanCopy.KEEP_SCANNING)
                    }
                    Button(onClick = onFinish, modifier = keepScanning) { Text(ScanScopeCopy.FINISH_HOME) }
                }
                else -> Button(onClick = onKeepScanning, modifier = keepScanning) { Text(ScanCopy.KEEP_SCANNING) }
            }
        }
        if (!empty) {
            FilledTonalButton(
                onClick = onSaveAndFinish,
                modifier = Modifier.fillMaxWidth().heightIn(min = SceneViewTokens.Layout.touchTarget),
            ) {
                Text(ScanScopeCopy.SAVE_AND_FINISH)
            }
        }
    }
}

/** Save scan, in progress: the card stays where the review was, with what it is doing. */
@Composable
internal fun ScanSavingCard() {
    OverlayCard(testTag = SCAN_SAVING_TAG) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(Space.md + Space.xs / 2),
                color = ArOverlay.onScrim,
                strokeWidth = Space.xs / 2,
            )
            Spacer(Modifier.width(Space.sm))
            Text(text = ScanCopy.SAVING, style = OnScrimTitle)
        }
    }
}

/**
 * A write that failed stays on screen until it is answered: the built scan is still held, Try
 * again writes it again, Discard drops it. Never a toast, never "Saved".
 */
@Composable
internal fun ScanSaveFailedCard(onDiscard: () -> Unit, onRetry: () -> Unit) {
    OverlayCard(testTag = SCAN_SAVE_FAILED_TAG) {
        Text(
            text = ScanCopy.SAVE_FAILED,
            style = SceneViewTokens.Type.title.copy(color = ArOverlay.accentGuidance),
        )
        Text(text = ScanCopy.SAVE_FAILED_DETAIL, style = OnScrimBody)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            FilledTonalButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text(ScanCopy.DISCARD) }
            Button(onClick = onRetry, modifier = Modifier.weight(1f)) { Text(ScanCopy.TRY_AGAIN) }
        }
    }
}

/**
 * Back during a capture: keep scanning, save it, or discard it — never a silent loss. After a
 * failed write, Save scan reads Try again. A capture that caught nothing ([empty]) has nothing
 * to save: Keep scanning or Discard only.
 */
@Composable
internal fun ScanLeaveDialog(
    saveFailed: Boolean,
    empty: Boolean,
    onKeepScanning: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val discard: @Composable () -> Unit = {
        TextButton(onClick = onDiscard) {
            Text(ScanCopy.DISCARD, color = MaterialTheme.colorScheme.error)
        }
    }
    AlertDialog(
        onDismissRequest = onKeepScanning,
        title = { Text(ScanCopy.LEAVE_TITLE) },
        text = { Text(if (empty) ScanCopy.LEAVE_EMPTY_DETAIL else ScanCopy.LEAVE_DETAIL) },
        confirmButton = {
            if (empty) {
                TextButton(onClick = onKeepScanning) { Text(ScanCopy.KEEP_SCANNING) }
            } else {
                TextButton(onClick = onSave) { Text(if (saveFailed) ScanCopy.TRY_AGAIN else ScanCopy.SAVE_SCAN) }
            }
        },
        dismissButton = {
            if (empty) {
                discard()
            } else {
                Row {
                    discard()
                    TextButton(onClick = onKeepScanning) {
                        Text(if (saveFailed) ScanCopy.CANCEL else ScanCopy.KEEP_SCANNING)
                    }
                }
            }
        },
        modifier = Modifier.testTag(SCAN_LEAVE_TAG),
    )
}

/**
 * Discard on the "Couldn't save" card: the scan was never written, so dropping it is final and
 * asks first.
 */
@Composable
internal fun ScanDiscardDialog(onCancel: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(ScanCopy.DISCARD_TITLE) },
        text = { Text(ScanCopy.DISCARD_DETAIL) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(ScanCopy.DISCARD, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(ScanCopy.CANCEL) }
        },
        modifier = Modifier.testTag(SCAN_DISCARD_TAG),
    )
}

@Composable
private fun ScanHint(text: String) {
    Text(
        text = text,
        style = SceneViewTokens.Type.body.copy(color = ArOverlay.onScrim),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .background(ArOverlay.scrimDark, CircleShape)
            .padding(horizontal = Space.md, vertical = Space.sm)
            .testTag(SCAN_HINT_TAG),
    )
}

/**
 * The scan growing in 3D while it records, full screen (#4628): the camera dimmed under it, so
 * what is not scanned yet still shows, and on top the points in the colours the camera saw —
 * no path, no photo frames, no plane grid. The view follows the phone like a navigation app,
 * heading-up ([ScanFollow]); a drag or a pinch takes it over, and [ScanRecenterPill] gives it
 * back. While it follows, a marker holds the phone's place, where [ScanFollow.LIFT] draws it.
 */
@Composable
@Suppress("LongParameterList") // the demo's shared engine, handed down once
internal fun ScanLiveView(
    session: ArDebugSession,
    orbit: ArDebugOrbitCamera,
    follow: ScanFollow,
    coverage: ScanCoverageState,
    following: Boolean,
    media: RerunReplayMedia,
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag(SCAN_STAGE_TAG)) {
        // Only the camera is dimmed: the cloud is drawn by its own view, above the veil.
        Box(Modifier.fillMaxSize().background(ArOverlay.cameraDim))
        ArDebugSceneView(
            session = session,
            orbit = orbit,
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            modifier = Modifier.fillMaxSize(),
            replay = media,
            transparent = true,
            visible = { it in SCAN_LIVE_GROUPS },
            follow = { frame -> follow.poseFor(frame.camera) },
            observe = coverage::observe,
        )
        if (following) {
            val youAreHere = stringResource(R.string.demo_ar_rerun_you_are_here)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = maxHeight * (0.5f - ScanFollow.LIFT) - ScanMarkerSize / 2)
                    .size(ScanMarkerSize)
                    .overMediaEdge(CircleShape)
                    .background(ArOverlay.accentProgress, CircleShape)
                    .semantics { contentDescription = youAreHere }
                    .testTag(SCAN_MARKER_TAG),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = null,
                    tint = ArOverlay.onAccentProgress,
                    modifier = Modifier.size(ScanMarkerSize - Space.sm),
                )
            }
        }
    }
}

/** Shown once a drag or a pinch has left the follow view: back to following the phone. */
@Composable
internal fun ScanRecenterPill(onRecenter: () -> Unit) {
    GlassActionPill(
        icon = Icons.Rounded.MyLocation,
        label = stringResource(R.string.demo_ar_rerun_recenter),
        contentDescription = stringResource(R.string.demo_ar_rerun_recenter_description),
        onClick = onRecenter,
        modifier = Modifier.testTag(SCAN_RECENTER_TAG),
    )
}

/**
 * Before a capture: the shutter that starts one, once ARCore has found the space. A capture in
 * progress has [ScanControls] instead.
 */
@Composable
internal fun ScanShutter(startEnabled: Boolean, onStart: () -> Unit) {
    RecordShutter(isRecording = false, startEnabled = startEnabled, onStart = onStart, onStop = {})
}

/** What the live view draws: the points, in the colours the camera saw. */
private val SCAN_LIVE_GROUPS = setOf(DebugGroup.Points)

/** The phone's marker: a 28 dp disc, read at a glance like a navigation app's arrow. */
private val ScanMarkerSize = Space.lg + Space.xs

/** A notch above the capture card's 10 dp dot: this one is read from a metre away. */
private val ScanDotSize = Space.md - Space.xs / 2

internal const val SCAN_HUD_TAG = "ar_rerun_scan_hud"
internal const val SCAN_STAGE_TAG = "ar_rerun_scan_stage"
internal const val SCAN_MARKER_TAG = "ar_rerun_scan_marker"
internal const val SCAN_RECENTER_TAG = "ar_rerun_scan_recenter"
internal const val SCAN_POINTS_TAG = "ar_rerun_scan_points"
internal const val SCAN_CONTROLS_TAG = "ar_rerun_scan_controls"
internal const val SCAN_HINT_TAG = "ar_rerun_scan_hint"
internal const val SCAN_REVIEW_TAG = "ar_rerun_scan_review"
internal const val SCAN_SAVING_TAG = "ar_rerun_scan_saving"
internal const val SCAN_SAVE_FAILED_TAG = "ar_rerun_scan_save_failed"
internal const val SCAN_LEAVE_TAG = "ar_rerun_scan_leave"
internal const val SCAN_DISCARD_TAG = "ar_rerun_scan_discard"
