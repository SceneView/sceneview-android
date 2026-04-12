package io.github.sceneview.demo.demos

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import io.github.sceneview.SceneView
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Demonstrates [MeshNode] with a hand-crafted pyramid built from raw [VertexBuffer] and
 * [IndexBuffer]. A toggle enables continuous Y-axis rotation.
 *
 * Since [MeshNode] does not expose position/rotation parameters directly, the mesh is wrapped
 * inside a parent [Node] that controls transform.
 */
@Composable
fun CustomMeshDemo(onBack: () -> Unit) {
    var rotating by remember { mutableStateOf(true) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    // Build vertex and index buffers for a 4-sided pyramid (5 vertices, 6 triangles = 18 indices).
    val vertexBuffer = remember(engine) {
        // Positions: apex + 4 base corners
        val positions = floatArrayOf(
            // apex
            0.0f, 0.6f, 0.0f,
            // base front-left
            -0.4f, -0.3f, 0.4f,
            // base front-right
            0.4f, -0.3f, 0.4f,
            // base back-right
            0.4f, -0.3f, -0.4f,
            // base back-left
            -0.4f, -0.3f, -0.4f
        )
        val posBuffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder())
            .apply { asFloatBuffer().put(positions); rewind() }

        VertexBuffer.Builder()
            .vertexCount(5)
            .bufferCount(1)
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                0,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                12
            )
            .build(engine)
            .also { it.setBufferAt(engine, 0, posBuffer) }
    }

    val indexBuffer = remember(engine) {
        // 4 side faces + 2 base triangles = 6 triangles = 18 indices
        val indices = shortArrayOf(
            // Side faces
            0, 1, 2,
            0, 2, 3,
            0, 3, 4,
            0, 4, 1,
            // Base (two triangles)
            1, 3, 2,
            1, 4, 3
        )
        val idxBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .apply { asShortBuffer().put(indices); rewind() }

        IndexBuffer.Builder()
            .indexCount(indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)
            .also { it.setBuffer(engine, idxBuffer) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "meshRotation")
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 8_000, easing = LinearEasing)),
        label = "meshRotationY"
    )

    DemoScaffold(
        title = "Custom Mesh",
        onBack = onBack,
        controls = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-Rotate", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = rotating, onCheckedChange = { rotating = it })
            }
        }
    ) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraManipulator = rememberCameraManipulator()
        ) {
            // Wrap MeshNode in a parent Node that handles rotation,
            // since MeshNode does not have position/rotation parameters.
            Node(
                rotation = if (rotating) Rotation(y = rotationY) else Rotation()
            ) {
                MeshNode(
                    primitiveType = RenderableManager.PrimitiveType.TRIANGLES,
                    vertexBuffer = vertexBuffer,
                    indexBuffer = indexBuffer,
                    materialInstance = materialLoader.createColorInstance(Color.Cyan)
                )
            }
        }
    }
}
