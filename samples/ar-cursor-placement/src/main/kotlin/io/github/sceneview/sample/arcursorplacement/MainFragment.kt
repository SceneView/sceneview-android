package io.github.sceneview.sample.arcursorplacement

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.CursorNode
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
                // If AR is not available, we add the model directly to the scene for a 3D only
                // usage
                sceneView.addChild(modelNode)
            }
            onTouchAr = { hitResult, _ ->
                anchorOrMove(hitResult.createAnchor())
            }
        }

        cursorNode = CursorNode(context = requireContext(), coroutineScope = lifecycleScope).apply {
            onTrackingChanged = { _, isTracking, _ ->
                if (!isLoading) {
                    actionButton.isGone = !isTracking
                }
            }
        }
        sceneView.addChild(cursorNode)
        sceneView.onTouch = { selectedNode, motionEvent ->
            when (sceneView.gestureType) {
                SceneView.GestureType.SINGLE_TAP -> {
                    if (selectedNode == null) {
                        Toast.makeText(context, "SINGLE TAP on screen", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "SINGLE TAP on node", Toast.LENGTH_SHORT).show()
                    }
                }
                SceneView.GestureType.DOUBLE_TAP -> {
                    if (selectedNode == null) {
                        Toast.makeText(context, "DOUBLE TAP on screen", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "DOUBLE TAP on node", Toast.LENGTH_SHORT).show()
                    }
                }
                SceneView.GestureType.LONG_PRESS -> {
                    if (selectedNode == null) {
                        Toast.makeText(context, "LONG PRESS on screen", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "LONG PRESS on node", Toast.LENGTH_SHORT).show()
                    }
                }
                SceneView.GestureType.FLING_UP -> {
                    if (selectedNode == null) {
                        Toast.makeText(context, "FLING UP on screen", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "FLING UP on node", Toast.LENGTH_SHORT).show()
                    }
                }
                SceneView.GestureType.FLING_DOWN -> {
                    if (selectedNode == null) {
                        Toast.makeText(context, "FLING DOWN on screen", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "FLING DOWN on node", Toast.LENGTH_SHORT).show()
                    }
                }
                SceneView.GestureType.FLING_LEFT -> {
                    if (selectedNode == null) {
                        Toast.makeText(context, "FLING LEFT on screen", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "FLING LEFT on node", Toast.LENGTH_SHORT).show()
                    }
                }
                SceneView.GestureType.FLING_RIGHT -> {
                    if (selectedNode == null) {
                        Toast.makeText(context, "FLING RIGHT on screen", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "FLING RIGHT on node", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> { }
            }
            sceneView.resetGestureType()
            true
        }

        isLoading = true
        modelNode = ArModelNode()
        modelNode.loadModelAsync(context = requireContext(),
            coroutineScope = lifecycleScope,
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