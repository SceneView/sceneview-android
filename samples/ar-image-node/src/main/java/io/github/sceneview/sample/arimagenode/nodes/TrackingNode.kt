package io.github.sceneview.sample.arimagenode.nodes

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.FatalException
import com.google.ar.core.exceptions.NotTrackingException
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import com.google.ar.sceneform.rendering.ViewRenderable
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.math.toRotation
import io.github.sceneview.node.ViewNode
import io.github.sceneview.sample.arimagenode.R


/**
 * ## Overview
 * * The `TrackingNode` is designed to see where the Smartphone is currently pointing
 * in the ARScene. It determines further if the camera is currently tracking and if a
 * hit test is successful.This class integrates with ARCore and the Android view system to
 * provide visual indicators in the AR environment.
 *
 * ## Constructor
 * * context (Context): The Android context in which the AR scene operates.
 * * sceneView (ARSceneView): The AR scene view that contains the nodes and camera.
 *
 * ## Example Usage
 * ```
 * // Create an instance of TrackingNode
 * val trackingNode = TrackingNode(context, sceneView)
 *
 * // Update the frame
 * trackingNode.frameUpdate(frame)
 *
 * // Check if the camera is tracking
 * val tracking = trackingNode.isTracking()
 *
 * // Check if a valid hit result is found
 * val hitting = trackingNode.isHitting()
 *
 * // Get the last hit result
 * val hitResult = trackingNode.getLastHitResult()
 * ```
 *
 * ## Notes
 * * Ensure that the AR environment and dependencies (like ARCore) are properly set up
 * for this class to function as intended.
 * * The HITTEST_SKIP_AMOUNT should be defined in your project, specifying how many frames
 * to skip between hit tests.
 * * This class handles exceptions that may occur during hit testing, ensuring that the AR
 * experience remains stable.
 */
