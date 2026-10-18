package io.github.sceneview.demo.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.google.android.filament.Engine
import io.github.sceneview.demo.demos.internal.ArDebugFormat
import io.github.sceneview.demo.demos.internal.ArDebugOrbitCamera
import io.github.sceneview.demo.demos.internal.ArDebugSession
import io.github.sceneview.demo.demos.internal.ScanCopy
import io.github.sceneview.demo.demos.internal.ScanPhase
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.theme.SceneViewTokens.ArOverlay
import io.github.sceneview.demo.theme.SceneViewTokens.Space
import io.github.sceneview.demo.ui.overMediaEdge
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader

/*
 * The capture screen of 3D Scan (#4627): the scan growing in a 3D card over the camera, one
 * compact line above it — "Scanning · 0:42" and the points captured so far — and, under it,
 * one coaching line with Pause and Finish. Finish opens a review that Keep scanning leaves for
 * the same capture; only Save scan builds and writes it.
 */

/**
 * The capture's status: the red dot and `Scanning · 0:42` (`Paused` while frozen), then the
 * points captured, counted off the scan from zero and never shown against a cap.
 */
@Composable
internal fun ScanHud(phase: ScanPhase, seconds: Float, points: Int) {
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
            Text(text = ScanCopy.status(phase, clock), style = OnScrimTitle, modifier = Modifier.weight(1f))
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
                Text(if (phase == ScanPhase.Paused) ScanCopy.RESUME else ScanCopy.PAUSE)
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
 * failed write, Save scan reads Try again.
 */
@Composable
internal fun ScanLeaveDialog(
    saveFailed: Boolean,
    onKeepScanning: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepScanning,
        title = { Text(ScanCopy.LEAVE_TITLE) },
        text = { Text(ScanCopy.LEAVE_DETAIL) },
        confirmButton = {
            TextButton(onClick = onSave) { Text(if (saveFailed) ScanCopy.TRY_AGAIN else ScanCopy.SAVE_SCAN) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) {
                    Text(ScanCopy.DISCARD, color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onKeepScanning) {
                    Text(if (saveFailed) ScanCopy.CANCEL else ScanCopy.KEEP_SCANNING)
                }
            }
        },
        modifier = Modifier.testTag(SCAN_LEAVE_TAG),
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
 * The scan growing in 3D while it records: the camera's path drawing itself, the points in the
 * colours the camera saw, the planes and the photos, framed as they grow. The same view the
 * replay opens on once the scan stops.
 */
@Composable
@Suppress("LongParameterList") // the demo's shared engine, handed down once
internal fun ScanStage(
    session: ArDebugSession,
    orbit: ArDebugOrbitCamera,
    media: RerunReplayMedia,
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
) {
    val shape = RoundedCornerShape(SceneViewTokens.Radius.lg)
    // The scaffold stacks top overlays Space.sm apart; one more brings the card to the Space.md
    // the HUD keeps from the header and the screen's edges.
    Box(modifier = Modifier.fillMaxWidth().padding(top = Space.sm), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .padding(horizontal = Space.md)
                .widthIn(max = ArOverlay.maxWidth)
                .fillMaxWidth()
                .aspectRatio(SCAN_STAGE_ASPECT)
                .shadow(elevation = SceneViewTokens.Elevation.lg, shape = shape, clip = false)
                .clip(shape)
                .background(SceneViewTokens.Stage.background)
                .testTag(SCAN_STAGE_TAG),
        ) {
            ArDebugSceneView(
                session = session,
                orbit = orbit,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                modifier = Modifier.fillMaxSize(),
                replay = media,
            )
            Box(Modifier.fillMaxSize().overMediaEdge(shape))
        }
    }
}

/**
 * Before a capture: the shutter that starts one, once ARCore has found the space. A capture in
 * progress has [ScanControls] instead.
 */
@Composable
internal fun ScanShutter(startEnabled: Boolean, onStart: () -> Unit) {
    RecordShutter(isRecording = false, startEnabled = startEnabled, onStart = onStart, onStop = {})
}

/** Wider than tall: the room reads across, and the camera keeps the lower half of the screen. */
private const val SCAN_STAGE_ASPECT = 1.35f

/** A notch above the capture card's 10 dp dot: this one is read from a metre away. */
private val ScanDotSize = Space.md - Space.xs / 2

internal const val SCAN_HUD_TAG = "ar_rerun_scan_hud"
internal const val SCAN_STAGE_TAG = "ar_rerun_scan_stage"
internal const val SCAN_POINTS_TAG = "ar_rerun_scan_points"
internal const val SCAN_CONTROLS_TAG = "ar_rerun_scan_controls"
internal const val SCAN_HINT_TAG = "ar_rerun_scan_hint"
internal const val SCAN_REVIEW_TAG = "ar_rerun_scan_review"
internal const val SCAN_SAVING_TAG = "ar_rerun_scan_saving"
internal const val SCAN_SAVE_FAILED_TAG = "ar_rerun_scan_save_failed"
internal const val SCAN_LEAVE_TAG = "ar_rerun_scan_leave"
