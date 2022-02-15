package io.github.sceneview.sample.arcursorplacement

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.CursorNode
import io.github.sceneview.utils.doOnApplyWindowInsets

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ArSceneView
    lateinit var loadingView: View
    lateinit var actionButton: ExtendedFloatingActionButton

    lateinit var cursorNode: CursorNode
    val modelNode: ArModelNode by lazy {
        isLoading = true
        ArModelNode().apply {
            loadModel(context = requireContext(),
                coroutineScope = lifecycleScope,
                glbFileLocation = "models/spiderbot.glb",
                onLoaded = {
                    actionButton.text = getString(R.string.move_object)
                    actionButton.icon = resources.getDrawable(R.drawable.ic_target)
                    isLoading = false
                })
        }
    }

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
            actionButton.isGone = value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById(R.id.sceneView)
        sceneView.planeRenderer.isEnabled = false
//        sceneView.planeRenderer.isVisible = false
        // Handle a fallback in case of non AR usage
        // The exception contains the failure reason
        // e.g. SecurityException in case of camera permission denied
        sceneView.onArSessionFailed = { exception: Exception ->
            // If AR is not available, we add the model directly to the scene for a 3D only usage
            sceneView.addChild(modelNode)
        }
        sceneView.onTouchAr = { hitResult, _ ->
            anchorOrMove(hitResult.createAnchor())
        }
        loadingView = view.findViewById(R.id.loadingView)
        actionButton = view.findViewById<ExtendedFloatingActionButton>(R.id.actionButton).apply {
            val bottomMargin = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    systemBarsInsets.bottom + bottomMargin
            }
            setOnClickListener { cursorNode.createAnchor()?.let { anchorOrMove(it) } }
        }

        cursorNode = CursorNode(context = requireContext(), coroutineScope = lifecycleScope)
        cursorNode.onTrackingChanged = { _, isTracking, _ ->
            if (!isLoading) {
                actionButton.isGone = !isTracking
            }
        }
        sceneView.addChild(cursorNode)
    }

    fun anchorOrMove(anchor: Anchor) {
        if (!sceneView.children.contains(modelNode)) {
            sceneView.addChild(modelNode)
        }
        modelNode.anchor = anchor
    }
}