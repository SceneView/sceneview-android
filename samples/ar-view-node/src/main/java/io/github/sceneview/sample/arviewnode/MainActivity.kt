package io.github.sceneview.sample.arviewnode

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.Config
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
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.ViewNode
import java.util.Locale


class MainActivity : AppCompatActivity(R.layout.activity_main), View.OnClickListener {

    lateinit var sceneView: ARSceneView
    private val nodeList: MutableList<AnchorNode> = mutableListOf()
    lateinit var viewNodeWindowManager: ViewNode.WindowManager

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
     * This is the timestamp of the used Frame to perform the HitTest.
     */
    private var lastHitTimestamp: Long = 0

    private var lastFps = 0.0

    private var frameTime: FrameTime? = null

    private var annotationCounter: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewNodeWindowManager = ViewNode.WindowManager(this@MainActivity)

        sceneView = findViewById<ARSceneView?>(R.id.sceneView).apply {
            configureSession { session, config ->
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.planeFindingMode = Config.PlaneFindingMode.DISABLED
            }

            onSessionUpdated = { _, frame ->
                updateIsTracking(frame)
                updateIsHitting(frame)
                updateRotationOfViewNodes()
            }

            viewNodeWindowManager = this@MainActivity.viewNodeWindowManager

            lifecycle = this@MainActivity.lifecycle
        }



        findViewById<Button>(R.id.btnAnnotate).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (isHitting) {
            lastHitResult?.let { hitResult ->
                hitResult.createAnchorOrNull().apply {
                    if (this != null) {
                        addAnchorNode(this)
                    }
                }
            }
        }
    }

    private fun addAnchorNode(anchor: Anchor) {
        annotationCounter++

        val view: View = View.inflate(
            this@MainActivity,
            R.layout.view_node_text_view_annotation,
            null
        )
        view.findViewById<TextView>(R.id.textView).text = String.format(
            Locale.getDefault(),
            getString(R.string.ar_tv_annotation),
            annotationCounter.toString()
        )

        val viewNode = ViewNode(
            sceneView.engine,
            viewNodeWindowManager,
            sceneView.materialLoader,
            view,
            unlit = true
        )
        viewNode.isEditable = true
        viewNode.isVisible = true
        viewNode.isRotationEditable = true

        val anchorNode = AnchorNode(sceneView.engine, anchor)
        anchorNode.isEditable = true
        anchorNode.isRotationEditable = true
        anchorNode.addChildNode(viewNode)

        nodeList.add(anchorNode)
        sceneView.addChildNode(anchorNode)
    }

    //////////////////////////
    // region Hittest On ScreenCenter
    private fun updateIsTracking(frame: Frame?): Boolean {
        //isTracking = false
        if (frame == null) return false
        isTracking = frame
            .camera
            .trackingState == TrackingState.TRACKING
        return isTracking
    }

    private fun updateIsHitting(frame: Frame?): Boolean {
        // Skip frames!
        if (currentHittestAttempt < HIT_TEST_SKIP_AMOUNT) {
            currentHittestAttempt++
            isHittestPaused = true
            return isHitting
        }
        if (!isTracking()) return false
        if (frame == null) return false
        if (frame.camera.trackingState != TrackingState.TRACKING) return false
        frameTime = FrameTime(frame.timestamp, lastHitTimestamp)
        if (frameDropDetected(frame, frameTime!!) && lastFps > 0.0 && lastHitTimestamp > 0) {
            isHittestPaused = true
            return isHitting
        }
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


    private fun frameDropDetected(
        frame: Frame,
        frameTime: FrameTime
    ): Boolean {
        lastHitTimestamp = frame.timestamp
        val fpsThreshold: Double = lastFps / 100.0 * 60
        val currentFps: Double = frameTime.fps
        val frameDrop = fpsThreshold > currentFps
        lastFps = frameTime.fps
        return frameDrop
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
    // endregion Hittest On ScreenCenter
    //////////////////////////

    ///////////////////
    // region Look at Rotation
    private fun updateRotationOfViewNodes() {
        nodeList.forEach {
            if (it.anchor.trackingState == TrackingState.TRACKING) {
                val cameraPosition = sceneView.cameraNode.worldPosition

                val nodePosition = it.worldPosition

                val cameraVec3 = cameraPosition.toVector3()
                val nodeVec3 = nodePosition.toVector3()

                val direction = Vector3.subtract(cameraVec3, nodeVec3)

                val lookRotation = Quaternion.lookRotation(direction, Vector3.up())

                it.worldQuaternion = dev.romainguy.kotlin.math.Quaternion(
                    lookRotation.x,
                    lookRotation.y,
                    lookRotation.z,
                    lookRotation.w)
            }
        }
    }
    // endregion Look at Rotation
    ///////////////////



    private fun resetValues() {
        currentHittestAttempt = 0
        isHitting = false
        isHittestPaused = false
    }

    private fun isTracking(): Boolean {
        return isTracking
    }

    companion object {
        const val HIT_TEST_SKIP_AMOUNT = 0
    }
}
