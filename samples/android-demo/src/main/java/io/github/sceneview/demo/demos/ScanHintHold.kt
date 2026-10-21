package io.github.sceneview.demo.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** How long a new scan hint must hold before it replaces the one on screen. */
internal const val SCAN_HINT_HOLD_MS = 3_000L

/**
 * [value], but a new value shows only once it has been [value] for [holdMs] in a row: one that
 * comes and goes within the hold is never shown, and one that flips back and forth keeps the
 * value on screen. Facing a wall, the side of a room's next gap flips with every sway of the
 * phone; a hint that flips with it says nothing.
 */
@Composable
internal fun <T> rememberHeld(value: T, holdMs: Long = SCAN_HINT_HOLD_MS): T {
    var shown by remember { mutableStateOf(value) }
    LaunchedEffect(value, holdMs) {
        if (value != shown) {
            delay(holdMs)
            shown = value
        }
    }
    return shown
}
