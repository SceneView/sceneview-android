package io.github.sceneview.demo.demos

import android.util.Log
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Session
import io.github.sceneview.demo.demos.internal.CameraStream
import io.github.sceneview.demo.demos.internal.CameraStreamPick
import java.util.EnumSet

/**
 * The 3D Scan's camera config: of the back streams that reach 30 fps, one that meets 1280 × 720,
 * then one fixed at 30 fps, then the largest CPU image ([CameraStreamPick]), so the photos and the
 * depth are read from the sharpest image the scan keeps up with. The session's default config stays when ARCore offers none, or throws. [line]
 * keeps the choice for the bake log, next to the image size the scan actually received: during a
 * playback, the recording's own stream wins.
 */
internal object RerunScanCamera {
    @Volatile
    var line: String = CameraStreamPick.logLine(emptyList(), null)
        private set

    fun select(session: Session): CameraConfig {
        val chosen = runCatching {
            val filter = CameraConfigFilter(session)
                .setFacingDirection(CameraConfig.FacingDirection.BACK)
                .setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
            val configs = session.getSupportedCameraConfigs(filter)
            val streams = configs.map(::streamOf)
            val pick = CameraStreamPick.pickOf(configs.zip(streams))
            line = CameraStreamPick.logLine(streams, pick?.second)
            pick?.first
        }.onFailure { line = "camera: default config, the offered streams could not be read ($it)" }.getOrNull()
        runCatching { Log.i(LOG_TAG, line) }
        return chosen ?: session.cameraConfig
    }

    private fun streamOf(config: CameraConfig) = CameraStream(
        cpuWidth = config.imageSize.width,
        cpuHeight = config.imageSize.height,
        minFps = config.fpsRange.lower,
        maxFps = config.fpsRange.upper,
        back = config.facingDirection == CameraConfig.FacingDirection.BACK,
        depthSensor = config.depthSensorUsage == CameraConfig.DepthSensorUsage.REQUIRE_AND_USE,
        gpuWidth = config.textureSize.width,
        gpuHeight = config.textureSize.height,
    )

    private const val LOG_TAG = "RerunScanCamera"
}
