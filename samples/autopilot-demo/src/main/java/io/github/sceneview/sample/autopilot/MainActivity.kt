package io.github.sceneview.sample.autopilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import io.github.sceneview.Scene
import io.github.sceneview.SceneScope
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.sample.SceneviewTheme

// ── Color palette (Tesla-style dark theme) ──────────────────────────────────

private val RoadGray = Color(0xFF2A2F3B)
private val LaneMarkingWhite = Color(0xFFFFFFFF)
private val EgoCarColor = Color(0xFF3B82F6)
private val OtherCarColor = Color(0xFF94A3B8)
private val BusColor = Color(0xFFF59E0B)
private val TruckColor = Color(0xFF8B5CF6)
private val AccentGreen = Color(0xFF22C55E)
private val HudBg = Color(0xCC111827)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SceneviewTheme {
                AutopilotScreen()
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Root screen
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AutopilotScreen() {
    val transition = rememberInfiniteTransition(label = "autopilot")

    val roadOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "roadOffset"
    )

    val cameraSway by transition.animateFloat(
        initialValue = -0.06f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cameraSway"
    )

    val speed by transition.animateFloat(
        initialValue = 65f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speed"
    )

    val laneWobble by transition.animateFloat(
        initialValue = -0.12f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laneWobble"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AutopilotScene(
            roadOffset = roadOffset,
            cameraSway = cameraSway,
            laneWobble = laneWobble
        )
        HudOverlay(
            speed = speed.toInt(),
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3D Scene
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AutopilotScene(
    roadOffset: Float,
    cameraSway: Float,
    laneWobble: Float
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    // ── Materials ────────────────────────────────────────────────────────────
    val roadMat = remember(materialLoader) {
        materialLoader.createColorInstance(RoadGray, metallic = 0f, roughness = 0.9f)
    }
    val laneMat = remember(materialLoader) {
        materialLoader.createColorInstance(LaneMarkingWhite, metallic = 0f, roughness = 0.4f)
    }
    val egoMat = remember(materialLoader) {
        materialLoader.createColorInstance(EgoCarColor, metallic = 0f, roughness = 0.4f)
    }
    val carMat = remember(materialLoader) {
        materialLoader.createColorInstance(OtherCarColor, metallic = 0f, roughness = 0.5f)
    }
    val busMat = remember(materialLoader) {
        materialLoader.createColorInstance(BusColor, metallic = 0f, roughness = 0.5f)
    }
    val truckMat = remember(materialLoader) {
        materialLoader.createColorInstance(TruckColor, metallic = 0f, roughness = 0.5f)
    }
    val wheelMat = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF1E293B), metallic = 0f, roughness = 0.9f)
    }
    val glassMat = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF93C5FD), metallic = 0f, roughness = 0.3f)
    }
    val groundMat = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF0F172A), metallic = 0f, roughness = 1.0f)
    }
    val tailMat = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFFEF4444), metallic = 0f, roughness = 0.15f)
    }
    val headMat = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFFF8FAFC), metallic = 0f, roughness = 0.1f)
    }

    // ── Camera ──────────────────────────────────────────────────────────────
    val cameraNode = rememberCameraNode(engine) {
        lookAt(
            eye = Position(x = 0f, y = 3.0f, z = 8.0f),
            center = Position(x = 0f, y = 0.5f, z = -10f),
            up = Direction(y = 1f)
        )
    }

    // ── Animated vehicle base positions ──────────────────────────────────────
    val car1X = -1.8f + laneWobble * 0.25f
    val car1Y = 0.35f
    val car1Z = -(roadOffset * 1.3f % 20f) + 6f

    val busX = 1.8f
    val busY = 0.7f
    val busZ = -(roadOffset * 0.55f % 28f) - 3f

    val truckX = 0.1f + laneWobble * 0.1f
    val truckY = 0.7f
    val truckZ = -(roadOffset * 0.35f % 32f) - 10f

    val car2X = 1.8f + laneWobble * 0.15f
    val car2Y = 0.35f
    val car2Z = (roadOffset * 0.8f % 24f) - 3f

    // ── Scene ───────────────────────────────────────────────────────────────
    Scene(
        modifier = Modifier.fillMaxSize(),
        surfaceType = SurfaceType.TextureSurface,
        engine = engine,
        materialLoader = materialLoader,
        environmentLoader = environmentLoader,
        isOpaque = true,
        cameraNode = cameraNode,
        cameraManipulator = null,
        onFrame = { _ ->
            cameraNode.lookAt(
                eye = Position(x = cameraSway, y = 3.0f, z = 8.0f),
                center = Position(x = 0f, y = 0.5f, z = -10f),
                up = Direction(y = 1f)
            )
        }
    ) {

        // ── Sun light ────────────────────────────────────────────────────────
        LightNode(
            type = LightManager.Type.SUN,
            apply = {
                castShadows(true)
                intensity(100_000f)
                direction(0.2f, -1f, -0.5f)
                color(1f, 0.98f, 0.9f)
            }
        )

        // ── Ground ──────────────────────────────────────────────────────────
        PlaneNode(
            size = Size(x = 60f, y = 0f, z = 60f),
            materialInstance = groundMat,
            position = Position(y = -0.05f)
        )

        // ── Road surface ────────────────────────────────────────────────────
        PlaneNode(
            size = Size(x = 8f, y = 0f, z = 40f),
            materialInstance = roadMat,
            position = Position(y = -0.01f)
        )

        // ── Solid lane edges ────────────────────────────────────────────────
        PlaneNode(
            size = Size(x = 0.15f, y = 0f, z = 40f),
            materialInstance = laneMat,
            position = Position(x = -3.6f, y = 0.001f)
        )
        PlaneNode(
            size = Size(x = 0.15f, y = 0f, z = 40f),
            materialInstance = laneMat,
            position = Position(x = 3.6f, y = 0.001f)
        )

        // ── Dashed center line (scrolls with roadOffset) ───────────────────
        for (i in 0 until 14) {
            key("dash_$i") {
                val z = ((i * 3f - roadOffset % 3f) + 42f) % 42f - 21f
                PlaneNode(
                    size = Size(x = 0.12f, y = 0f, z = 1.5f),
                    materialInstance = laneMat,
                    position = Position(y = 0.001f, z = z)
                )
            }
        }

        // ── Ego vehicle (blue, static) ────────────────────────────────────
        CubeNode(
            size = Size(x = 1.6f, y = 0.55f, z = 3.4f),
            materialInstance = egoMat,
            position = Position(y = 0.4f, z = 2.5f)
        )
        CubeNode(
            size = Size(x = 1.3f, y = 0.4f, z = 1.4f),
            materialInstance = glassMat,
            position = Position(y = 0.87f, z = 2.3f)
        )
        CubeNode(
            size = Size(x = 1.5f, y = 0.1f, z = 0.06f),
            materialInstance = tailMat,
            position = Position(y = 0.45f, z = 4.2f)
        )
        Wheels(material = wheelMat, bx = 0f, by = 0.4f, bz = 2.5f,
            xs = 0.85f, zf = -1.2f, zr = 1.2f, r = 0.25f, tag = "ego")

        // ── Oncoming car (left lane) ───────────────────────────────────────
        CubeNode(size = Size(x = 1.5f, y = 0.5f, z = 3.0f), materialInstance = carMat,
            position = Position(x = car1X, y = car1Y, z = car1Z))
        CubeNode(size = Size(x = 1.2f, y = 0.35f, z = 1.3f), materialInstance = glassMat,
            position = Position(x = car1X, y = car1Y + 0.42f, z = car1Z - 0.1f))
        CubeNode(size = Size(x = 1.4f, y = 0.08f, z = 0.06f), materialInstance = headMat,
            position = Position(x = car1X, y = car1Y, z = car1Z - 1.5f))
        Wheels(material = wheelMat, bx = car1X, by = car1Y, bz = car1Z,
            xs = 0.78f, zf = -1.1f, zr = 1.1f, r = 0.23f, tag = "car1")

        // ── Bus (right lane, ahead) ────────────────────────────────────────
        CubeNode(size = Size(x = 2.1f, y = 1.4f, z = 5.5f), materialInstance = busMat,
            position = Position(x = busX, y = busY, z = busZ))
        CubeNode(size = Size(x = 2.12f, y = 0.55f, z = 4.5f), materialInstance = glassMat,
            position = Position(x = busX, y = busY + 0.35f, z = busZ))
        CubeNode(size = Size(x = 2.0f, y = 0.12f, z = 0.06f), materialInstance = tailMat,
            position = Position(x = busX, y = busY - 0.35f, z = busZ + 2.75f))
        Wheels(material = wheelMat, bx = busX, by = busY, bz = busZ,
            xs = 1.05f, zf = -2.0f, zr = 2.0f, r = 0.32f, tag = "bus")

        // ── Truck (same lane, far ahead) ───────────────────────────────────
        CubeNode(size = Size(x = 1.8f, y = 1.0f, z = 2.0f), materialInstance = truckMat,
            position = Position(x = truckX, y = truckY, z = truckZ - 2.0f))
        CubeNode(size = Size(x = 1.5f, y = 0.45f, z = 0.06f), materialInstance = glassMat,
            position = Position(x = truckX, y = truckY + 0.2f, z = truckZ - 2.95f))
        CubeNode(size = Size(x = 2.0f, y = 1.5f, z = 3.8f), materialInstance = carMat,
            position = Position(x = truckX, y = truckY + 0.2f, z = truckZ + 0.3f))
        CubeNode(size = Size(x = 1.9f, y = 0.12f, z = 0.06f), materialInstance = tailMat,
            position = Position(x = truckX, y = truckY - 0.15f, z = truckZ + 2.2f))
        Wheels(material = wheelMat, bx = truckX, by = truckY, bz = truckZ,
            xs = 0.95f, zf = -2.4f, zr = 1.5f, r = 0.32f, tag = "truck")

        // ── Second car (right lane, behind) ───────────────────────────────
        CubeNode(size = Size(x = 1.5f, y = 0.5f, z = 3.0f), materialInstance = carMat,
            position = Position(x = car2X, y = car2Y, z = car2Z))
        CubeNode(size = Size(x = 1.2f, y = 0.35f, z = 1.3f), materialInstance = glassMat,
            position = Position(x = car2X, y = car2Y + 0.42f, z = car2Z - 0.1f))
        CubeNode(size = Size(x = 1.4f, y = 0.08f, z = 0.06f), materialInstance = headMat,
            position = Position(x = car2X, y = car2Y, z = car2Z - 1.5f))
        Wheels(material = wheelMat, bx = car2X, by = car2Y, bz = car2Z,
            xs = 0.78f, zf = -1.1f, zr = 1.1f, r = 0.23f, tag = "car2")
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Flat wheel helper — 4 cylinders at absolute world positions
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SceneScope.Wheels(
    material: MaterialInstance,
    bx: Float, by: Float, bz: Float,
    xs: Float, zf: Float, zr: Float,
    r: Float, tag: String
) {
    val wy = by - (r + 0.05f)
    for ((i, pair) in listOf(Pair(-xs, zf), Pair(xs, zf), Pair(-xs, zr), Pair(xs, zr)).withIndex()) {
        key("${tag}_w$i") {
            CylinderNode(
                radius = r, height = 0.18f, materialInstance = material,
                position = Position(x = bx + pair.first, y = wy, z = bz + pair.second),
                rotation = Rotation(z = 90f)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Compose HUD overlay
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun HudOverlay(speed: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {

        // Top status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(HudBg, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SceneView Autopilot",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "ACTIVE",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Speed (bottom-left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(16.dp)
                .background(HudBg, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$speed",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 42.sp
            )
            Text("km/h", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }

        // Detection panel (bottom-right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
                .background(HudBg, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DetectionRow("Cars", 3, OtherCarColor)
            DetectionRow("Bus", 1, BusColor)
            DetectionRow("Truck", 1, TruckColor)
        }

        // Watermark
        Text(
            "Powered by SceneView + Jetpack Compose",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun DetectionRow(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        Text("$count", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
