package io.github.sceneview.sample.armodelviewer

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.sample.doOnApplyWindowInsets
import io.github.sceneview.sample.setFullScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    lateinit var sceneView: ARSceneView
    lateinit var loadingView: View
    lateinit var instructionText: TextView
    lateinit var changeModelButton: ExtendedFloatingActionButton

    data class ModelAsset(
        val fileLocation: String,
        val scaleUnits: Float? = null
    )

    val modelAssets = listOf(
        ModelAsset(
            fileLocation = "https://sceneview.github.io/assets/models/DamagedHelmet.glb",
            scaleUnits = 0.5f
        ),
        ModelAsset(
            fileLocation = "https://storage.googleapis.com/ar-answers-in-search-models/static/GiantPanda/model.glb",
            // Display the Tiger with a size of 1.5 m height
            scaleUnits = 1.5f
        ),
        ModelAsset(
            fileLocation = "https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb",
            // Display the Tiger with a size of 3 m long
            scaleUnits = 2.5f
        )
    )

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    var anchorNode: AnchorNode? = null
        set(value) {
            if (field != value) {
                field = value
                changeModelButton.isGone = value == null
                updateInstructions()
            }
        }

    lateinit var modelNode: ModelNode

    var trackingFailureReason: TrackingFailureReason? = null
        set(value) {
            if (field != value) {
                field = value
                updateInstructions()
            }
        }

    var modelIndex = 0
        set(value) {
            field = value
            modelLoadingJob?.cancel()
            modelLoadingJob = lifecycleScope.launch {
                isLoading = true
                val modelAsset = modelAssets[value]
                val modelInstance = loadedModels.getOrPut(modelAsset) {
                    sceneView.modelLoader.loadModelInstance(modelAsset.fileLocation)!!
                }
                modelNode.setModelInstance(
                    modelInstance = modelInstance,
                    autoAnimate = true,
                    scaleToUnits = modelAsset.scaleUnits,
                    // Place the model origin at the bottom center
                    centerOrigin = Position(y = -1.0f)
                )

                isLoading = false
                modelLoadingJob = null
            }
        }

    val loadedModels = mutableMapOf<ModelAsset, ModelInstance>()
    var modelLoadingJob: Job? = null

    fun updateInstructions() {
        instructionText.text = trackingFailureReason?.let {
            it.getDescription(this)
        } ?: if (anchorNode == null) {
            getString(R.string.point_your_phone_down)
        } else {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFullScreen(
            findViewById(R.id.rootView),
            fullScreen = true,
            hideSystemBars = false,
            fitsSystemWindows = false
        )

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar)?.apply {
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).topMargin = systemBarsInsets.top
            }
            title = ""
        })
        instructionText = findViewById(R.id.instructionText)
        loadingView = findViewById(R.id.loadingView)
        changeModelButton =
            findViewById<ExtendedFloatingActionButton>(R.id.changeModelButton).apply {
                // Add system bar margins
                val bottomMargin = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
                doOnApplyWindowInsets { systemBarsInsets ->
                    (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                        systemBarsInsets.bottom + bottomMargin
                }
                setOnClickListener {
                    modelIndex = ((modelIndex + 1) % modelAssets.size)
                }
            }
        sceneView = findViewById(R.id.sceneView)
        sceneView.apply {
            configureSession { session, config ->
                config.depthMode = Config.DepthMode.AUTOMATIC
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            }
            modelNode = ModelNode(engine = engine).apply {
                isEditable = true
            }
            modelIndex = 0
            onSessionUpdated = { _, frame ->
                if (anchorNode == null) {
                    frame.getUpdatedPlanes()
                        .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                        ?.let { plane ->
                            addChildNode(
                                AnchorNode(engine, plane.createAnchor(plane.centerPose))
                                    .apply { isEditable = true }
                                    .also { anchorNode = it }
                                    .addChildNode(modelNode)
                            )
                        }
                }
            }
            onTrackingFailureChanged = { reason ->
                this@MainActivity.trackingFailureReason = reason
            }
        }
    }
}