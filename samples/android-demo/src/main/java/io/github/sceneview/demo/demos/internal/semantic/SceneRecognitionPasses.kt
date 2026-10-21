package io.github.sceneview.demo.demos.internal.semantic

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * No 2D detector — not on the device yet, or not usable. A recognition with it reads no photo and
 * lifts nothing, so all it finds are the walls' doors and windows, which come from the dense cloud.
 */
object NoDetector : SceneDetector {
    override val name = "none"
    override fun detect(image: SceneImage) = emptyList<Detection2D>()
}

/**
 * A scan recognised in two passes, so that what needs no detector does not wait for it. The first
 * pass runs at once with [NoDetector] and no photo read: the doors and windows. The second runs
 * once [loadDetector] has the detector — a 20 MB download the first time, which may take minutes
 * or never end — and finds everything, the furniture included, with the first pass's openings
 * kept as they were. [loadDetector] starts before the first pass and runs alongside it; `null`
 * from it means no second pass.
 *
 * [recognize] runs a pass with the recognizer it is given (`null` when the pass failed); [onPass]
 * hears each pass that came out, in order. The detector is closed once its pass is done, or when
 * the caller is cancelled.
 */
suspend fun recognizeInPasses(
    loadDetector: suspend () -> SceneDetector?,
    recognize: suspend (SceneRecognizer) -> SceneRecognition?,
    onPass: (SceneRecognition) -> Unit,
    openings: WallOpenings = CloudWallOpenings(),
) {
    var loaded: SceneDetector? = null
    try {
        coroutineScope {
            val loading = launch(start = CoroutineStart.UNDISPATCHED) { loaded = loadDetector() }
            val first = recognize(SceneRecognizer(NoDetector, openings, maxFrames = 0))
            first?.let(onPass)
            loading.join()
            val detector = loaded ?: return@coroutineScope
            val found = first?.openings
            val kept = if (found != null) WallOpenings { _, _ -> found } else openings
            recognize(SceneRecognizer(detector, kept))?.let(onPass)
        }
    } finally {
        loaded?.close()
    }
}
