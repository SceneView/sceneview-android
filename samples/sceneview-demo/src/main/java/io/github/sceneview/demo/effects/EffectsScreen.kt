@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.effects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.DynamicSkyNode
import io.github.sceneview.node.FogNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView

@Composable
fun EffectsScreen() {
    // ── Effect state ──────────────────────────────────────────────────────────
    var timeOfDay by remember { mutableFloatStateOf(12f) }
    var isAnimatingTime by remember { mutableStateOf(false) }
    var turbidity by remember { mutableFloatStateOf(3f) }
    var fogEnabled by remember { mutableStateOf(false) }
    var fogDensity by remember { mutableFloatStateOf(0.04f) }
    var ssaoEnabled by remember { mutableStateOf(false) }

    // ── Scene resources ───────────────────────────────────────────────────────
    val engine = rememberEngine()
    val view = rememberView(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(z = 3.5f, y = 0.5f)
        lookAt(Float3(0f, 0f, 0f))
    }
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("environments/outdoor_cloudy_2k.hdr")
            ?: environmentLoader.createHDREnvironment("environments/rooftop_night_2k.hdr")!!
    }
    val modelInstance = rememberModelInstance(modelLoader, "models/space_helmet.glb")

    // ── Auto time-of-day animation (5 AM → 21 PM → 5 AM loop) ───────────────
    val timeTransition = rememberInfiniteTransition(label = "TimeOfDay")
    val animatedTime by timeTransition.animateFloat(
        initialValue = 5f,
        targetValue = 21f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animatedTime"
    )
    val effectiveTime = if (isAnimatingTime) animatedTime else timeOfDay

    // ── Apply view-level effects reactively ───────────────────────────────────
    SideEffect {
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled = ssaoEnabled
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Full-screen 3D Scene ─────────────────────────────────────────────
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            environment = environment
        ) {
            DynamicSkyNode(
                timeOfDay = effectiveTime,
                turbidity = turbidity
            )
            if (fogEnabled) {
                FogNode(
                    view = view,
                    density = fogDensity,
                    height = 2f,
                    color = Color(0xFFCCDDFF),
                    enabled = true
                )
            }
            modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.8f,
                    autoAnimate = false
                )
            }
        }

        // ── Top overlay ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = "Effects",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(formatHour(effectiveTime))
                        if (isAnimatingTime) append(" · Auto")
                        if (fogEnabled) append(" · Fog")
                        if (ssaoEnabled) append(" · SSAO")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        // ── Bottom effects panel ─────────────────────────────────────────────
        EffectsPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            timeOfDay = timeOfDay,
            isAnimatingTime = isAnimatingTime,
            onTimeChange = { timeOfDay = it },
            onToggleAnimate = { isAnimatingTime = !isAnimatingTime },
            turbidity = turbidity,
            onTurbidityChange = { turbidity = it },
            fogEnabled = fogEnabled,
            onFogToggle = { fogEnabled = it },
            fogDensity = fogDensity,
            onFogDensityChange = { fogDensity = it },
            ssaoEnabled = ssaoEnabled,
            onSsaoToggle = { ssaoEnabled = it }
        )
    }
}

@Composable
private fun EffectsPanel(
    modifier: Modifier = Modifier,
    timeOfDay: Float,
    isAnimatingTime: Boolean,
    onTimeChange: (Float) -> Unit,
    onToggleAnimate: () -> Unit,
    turbidity: Float,
    onTurbidityChange: (Float) -> Unit,
    fogEnabled: Boolean,
    onFogToggle: (Boolean) -> Unit,
    fogDensity: Float,
    onFogDensityChange: (Float) -> Unit,
    ssaoEnabled: Boolean,
    onSsaoToggle: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                )
            )
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Sky card ─────────────────────────────────────────────────────────
        EffectCard(
            icon = Icons.Default.WbSunny,
            title = "Dynamic Sky",
            subtitle = "Sun position and atmospheric haze",
            alwaysExpanded = true
        ) {
            // Time of day row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.width(56.dp)
                )
                Slider(
                    value = if (isAnimatingTime) timeOfDay else timeOfDay,
                    onValueChange = { if (!isAnimatingTime) onTimeChange(it) },
                    valueRange = 0f..24f,
                    enabled = !isAnimatingTime,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        disabledThumbColor = Color.White.copy(alpha = 0.4f),
                        disabledActiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onToggleAnimate,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isAnimatingTime) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        }
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isAnimatingTime) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAnimatingTime) "Pause" else "Auto-animate",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // Turbidity row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Haze",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.width(56.dp)
                )
                Slider(
                    value = turbidity,
                    onValueChange = onTurbidityChange,
                    valueRange = 1f..10f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "%.1f".format(turbidity),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(28.dp)
                )
            }
        }

        // ── Fog card ─────────────────────────────────────────────────────────
        EffectCard(
            icon = Icons.Default.CloudQueue,
            title = "Atmospheric Fog",
            subtitle = "Height-based volumetric fog",
            enabled = fogEnabled,
            onToggle = onFogToggle
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Density",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.width(56.dp)
                )
                Slider(
                    value = fogDensity,
                    onValueChange = onFogDensityChange,
                    valueRange = 0.01f..0.3f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "%.2f".format(fogDensity),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.width(32.dp)
                )
            }
        }

        // ── SSAO card ────────────────────────────────────────────────────────
        EffectCard(
            icon = Icons.Default.WbSunny,
            title = "Ambient Occlusion",
            subtitle = "Screen-space contact shadows (SSAO)",
            enabled = ssaoEnabled,
            onToggle = onSsaoToggle,
            alwaysExpanded = false
        )
    }
}

@Composable
private fun EffectCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onToggle: ((Boolean) -> Unit)? = null,
    alwaysExpanded: Boolean = true,
    content: (@Composable () -> Unit)? = null
) {
    val isExpanded = alwaysExpanded || enabled

    val containerAlpha by animateColorAsState(
        targetValue = if (enabled || alwaysExpanded) {
            Color.White.copy(alpha = 0.12f)
        } else {
            Color.White.copy(alpha = 0.06f)
        },
        label = "cardAlpha"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerAlpha
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White.copy(alpha = if (enabled || alwaysExpanded) 0.9f else 0.4f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = if (enabled || alwaysExpanded) 1f else 0.5f)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                if (onToggle != null) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            if (content != null) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

private fun formatHour(hour: Float): String {
    val h = hour.toInt().coerceIn(0, 23)
    val m = ((hour - hour.toInt()) * 60).toInt()
    val amPm = if (h < 12) "AM" else "PM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "%d:%02d %s".format(h12, m, amPm)
}
