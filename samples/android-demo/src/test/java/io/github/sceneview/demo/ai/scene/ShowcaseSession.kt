package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.demos.internal.ArDebugTrace
import io.github.sceneview.demo.demos.internal.ReplayManifest
import io.github.sceneview.demo.demos.internal.of
import io.github.sceneview.demo.demos.internal.RerunReplayAssets
import io.github.sceneview.demo.demos.internal.parseArDebugLog
import java.io.File

/** The bundled Rerun showcase — the real recorded session the AI Scene demo replays — read from disk. */
object ShowcaseSession {
    private val dir: File by lazy {
        listOf(
            File("src/main/assets/${RerunReplayAssets.DIR}"),
            File("samples/android-demo/src/main/assets/${RerunReplayAssets.DIR}"),
        ).first { it.isDirectory }
    }

    val session: SpatialSession by lazy {
        val manifest = ReplayManifest.parse(File(dir, "showcase-manifest.json").readText())!!
        val trace = ArDebugTrace.of(File(dir, "showcase-session.jsonl").useLines { parseArDebugLog(it) })
        SpatialSession.of(trace, manifest.lens, manifest.floorY)
    }
}
