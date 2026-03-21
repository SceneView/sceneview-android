package io.github.sceneview.sample.autopilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.google.android.filament.Skybox
import io.github.sceneview.Scene
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.LightNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.sample.SceneviewTheme
import kotlin.math.sin

// ── HUD palette ───────────────────────────────────────────────────────────────
private val HudBg         = Color(0xDD050A14)
private val AccentGreen   = Color(0xFF22C55E)
private val AccentBlue    = Color(0xFF38BDF8)
private val OtherCarColor = Color(0xFF22D3EE)
private val BusColor      = Color(0xFFF59E0B)
private val TruckColor    = Color(0xFFF97316)
private val BikeColor     = Color(0xFF10B981)
private val PedColor      = Color(0xFFFB7185)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SceneviewTheme { AutopilotScreen() } }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun AutopilotScreen() {
    val t = rememberInfiniteTransition(label = "ap")
    val roadOffset by t.animateFloat(0f, 200f,
        infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart), "road")
    val speed by t.animateFloat(92f, 118f,
        infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Reverse), "speed")
    val time by t.animateFloat(0f, 2000f,
        infiniteRepeatable(tween(2_000_000, easing = LinearEasing), RepeatMode.Restart), "time")

    Box(Modifier.fillMaxSize()) {
        AutopilotScene(roadOffset, time)
        TeslaHud(speed.toInt(), Modifier.fillMaxSize())
    }
}

// ── Material factory ───────────────────────────────────────────────────────────
// Explicitly extract R,G,B from ARGB Long — avoids Color(Long) ambiguity
private fun MaterialLoader.mat(argb: Long, roughness: Float = 0.5f): MaterialInstance {
    val r = ((argb shr 16) and 0xFFL) / 255f
    val g = ((argb shr 8)  and 0xFFL) / 255f
    val b = (argb          and 0xFFL) / 255f
    return createColorInstance(Color(r, g, b, 1f), metallic = 0f, roughness = roughness)
}

