package io.github.sceneview.sample.autopilot

import android.content.Context
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Skybox
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.PlaneNode
import io.github.sceneview.sample.SceneviewTheme
import kotlin.math.sin

// ── HUD color palette ─────────────────────────────────────────────────────────
private val HudBg          = Color(0xCC0A0E1A)
private val AccentGreen    = Color(0xFF22C55E)
private val AccentBlue     = Color(0xFF3B82F6)
private val OtherCarColor  = Color(0xFF94A3B8)
private val BusColor       = Color(0xFFF59E0B)
private val TruckColor     = Color(0xFF8B5CF6)
private val BikeColor      = Color(0xFF6EE7B7)
private val PedestrianColor = Color(0xFFFCA5A5)

// ── Road layout (right-hand traffic) ─────────────────────────────────────────
//   Road: x = -6 to +6  (12 m, two lanes)
//   Left edge stripe:  x = -5.5
//   Center dashes:     x =  0.0
//   Right edge stripe: x = +5.5
//   Bike lane stripe:  x = +4.7
//   Left  lane center (oncoming): x = -2.75
//   Right lane center (EGO):      x = +2.75
//   Bike lane:                    x = +5.1
//   Sidewalk:                     x = +6.5 to +8.0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SceneviewTheme { AutopilotScreen() } }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Root screen
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AutopilotScreen() {
    val transition = rememberInfiniteTransition(label = "autopilot")

    val roadOffset by transition.animateFloat(
        initialValue = 0f, targetValue = 200f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "road"
    )
    val cameraSway by transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "sway"
    )
    val speed by transition.animateFloat(
        initialValue = 95f, targetValue = 118f,
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "speed"
    )
    val time by transition.animateFloat(
        initialValue = 0f, targetValue = 2000f,
        animationSpec = infiniteRepeatable(tween(2_000_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AutopilotScene(roadOffset = roadOffset, cameraSway = cameraSway, time = time)
        TeslaHud(speed = speed.toInt(), modifier = Modifier.fillMaxSize())
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3-D Scene
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AutopilotScene(roadOffset: Float, cameraSway: Float, time: Float) {
    val context = LocalContext.current
    val sceneView = remember { AutopilotSceneView(context) }
    DisposableEffect(sceneView) { onDispose { sceneView.destroy() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { sceneView },
        update = { sv -> sv.updateAnimation(roadOffset, cameraSway, time) }
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// SceneView subclass
// ═════════════════════════════════════════════════════════════════════════════

class AutopilotSceneView(context: Context) : SceneView(context) {

    // ── Material helpers ─────────────────────────────────────────────────────
    private fun mat(argb: Long, roughness: Float): MaterialInstance =
        materialLoader.createColorInstance(Color(argb), metallic = 0f, roughness = roughness)

    // ── Materials ────────────────────────────────────────────────────────────
    private val roadMat        = mat(0xFF252832, 0.9f)
    private val laneMat        = mat(0xFFFFFFFF, 0.35f)
    private val yellowMat      = mat(0xFFFFCC00, 0.35f)
    private val groundMat      = mat(0xFF1A2E18, 1.0f)   // dark grass
    private val sidewalkMat    = mat(0xFF8B9088, 0.9f)
    private val egoMat         = mat(0xFF2563EB, 0.3f)   // vivid blue
    private val carMat         = mat(0xFF94A3B8, 0.5f)
    private val busMat         = mat(0xFFF59E0B, 0.5f)
    private val truckMat       = mat(0xFF8B5CF6, 0.5f)
    private val bikeMat        = mat(0xFF6EE7B7, 0.5f)
    private val personSkinMat  = mat(0xFFFCA5A5, 0.7f)
    private val personClothMat = mat(0xFF374151, 0.8f)
    private val wheelMat       = mat(0xFF1E293B, 0.95f)
    private val glassMat       = mat(0xFF93C5FD, 0.25f)
    private val tailMat        = mat(0xFFEF4444, 0.15f)
    private val headlightMat   = mat(0xFFF8FAFC, 0.1f)
    private val tlGreenLitMat  = mat(0xFF00FF44, 0.1f)
    private val tlOffMat       = mat(0xFF1C1C1C, 0.9f)
    private val poleMat        = mat(0xFF78716C, 0.7f)
    private val guardrailMat   = mat(0xFFB0B8C0, 0.55f)
    private val treeMat        = mat(0xFF166534, 0.95f)
    private val trunkMat       = mat(0xFF78350F, 0.95f)
    private val buildingMat    = mat(0xFF374151, 0.8f)
    private val windowMat      = mat(0xFF7DD3FC, 0.2f)

    // ── Ground and road ──────────────────────────────────────────────────────
    private val ground    = PlaneNode(engine, Size(80f, 0f, 80f), materialInstance = groundMat)
        .apply { position = Position(y = -0.06f) }
    private val road      = PlaneNode(engine, Size(12f, 0f, 60f), materialInstance = roadMat)
        .apply { position = Position(y = -0.01f) }
    private val sidewalk  = PlaneNode(engine, Size(2.5f, 0f, 60f), materialInstance = sidewalkMat)
        .apply { position = Position(x = 6.8f, y = 0f) }
    private val shoulderL = PlaneNode(engine, Size(1.5f, 0f, 60f), materialInstance = sidewalkMat)
        .apply { position = Position(x = -6.8f, y = 0f) }

    // ── Lane markings ────────────────────────────────────────────────────────
    private val edgeLeft  = PlaneNode(engine, Size(0.15f, 0f, 60f), materialInstance = laneMat)
        .apply { position = Position(x = -5.5f, y = 0.001f) }
    private val edgeRight = PlaneNode(engine, Size(0.15f, 0f, 60f), materialInstance = laneMat)
        .apply { position = Position(x = 5.5f, y = 0.001f) }
    private val bikeLine  = PlaneNode(engine, Size(0.1f, 0f, 60f), materialInstance = laneMat)
        .apply { position = Position(x = 4.7f, y = 0.001f) }
    private val centerDashes = Array(18) {
        PlaneNode(engine, Size(0.16f, 0f, 2.0f), materialInstance = yellowMat)
            .apply { position = Position(y = 0.001f) }
    }

    // ── Guard rails ──────────────────────────────────────────────────────────
    private val railL = CubeNode(engine, Size(0.18f, 0.45f, 60f), materialInstance = guardrailMat)
        .apply { position = Position(x = -5.9f, y = 0.22f) }
    private val railR = CubeNode(engine, Size(0.18f, 0.45f, 60f), materialInstance = guardrailMat)
        .apply { position = Position(x = 5.9f, y = 0.22f) }

    // ── Ego vehicle (right lane x = +2.75, static) ──────────────────────────
    private val egoBody  = CubeNode(engine, Size(1.85f, 0.58f, 3.7f), materialInstance = egoMat)
        .apply { position = Position(x = 2.75f, y = 0.44f, z = 2.5f) }
    private val egoRoof  = CubeNode(engine, Size(1.5f, 0.44f, 1.7f), materialInstance = glassMat)
        .apply { position = Position(x = 2.75f, y = 0.94f, z = 2.2f) }
    private val egoTail  = CubeNode(engine, Size(1.7f, 0.13f, 0.07f), materialInstance = tailMat)
        .apply { position = Position(x = 2.75f, y = 0.46f, z = 4.37f) }
    private val egoHead  = CubeNode(engine, Size(1.7f, 0.13f, 0.07f), materialInstance = headlightMat)
        .apply { position = Position(x = 2.75f, y = 0.46f, z = 0.65f) }
    private val egoWheels = makeWheels(2.75f, 0.44f, 2.5f, 0.96f, -1.5f, 1.5f, 0.29f)

    // ── Truck ahead, same lane ───────────────────────────────────────────────
    private val truckCab   = CubeNode(engine, Size(2.0f, 1.1f, 2.2f), materialInstance = truckMat)
    private val truckWind  = CubeNode(engine, Size(1.7f, 0.5f, 0.07f), materialInstance = glassMat)
    private val truckBed   = CubeNode(engine, Size(2.2f, 1.7f, 4.4f), materialInstance = carMat)
    private val truckTail  = CubeNode(engine, Size(2.1f, 0.14f, 0.07f), materialInstance = tailMat)
    private val truckWheels = makeWheels(0f, 0.78f, 0f, 1.1f, -2.7f, 1.7f, 0.36f)

    // ── Oncoming car ─────────────────────────────────────────────────────────
    private val car1Body   = CubeNode(engine, Size(1.65f, 0.53f, 3.3f), materialInstance = carMat)
    private val car1Roof   = CubeNode(engine, Size(1.3f, 0.38f, 1.45f), materialInstance = glassMat)
    private val car1Head   = CubeNode(engine, Size(1.5f, 0.11f, 0.07f), materialInstance = headlightMat)
    private val car1Wheels = makeWheels(0f, 0.37f, 0f, 0.85f, -1.25f, 1.25f, 0.26f)

    // ── Oncoming bus ─────────────────────────────────────────────────────────
    private val busBody    = CubeNode(engine, Size(2.5f, 1.7f, 7.0f), materialInstance = busMat)
    private val busWin     = CubeNode(engine, Size(2.52f, 0.7f, 5.8f), materialInstance = glassMat)
    private val busTail    = CubeNode(engine, Size(2.4f, 0.15f, 0.07f), materialInstance = tailMat)
    private val busWheels  = makeWheels(0f, 0.85f, 0f, 1.25f, -2.8f, 2.8f, 0.4f)

    // ── Car behind ego ────────────────────────────────────────────────────────
    private val car2Body   = CubeNode(engine, Size(1.65f, 0.53f, 3.3f), materialInstance = carMat)
    private val car2Roof   = CubeNode(engine, Size(1.3f, 0.38f, 1.45f), materialInstance = glassMat)
    private val car2Tail   = CubeNode(engine, Size(1.5f, 0.11f, 0.07f), materialInstance = tailMat)
    private val car2Wheels = makeWheels(0f, 0.37f, 0f, 0.85f, -1.25f, 1.25f, 0.26f)

    // ── Bikes ────────────────────────────────────────────────────────────────
    private val bike1Frame  = CubeNode(engine, Size(0.07f, 0.45f, 1.05f), materialInstance = bikeMat)
    private val bike1WheelF = cylNode(0.29f, 0.07f)
    private val bike1WheelR = cylNode(0.29f, 0.07f)
    private val bike1Rider  = CubeNode(engine, Size(0.3f, 0.55f, 0.22f), materialInstance = personClothMat)
    private val bike1Head   = CubeNode(engine, Size(0.24f, 0.24f, 0.24f), materialInstance = personSkinMat)

    private val bike2Frame  = CubeNode(engine, Size(0.07f, 0.45f, 1.05f), materialInstance = bikeMat)
    private val bike2WheelF = cylNode(0.29f, 0.07f)
    private val bike2WheelR = cylNode(0.29f, 0.07f)
    private val bike2Rider  = CubeNode(engine, Size(0.3f, 0.55f, 0.22f), materialInstance = personClothMat)
    private val bike2Head   = CubeNode(engine, Size(0.24f, 0.24f, 0.24f), materialInstance = personSkinMat)

    // ── Pedestrians ──────────────────────────────────────────────────────────
    private val p1Legs  = CubeNode(engine, Size(0.36f, 0.55f, 0.22f), materialInstance = personClothMat)
    private val p1Torso = CubeNode(engine, Size(0.4f, 0.65f, 0.24f), materialInstance = personClothMat)
    private val p1Head  = CubeNode(engine, Size(0.29f, 0.29f, 0.29f), materialInstance = personSkinMat)

    private val p2Legs  = CubeNode(engine, Size(0.36f, 0.55f, 0.22f), materialInstance = personSkinMat)
    private val p2Torso = CubeNode(engine, Size(0.4f, 0.65f, 0.24f), materialInstance = personSkinMat)
    private val p2Head  = CubeNode(engine, Size(0.29f, 0.29f, 0.29f), materialInstance = personSkinMat)

    private val p3Legs  = CubeNode(engine, Size(0.36f, 0.55f, 0.22f), materialInstance = personClothMat)
    private val p3Torso = CubeNode(engine, Size(0.4f, 0.65f, 0.24f), materialInstance = personClothMat)
    private val p3Head  = CubeNode(engine, Size(0.29f, 0.29f, 0.29f), materialInstance = personSkinMat)

    // ── Traffic lights (right side, z = -24) ────────────────────────────────
    private val tlPoleR    = CubeNode(engine, Size(0.15f, 5.0f, 0.15f), materialInstance = poleMat)
        .apply { position = Position(x = 6.3f, y = 2.5f, z = -24f) }
    private val tlArmR     = CubeNode(engine, Size(3.0f, 0.15f, 0.15f), materialInstance = poleMat)
        .apply { position = Position(x = 4.8f, y = 5.0f, z = -24f) }
    private val tlBoxR     = CubeNode(engine, Size(0.4f, 1.3f, 0.32f), materialInstance = poleMat)
        .apply { position = Position(x = 3.3f, y = 4.35f, z = -24f) }
    private val tlRedR     = CubeNode(engine, Size(0.26f, 0.26f, 0.22f), materialInstance = tlOffMat)
        .apply { position = Position(x = 3.3f, y = 4.83f, z = -24f) }
    private val tlYellowR  = CubeNode(engine, Size(0.26f, 0.26f, 0.22f), materialInstance = tlOffMat)
        .apply { position = Position(x = 3.3f, y = 4.45f, z = -24f) }
    private val tlGreenR   = CubeNode(engine, Size(0.26f, 0.26f, 0.22f), materialInstance = tlGreenLitMat)
        .apply { position = Position(x = 3.3f, y = 4.05f, z = -24f) }

    // Left side traffic light
    private val tlPoleL    = CubeNode(engine, Size(0.15f, 5.0f, 0.15f), materialInstance = poleMat)
        .apply { position = Position(x = -6.3f, y = 2.5f, z = -24f) }
    private val tlArmL     = CubeNode(engine, Size(3.0f, 0.15f, 0.15f), materialInstance = poleMat)
        .apply { position = Position(x = -4.8f, y = 5.0f, z = -24f) }
    private val tlBoxL     = CubeNode(engine, Size(0.4f, 1.3f, 0.32f), materialInstance = poleMat)
        .apply { position = Position(x = -3.3f, y = 4.35f, z = -24f) }
    private val tlGreenL   = CubeNode(engine, Size(0.26f, 0.26f, 0.22f), materialInstance = tlGreenLitMat)
        .apply { position = Position(x = -3.3f, y = 4.05f, z = -24f) }

    // ── Trees (both sides, static) ────────────────────────────────────────────
    private val trees = (-5..5).flatMap { i ->
        val z = i * 9f - 12f
        listOf(
            CubeNode(engine, Size(2.2f, 3.0f, 2.2f), materialInstance = treeMat)
                .apply { position = Position(x = -8.5f, y = 1.5f, z = z) },
            CubeNode(engine, Size(0.3f, 1.8f, 0.3f), materialInstance = trunkMat)
                .apply { position = Position(x = -8.5f, y = -0.4f, z = z) },
            CubeNode(engine, Size(1.9f, 2.6f, 1.9f), materialInstance = treeMat)
                .apply { position = Position(x = 9.2f, y = 1.3f, z = z + 3f) },
            CubeNode(engine, Size(0.28f, 1.6f, 0.28f), materialInstance = trunkMat)
                .apply { position = Position(x = 9.2f, y = -0.4f, z = z + 3f) }
        )
    }

    // ── Buildings (right background) ─────────────────────────────────────────
    private val buildings = listOf(
        Triple(11.5f, -6f,  Pair(5f, 9f)),
        Triple(13.5f, -14f, Pair(4f, 13f)),
        Triple(11.0f, -22f, Pair(6f, 7f)),
        Triple(14.0f, -30f, Pair(4.5f, 11f))
    ).flatMap { (x, z, wh) ->
        val (w, h) = wh
        listOf(
            CubeNode(engine, Size(w, h, 5f), materialInstance = buildingMat)
                .apply { position = Position(x = x, y = h / 2f, z = z) },
            CubeNode(engine, Size(w * 0.75f, h * 0.55f, 0.1f), materialInstance = windowMat)
                .apply { position = Position(x = x, y = h * 0.6f, z = z - 2.55f) }
        )
    }

    // ── Sun light ────────────────────────────────────────────────────────────
    private val sunLight = LightNode(engine, LightManager.Type.SUN) {
        castShadows(true)
        intensity(110_000f)
        direction(0.35f, -1f, -0.45f)
        color(1f, 0.96f, 0.88f)
    }

    // ── Wheel helpers ────────────────────────────────────────────────────────
    private fun cylNode(r: Float, h: Float) =
        CylinderNode(engine, r, h, materialInstance = wheelMat)
            .apply { rotation = Rotation(z = 90f) }

    private fun makeWheels(
        bx: Float, by: Float, bz: Float,
        xs: Float, zf: Float, zr: Float, r: Float
    ): List<CylinderNode> {
        val wy = by - (r + 0.04f)
        return listOf(Pair(-xs, zf), Pair(xs, zf), Pair(-xs, zr), Pair(xs, zr)).map { (dx, dz) ->
            CylinderNode(engine, r, 0.2f, materialInstance = wheelMat).apply {
                position = Position(x = bx + dx, y = wy, z = bz + dz)
                rotation = Rotation(z = 90f)
            }
        }
    }

    private fun updateWheels(
        wheels: List<CylinderNode>,
        bx: Float, by: Float, bz: Float,
        xs: Float, zf: Float, zr: Float, r: Float
    ) {
        val wy = by - (r + 0.04f)
        listOf(Pair(-xs, zf), Pair(xs, zf), Pair(-xs, zr), Pair(xs, zr))
            .forEachIndexed { i, (dx, dz) ->
                wheels[i].position = Position(x = bx + dx, y = wy, z = bz + dz)
            }
    }

    // ── Init ─────────────────────────────────────────────────────────────────
    init {
        // Daytime sky — SH-based ambient for soft fill light (FL1-safe, no GPU compute)
        skybox = Skybox.Builder().color(0.47f, 0.73f, 0.97f, 1.0f).build(engine)
        indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(0.45f, 0.50f, 0.60f))
            .intensity(18_000f)
            .build(engine)
        cameraManipulator = null

        // Camera behind ego car, centered on right lane
        cameraNode.lookAt(
            eye    = Position(2.75f, 3.6f, 10.0f),
            center = Position(2.75f, 0.7f, -12f),
            up     = Direction(y = 1f)
        )

        childNodes = buildList {
            add(sunLight)
            // Road
            addAll(listOf(ground, road, sidewalk, shoulderL))
            addAll(listOf(edgeLeft, edgeRight, bikeLine))
            addAll(centerDashes)
            addAll(listOf(railL, railR))
            // Ego
            addAll(listOf(egoBody, egoRoof, egoTail, egoHead))
            addAll(egoWheels)
            // Truck ahead
            addAll(listOf(truckCab, truckWind, truckBed, truckTail))
            addAll(truckWheels)
            // Oncoming car
            addAll(listOf(car1Body, car1Roof, car1Head))
            addAll(car1Wheels)
            // Oncoming bus
            addAll(listOf(busBody, busWin, busTail))
            addAll(busWheels)
            // Car behind
            addAll(listOf(car2Body, car2Roof, car2Tail))
            addAll(car2Wheels)
            // Bikes
            addAll(listOf(bike1Frame, bike1WheelF, bike1WheelR, bike1Rider, bike1Head))
            addAll(listOf(bike2Frame, bike2WheelF, bike2WheelR, bike2Rider, bike2Head))
            // Pedestrians
            addAll(listOf(p1Legs, p1Torso, p1Head))
            addAll(listOf(p2Legs, p2Torso, p2Head))
            addAll(listOf(p3Legs, p3Torso, p3Head))
            // Traffic lights
            addAll(listOf(tlPoleR, tlArmR, tlBoxR, tlRedR, tlYellowR, tlGreenR))
            addAll(listOf(tlPoleL, tlArmL, tlBoxL, tlGreenL))
            // Environment
            addAll(trees)
            addAll(buildings)
        }
    }

    // ── Per-frame update ─────────────────────────────────────────────────────
    fun updateAnimation(roadOffset: Float, cameraSway: Float, time: Float) {
        // Camera gently sways around ego lane center
        cameraNode.lookAt(
            eye    = Position(2.75f + cameraSway * 0.12f, 3.6f, 10.0f),
            center = Position(2.75f, 0.7f, -12f),
            up     = Direction(y = 1f)
        )

        // Dashed center lines scroll forward
        val dashSpacing = 4.0f
        centerDashes.forEachIndexed { i, dash ->
            val z = ((i * dashSpacing - roadOffset % dashSpacing) + 72f) % 72f - 36f
            dash.position = Position(y = 0.001f, z = z)
        }

        // Truck (ego lane, far ahead, slowly closing)
        val tX = 2.75f; val tY = 0.78f
        val tZ = -(roadOffset * 0.28f % 42f) - 12f
        truckCab.position  = Position(x = tX, y = tY,          z = tZ - 2.3f)
        truckWind.position = Position(x = tX, y = tY + 0.27f,  z = tZ - 3.35f)
        truckBed.position  = Position(x = tX, y = tY + 0.22f,  z = tZ + 0.4f)
        truckTail.position = Position(x = tX, y = tY - 0.2f,   z = tZ + 2.5f)
        updateWheels(truckWheels, tX, tY, tZ, 1.1f, -2.7f, 1.7f, 0.36f)

        // Car 1 (oncoming, left lane) — moving toward camera
        val c1X = -2.75f; val c1Y = 0.36f
        val c1Z = -(roadOffset * 1.7f % 28f) + 10f
        car1Body.position  = Position(x = c1X, y = c1Y,         z = c1Z)
        car1Roof.position  = Position(x = c1X, y = c1Y + 0.45f, z = c1Z - 0.1f)
        car1Head.position  = Position(x = c1X, y = c1Y + 0.06f, z = c1Z - 1.65f)
        updateWheels(car1Wheels, c1X, c1Y, c1Z, 0.85f, -1.25f, 1.25f, 0.26f)

        // Bus (oncoming, left lane)
        val bX = -2.75f; val bY = 0.85f
        val bZ = -(roadOffset * 0.95f % 50f) + 18f
        busBody.position = Position(x = bX, y = bY,          z = bZ)
        busWin.position  = Position(x = bX, y = bY + 0.4f,   z = bZ)
        busTail.position = Position(x = bX, y = bY - 0.42f,  z = bZ + 3.5f)
        updateWheels(busWheels, bX, bY, bZ, 1.25f, -2.8f, 2.8f, 0.4f)

        // Car 2 (behind ego, same lane)
        val c2X = 2.75f; val c2Y = 0.36f
        val c2Z = (roadOffset * 0.55f % 32f) + 5f
        car2Body.position  = Position(x = c2X, y = c2Y,         z = c2Z)
        car2Roof.position  = Position(x = c2X, y = c2Y + 0.45f, z = c2Z - 0.1f)
        car2Tail.position  = Position(x = c2X, y = c2Y + 0.06f, z = c2Z + 1.65f)
        updateWheels(car2Wheels, c2X, c2Y, c2Z, 0.85f, -1.25f, 1.25f, 0.26f)

        // Bikes (bike lane x ≈ +5.1)
        updateBike(bike1Frame, bike1WheelF, bike1WheelR, bike1Rider, bike1Head,
            bx = 5.1f, bz = -(roadOffset * 0.13f % 55f) + 8f)
        updateBike(bike2Frame, bike2WheelF, bike2WheelR, bike2Rider, bike2Head,
            bx = 5.3f, bz = -(roadOffset * 0.16f % 55f) + 20f)

        // Pedestrians (sidewalk x ≈ +6.6 to +7.0)
        val pSpeed = roadOffset * 0.035f
        updatePed(p1Legs, p1Torso, p1Head, px = 6.6f, pz = -(pSpeed % 35f) + 6f,  t = time)
        updatePed(p2Legs, p2Torso, p2Head, px = 7.0f, pz = -(pSpeed % 35f) + 14f, t = time + 0.6f)
        updatePed(p3Legs, p3Torso, p3Head, px = 6.7f, pz = -(pSpeed * 0.65f % 35f) + 22f, t = time + 1.2f)
    }

    private fun updateBike(
        frame: CubeNode, wf: CylinderNode, wr: CylinderNode,
        rider: CubeNode, head: CubeNode,
        bx: Float, bz: Float
    ) {
        frame.position = Position(x = bx, y = 0.6f,  z = bz)
        wf.position    = Position(x = bx, y = 0.29f, z = bz - 0.48f)
        wr.position    = Position(x = bx, y = 0.29f, z = bz + 0.48f)
        rider.position = Position(x = bx, y = 1.0f,  z = bz - 0.08f)
        head.position  = Position(x = bx, y = 1.42f, z = bz - 0.08f)
    }

    private fun updatePed(
        legs: CubeNode, torso: CubeNode, head: CubeNode,
        px: Float, pz: Float, t: Float
    ) {
        val bob = sin(t * 3.8f) * 0.03f
        legs.position  = Position(x = px, y = 0.29f + bob, z = pz)
        torso.position = Position(x = px, y = 0.85f + bob, z = pz)
        head.position  = Position(x = px, y = 1.35f + bob, z = pz)
    }

    // ── Destroy ──────────────────────────────────────────────────────────────
    override fun destroy() {
        listOf(
            roadMat, laneMat, yellowMat, groundMat, sidewalkMat, egoMat, carMat,
            busMat, truckMat, bikeMat, personSkinMat, personClothMat, wheelMat,
            glassMat, tailMat, headlightMat, tlGreenLitMat, tlOffMat, poleMat,
            guardrailMat, treeMat, trunkMat, buildingMat, windowMat
        ).forEach { engine.destroyMaterialInstance(it) }
        super.destroy()
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Tesla-style HUD overlay
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TeslaHud(speed: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {

        // Top bar
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
            Column {
                Text("AUTOPILOT", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("SceneView Driving AI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FOLLOWING", color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp)
                Text("COMFORTABLE", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(AccentGreen))
                Spacer(Modifier.width(6.dp))
                Text("ACTIVE", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Speed + limit (bottom-left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speed limit sign
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White, CircleShape)
                    .border(3.dp, Color.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("120", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(HudBg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$speed",
                        color = if (speed > 115) Color(0xFFEF4444) else Color.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 52.sp
                    )
                    Text("km/h", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
                }
            }
        }

        // Detection panel (bottom-right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
                .background(HudBg, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "DETECTED OBJECTS",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            DetectionRow("Cars",    3, OtherCarColor)
            DetectionRow("Bus",     1, BusColor)
            DetectionRow("Truck",   1, TruckColor)
            DetectionRow("Bikes",   2, BikeColor)
            DetectionRow("Persons", 3, PedestrianColor)
        }

        Text(
            "Powered by SceneView + Jetpack Compose",
            color = Color.White.copy(alpha = 0.22f),
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
