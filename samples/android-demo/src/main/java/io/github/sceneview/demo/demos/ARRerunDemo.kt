package io.github.sceneview.demo.demos

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rerun.RerunBridge
import io.github.sceneview.ar.rerun.rememberRerunBridge
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.math.Position
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener

/**
 * AR debug streaming to Rerun.io demo.
 *
 * Streams ARCore session data (camera pose, planes, point cloud) to a Rerun viewer via TCP.
 * Uses [rememberRerunBridge] to manage the connection lifecycle.
 *
 * Setup:
 * 1. Run `python tools/rerun-bridge.py` on your dev machine
 * 2. Run `adb reverse tcp:9876 tcp:9876`
 * 3. Open the Rerun viewer
 * 4. Launch this demo and tap "Connect"
 */
@Composable
fun ARRerunDemo(onBack: () -> Unit) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    var host by remember { mutableStateOf(RerunBridge.DEFAULT_HOST) }
    var port by remember { mutableStateOf(RerunBridge.DEFAULT_PORT.toString()) }
    var isConnected by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(false) }
    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var frameCount by remember { mutableStateOf(0L) }
    var latestFrame by remember { mutableStateOf<Frame?>(null) }
    val anchors = remember { mutableStateListOf<Anchor>() }

    val modelInstance = rememberModelInstance(modelLoader, "models/khronos_damaged_helmet.glb")

    // The bridge is created once and connects/disconnects based on the `enabled` flag.
    val bridge = rememberRerunBridge(
        host = host,
        port = port.toIntOrNull() ?: RerunBridge.DEFAULT_PORT,
        rateHz = 10,
        enabled = isConnected
    )

    DemoScaffold(
        title = "Rerun Debug",
        onBack = onBack,
        controls = {
            Text("Rerun Connection", style = MaterialTheme.typography.labelLarge)

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Server IP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnected
            )

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnected
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isConnected) {
                    Button(
                        onClick = { isConnected = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect")
                    }
                } else {
                    OutlinedButton(
                        onClick = { isConnected = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Disconnect")
                    }
                }
            }

            if (isConnected) {
                Text(
                    text = "Streaming \u2014 $frameCount frames sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ARSceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                planeRenderer = true,
                sessionConfiguration = { _: Session, config: Config ->
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                },
                onSessionUpdated = { session: Session, frame: Frame ->
                    latestFrame = frame
                    isTracking = frame.camera.trackingState == TrackingState.TRACKING
                    // Stream every frame to Rerun (bridge handles rate limiting internally)
                    bridge.logFrame(session, frame)
                    if (isConnected) frameCount++
                },
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
                                result.distance <= 5.0f
                        }
                        if (hit != null) {
                            anchors.add(hit.createAnchor())
                        }
                    }
                )
            ) {
                anchors.forEach { anchor ->
                    AnchorNode(anchor = anchor) {
                        modelInstance?.let { instance ->
                            ModelNode(
                                modelInstance = instance,
                                scaleToUnits = 0.3f,
                                centerOrigin = Position(0f, 0f, 0f)
                            )
                        }
                    }
                }
            }

            // Status overlay
            AnimatedVisibility(
                visible = !isTracking,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(
                    text = trackingFailureReason?.let { reason ->
                        when (reason) {
                            TrackingFailureReason.NONE -> "Point your camera at a surface"
                            TrackingFailureReason.BAD_STATE -> "AR session error"
                            TrackingFailureReason.INSUFFICIENT_LIGHT -> "Not enough light"
                            TrackingFailureReason.EXCESSIVE_MOTION -> "Moving too fast"
                            TrackingFailureReason.INSUFFICIENT_FEATURES ->
                                "Not enough detail \u2014 try a textured surface"
                            TrackingFailureReason.CAMERA_UNAVAILABLE -> "Camera unavailable"
                        }
                    } ?: "Scanning for surfaces\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}
