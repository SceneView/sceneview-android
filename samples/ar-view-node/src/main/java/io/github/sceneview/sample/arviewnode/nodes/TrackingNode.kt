package io.github.sceneview.sample.arviewnode.nodes

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
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.math.toRotation
import io.github.sceneview.node.ViewNode
import io.github.sceneview.sample.arviewnode.MainActivity
import io.github.sceneview.sample.arviewnode.R


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

    private var isValid = ValidIndicator.IS_NOT_VALID

    private var trackingNode: ViewNode? = null

    /**
     * The tracking indicator must be rotated so that
     * it lies flat on the floor
     */
    private val rotationMarkerVertical = Quaternion
        .fromAxisAngle(Float3(1.0f, 0.0f, 0.0f), 90f)

    fun frameUpdate(frame: Frame?) {
        updateIsTracking(frame)
        updateIsHitting(frame)
        updateTrackingNode()
    }


    ///////////////////
    // region UpdateIsTracking
    private fun updateIsTracking(frame: Frame?): Boolean {
        //isTracking = false
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
    private fun updateIsHitting(frame: Frame?): Boolean {
        // Skip frames!
        if (currentHittestAttempt < MainActivity.HITTEST_SKIP_AMOUNT) {
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


    private fun getScreenCenter(): android.graphics.Point {
        return android.graphics.Point(sceneView.width / 2, sceneView.height / 2)
    }

    private fun validatePlane(trackable: Trackable, hit: HitResult): Boolean {
        return (trackable as Plane).isPoseInPolygon(hit.hitPose) && trackable.trackingState == TrackingState.TRACKING
    }

    private fun validatePoint(trackable: Trackable): Boolean {
        return (trackable as Point).orientationMode === Point.OrientationMode.ESTIMATED_SURFACE_NORMAL && trackable.trackingState == TrackingState.TRACKING
    }

    private fun validateDepthPoint(trackable: Trackable): Boolean {
        return trackable.trackingState == TrackingState.TRACKING
    }

    private fun resetValues() {
        currentHittestAttempt = 0
        isHitting = false
        isHittestPaused = false
    }
    // endregion UpdateIsHitting
    //////////////////


    /////////////////////
    // region UpdateTrackingNode
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

    private fun create() {
        getLastHitResult()?.let { hitResult ->
            hitResult.createAnchorOrNull().apply {
                if (this != null) {
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
            }
        }
    }

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

    private fun createView(): View {
        val view: View = View.inflate(
            context,
            R.layout.view_node_circle_outlined,
            null
        )

        return view
    }

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
    fun isTracking(): Boolean {
        return isTracking
    }

    fun isHitting(): Boolean {
        return isHitting
    }

    fun getLastHitResult(): HitResult? {
        return lastHitResult
    }
    // endregion Public Functions
    ///////////////////

    enum class ValidIndicator {
        IS_VALID,
        IS_NOT_VALID
    }
}