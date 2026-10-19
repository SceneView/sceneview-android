package io.github.sceneview.camera

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.length
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [CinematicCameraManipulator]: the animator owns the camera while playing, a touch hands over without a cut. */
class CinematicCameraManipulatorTest {

    private val home = CameraShot(Position(0f, 0f, 0f), 35f, 32f, 3f)

    /** A manipulator that keeps its own pose, like the default orbit manipulator. */
    private open class FixedInner(var pose: CameraShot) : CameraGestureDetector.CameraManipulator {
        var grabs = 0
        var scrolls = 0
        var updates = 0
        var transforms = 0
        override fun setViewport(width: Int, height: Int) = Unit
        override fun getTransform(): Transform {
            transforms++
            return pose.transform()
        }
        override fun grabBegin(x: Int, y: Int, strafe: Boolean) { grabs++ }
        override fun grabUpdate(x: Int, y: Int) = Unit
        override fun grabEnd() = Unit
        override fun scrollBegin(x: Int, y: Int, separation: Float) { scrolls++ }
        override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) = Unit
        override fun scrollEnd() = Unit
        override fun doubleTapZoom(x: Int, y: Int, zoomIn: Boolean) = Unit
        override fun update(deltaTime: Float) { updates++ }
    }

    /** A manipulator that can be handed the camera: it just records what it got. */
    private class SinkInner(pose: CameraShot) : FixedInner(pose), CameraShotSink {
        var handed: Pair<CameraShot, CameraShotVelocity>? = null
        override fun takeOver(shot: CameraShot, velocity: CameraShotVelocity) {
            handed = shot to velocity
            pose = shot
        }
    }

    @Test
    fun atRestItIsTheInner() {
        val inner = FixedInner(home)
        val manipulator = CinematicCameraManipulator(CameraAnimator(home), inner)
        assertTrue(!manipulator.isPlaying)
        assertEquals(home.transform(), manipulator.getTransform())
        assertEquals(1, inner.transforms)
        manipulator.update(1f / 60f)
        assertEquals(1, inner.updates)
    }

    @Test
    fun whilePlayingTheAnimatorOwnsTheCamera() {
        val inner = FixedInner(home)
        val animator = CameraAnimator(home)
        val manipulator = CinematicCameraManipulator(animator, inner)
        animator.play(CameraPath.tour(home, durationSeconds = 8f))
        manipulator.update(1f / 60f)
        val transform = manipulator.getTransform()
        assertEquals(animator.shot.transform(), transform)
        assertEquals(0, inner.transforms)
        assertTrue(manipulator.isPlaying)
        // The inner still ticks, so its own inertia settles.
        assertEquals(1, inner.updates)
    }

    @Test
    fun aTouchHandsShotAndVelocityToASink() {
        val inner = SinkInner(home)
        val animator = CameraAnimator(home)
        val manipulator = CinematicCameraManipulator(animator, inner)
        animator.play(CameraPath.tour(home, durationSeconds = 8f))
        repeat(30) { manipulator.update(1f / 60f) }
        val onScreen = animator.shot
        manipulator.grabBegin(10, 10, strafe = false)
        val (shot, velocity) = checkNotNull(inner.handed)
        assertEquals(onScreen, shot)
        assertEquals(45f, velocity.azimuthDegreesPerSecond, 1.5f)
        assertEquals(1, inner.grabs)
        assertTrue(!manipulator.isPlaying)
        // No cut: the next frame is the sink's pose, which is the shot it was handed.
        assertEquals(onScreen.transform(), manipulator.getTransform())
    }

    @Test
    fun aTouchGlidesOntoAManipulatorThatKeepsItsOwnPose() {
        val elsewhere = home.copy(azimuthDegrees = 180f, distance = 6f)
        val inner = FixedInner(elsewhere)
        val animator = CameraAnimator(home)
        val manipulator = CinematicCameraManipulator(animator, inner)
        animator.play(CameraPath.reveal(home, durationSeconds = 2f))
        repeat(20) {
            manipulator.update(1f / 60f)
            manipulator.getTransform()
        }
        val lastShown = eyeOf(manipulator.getTransform())
        manipulator.scrollBegin(0, 0, 10f)
        assertEquals(1, inner.scrolls)
        // First frame after the touch is still where the move was — no cut.
        val first = eyeOf(manipulator.getTransform())
        assertTrue(length(first - lastShown) < 0.05f)
        // Then it glides onto the inner's pose and stays there.
        var previousGap = length(first - elsewhere.eye)
        repeat(30) {
            manipulator.update(1f / 60f)
            val gap = length(eyeOf(manipulator.getTransform()) - elsewhere.eye)
            assertTrue(gap <= previousGap + 1e-4f)
            previousGap = gap
        }
        assertTrue(previousGap < 0.05f)
        assertEquals(elsewhere.transform(), manipulator.getTransform())
    }

    @Test
    fun interruptAtRestDoesNothing() {
        val inner = SinkInner(home)
        val manipulator = CinematicCameraManipulator(CameraAnimator(home), inner)
        manipulator.interrupt()
        manipulator.doubleTapZoom(0, 0, zoomIn = true)
        assertNull(inner.handed)
    }

    private fun eyeOf(transform: Transform) = Float3(transform[3].x, transform[3].y, transform[3].z)
}
