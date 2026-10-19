package io.github.sceneview.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.sceneview.TransformBlend
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.math.Transform

/**
 * A gesture manipulator that can be handed the camera mid-move: where a cinematic stops, the
 * gesture starts from that very [shot], at that very [velocity], so a drag on a turning camera
 * keeps turning under the finger. Implement it on an orbit manipulator that keeps its own pose
 * and inertia; [CinematicCameraManipulator] calls it on the first touch of a playing move.
 */
interface CameraShotSink {
    /** The camera is yours from [shot], moving at [velocity]. */
    fun takeOver(shot: CameraShot, velocity: CameraShotVelocity)
}

/**
 * Cinematic moves over a gesture camera, in one `cameraManipulator`.
 *
 * While the [animator] plays, the camera is its [CameraAnimator.shot]; the first touch
 * interrupts the move and the [inner] manipulator takes over, without a cut:
 *
 * - an [inner] that is a [CameraShotSink] is handed the shot and the velocity, and carries on
 *   from there — the demo app's orbit camera does this;
 * - any other manipulator (the default orbit manipulator included) keeps its own pose, so the
 *   camera glides from the move's last shot onto that pose over [handoverSeconds], following it
 *   as the finger drags, the same glide a `cameraManipulator` swap gets.
 *
 * `getTransform()` is called once per frame by the render loop and only then, as the
 * [CameraGestureDetector.CameraManipulator] contract requires; this calls [inner]'s at most
 * once per frame too.
 *
 * ```kotlin
 * val animator = remember { CameraAnimator(initial = home) }
 * SceneView(cameraManipulator = rememberCinematicCameraManipulator(animator)) { … }
 * LaunchedEffect(model) { animator.play(CameraPath.reveal(home)) }
 * ```
 */
class CinematicCameraManipulator(
    val animator: CameraAnimator,
    val inner: CameraGestureDetector.CameraManipulator,
    private val handoverSeconds: Float = HANDOVER_SECONDS,
) : CameraGestureDetector.CameraManipulator {

    private var blend: TransformBlend? = null

    /** The last transform shown while the animator played: where a handover glide starts. */
    private var lastAnimated: Transform? = null

    /** `true` while the animator owns the camera. */
    val isPlaying: Boolean get() = animator.isPlaying

    override fun setViewport(width: Int, height: Int) = inner.setViewport(width, height)

    override fun getTransform(): Transform {
        if (animator.isPlaying) {
            blend = null
            return animator.shot.transform().also { lastAnimated = it }
        }
        val live = inner.getTransform()
        val glide = blend ?: return live
        if (glide.isFinished) {
            blend = null
            return live
        }
        return glide.sample(live)
    }

    override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
        interrupt()
        inner.grabBegin(x, y, strafe)
    }

    override fun grabUpdate(x: Int, y: Int) = inner.grabUpdate(x, y)

    override fun grabEnd() = inner.grabEnd()

    override fun scrollBegin(x: Int, y: Int, separation: Float) {
        interrupt()
        inner.scrollBegin(x, y, separation)
    }

    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) =
        inner.scrollUpdate(x, y, prevSeparation, currSeparation)

    override fun scrollEnd() = inner.scrollEnd()

    override fun doubleTapZoom(x: Int, y: Int, zoomIn: Boolean) {
        interrupt()
        inner.doubleTapZoom(x, y, zoomIn)
    }

    override fun update(deltaTime: Float) {
        if (animator.isPlaying) animator.advance(deltaTime)
        blend?.advance(deltaTime)
        inner.update(deltaTime)
    }

    /**
     * Stops a playing move and hands the camera to [inner]: by shot and velocity when it is a
     * [CameraShotSink], by a glide from the last animated transform otherwise. Nothing happens
     * at rest.
     */
    fun interrupt() {
        if (!animator.isPlaying) return
        val velocity = animator.interrupt()
        val sink = inner as? CameraShotSink
        if (sink != null) {
            sink.takeOver(animator.shot, velocity)
            blend = null
        } else {
            blend = TransformBlend(lastAnimated ?: animator.shot.transform(), handoverSeconds)
        }
    }

    companion object {
        /** The handover glide onto a manipulator that keeps its own pose: brisk, under a finger. */
        const val HANDOVER_SECONDS = 0.35f
    }
}

/**
 * Remembers a [CinematicCameraManipulator] over [animator] and [inner], rebuilt when either
 * changes. The render loop's camera-swap glide covers a rebuild, so neither cuts.
 */
@Composable
fun rememberCinematicCameraManipulator(
    animator: CameraAnimator,
    inner: CameraGestureDetector.CameraManipulator,
): CinematicCameraManipulator = remember(animator, inner) { CinematicCameraManipulator(animator, inner) }
