package io.github.sceneview.ar.node.infos

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.os.Build
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.core.TrackingFailureReason
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.R
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.node.ModelNode

open class SearchPlaneInfoNode(context: Context, coroutineScope: LifecycleCoroutineScope) :
    ModelNode(
        position = Vector3(0.0f, 0.0f, -2.0f)
    ) {

    var textView: TextView? = null

    private val animator by lazy {
        ObjectAnimator.ofPropertyValuesHolder(
            this,
            PropertyValuesHolder.ofFloat("rotationX", 0.0f, 15.0f, -15.0f, 0.0f),
            PropertyValuesHolder.ofFloat("rotationY", 0.0f, 15.0f, -15.0f, 0.0f)
        ).apply {
            interpolator = LinearInterpolator()
            duration = 2500
            repeatCount = ObjectAnimator.INFINITE
        }
    }

    init {
        isSelectable = false

        setView(
            context = context,
            coroutineScope = coroutineScope,
            layoutResId = R.layout.sceneview_view_info_search_plane,
            onLoaded = { renderableInstance: RenderableInstance, view: View ->
                textView = view.findViewById(R.id.textView)
                renderableInstance.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                    renderPriority = 0
                }
            })
    }

    override fun onRenderingChanged(isRendering: Boolean) {
        super.onRenderingChanged(isRendering)

        if (isRendering) {
            animator.start()
        } else {
            animator.pause()
        }
    }

//    override fun onArFrame(arFrame: ArFrame) {
//        // Show a message based on whether tracking has failed, if planes are detected, and if the user
//        // has placed any objects.
//        textView?.text = trackingStateText(arFrame)
//    }

    open var trackingStateText: (frame: ArFrame) -> String = { frame ->
        when (val reason = frame.camera.trackingFailureReason) {
            TrackingFailureReason.NONE -> context.getString(R.string.sceneview_searching_planes)
            TrackingFailureReason.BAD_STATE -> context.getString(R.string.sceneview_bad_state_message)
            TrackingFailureReason.INSUFFICIENT_LIGHT -> context.getString(
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                    R.string.sceneview_insufficient_light_message
                } else {
                    R.string.sceneview_insufficient_light_android_s_message
                }
            )
            TrackingFailureReason.EXCESSIVE_MOTION -> context.getString(R.string.sceneview_excessive_motion_message)
            TrackingFailureReason.INSUFFICIENT_FEATURES -> context.getString(R.string.sceneview_insufficient_features_message)
            TrackingFailureReason.CAMERA_UNAVAILABLE -> context.getString(R.string.sceneview_camera_unavailable_message)
            else -> context.getString(R.string.sceneview_unknown_tracking_failure, reason)
        }
    }
}
