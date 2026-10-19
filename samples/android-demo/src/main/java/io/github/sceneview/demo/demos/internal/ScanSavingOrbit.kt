package io.github.sceneview.demo.demos.internal

import io.github.sceneview.math.Position

/**
 * The saving orbit of one save (#4719): started on its first frame from the phone's eye of the
 * moment — the last one ARCore gave the live view ([ScanLens]), else the follow pose — round the
 * cloud's centre and reach read once, since the cloud no longer grows while saving.
 *
 * Seeded on the eye, the orbit's first pose stands where the phone stood, looking where it looked
 * ([ScanFollow.eyePose]), and the camera is put on it at once: the view leaves the camera image
 * without a cut ([EyeHandoff]), then eases out onto the orbit. A view a drag had taken off the
 * phone sets out from where it is instead. [nanoTime] is the clock, a fake one in tests.
 */
class ScanSavingOrbit(private val nanoTime: () -> Long = System::nanoTime) {
    private var from: OrbitPose? = null
    private var started = 0L
    private var centre: Position? = null
    private var radius = 0f

    /** The pose the orbit set out from, once it has: the eye it was seeded with. */
    val start: OrbitPose? get() = from

    fun poseFor(phone: OrbitPose?, points: FloatArray, orbit: ArDebugOrbitCamera): OrbitPose? {
        val around = centre ?: (ScanFollow.cloudCentre(points) ?: return phone).also {
            centre = it
            radius = ScanFollow.cloudRadius(points, it)
        }
        val start = from ?: seed(phone, around, orbit).also {
            from = it
            started = nanoTime()
        }
        val seconds = (nanoTime() - started) / NS_PER_S
        return ScanFollow.savingPose(start, around, radius, seconds, orbit.verticalFovDegrees)
    }

    /** The eye, the camera put on it at once, while the view still is the phone's; else as is. */
    private fun seed(phone: OrbitPose?, around: Position, orbit: ArDebugOrbitCamera): OrbitPose {
        val eye = orbit.firstPerson?.takeIf { it.held && orbit.seesThroughEye }
            ?: return phone ?: orbit.pose
        return ScanFollow.eyePose(eye.cameraToWorld, around).also { orbit.snapTo(it) }
    }

    private companion object {
        const val NS_PER_S = 1e9f
    }
}
