package io.github.sceneview.sample.arpointcloud

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.fps
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.sample.doOnApplyWindowInsets

const val kMaxPointCloudPerSecond = 10

class MainFragment : Fragment(R.layout.fragment_main) {

    class PointCloudNode(
        modelInstance: ModelInstance,
        var id: Int,
        var confidence: Float
    ) : ModelNode(modelInstance)

    lateinit var sceneView: ARSceneView

    lateinit var scoreText: TextView

    lateinit var confidenceSeekbar: SeekBar
    lateinit var confidenceText: TextView

    lateinit var maxPointsSeekbar: SeekBar
    lateinit var maxPointsText: TextView

    lateinit var loadingView: View

    private var pointCloudModelInstances = mutableListOf<ModelInstance>()
    private val pointCloudNodes = mutableListOf<PointCloudNode>()

    private var lastPointCloudTimestamp: Long? = null
    private var lastPointCloudFrame: Frame? = null

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    private var minConfidence: Float = 0.1f
        set(value) {
            if (field != value) {
                field = value
                confidenceText.text = getString(R.string.min_confidence, value)
                pointCloudNodes.filter { it.confidence < value }
                    .forEach { sceneView.removeChildNode(it) }
            }
        }

    private var maxPoints: Int = 500
        set(value) {
            if (field != value) {
                field = value
                maxPointsText.text = getString(R.string.max_points, value)
                if (pointCloudNodes.size > value) {
                    pointCloudNodes.slice(0 until value).forEach {
                        removePointCloudNode(it)
                    }
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById<ARSceneView?>(R.id.sceneView).apply {
            planeRenderer.isEnabled = false
            sessionConfiguration = { session, config ->
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
            }
            environment = null
            onSessionUpdated = this@MainFragment::onSessionUpdated
        }

        scoreText = view.findViewById<TextView>(R.id.scoreText).apply {
            val topMargin = (layoutParams as ViewGroup.MarginLayoutParams).topMargin
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                    systemBarsInsets.top + topMargin
            }
            text = getString(R.string.score, 0.0f, 0)
        }

        confidenceText = view.findViewById<TextView>(R.id.confidenceText).apply {
            text = getString(R.string.min_confidence, minConfidence)
        }
        confidenceSeekbar = view.findViewById<SeekBar>(R.id.confidenceSeekbar).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                min = 0
            }
            max = 40
            progress = (minConfidence * 100).toInt()
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        minConfidence = progress / 100.0f
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        view.findViewById<LinearLayout>(R.id.maxPointsLayout).apply {
            val bottomMargin = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    systemBarsInsets.bottom + bottomMargin
            }
        }
        maxPointsText = view.findViewById<TextView>(R.id.maxPointsText).apply {
            text = getString(R.string.max_points, maxPoints)
        }
        maxPointsSeekbar = view.findViewById<SeekBar>(R.id.maxPointsSeekbar).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                min = 100
            }
            max = 1500
            progress = maxPoints
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        maxPoints = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        loadingView = view.findViewById(R.id.loadingView)
    }

    private fun getPointCloudModelInstance(): ModelInstance? {
        if (pointCloudModelInstances.size == 0) {
            pointCloudModelInstances = sceneView.modelLoader.createInstancedModel(
                assetFileLocation = "models/point_cloud.glb",
                count = maxPoints
            ).toMutableList()
        }
        return pointCloudModelInstances.removeLastOrNull()
    }

    fun onSessionUpdated(session: Session, frame: Frame) {
        frame.takeIf { it.fps(lastPointCloudFrame) < kMaxPointCloudPerSecond }
            ?.acquirePointCloud()
            ?.takeIf { it.timestamp != lastPointCloudTimestamp }
            ?.use { pointCloud ->
                if (pointCloud.ids == null) return

                lastPointCloudFrame = frame
                lastPointCloudTimestamp = pointCloud.timestamp
                val idsBuffer = pointCloud.ids
                val pointsSize = idsBuffer.limit()
                val ids = mutableListOf<Int>()
                val pointsBuffer = pointCloud.points
                for (index in 0 until pointsSize) {
                    val id = idsBuffer[index]
                    ids += id
                    if (pointCloudNodes.firstOrNull { it.id == id } == null) {
                        val pointIndex = index * 4
                        val position = Position(
                            pointsBuffer[pointIndex],
                            pointsBuffer[pointIndex + 1],
                            pointsBuffer[pointIndex + 2]
                        )
                        val confidence = pointsBuffer.get(pointIndex + 3)
                        if (confidence > minConfidence) {
                            addPointCloudNode(id, position, confidence)
                        }
                    }
                }
                val score = if (pointCloudNodes.size > 0) {
                    pointCloudNodes.sumOf {
                        it.confidence.toDouble()
                    } / pointCloudNodes.size.toFloat()
                } else 0.0
                scoreText.text = getString(R.string.score, score, pointCloudNodes.size)
            }
    }

    override fun onPause() {
        super.onPause()
        pointCloudNodes.toList().forEach { removePointCloudNode(it) }
    }

    fun addPointCloudNode(id: Int, position: Position, confidence: Float) {
        if (pointCloudNodes.size < maxPoints) {
            getPointCloudModelInstance()?.let { modelInstance ->
                val pointCloudNode = PointCloudNode(modelInstance, id, confidence).apply {
                    this.position = position
                }
                pointCloudNodes += pointCloudNode
                sceneView.addChildNode(pointCloudNode)
            }
        } else {
            val pointCloudNode = pointCloudNodes.first().apply {
                this.id = id
                this.worldPosition = position
                this.confidence = confidence
            }
            pointCloudNodes.removeFirst()
            pointCloudNodes.add(pointCloudNode)
        }
    }

    fun removePointCloudNode(pointCloudNode: PointCloudNode) {
        pointCloudNodes -= pointCloudNode
        sceneView.removeChildNode(pointCloudNode)
        pointCloudNode.destroy()
    }
}