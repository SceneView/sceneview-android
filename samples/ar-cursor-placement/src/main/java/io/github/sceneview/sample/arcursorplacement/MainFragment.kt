package io.github.sceneview.sample.arcursorplacement

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.CursorNode
import io.github.sceneview.math.Position
import io.github.sceneview.utils.doOnApplyWindowInsets

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ArSceneView
    lateinit var loadingView: View
    lateinit var actionButton: ExtendedFloatingActionButton

    lateinit var cursorNode: CursorNode
    lateinit var modelNode: ArModelNode

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
            actionButton.isGone = value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingView = view.findViewById(R.id.loadingView)
        actionButton = view.findViewById<ExtendedFloatingActionButton>(R.id.actionButton).apply {
            val bottomMargin = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    systemBarsInsets.bottom + bottomMargin
            }
            setOnClickListener { cursorNode.createAnchor()?.let { anchorOrMove(it) } }
        }

        sceneView = view.findViewById<ArSceneView?>(R.id.sceneView).apply {
            planeRenderer.isVisible = false
            // Handle a fallback in case of non AR usage. The exception contains the failure reason
            // e.g. SecurityException in case of camera permission denied
            onArSessionFailed = { _: Exception ->
                // If AR is not available or the camara permission has been denied, we add the model
                // directly to the scene for a fallback 3D only usage
                modelNode.centerModel(origin = Position(x = 0.0f, y = 0.0f, z = 0.0f))
                modelNode.scaleModel(units = 1.0f)
                sceneView.addChild(modelNode)
            }
            onTouchAr = { hitResult, _ ->
                anchorOrMove(hitResult.createAnchor())
            }
        }

        cursorNode = CursorNode(context = requireContext(), lifecycle = lifecycle).apply {
            onTrackingChanged = { _, isTracking, _ ->
                if (!isLoading) {
                    actionButton.isGone = !isTracking
                }
            }
        }
        sceneView.addChild(cursorNode)

        isLoading = true
        modelNode = ArModelNode()
        modelNode.loadModelAsync(context = requireContext(),
            lifecycle = lifecycle,
            glbFileLocation = "models/spiderbot.glb",
            onLoaded = {
                actionButton.text = getString(R.string.move_object)
                actionButton.setIconResource(R.drawable.ic_target)
                isLoading = false
            })
    }

    fun anchorOrMove(anchor: Anchor) {
        if (!sceneView.children.contains(modelNode)) {
            sceneView.addChild(modelNode)
        }
        modelNode.anchor = anchor
    }
}