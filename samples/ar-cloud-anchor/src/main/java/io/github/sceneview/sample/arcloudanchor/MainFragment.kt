package io.github.sceneview.sample.arcloudanchor

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor
import com.google.ar.core.Session
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.utils.doOnApplyWindowInsets

class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var sceneView: ArSceneView
    private lateinit var loadingView: View
    private lateinit var editText: EditText
    private lateinit var hostButton: Button
    private lateinit var resolveButton: Button
    private lateinit var actionButton: ExtendedFloatingActionButton

    private lateinit var cloudAnchorNode: ArModelNode

    private var mode = Mode.HOME

    private var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topGuideline = view.findViewById<Guideline>(R.id.topGuideline)
        topGuideline.doOnApplyWindowInsets { systemBarsInsets ->
            // Add the action bar margin
            val actionBarHeight =
                (requireActivity() as AppCompatActivity).supportActionBar?.height ?: 0
            topGuideline.setGuidelineBegin(systemBarsInsets.top + actionBarHeight)
        }
        val bottomGuideline = view.findViewById<Guideline>(R.id.bottomGuideline)
        bottomGuideline.doOnApplyWindowInsets { systemBarsInsets ->
            // Add the navigation bar margin
            bottomGuideline.setGuidelineEnd(systemBarsInsets.bottom)
        }

        sceneView = view.findViewById(R.id.sceneView)
        sceneView.apply {
            cloudAnchorEnabled = true
        }

        loadingView = view.findViewById(R.id.loadingView)

        actionButton = view.findViewById(R.id.actionButton)
        actionButton.setOnClickListener {
            actionButtonClicked()
        }

        editText = view.findViewById(R.id.editText)
        editText.addTextChangedListener {
            actionButton.isEnabled = !it.isNullOrBlank()
        }

        hostButton = view.findViewById(R.id.hostButton)
        hostButton.setOnClickListener {
            selectMode(Mode.HOST)
        }

        resolveButton = view.findViewById(R.id.resolveButton)
        resolveButton.setOnClickListener {
            selectMode(Mode.RESOLVE)
        }

        isLoading = true
        cloudAnchorNode =
            ArModelNode(
                engine = sceneView.engine,
                placementMode = PlacementMode.PLANE_HORIZONTAL
            ).apply {
                parent = sceneView
                isSmoothPoseEnable = false
                isVisible = false
                loadModelGlbAsync(
                    glbFileLocation = "models/spiderbot.glb"
                ) {
                    isLoading = false
                }
            }
    }

    private fun actionButtonClicked() {
        when (mode) {
            Mode.HOME -> {}
            Mode.HOST -> {
                val frame = sceneView.currentFrame ?: return

                if (!cloudAnchorNode.isAnchored) {
                    cloudAnchorNode.anchor()
                }

                if (sceneView.arSession?.estimateFeatureMapQualityForHosting(frame.camera.pose) == Session.FeatureMapQuality.INSUFFICIENT) {
                    Toast.makeText(context, R.string.insufficient_visual_data, Toast.LENGTH_LONG)
                        .show()
                    return
                }

                cloudAnchorNode.hostCloudAnchor { anchor: Anchor, success: Boolean ->
                    if (success) {
                        editText.setText(anchor.cloudAnchorId)
                        selectMode(Mode.RESET)
                    } else {
                        Toast.makeText(context, R.string.error_occurred, Toast.LENGTH_LONG).show()
                        Log.d(
                            TAG,
                            "Unable to host the Cloud Anchor. The Cloud Anchor state is ${anchor.cloudAnchorState}"
                        )
                        selectMode(Mode.HOST)
                    }
                }

                actionButton.apply {
                    setText(R.string.hosting)
                    isEnabled = true
                }
            }
            Mode.RESOLVE -> {
                cloudAnchorNode.resolveCloudAnchor(editText.text.toString()) { anchor: Anchor, success: Boolean ->
                    if (success) {
                        cloudAnchorNode.isVisible = true
                        selectMode(Mode.RESET)
                    } else {
                        Toast.makeText(context, R.string.error_occurred, Toast.LENGTH_LONG).show()
                        Log.d(
                            TAG,
                            "Unable to resolve the Cloud Anchor. The Cloud Anchor state is ${anchor.cloudAnchorState}"
                        )
                        selectMode(Mode.RESOLVE)
                    }
                }

                actionButton.apply {
                    setText(R.string.resolving)
                    isEnabled = false
                }
            }
            Mode.RESET -> {
                cloudAnchorNode.detachAnchor()
                selectMode(Mode.HOME)
            }
        }
    }

    private fun selectMode(mode: Mode) {
        this.mode = mode

        when (mode) {
            Mode.HOME -> {
                editText.isVisible = false
                hostButton.isVisible = true
                resolveButton.isVisible = true
                actionButton.isVisible = false
                cloudAnchorNode.isVisible = false
            }
            Mode.HOST -> {
                hostButton.isVisible = false
                resolveButton.isVisible = false
                actionButton.apply {
                    setIconResource(R.drawable.ic_host)
                    setText(R.string.host)
                    isVisible = true
                    isEnabled = true
                }
                cloudAnchorNode.isVisible = true
            }
            Mode.RESOLVE -> {
                editText.isVisible = true
                hostButton.isVisible = false
                resolveButton.isVisible = false
                actionButton.apply {
                    setIconResource(R.drawable.ic_resolve)
                    setText(R.string.resolve)
                    isVisible = true
                    isEnabled = editText.text.isNotEmpty()
                }
            }
            Mode.RESET -> {
                editText.isVisible = true
                actionButton.apply {
                    setIconResource(R.drawable.ic_reset)
                    setText(R.string.reset)
                    isEnabled = true
                }
            }
        }
    }

    private enum class Mode {
        HOME, HOST, RESOLVE, RESET
    }

    companion object {
        private const val TAG = "MainFragment"
    }
}