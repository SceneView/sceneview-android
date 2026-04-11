package io.github.sceneview.ar.rerun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Creates and remembers a [RerunBridge] tied to the composable lifecycle.
 *
 * The bridge opens its TCP socket in a [LaunchedEffect] when `enabled`
 * flips to true, and closes it in [DisposableEffect]'s `onDispose` when
 * the composable leaves the tree. Passing `enabled = false` disables the
 * bridge entirely — no socket is opened, and every `log*` call is a no-op.
 *
 * ### Typical usage
 *
 * ```kotlin
 * @Composable
 * fun MyARScreen() {
 *     val bridge = rememberRerunBridge(
 *         host = "127.0.0.1",
 *         port = 9876,
 *         rateHz = 10,
 *         enabled = BuildConfig.DEBUG,
 *     )
 *     ARSceneView(
 *         modifier = Modifier.fillMaxSize(),
 *         onSessionUpdated = { session, frame -> bridge.logFrame(session, frame) },
 *     )
 * }
 * ```
 *
 * ### Why a helper
 *
 * Direct instantiation would leak the socket if the composable is disposed
 * before [RerunBridge.disconnect] is called. Wrapping in [DisposableEffect]
 * guarantees the socket is released on every recomposition that changes
 * the host/port/enabled tuple, and on composable removal.
 *
 * @param host Host to connect to. Default `127.0.0.1` (via `adb reverse`).
 * @param port TCP port the Python sidecar listens on. Default `9876`.
 * @param rateHz Maximum frames per second sent. Default `10`.
 * @param enabled When `false`, returns a no-op bridge — safe to wire
 *   unconditionally in release builds and gate with `BuildConfig.DEBUG`.
 */
@Composable
public fun rememberRerunBridge(
    host: String = RerunBridge.DEFAULT_HOST,
    port: Int = RerunBridge.DEFAULT_PORT,
    rateHz: Int = RerunBridge.DEFAULT_RATE_HZ,
    enabled: Boolean = true,
): RerunBridge {
    // Recompose the bridge when any connection-defining param changes.
    val bridge = remember(host, port, rateHz) {
        RerunBridge(host = host, port = port, rateHz = rateHz)
    }

    // Keep the bridge's runtime enabled flag in sync without rebuilding it.
    // `enabled` is intentionally not a key of `remember` so toggling the
    // flag doesn't drop/reopen the socket — only the hot-path short-circuit.
    DisposableEffect(bridge, enabled) {
        bridge.setEnabled(enabled)
        if (enabled) {
            bridge.connect()
            onDispose { bridge.disconnect() }
        } else {
            bridge.disconnect()
            onDispose { /* already disconnected */ }
        }
    }

    return bridge
}