class TrackingNode(
    private val context: Context,
    private val sceneView: ARSceneView
) {

    /**
     * Indicates if the Camera is currently tracking.
     */
    private var isTracking = false

    /**
     * Indicates if the HitTest was successful.
     */
    private var isHitting = false

    /**
     * If we skip Frames this value is set to true, otherwise false.
     */
    private var isHittestPaused = true

    /**
     * Tracks if the create function has been called.
     */
    private var createCalled = false

    /**
     * This is a simple counter to track skipped frames. After HITTEST_SKIP_AMOUNT is
     * reached this counter is set back to 0.
     */
    private var currentHittestAttempt = 0

    /**
     * That is our HitResult. This variable is the most important one in this
     * class. Based on the HitResult Anchors can be created.
     */
    private var lastHitResult: HitResult? = null

    /**
     * Indicates the validity of the current hit result.
     */
    private var isValid = ValidIndicator.IS_NOT_VALID

    /**
     * Stores the view node used for tracking.
     */
    private var trackingNode: ViewNode? = null

    /**
     * The tracking indicator must be rotated so that
     * it lies flat on the floor
     */
    private val rotationMarkerVertical = Quaternion
        .fromAxisAngle(Float3(1.0f, 0.0f, 0.0f), 90f)

    /**
     * ## Overview
     * * Updates the tracking and hit test states, and manages the tracking node.
     *
     * ## Implementation Details:
     * * Update Tracking State:
     *    * Calls updateIsTracking(frame) to update the tracking state.
     * * Update Hit Test State:
     *    * Calls updateIsHitting(frame) to update the hit test state.
     * * Update Tracking Node:
     *    * Calls updateTrackingNode() to manage the creation and updating of the tracking node.
     *
     * @param frame (Frame?): The current AR frame.
     */
    fun frameUpdate(frame: Frame?) {
        updateIsTracking(frame)
        updateIsHitting(frame)
        updateTrackingNode()
    }


    ///////////////////
    // region UpdateIsTracking
    /**
     * Updates the isTracking property based on the camera's tracking state.
     *
     * @param frame (Frame?): The current AR frame.
     *
     * @return Boolean: True if a valid hit result is found, false otherwise.
     */
    private fun updateIsTracking(frame: Frame?): Boolean {
        if (frame == null) return false
        isTracking = frame
            .camera
            .trackingState == TrackingState.TRACKING
        return isTracking
    }
    // endregion UpdateIsTracking
    ///////////////////


    //////////////////
    // region UpdateIsHitting
    /**
     * ## Overview
     * * Updates the isHitting property based on the hit test results.
     *
     * ## Implementation Details:
     * * Frame Skipping:
     *    * Increments currentHittestAttempt and skips hit testing if the attempt is below HITTEST_SKIP_AMOUNT.
     * * Hit Testing:
     *    * Performs hit testing if the camera is tracking and the frame is not null.
     *    * Calls updateHitTestInternal(frame, getScreenCenter()) to perform the actual hit test.
     * * Exception Handling:
     *    * Resets values if hit testing fails due to exceptions.
     *
     * @param frame (Frame?): The current AR frame.
     *
     * @return Boolean: True if a valid hit result is found, false otherwise.
     */
    private fun updateIsHitting(frame: Frame?): Boolean {
        // Skip frames!
        if (currentHittestAttempt < HITTEST_SKIP_AMOUNT) {
            currentHittestAttempt++
            isHittestPaused = true
            return isHitting
        }
        if (!isTracking()) return false
        if (frame == null) return false
        if (frame.camera.trackingState != TrackingState.TRACKING) return false

        return try {
            val pt = getScreenCenter()
            updateHitTestInternal(frame, pt)
        } catch (e: NotTrackingException) {
            resetValues()
            false
        } catch (e: DeadlineExceededException) {
            resetValues()
            false
        } catch (e: FatalException) {
            resetValues()
            false
        }
    }

    /**
     * Performs the hit test and updates the hit test result.
     *
     * @param frame (Frame): The current AR frame.
     * @param pt (android.graphics.Point): The point on the screen to perform the hit test.
     *
     * @return Boolean: True if a valid hit result is found, false otherwise
     */
    private fun updateHitTestInternal(
        frame: Frame,
        pt: android.graphics.Point
    ): Boolean {
        resetValues()

        val hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())

        for (hit in hits) {
            val trackable = hit.trackable

            // Check what kind of Trackable we have.
            isHitting = when (trackable) {
                is Plane -> validatePlane(trackable, hit)
                is DepthPoint -> validateDepthPoint(trackable)
                is Point -> validatePoint(trackable)
                else -> false
            }

            // If we have found a valid Trackable leave the for-loop.
            if (isHitting) {
                lastHitResult = hit
                break
            }
        }
        return isHitting
    }

    /**
     * Returns the center point of the screen.
     *
     * @return android.graphics.Point: The center point of the screen.
     */
    private fun getScreenCenter(): android.graphics.Point {
        return android.graphics.Point(sceneView.width / 2, sceneView.height / 2)
    }

    /**
     * Validates the hit result for a plane trackable.
     *
     * @param trackable (Trackable): The trackable being validated.
     * @param hit (HitResult): The hit result.
     *
     * @return Boolean: True if the hit result is valid, false otherwise.
     */
    private fun validatePlane(trackable: Trackable, hit: HitResult): Boolean {
        return (trackable as Plane).isPoseInPolygon(hit.hitPose) && trackable.trackingState == TrackingState.TRACKING
    }

    /**
     * Validates the hit result for a point trackable.
     *
     * @param trackable (Trackable): The trackable being validated.
     *
     * @return Boolean: True if the hit result is valid, false otherwise.
     */
    private fun validatePoint(trackable: Trackable): Boolean {
        return (trackable as Point).orientationMode === Point.OrientationMode.ESTIMATED_SURFACE_NORMAL && trackable.trackingState == TrackingState.TRACKING
    }

    /**
     * Validates the hit result for a depth point trackable.
     *
     * @param trackable (Trackable): The trackable being validated.
     *
     * @return Boolean: True if the hit result is valid, false otherwise.
     */
    private fun validateDepthPoint(trackable: Trackable): Boolean {
        return trackable.trackingState == TrackingState.TRACKING
    }

    /**
     * Resets the hit test related values.
     */
    private fun resetValues() {
        currentHittestAttempt = 0
        isHitting = false
        isHittestPaused = false
    }
    // endregion UpdateIsHitting
    //////////////////


    /////////////////////
    // region UpdateTrackingNode
    /**
     * Manages the creation and updating of the tracking node.
     */
    private fun updateTrackingNode() {
        if (trackingNode == null &&
            isHitting &&
            !createCalled
        ) {
            createCalled = true
            create()

        } else if (trackingNode != null &&
            createCalled
        ) {
            update()
        }
    }

    /**
     * Creates the tracking node.
     */
    private fun create() {
        val view = createView()

        val viewRenderable = ViewRenderable
            .builder()
            .setView(context, view)
            .build(sceneView.engine)

        viewRenderable!!.thenAccept {
            val viewAttachmentManager = ViewAttachmentManager(
                context,
                sceneView
            )
            viewAttachmentManager.onResume()

            trackingNode = ViewNode(
                sceneView.engine,
                sceneView.modelLoader,
                viewAttachmentManager
            ).apply {
                setRenderable(it)
            }

            sceneView.addChildNode(trackingNode!!)
        }
    }

    /**
     * Updates the tracking node based on the current hit result.
     */
    private fun update() {
        val isValid = getIsValid()

        if (this.isValid != isValid) {
            this.isValid = isValid

            if (isValid == ValidIndicator.IS_VALID) {
                (trackingNode as ViewNode)
                    .renderable
                    ?.view
                    ?.findViewById<ImageView>(R.id.imageView)
                    ?.backgroundTintList = ContextCompat
                    .getColorStateList(
                        context,
                        R.color.white
                    )

            } else if (isValid == ValidIndicator.IS_NOT_VALID) {
                (trackingNode as ViewNode)
                    .renderable
                    ?.view
                    ?.findViewById<ImageView>(R.id.imageView)
                    ?.backgroundTintList = ContextCompat
                    .getColorStateList(
                        context,
                        R.color.red_500
                    )
            }
        }

        getLastHitResult()?.let { hitResult ->
            trackingNode?.worldPosition = hitResult.hitPose.position
            trackingNode?.worldRotation = rotationMarkerVertical.toRotation()
        }
    }

    /**
     * Creates the view for the tracking node.
     *
     * @return View: The created view.
     */
    private fun createView(): View {
        val view: View = View.inflate(
            context,
            R.layout.view_node_circle_outlined,
            null
        )

        return view
    }

    /**
     * Determines the validity of the current hit result.
     *
     * @return ValidIndicator: The validity indicator.
     */
    private fun getIsValid(): ValidIndicator {
        return if (isHitting) {
            ValidIndicator.IS_VALID
        } else {
            ValidIndicator.IS_NOT_VALID
        }
    }
    // endregion UpdateTrackingNode
    /////////////////////


    ///////////////////
    // region Public Functions
    /**
     * Returns the tracking state.
     *
     * @return Boolean: True if the camera is tracking, false otherwise.
     */
    fun isTracking(): Boolean {
        return isTracking
    }

    /**
     * Returns the hit test state.
     *
     * @return Boolean: True if a valid hit result is found, false otherwise.
     */
    fun isHitting(): Boolean {
        return isHitting
    }

    /**
     * Returns the last hit result.
     *
     * @return HitResult?: The last hit result, or null if no valid hit result is found.
     */
    fun getLastHitResult(): HitResult? {
        return lastHitResult
    }
    // endregion Public Functions
    ///////////////////

    enum class ValidIndicator {
        IS_VALID,
        IS_NOT_VALID
    }

    companion object {
        const val HITTEST_SKIP_AMOUNT = 0
    }
}