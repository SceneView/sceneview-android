package io.github.sceneview.demo.demos.internal

/**
 * One camera stream ARCore offers, reduced to what the 3D Scan's choice reads: the CPU image
 * (the one the photos and the depth are read from), the frame-rate range, the facing, whether the
 * depth sensor is in use, and the GPU texture the preview draws.
 */
data class CameraStream(
    val cpuWidth: Int,
    val cpuHeight: Int,
    val minFps: Int,
    val maxFps: Int,
    val back: Boolean = true,
    val depthSensor: Boolean = false,
    val gpuWidth: Int = 0,
    val gpuHeight: Int = 0,
) {
    val cpuPixels: Long get() = cpuWidth.toLong() * cpuHeight
    val gpuPixels: Long get() = gpuWidth.toLong() * gpuHeight
}

/**
 * The 3D Scan's camera stream. Of what ARCore offers, the back stream whose CPU image is the
 * largest while the frame rate holds 30 fps: a stream fixed at 30 fps first, then one that may
 * drop below it, never one that only runs faster. The depth sensor, then the larger GPU texture,
 * break a tie. No stream to pick keeps the session's default config.
 */
object CameraStreamPick {
    const val FPS = 30

    /** The image the scan aims for, on the long and the short side: 1280 × 720. */
    const val TARGET_LONG = 1280
    const val TARGET_SHORT = 720

    private val ORDER = compareBy<CameraStream>(
        { it.minFps >= FPS },
        { it.cpuPixels },
        { it.depthSensor },
        { it.gpuPixels },
    )

    fun pick(streams: List<CameraStream>): CameraStream? = streams.filter(::eligible).maxWithOrNull(ORDER)

    fun eligible(stream: CameraStream): Boolean =
        stream.back && stream.minFps <= FPS && stream.maxFps >= FPS && stream.cpuPixels > 0

    fun meetsTarget(stream: CameraStream): Boolean =
        maxOf(stream.cpuWidth, stream.cpuHeight) >= TARGET_LONG && minOf(stream.cpuWidth, stream.cpuHeight) >= TARGET_SHORT

    /** The choice in one line for the bake log: the stream, what was offered, and the target. */
    fun logLine(offered: List<CameraStream>, chosen: CameraStream?): String {
        val list = if (offered.isEmpty()) "" else offered.map(::label).distinct().joinToString(", ", " [", "]")
        if (chosen == null) return "camera: default config, none of ${offered.size} offered holds $FPS fps$list"
        val extras = listOfNotNull(
            "gpu ${chosen.gpuWidth}x${chosen.gpuHeight}".takeIf { chosen.gpuPixels > 0 },
            "depth sensor".takeIf { chosen.depthSensor },
        )
        val details = if (extras.isEmpty()) "" else extras.joinToString(", ", " (", ")")
        val target = if (meetsTarget(chosen)) "meets" else "below"
        return "camera: ${chosen.cpuWidth}x${chosen.cpuHeight} at ${fps(chosen)} fps$details, " +
            "picked from ${offered.size} offered$list; $target ${TARGET_LONG}x$TARGET_SHORT"
    }

    private fun label(stream: CameraStream): String =
        "${stream.cpuWidth}x${stream.cpuHeight} ${fps(stream)}" + if (stream.back) "" else " front"

    private fun fps(stream: CameraStream): String =
        if (stream.minFps == stream.maxFps) "${stream.maxFps}" else "${stream.minFps}-${stream.maxFps}"
}
