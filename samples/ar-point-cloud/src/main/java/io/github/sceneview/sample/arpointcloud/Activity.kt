package io.github.sceneview.sample.arpointcloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.Config
import com.google.ar.core.Frame
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.fps
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.sample.SceneviewTheme

const val kMaxPointCloudPerSecond = 10

class Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)

                    var minConfidence by remember { mutableFloatStateOf(0.1f) }
                    var maxPoints by remember { mutableIntStateOf(500) }
                    var pointCloudNodes by remember { mutableStateOf<List<PointCloudNode>>(emptyList()) }
                    var score by remember { mutableStateOf(0.0) }
                    var lastPointCloudTimestamp by remember { mutableStateOf<Long?>(null) }
                    var lastPointCloudFrame by remember { mutableStateOf<Frame?>(null) }

                    val pointCloudModelInstances = remember { mutableListOf<ModelInstance>() }

                    fun getPointCloudModelInstance(): ModelInstance? {
                        if (pointCloudModelInstances.size == 0) {
                            pointCloudModelInstances.addAll(
                                modelLoader.createInstancedModel(
                                    assetFileLocation = "models/point_cloud.glb",
                                    count = maxPoints
                                )
                            )
                        }
                        return pointCloudModelInstances.removeLastOrNull()
                    }

                    ARScene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        planeRenderer = false,
                        sessionConfiguration = { _, config ->
                            config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                        },
                        onSessionUpdated = { _, frame ->
                            frame.takeIf { it.fps(lastPointCloudFrame) < kMaxPointCloudPerSecond }
                                ?.acquirePointCloud()
                                ?.takeIf { it.timestamp != lastPointCloudTimestamp }
                                ?.use { pointCloud ->
                                    if (pointCloud.ids == null) return@use

                                    lastPointCloudFrame = frame
                                    lastPointCloudTimestamp = pointCloud.timestamp
                                    val idsBuffer = pointCloud.ids ?: return@use
                                    val pointsSize = idsBuffer.limit()
                                    val pointsBuffer = pointCloud.points
                                    val ids = mutableListOf<Int>()

                                    for (index in 0 until pointsSize) {
                                        val id = idsBuffer[index]
                                        ids += id
                                        if (pointCloudNodes.none { it.id == id }) {
                                            val pointIndex = index * 4
                                            val position = Position(
                                                pointsBuffer[pointIndex],
                                                pointsBuffer[pointIndex + 1],
                                                pointsBuffer[pointIndex + 2]
                                            )
                                            val confidence = pointsBuffer[pointIndex + 3]
                                            if (confidence > minConfidence && pointCloudNodes.size < maxPoints) {
                                                val modelInstance = getPointCloudModelInstance()
                                                modelInstance?.let {
                                                    val newNode = PointCloudNode(it, id, confidence).apply {
                                                        this.position = position
                                                    }
                                                    pointCloudNodes = pointCloudNodes + newNode
                                                }
                                            }
                                        }
                                    }

                                    pointCloudNodes = pointCloudNodes.filter { it.confidence >= minConfidence }
                                        .take(maxPoints)

                                    score = if (pointCloudNodes.isNotEmpty()) {
                                        pointCloudNodes.sumOf {
                                            it.confidence.toDouble()
                                        } / pointCloudNodes.size
                                    } else 0.0
                                }
                        }
                    ) {
                        pointCloudNodes.forEach { cloudNode ->
                            ModelNode(
                                modelInstance = cloudNode.modelInstance,
                                position = cloudNode.position
                            )
                        }
                    }

                    // ── Status overlay ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Point Cloud",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${pointCloudNodes.size} points detected",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Avg confidence: ${"%.0f".format(score * 100)}%",
                            color = if (score > 0.5) Color(0xFF4CAF50) else Color(0xFFFFA726),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // ── Bottom hint ──
                    Text(
                        text = "Move your phone slowly to detect feature points",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

class PointCloudNode(
    val modelInstance: ModelInstance,
    var id: Int,
    var confidence: Float,
    var position: Position = Position()
)
