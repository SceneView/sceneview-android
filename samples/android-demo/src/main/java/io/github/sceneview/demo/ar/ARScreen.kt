@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.ar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.demo.R
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay

private data class ARModel(
    val label: String,
    val assetFile: String,
    val scaleToUnits: Float,
    val scaleRange: ClosedFloatingPointRange<Float>
)

private val arModels = listOf(
    ARModel("Toy Car", "models/toy_car.glb", 0.4f, 0.15f..0.8f),
    ARModel("Space Helmet", "models/space_helmet.glb", 0.4f, 0.15f..0.8f),
    ARModel("Chair", "models/sheen_chair.glb", 0.3f, 0.1f..0.6f),
    ARModel("Mask", "models/geisha_mask.glb", 0.3f, 0.1f..0.6f),
    ARModel("Lamp", "models/iridescence_lamp.glb", 0.3f, 0.1f..0.6f),
    ARModel("Seal", "models/seal_statuette.glb", 0.3f, 0.1f..0.6f),
)

@Composable
fun ARScreen() {
    val context = LocalContext.current
    val arAvailability = remember {
        try {
            ArCoreApk.getInstance().checkAvailability(context)
        } catch (e: Exception) {
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        }
    }

    if (!arAvailability.isSupported) {
        ARNotAvailableScreen()
        return
    }

    val arSceneDescription = stringResource(R.string.cd_ar_scene)
    Box(modifier = Modifier
        .fillMaxSize()
        .semantics { contentDescription = arSceneDescription }
    ) {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val materialLoader = rememberMaterialLoader(engine)
        val cameraNode = rememberARCameraNode(engine)
        val view = rememberView(engine)
        val collisionSystem = rememberCollisionSystem(view)

        var selectedModel by remember { mutableStateOf(arModels[0]) }
        var anchor by remember { mutableStateOf<Anchor?>(null) }
        var frame by remember { mutableStateOf<Frame?>(null) }
        var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
        var showGestureHint by remember { mutableStateOf(false) }

        val modelInstance = rememberModelInstance(modelLoader, selectedModel.assetFile)

        val clearAnchor = { anchor?.detach(); anchor = null }

        // Detach anchor when composable leaves composition
        DisposableEffect(Unit) {
            onDispose { anchor?.detach() }
        }

        // Show gesture hints briefly after the model is placed
        LaunchedEffect(anchor) {
            if (anchor != null) {
                showGestureHint = true
                delay(4000)
                showGestureHint = false
            }
        }

        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            collisionSystem = collisionSystem,
            cameraNode = cameraNode,
            planeRenderer = true,
            sessionConfiguration = { session, config ->
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            onTrackingFailureChanged = { trackingFailureReason = it },
            onSessionUpdated = { _, updatedFrame ->
                frame = updatedFrame
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { motionEvent, node ->
                    if (node == null) {
                        frame?.hitTest(motionEvent.x, motionEvent.y)
                            ?.firstOrNull { it.isValid(depthPoint = false, point = false) }
                            ?.createAnchorOrNull()
                            ?.let { anchor = it }
                    }
                }
            )
        ) {
            anchor?.let {
                AnchorNode(anchor = it) {
                    modelInstance?.let { instance ->
                        ModelNode(
                            modelInstance = instance,
                            scaleToUnits = selectedModel.scaleToUnits,
                            isEditable = true,
                            apply = { editableScaleRange = selectedModel.scaleRange }
                        )
                    }
                }
            }
        }

        // Scanning reticle — visible while searching for a surface
        AnimatedVisibility(
            visible = anchor == null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(400))
        ) {
            ScanningReticle()
        }

        // Status pill — top center
        AnimatedContent(
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            targetState = trackingFailureReason?.getDescription(LocalContext.current)
                ?: if (anchor == null) stringResource(R.string.ar_scan_surface)
                else stringResource(R.string.ar_model_placed),
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "StatusText"
        ) { text ->
            if (text.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Gesture hint — appears briefly after placement
        AnimatedVisibility(
            visible = showGestureHint,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500))
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.ar_drag_to_move), color = Color.White, fontSize = 13.sp)
                    Text(stringResource(R.string.ar_pinch_to_scale), color = Color.White, fontSize = 13.sp)
                    Text(stringResource(R.string.ar_twist_to_rotate), color = Color.White, fontSize = 13.sp)
                }
            }
        }

        // Bottom bar — model picker + remove button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Remove button
            AnimatedVisibility(visible = anchor != null) {
                OutlinedButton(
                    onClick = { clearAnchor() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_remove_model),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "  " + stringResource(R.string.ar_remove),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Model picker chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                arModels.forEach { model ->
                    FilterChip(
                        selected = selectedModel == model,
                        onClick = {
                            if (selectedModel != model) {
                                clearAnchor()
                                selectedModel = model
                            }
                        },
                        label = {
                            Text(
                                text = model.label,
                                fontWeight = if (selectedModel == model) FontWeight.SemiBold
                                else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            labelColor = Color.White.copy(alpha = 0.8f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedModel == model,
                            borderColor = Color.White.copy(alpha = 0.35f),
                            selectedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(50)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanningReticle(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ScanReticle")
    val animSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(1400, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
    val pulse by transition.animateFloat(0.72f, 1f, animSpec, label = "Pulse")
    val ringAlpha by transition.animateFloat(0.25f, 0.65f, animSpec, label = "RingAlpha")

    Canvas(modifier = modifier.size(220.dp)) {
        val strokeWidth = 2.dp.toPx()
        val cornerLen = 26.dp.toPx()
        val cx = center.x
        val cy = center.y
        val half = size.minDimension * 0.42f

        drawCircle(
            color = Color.White.copy(alpha = ringAlpha),
            radius = size.minDimension / 2 * pulse,
            style = Stroke(strokeWidth)
        )

        fun bracket(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(
            color = Color.White,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = strokeWidth * 1.5f,
            cap = StrokeCap.Round
        )

        bracket(cx - half, cy - half, cx - half + cornerLen, cy - half)
        bracket(cx - half, cy - half, cx - half, cy - half + cornerLen)
        bracket(cx + half, cy - half, cx + half - cornerLen, cy - half)
        bracket(cx + half, cy - half, cx + half, cy - half + cornerLen)
        bracket(cx - half, cy + half, cx - half + cornerLen, cy + half)
        bracket(cx - half, cy + half, cx - half, cy + half - cornerLen)
        bracket(cx + half, cy + half, cx + half - cornerLen, cy + half)
        bracket(cx + half, cy + half, cx + half, cy + half - cornerLen)

        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 3.dp.toPx())
    }
}

@Composable
private fun ARNotAvailableScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.VideocamOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Text(
                text = stringResource(R.string.ar_not_available_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.ar_not_available_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
