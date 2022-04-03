package io.github.sceneview.sample.arcloudanchor

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.utils.doOnApplyWindowInsets

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ArSceneView
    lateinit var loadingView: View
    lateinit var actionButton: ExtendedFloatingActionButton

    lateinit var cloudAnchorNode: ArModelNode
    private var anchorId: String = ""

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        sceneView = view.findViewById(R.id.sceneView)
        loadingView = view.findViewById(R.id.loadingView)
        actionButton = view.findViewById<ExtendedFloatingActionButton>(R.id.actionButton).apply {
            // Add system bar margins
            val bottomMargin = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    systemBarsInsets.bottom + bottomMargin
            }
            setOnClickListener { actionButtonClicked() }
        }
        sceneView.configureSession { _: ArSession, config: Config ->
            config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
            config.lightEstimationMode = Config.LightEstimationMode.DISABLED //to combat maxImages buffer filling to quickly
        }

        isLoading = true
        cloudAnchorNode = ArModelNode(placementMode = PlacementMode.PLANE_HORIZONTAL).apply {
            loadModelAsync(
                context = requireContext(),
                glbFileLocation = "models/spiderbot.glb",
                coroutineScope = lifecycleScope,
                autoAnimate = false,
                autoScale = false,
            ) {
                isLoading = false
            }
        }
        sceneView.onTouchAr = { hitResult: HitResult, motionEvent: MotionEvent ->
            cloudAnchorNode.anchor = hitResult.createAnchor()
            cloudAnchorNode.parent = sceneView
            sceneView.addChild(cloudAnchorNode)
            cloudAnchorNode.hostCloudAnchor(1) { anchor: Anchor, success: Boolean ->
                if (success) {
                    if (cloudAnchorNode.anchor == anchor) {
                        anchorId = anchor.cloudAnchorId
                        cloudAnchorNode.parent = null
                        cloudAnchorNode.anchor = null
                        actionButton.visibility = View.VISIBLE
                        Log.d("DEBUG", "Hosting success with id ${anchor.cloudAnchorId}")
                    } else {
                        Log.d(
                            "DEBUG",
                            "Hosting successful, but anchor != cloudAnchornode.anchor"
                        )
                    }
                } else {
                    cloudAnchorNode.parent = null
                    cloudAnchorNode.anchor = null
                    actionButton.visibility = View.GONE
                    Log.d("DEBUG", "Hosting complete but unsuccessful")
                }
            }

        }
    }

    fun actionButtonClicked() {
        if (anchorId.isNotBlank()) {
            actionButton.isEnabled = false
            actionButton.setText(R.string.resolving_object)
            Log.d("DEBUG", "Resolving clicked with id: $anchorId")
            cloudAnchorNode.resolveCloudAnchor(anchorId) { anchor: Anchor, success: Boolean ->
                Log.d("DEBUG", "Resolve Completed")
                actionButton.isEnabled = true
                if (success) {
                    if (cloudAnchorNode.anchor == anchor) {
                        Log.d("DEBUG", "Resolve Success")
                        cloudAnchorNode.anchor = anchor
                        cloudAnchorNode.parent = sceneView
                        sceneView.addChild(cloudAnchorNode)
                    } else {
                        Log.d(
                            "DEBUG",
                            "Resolved successful, but anchor != cloudAnchornode.anchor"
                        )
                    }
                } else {
                    Log.d("DEBUG", "Resolved complete but unsuccessful")
                }
            }
        } else {
            Log.d("DEBUG", "Resolved clicked but nothing hosted yet")
        }
    }


}