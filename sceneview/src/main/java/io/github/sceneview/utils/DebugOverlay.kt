package io.github.sceneview.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.ExperimentalSceneViewApi
import kotlinx.coroutines.delay

/**
 * Real-time performance statistics for a running [io.github.sceneview.Scene].
 *
 * Create with [rememberDebugStats] and update from the `onFrame` callback. The properties
 * are observable Compose state values that automatically trigger recomposition of any
 * [DebugOverlay] reading them.
 *
 * @property fps             Smoothed frames-per-second estimate.
 * @property frameTimeMs     Duration of the last rendered frame in milliseconds.
 * @property nodeCount       Total number of nodes in the scene graph (user-managed count).
 */
@ExperimentalSceneViewApi
class DebugStats {
    internal var _fps = mutableFloatStateOf(0f)
    internal var _frameTimeMs = mutableFloatStateOf(0f)
    internal var _nodeCount = mutableIntStateOf(0)
    internal var _lastFrameNanos = mutableLongStateOf(0L)
    internal var _frameCount = mutableIntStateOf(0)
    internal var _fpsAccumulator = mutableFloatStateOf(0f)

    /** Smoothed frames-per-second estimate. */
    val fps: Float get() = _fps.floatValue

    /** Duration of the last rendered frame in milliseconds. */
    val frameTimeMs: Float get() = _frameTimeMs.floatValue

    /** Total number of nodes in the scene (as last reported). */
    val nodeCount: Int get() = _nodeCount.intValue

    /**
     * Call this once per frame from the `onFrame` callback to update timing stats.
     *
     * ```kotlin
     * val stats = rememberDebugStats()
     * Scene(onFrame = { frameNanos -> stats.onFrame(frameNanos, nodeCount = 12) }) { ... }
     * ```
     */
    fun onFrame(frameTimeNanos: Long, nodeCount: Int = 0) {
        val lastNanos = _lastFrameNanos.longValue
        if (lastNanos > 0) {
            val deltaNanos = frameTimeNanos - lastNanos
            val deltaMs = deltaNanos / 1_000_000f
            _frameTimeMs.floatValue = deltaMs
            _fpsAccumulator.floatValue += if (deltaMs > 0f) 1000f / deltaMs else 0f
            _frameCount.intValue++

            // Update FPS as a rolling average every 30 frames.
            if (_frameCount.intValue >= 30) {
                _fps.floatValue = _fpsAccumulator.floatValue / _frameCount.intValue
                _frameCount.intValue = 0
                _fpsAccumulator.floatValue = 0f
            }
        }
        _lastFrameNanos.longValue = frameTimeNanos
        _nodeCount.intValue = nodeCount
    }
}

/**
 * Creates and remembers a [DebugStats] instance.
 *
 * Wire it into the `Scene` `onFrame` callback:
 * ```kotlin
 * val stats = rememberDebugStats()
 * Scene(onFrame = { stats.onFrame(it, nodeCount = 5) }) { ... }
 * DebugOverlay(stats)
 * ```
 */
@ExperimentalSceneViewApi
@Composable
fun rememberDebugStats(): DebugStats = remember { DebugStats() }

/**
 * A semi-transparent overlay that displays real-time performance metrics.
 *
 * Place this composable alongside (not inside) a `Scene { }` composable, typically in a `Box`:
 *
 * ```kotlin
 * Box {
 *     Scene(onFrame = { stats.onFrame(it) }) { ... }
 *     DebugOverlay(stats, modifier = Modifier.align(Alignment.TopStart))
 * }
 * ```
 *
 * Displays:
 * - **FPS** — smoothed frames per second
 * - **Frame** — last frame time in milliseconds
 * - **Nodes** — total scene node count
 *
 * @param stats    The [DebugStats] instance updated from `onFrame`.
 * @param modifier Modifier for positioning and sizing the overlay.
 */
@ExperimentalSceneViewApi
@Composable
fun DebugOverlay(
    stats: DebugStats,
    modifier: Modifier = Modifier
) {
    // Force periodic recomposition to keep the display fresh even if no other state changes.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            tick++
        }
    }
    // Read tick to trigger recomposition
    @Suppress("UNUSED_EXPRESSION")
    tick

    Column(
        modifier = modifier
            .background(Color(0xAA000000.toInt()))
            .padding(8.dp)
    ) {
        val monoFamily = FontFamily.Monospace

        BasicText(
            text = "FPS: %.1f".format(stats.fps),
            style = TextStyle(
                color = if (stats.fps >= 55f) Color.Green
                else if (stats.fps >= 30f) Color.Yellow
                else Color.Red,
                fontSize = 12.sp,
                fontFamily = monoFamily
            )
        )
        BasicText(
            text = "Frame: %.1f ms".format(stats.frameTimeMs),
            style = TextStyle(
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = monoFamily
            )
        )
        BasicText(
            text = "Nodes: %d".format(stats.nodeCount),
            style = TextStyle(
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = monoFamily
            )
        )
    }
}