@Composable
private fun AutopilotScene(roadOffset: Float, time: Float) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    // ── Environment: Tesla FSD dark sky — no IBL, just SUN light ─────────────
    val environment = remember(engine) {
        Environment(
            indirectLight = null,
            skybox = Skybox.Builder().color(0.02f, 0.04f, 0.08f, 1.0f).build(engine),
            sphericalHarmonics = null
        )
    }

    // ── Camera: fixed bird's-eye view ─────────────────────────────────────────
    val cameraNode = rememberCameraNode(engine) {
        lookAt(
            eye    = Position(2.75f, 4.0f, 11.0f),
            center = Position(2.75f, 0.5f, -10f),
            up     = Direction(y = 1f)
        )
    }

    // ── SUN light ─────────────────────────────────────────────────────────────
    val mainLightNode = rememberMainLightNode(engine) {
        // No-op — we override with our sun node below via rememberNode
    }

    val sunNode = rememberNode {
        LightNode(engine, LightManager.Type.SUN) {
            castShadows(true)
            intensity(160_000f)
            direction(0.2f, -0.75f, -0.62f)
            color(1f, 0.97f, 0.90f)
        }
    }

    // ── Materials ─────────────────────────────────────────────────────────────
    val roadMat   = remember(materialLoader) { materialLoader.mat(0x1E2436, 0.88f) }
    val laneMat   = remember(materialLoader) { materialLoader.mat(0xFFFFFF, 0.25f) }
    val yellowMat = remember(materialLoader) { materialLoader.mat(0xFFCC00, 0.25f) }
    val groundMat = remember(materialLoader) { materialLoader.mat(0x0A0F0A, 1.00f) }
    val egoMat    = remember(materialLoader) { materialLoader.mat(0xE2E8F0, 0.20f) }
    val wheelMat  = remember(materialLoader) { materialLoader.mat(0x1E293B, 0.90f) }
    val tailMat   = remember(materialLoader) { materialLoader.mat(0xFF3333, 0.10f) }
    val carMat    = remember(materialLoader) { materialLoader.mat(0x22D3EE, 0.15f) }
    val truckMat  = remember(materialLoader) { materialLoader.mat(0xF97316, 0.15f) }
    val busMat    = remember(materialLoader) { materialLoader.mat(0xF59E0B, 0.15f) }
    val bikeMat   = remember(materialLoader) { materialLoader.mat(0x10B981, 0.20f) }
    val pedMat    = remember(materialLoader) { materialLoader.mat(0xFB7185, 0.40f) }
    val tlBoxMat  = remember(materialLoader) { materialLoader.mat(0x111111, 0.90f) }
    val tlRedMat  = remember(materialLoader) { materialLoader.mat(0xFF2200, 0.85f) }
    val tlYelMat  = remember(materialLoader) { materialLoader.mat(0xFFCC00, 0.85f) }
    val tlGrnMat  = remember(materialLoader) { materialLoader.mat(0x00FF44, 0.85f) }

    // ── Traffic light cycle state ──────────────────────────────────────────────
    val tlPhase = (time % 24f).let { if (it < 14f) 0 else if (it < 17f) 1 else 2 }

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        environment = environment,
        cameraNode = cameraNode,
        mainLightNode = sunNode,
        cameraManipulator = null
    ) {
        // ── Road (5 static nodes) ──────────────────────────────────────────────
        PlaneNode(Size(60f, 0f, 60f), materialInstance = groundMat,
            position = Position(y = -0.06f))
        PlaneNode(Size(12f, 0f, 60f), materialInstance = roadMat,
            position = Position(y = -0.01f))
        PlaneNode(Size(0.15f, 0f, 60f), materialInstance = laneMat,
            position = Position(x = -5.5f, y = 0.001f))
        PlaneNode(Size(0.15f, 0f, 60f), materialInstance = laneMat,
            position = Position(x = 5.5f, y = 0.001f))
        PlaneNode(Size(0.10f, 0f, 60f), materialInstance = laneMat,
            position = Position(x = 4.7f, y = 0.001f))

        // ── Dashed centre lines (8 tiles) — scrolling ─────────────────────────
        for (i in 0 until 8) {
            PlaneNode(Size(0.16f, 0f, 2.0f), materialInstance = yellowMat,
                position = Position(y = 0.001f, z = ((i * 4f - roadOffset % 4f) + 32f) % 32f - 16f))
        }

        // ── Ego car — white, STATIC in right lane (x = +2.75) ────────────────
        CubeNode(Size(1.85f, 0.60f, 3.70f), materialInstance = egoMat,
            position = Position(x = 2.75f, y = 0.45f, z = 2.5f))
        CubeNode(Size(1.50f, 0.42f, 1.70f), materialInstance = egoMat,
            position = Position(x = 2.75f, y = 0.93f, z = 2.2f))
        CubeNode(Size(1.65f, 0.12f, 0.07f), materialInstance = tailMat,
            position = Position(x = 2.75f, y = 0.46f, z = 4.37f))

        // Ego wheels (4 nodes)
        val bx = 2.75f; val by = 0.45f; val bz = 2.5f; val xs = 0.95f; val r = 0.29f
        CylinderNode(r, 0.2f, materialInstance = wheelMat,
            position = Position(x = bx - xs, y = by - (r + 0.04f), z = bz - 1.5f),
            rotation = Rotation(z = 90f))
        CylinderNode(r, 0.2f, materialInstance = wheelMat,
            position = Position(x = bx + xs, y = by - (r + 0.04f), z = bz - 1.5f),
            rotation = Rotation(z = 90f))
        CylinderNode(r, 0.2f, materialInstance = wheelMat,
            position = Position(x = bx - xs, y = by - (r + 0.04f), z = bz + 1.5f),
            rotation = Rotation(z = 90f))
        CylinderNode(r, 0.2f, materialInstance = wheelMat,
            position = Position(x = bx + xs, y = by - (r + 0.04f), z = bz + 1.5f),
            rotation = Rotation(z = 90f))

        // ── Truck — oscillates ahead in ego lane ───────────────────────────────
        val tZ = -15f - sin(time * 0.12f) * 7f
        CubeNode(Size(2.4f, 1.55f, 6.5f), materialInstance = truckMat,
            position = Position(x = 2.75f, y = 0.77f, z = tZ))

        // ── Oncoming — left lane ───────────────────────────────────────────────
        val c1Z = (roadOffset * 1.5f % 51f) - 35f
        CubeNode(Size(1.7f, 1.10f, 3.5f), materialInstance = carMat,
            position = Position(x = -2.75f, y = 0.55f, z = c1Z))
        val c3Z = ((roadOffset * 1.3f + 25f) % 51f) - 35f
        CubeNode(Size(1.7f, 1.10f, 3.5f), materialInstance = carMat,
            position = Position(x = -2.75f, y = 0.55f, z = c3Z))
        val bZ = (roadOffset * 0.9f % 65f) - 45f
        CubeNode(Size(2.5f, 1.80f, 7.0f), materialInstance = busMat,
            position = Position(x = -2.75f, y = 0.9f, z = bZ))

        // ── Car behind ego ─────────────────────────────────────────────────────
        CubeNode(Size(1.7f, 1.10f, 3.5f), materialInstance = carMat,
            position = Position(x = 2.75f, y = 0.55f, z = 6f + roadOffset * 0.18f % 12f))

        // ── Bikes (bike lane x≈5.0) ────────────────────────────────────────────
        val bk1z = -(roadOffset * 0.11f % 55f) + 8f
        CubeNode(Size(0.22f, 0.80f, 1.0f), materialInstance = bikeMat,
            position = Position(x = 5.0f, y = 0.65f, z = bk1z))
        CubeNode(Size(0.40f, 0.55f, 0.30f), materialInstance = pedMat,
            position = Position(x = 5.0f, y = 1.20f, z = bk1z - 0.1f))
        val bk2z = -(roadOffset * 0.14f % 55f) + 22f
        CubeNode(Size(0.22f, 0.80f, 1.0f), materialInstance = bikeMat,
            position = Position(x = 5.2f, y = 0.65f, z = bk2z))
        CubeNode(Size(0.40f, 0.55f, 0.30f), materialInstance = pedMat,
            position = Position(x = 5.2f, y = 1.20f, z = bk2z - 0.1f))

        // ── Pedestrians (sidewalk x≈6.6) ───────────────────────────────────────
        val ps = roadOffset * 0.03f
        CubeNode(Size(0.40f, 1.55f, 0.30f), materialInstance = pedMat,
            position = Position(x = 6.5f, y = 0.77f + sin(time * 3.8f) * 0.03f, z = -(ps % 30f) + 5f))
        CubeNode(Size(0.40f, 1.55f, 0.30f), materialInstance = pedMat,
            position = Position(x = 6.9f, y = 0.77f + sin(time * 3.8f + 0.6f) * 0.03f, z = -(ps % 30f) + 13f))
        CubeNode(Size(0.40f, 1.55f, 0.30f), materialInstance = pedMat,
            position = Position(x = 6.7f, y = 0.77f + sin(time * 3.8f + 1.2f) * 0.03f, z = -(ps * 0.7f % 30f) + 20f))

        // ── Traffic lights — right-side gantry at z = -12 ─────────────────────
        CubeNode(Size(0.15f, 5.0f, 0.15f), materialInstance = tlBoxMat,
            position = Position(x = 6.3f, y = 2.5f, z = -12f))
        CubeNode(Size(3.4f, 0.15f, 0.15f), materialInstance = tlBoxMat,
            position = Position(x = 4.6f, y = 5.0f, z = -12f))
        CubeNode(Size(0.42f, 1.35f, 0.32f), materialInstance = tlBoxMat,
            position = Position(x = 3.2f, y = 4.37f, z = -12f))
        // Dark lens backgrounds
        CubeNode(Size(0.26f, 0.26f, 0.20f), materialInstance = tlBoxMat,
            position = Position(x = 3.2f, y = 4.83f, z = -11.88f))
        CubeNode(Size(0.26f, 0.26f, 0.20f), materialInstance = tlBoxMat,
            position = Position(x = 3.2f, y = 4.45f, z = -11.88f))
        CubeNode(Size(0.26f, 0.26f, 0.20f), materialInstance = tlBoxMat,
            position = Position(x = 3.2f, y = 4.07f, z = -11.88f))

        // Animated glow — shown based on traffic light phase
        CubeNode(Size(0.22f, 0.22f, 0.08f), materialInstance = tlGrnMat,
            position = if (tlPhase == 0) Position(x = 3.2f, y = 4.07f, z = -11.68f)
                       else Position(y = -100f))
        CubeNode(Size(0.22f, 0.22f, 0.08f), materialInstance = tlYelMat,
            position = if (tlPhase == 1) Position(x = 3.2f, y = 4.45f, z = -11.68f)
                       else Position(y = -100f))
        CubeNode(Size(0.22f, 0.22f, 0.08f), materialInstance = tlRedMat,
            position = if (tlPhase == 2) Position(x = 3.2f, y = 4.83f, z = -11.68f)
                       else Position(y = -100f))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Tesla FSD HUD
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TeslaHud(speed: Int, modifier: Modifier = Modifier) {
    Box(modifier) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal=16.dp, vertical=8.dp)
                .background(HudBg, RoundedCornerShape(12.dp))
                .padding(horizontal=16.dp, vertical=10.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Column {
                Text("AUTOPILOT", color=AccentBlue, fontSize=10.sp, fontWeight=FontWeight.Bold)
                Text("SceneView FSD", color=Color.White, fontSize=13.sp, fontWeight=FontWeight.Medium)
            }
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text("FOLLOWING DIST", color=Color.White.copy(.40f), fontSize=9.sp)
                Text("COMFORTABLE", color=AccentGreen, fontSize=11.sp, fontWeight=FontWeight.SemiBold)
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(AccentGreen))
                Spacer(Modifier.width(6.dp))
                Text("ACTIVE", color=AccentGreen, fontSize=12.sp, fontWeight=FontWeight.Bold)
            }
        }

        // Speed + limit — bottom-left
        Column(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(16.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(52.dp).background(Color.White, CircleShape)
                    .border(3.dp, Color.Red, CircleShape),
                Alignment.Center
            ) { Text("120", color=Color.Black, fontSize=14.sp, fontWeight=FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.background(HudBg, RoundedCornerShape(14.dp))
                .padding(horizontal=22.dp, vertical=12.dp), Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("$speed",
                        color=if (speed > 115) Color(0xFFEF4444) else Color.White,
                        fontSize=52.sp, fontWeight=FontWeight.Bold, lineHeight=52.sp)
                    Text("km/h", color=Color.White.copy(.5f), fontSize=13.sp)
                }
            }
        }

        // Detections — bottom-right
        Column(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp)
                .background(HudBg, RoundedCornerShape(16.dp))
                .padding(horizontal=16.dp, vertical=12.dp),
            Arrangement.spacedBy(5.dp)
        ) {
            Text("DETECTED", color=Color.White.copy(.38f), fontSize=9.sp, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            DetRow("Cars",    3, OtherCarColor)
            DetRow("Bus",     1, BusColor)
            DetRow("Truck",   1, TruckColor)
            DetRow("Bikes",   2, BikeColor)
            DetRow("Persons", 3, PedColor)
        }

        Text("Powered by SceneView + Jetpack Compose",
            color=Color.White.copy(.18f), fontSize=10.sp,
            modifier=Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding().padding(bottom=4.dp))
    }
}

@Composable
private fun DetRow(label: String, count: Int, color: Color) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color=Color.White.copy(.8f), fontSize=13.sp)
        Spacer(Modifier.width(12.dp))
        Text("$count", color=Color.White, fontSize=13.sp, fontWeight=FontWeight.Bold)
    }
}
