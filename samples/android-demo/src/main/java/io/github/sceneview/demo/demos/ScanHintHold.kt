package io.github.sceneview.demo.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** The least time a scan hint stays on screen before another one replaces it. */
internal const val SCAN_HINT_HOLD_MS = 3_000L

/**
 * [value], but each value it returns stays at least [holdMs]: a change that comes sooner waits
 * for the end of the hold, and one that comes and goes within it is never shown. Facing a wall,
 * the side of a room's next gap flips with every sway of the phone; a hint that flips with it
 * says nothing.
 */
@Composable
internal fun <T> rememberHeld(value: T, holdMs: Long = SCAN_HINT_HOLD_MS): T {
    val latest by rememberUpdatedState(value)
    var shown by remember { mutableStateOf(value) }
    LaunchedEffect(holdMs) {
        while (true) {
            delay(holdMs)
            shown = snapshotFlow { latest }.first { it != shown }
        }
    }
    return shown
}
