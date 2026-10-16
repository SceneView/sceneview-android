package io.github.sceneview.demo.demos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Colors
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.View
import io.github.sceneview.SceneScope
import io.github.sceneview.demo.ai.scene.DimensionKind
import io.github.sceneview.demo.ai.scene.Look
import io.github.sceneview.demo.ai.scene.OpeningKind
import io.github.sceneview.demo.ai.scene.OrientedBox
import io.github.sceneview.demo.ai.scene.RoomModel
import io.github.sceneview.demo.ai.scene.RoomOpening
import io.github.sceneview.demo.ai.scene.RoomWall
import io.github.sceneview.demo.ai.scene.SceneToolExecutor
import io.github.sceneview.demo.demos.internal.RoomStage
import io.github.sceneview.demo.demos.internal.TrayStage
import io.github.sceneview.demo.demos.internal.Vec3
import io.github.sceneview.demo.theme.DebugPalette
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The AI Scene's 3D layer, drawn over the recorded cloud by [ArDebugSceneView]'s content slot:
 * the room the model rebuilt — plaster walls with their windows glazed, an oak floor, the
 * furniture the scan recognised and the catalogue items the AI placed, all lit and shadowed —
 * then what the model looked at while doing so (rays, probed boxes, tried walls) and the
 * dimensions it computed. Every size comes from [RoomModel]; nothing here measures.
 */

/** The unlit inks of the analysis overlays, from the stage's own debug palette (`DESIGN.md`). */
internal class RoomInks(
    val look: MaterialInstance,
    val lookMiss: MaterialInstance,
    val refused: MaterialInstance,
    val region: MaterialInstance,
    val dimension: MaterialInstance,
) {
    val all: List<MaterialInstance>
        get() = listOf(look, lookMiss, refused, region, dimension)

    companion object {
        /** On the main thread, as every Filament call. Destroy [all] after the SceneView. */
        fun of(loader: MaterialLoader, palette: DebugPalette, danger: Color): RoomInks {
            fun ink(color: Color) = loader.createUnlitColorInstance(color)
            return RoomInks(
                look = ink(palette.livePoint),
                lookMiss = ink(palette.livePoint.copy(alpha = LOOK_MISS_ALPHA)),
                refused = ink(danger),
                region = ink(palette.anchor.copy(alpha = REGION_ALPHA)),
                dimension = ink(palette.floorOutline),
            )
        }
    }
}

/**
 * The lit finishes of the rebuilt room (`DESIGN.md` `stage-room-*`): the same in both themes,
 * because the room is an object on the stage. Created once with [rememberRoomFinish].
 */
internal class RoomFinish(
    val plaster: MaterialInstance,
    val skirting: MaterialInstance,
    val oak: List<MaterialInstance>,
    val walnut: MaterialInstance,
    val slab: MaterialInstance,
    val frame: MaterialInstance,
    val glass: MaterialInstance,
    val frosted: MaterialInstance,
    val linen: MaterialInstance,
    val pillow: MaterialInstance,
    val throwFabric: MaterialInstance,
    val headboard: MaterialInstance,
    val radiator: MaterialInstance,
    val pot: MaterialInstance,
    val leaf: MaterialInstance,
    val leafLight: MaterialInstance,
    val brass: MaterialInstance,
    val shade: MaterialInstance,
) {
    val all: List<MaterialInstance>
        get() = oak + listOf(
            plaster, skirting, walnut, slab, frame, glass, frosted, linen, pillow, throwFabric,
            headboard, radiator, pot, leaf, leafLight, brass, shade,
        )

    /** The finish of a catalogue material name (`oak`, `walnut`, `linen`…). */
    fun item(material: String): MaterialInstance = when (material) {
        "walnut" -> walnut
        "linen" -> linen
        "charcoal" -> frame
        "terracotta" -> pot
        "sage" -> leafLight
        else -> oak.first()
    }
}

/** The room's finishes for the life of the screen, destroyed with it. Main thread. */
@Composable
internal fun rememberRoomFinish(materialLoader: MaterialLoader): RoomFinish {
    val finish = remember(materialLoader) { createRoomFinish(materialLoader) }
    DisposableEffect(finish) {
        onDispose { finish.all.forEach(materialLoader::destroyMaterialInstance) }
    }
    return finish
}

private fun createRoomFinish(loader: MaterialLoader): RoomFinish {
    val wood = loader.createMaterial("materials/tray_wood.filamat")
    val pbr = loader.createMaterial("materials/studio_pbr.filamat")
    val glass = loader.createMaterial("materials/studio_glass.filamat")
    fun grain(early: Color, late: Color, rings: Float, offset: Position) = loader.createInstance(wood).apply {
        setSrgb("earlyColor", early)
        setSrgb("lateColor", late)
        setParameter("grainAxis", 1f, 0f, 0f)
        setParameter("grainOffset", offset.x, offset.y, offset.z)
        setParameter("ringDensity", rings)
        setParameter("roughness", RoomStage.OAK_ROUGHNESS)
        setParameter("clearCoat", RoomStage.OAK_VARNISH)
        setParameter("clearCoatRoughness", RoomStage.OAK_VARNISH_ROUGHNESS)
    }
    fun studio(color: Color, roughness: Float, metallic: Float = 0f, sheen: Color? = null) =
        loader.createInstance(pbr).apply {
            setParameter("color", Colors.RgbaType.SRGB, color.red, color.green, color.blue, 1f)
            setParameter("metallic", metallic)
            setParameter("roughness", roughness)
            setParameter("reflectance", DIELECTRIC_REFLECTANCE)
            // One shader serves every look: each optional layer is written, an unwritten uniform
            // is not a guaranteed zero.
            setParameter("clearCoat", 0f)
            setParameter("clearCoatRoughness", 0f)
            setSrgb("sheenColor", sheen ?: Color.Black)
            setParameter("sheenRoughness", if (sheen != null) SHEEN_ROUGHNESS else 0f)
            setParameter("emissive", 0f, 0f, 0f)
        }
    fun pane(roughness: Float) = loader.createInstance(glass).apply {
        setParameter(
            "color",
            Colors.RgbaType.SRGB,
            RoomStage.GLASS.red,
            RoomStage.GLASS.green,
            RoomStage.GLASS.blue,
            1f,
        )
        setParameter("roughness", roughness)
        setParameter("reflectance", DIELECTRIC_REFLECTANCE)
        setParameter("transmission", 1f)
        setParameter("ior", RoomStage.GLASS_IOR)
    }
    return RoomFinish(
        plaster = studio(RoomStage.PLASTER, RoomStage.PLASTER_ROUGHNESS),
        skirting = studio(RoomStage.SKIRTING, RoomStage.SKIRTING_ROUGHNESS),
        // Three cuts of the same oak, so neighbouring planks never share a grain.
        oak = OAK_GRAIN_OFFSETS.map { grain(RoomStage.OAK_EARLY, RoomStage.OAK_LATE, RoomStage.OAK_RING_DENSITY, it) },
        walnut = grain(
            TrayStage.WALNUT_EARLY,
            TrayStage.WALNUT_LATE,
            TrayStage.WALNUT_RING_DENSITY,
            OAK_GRAIN_OFFSETS[0],
        ),
        slab = studio(RoomStage.SLAB, RoomStage.PLASTER_ROUGHNESS),
        frame = studio(RoomStage.FRAME, RoomStage.FRAME_ROUGHNESS, metallic = 1f),
        glass = pane(RoomStage.GLASS_ROUGHNESS),
        frosted = pane(FROSTED_ROUGHNESS),
        linen = studio(RoomStage.LINEN, RoomStage.FABRIC_ROUGHNESS, sheen = RoomStage.PILLOW),
        pillow = studio(RoomStage.PILLOW, RoomStage.FABRIC_ROUGHNESS, sheen = RoomStage.PILLOW),
        throwFabric = studio(RoomStage.THROW, RoomStage.FABRIC_ROUGHNESS, sheen = RoomStage.LINEN),
        headboard = studio(RoomStage.HEADBOARD, RoomStage.FABRIC_ROUGHNESS, sheen = RoomStage.LINEN),
        radiator = studio(RoomStage.RADIATOR, RoomStage.RADIATOR_ROUGHNESS),
        pot = studio(RoomStage.POT, RoomStage.PLASTER_ROUGHNESS),
        leaf = studio(RoomStage.LEAF, RoomStage.LEAF_ROUGHNESS),
        leafLight = studio(RoomStage.LEAF_LIGHT, RoomStage.LEAF_ROUGHNESS),
        brass = studio(RoomStage.BRASS, RoomStage.BRASS_ROUGHNESS, metallic = 1f),
        shade = studio(RoomStage.SHADE, RoomStage.FABRIC_ROUGHNESS, sheen = RoomStage.PILLOW),
    )
}

private fun MaterialInstance.setSrgb(name: String, color: Color) =
    setParameter(name, Colors.RgbType.SRGB, color.red, color.green, color.blue)

/**
 * The rebuilt room's light: shadows on (PCF: DPCF cuts flat lids into triangles), and ambient
 * occlusion in the corners. Called once the debug view is configured, which turns both off for
 * the bare cloud.
 */
internal fun View.configureForRoom() {
    setShadowingEnabled(true)
    setShadowType(View.ShadowType.PCF)
    ambientOcclusionOptions = ambientOcclusionOptions.apply {
        enabled = true
        intensity = AO_INTENSITY
        radius = AO_RADIUS_M
    }
}

/** A label's colours: the overlay card's fill and text, so a label reads like the HUD. */
internal class LabelInk(val text: Int, val background: Int)

/** One solid piece of a wall, wall-local metres: [x0]…[x1] along it, [y0]…[y1] above the floor. */
internal data class WallPiece(val x0: Float, val x1: Float, val y0: Float, val y1: Float)

/**
 * The wall [wall] as solid pieces around its [openings]: full height between them, below each
 * sill and above each head inside them. Openings are clamped to the wall; overlaps merge.
 */
internal fun wallPieces(wall: RoomWall, openings: List<RoomOpening>): List<WallPiece> {
    val length = wall.length
    val height = wall.height
    val cuts = openings
        .map { o -> o.copy(
            from = o.from.coerceIn(0f, length),
            to = o.to.coerceIn(0f, length),
            sill = o.sill.coerceIn(0f, height),
            head = o.head.coerceIn(0f, height),
        ) }
        .filter { it.to - it.from > MIN_PIECE_M }
        .sortedBy { it.from }
    val pieces = ArrayList<WallPiece>()
    var cursor = 0f
    for (cut in cuts) {
        val start = cut.from.coerceAtLeast(cursor)
        if (start - cursor > MIN_PIECE_M) pieces += WallPiece(cursor, start, 0f, height)
        if (cut.to > start) {
            if (cut.sill > MIN_PIECE_M) pieces += WallPiece(start, cut.to, 0f, cut.sill)
            if (height - cut.head > MIN_PIECE_M) pieces += WallPiece(start, cut.to, cut.head, height)
        }
        cursor = cursor.coerceAtLeast(cut.to)
    }
    if (length - cursor > MIN_PIECE_M) pieces += WallPiece(cursor, length, 0f, height)
    return pieces
}

/**
 * The floor under a wall, in that wall's frame (x along it from `from`, z across it): which side
 * the room is on ([inward], ±1 on z — where the furniture, items and free floor the AI found
 * lie), and the extent that covers all of it with a margin. On the wall's side it stops at the
 * wall's back face.
 */
internal data class RoomFloor(val inward: Float, val x0: Float, val x1: Float, val z0: Float, val z1: Float)

internal fun roomFloor(wall: RoomWall, model: RoomModel): RoomFloor {
    val l = wall.length.coerceAtLeast(MIN_PIECE_M)
    val ux = (wall.to.x - wall.from.x) / l
    val uz = (wall.to.z - wall.from.z) / l
    val corners = (model.objects.map { it.box } + model.items.map { it.box } + model.regions.map { it.box })
        .flatMap { it.floorCorners() } +
        model.walls.flatMap { listOf(it.from.x to it.from.z, it.to.x to it.to.z) }
    val local = corners.map { (x, z) ->
        val dx = x - wall.from.x
        val dz = z - wall.from.z
        (dx * ux + dz * uz) to (-dx * uz + dz * ux)
    }
    val furniture = (model.objects.map { it.box } + model.items.map { it.box }).map { b ->
        -(b.center.x - wall.from.x) * uz + (b.center.z - wall.from.z) * ux
    }
    val inward = if (furniture.sum() < 0f) -1f else 1f
    val xs = local.map { it.first } + listOf(0f, l)
    val zs = local.map { it.second * inward }.filter { it > 0f }
    val depth = (zs.maxOrNull() ?: DEFAULT_FLOOR_DEPTH_M) + FLOOR_MARGIN_M
    val back = WALL_THICKNESS_M
    val (z0, z1) = if (inward > 0f) -back to depth else -depth to back
    return RoomFloor(inward, xs.min() - FLOOR_MARGIN_M, xs.max() + FLOOR_MARGIN_M, z0, z1)
}

private fun OrientedBox.floorCorners(): List<Pair<Float, Float>> =
    listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).map { (sx, sz) ->
        val p = toWorld(sx * size.x / 2, 0f, sz * size.z / 2)
        p.x to p.z
    }

/**
 * The room layer. [lookFade] fades the looks out once a request is over (1 shown, 0 gone);
 * [showModel] hides the rebuilt room and its items to compare with the bare cloud. [products]
 * are the catalogue models on disk ([rememberRoomProductFiles]).
 */
@Composable
internal fun SceneScope.AiRoomLayer(
    model: RoomModel,
    looks: List<Look>,
    lookFade: Float,
    showModel: Boolean,
    floorY: Float,
    inks: RoomInks,
    finish: RoomFinish,
    products: Map<String, File>,
    label: LabelInk,
    eye: () -> Position,
) {
    if (showModel) {
        // The floor is laid from the first wall the model kept: it is the room's reference side.
        model.walls.firstOrNull()?.let { wall ->
            key("floor-${wall.id}") { OakFloor(wall, remember(wall, model) { roomFloor(wall, model) }, floorY, finish) }
        }
        model.regions.forEach { region -> key("region-${region.id}") { FreeRegionSlab(region.box, floorY, inks) } }
        model.walls.forEach { wall ->
            key("wall-${wall.id}") {
                val inward = remember(wall, model) { roomFloor(wall, model).inward }
                RebuiltWall(wall, model.openings.filter { it.wallId == wall.id }, inward, floorY, finish, label, eye)
            }
        }
        model.objects.forEach { obj ->
            key("object-${obj.id}") {
                val front = remember(obj, model) { frontOf(obj, model) }
                RoomObjectNode(obj, front, floorY, finish, products, rememberGrow())
            }
        }
        model.rejections.forEachIndexed { i, rejection ->
            key("rejection-$i-${rejection.what}") {
                val g = rememberGrow()
                val from = Vec3(rejection.from.x, floorY + LINE_LIFT_M, rejection.from.z)
                val to = Vec3(rejection.to.x, floorY + LINE_LIFT_M, rejection.to.z)
                Segment(from, to, inks.refused, g, SEGMENT_RADIUS_M)
                val mid = (from + to) * 0.5f
                Label(REJECTED_LABEL, Position(mid.x, floorY + REJECTED_LABEL_LIFT_M, mid.z), label, eye, g)
            }
        }
        model.items.forEach { item ->
            key("item-${item.id}") { RoomItemNode(item, model, floorY, finish, products, rememberGrow()) }
        }
        model.dimensions.forEach { dim ->
            key("dimension-${dim.id}") { Dimension(dim.from, dim.to, dim.kind, dim.value, inks, label, eye) }
        }
    }
    if (lookFade > 0f) {
        looks.forEachIndexed { i, look ->
            key("look-$i") { LookNode(look, rememberGrow() * lookFade, inks) }
        }
    }
}

/**
 * The key light of the rebuilt room: a soft sun from the room's open side, high and raking along
 * the wall, so the furniture throws its shadows towards the wall and the plaster takes the light.
 * Without a wall, from the camera's usual side.
 */
@Composable
internal fun SceneScope.RoomKeyLight(model: RoomModel) {
    val direction = remember(model.walls, model.objects, model.items) {
        val wall = model.walls.firstOrNull() ?: return@remember Direction(-0.35f, -1f, -0.45f)
        val l = wall.length.coerceAtLeast(MIN_PIECE_M)
        val ux = (wall.to.x - wall.from.x) / l
        val uz = (wall.to.z - wall.from.z) / l
        val inward = roomFloor(wall, model).inward
        // The wall's inward normal is (−uz, ux) × inward; the light travels against it.
        val x = inward * uz * KEY_ACROSS + ux * KEY_ALONG
        val z = -inward * ux * KEY_ACROSS + uz * KEY_ALONG
        val n = sqrt(x * x + 1f + z * z)
        Direction(x / n, -1f / n, z / n)
    }
    LightNode(
        type = LightManager.Type.DIRECTIONAL,
        direction = direction,
        apply = {
            intensity(KEY_LIGHT_LUX)
            castShadows(true)
            shadowOptions(
                LightManager.ShadowOptions().apply {
                    mapSize = SHADOW_MAP_SIZE
                    stable = true
                    // Flat lids and duvets lit at a grazing angle self-shadow in triangles
                    // without a little more bias than the default.
                    constantBias = SHADOW_CONSTANT_BIAS
                    normalBias = SHADOW_NORMAL_BIAS
                },
            )
        },
    )
}

/** Oak planks along [wall], staggered, over a screed slab whose cut edge shows at the margin. */
@Composable
private fun SceneScope.OakFloor(wall: RoomWall, floor: RoomFloor, floorY: Float, finish: RoomFinish) {
    val g = rememberGrow()
    val planks = remember(floor) { plankLayout(floor) }
    // The floor rolls out from the wall's foot into the room.
    Node(
        position = Position(wall.from.x, floorY, wall.from.z),
        rotation = Rotation(y = wall.yawDegrees),
        scale = Scale(1f, 1f, g.coerceAtLeast(GROW_FLOOR)),
    ) {
        CubeNode(
            size = Size(floor.x1 - floor.x0, SLAB_M, floor.z1 - floor.z0),
            center = Position(
                (floor.x0 + floor.x1) / 2,
                -PLANK_M - SLAB_M / 2 + PLANK_GAP_M,
                (floor.z0 + floor.z1) / 2,
            ),
            materialInstance = finish.slab,
        )
        planks.forEachIndexed { i, p ->
            key("plank-$i") {
                CubeNode(
                    size = Size(p.x1 - p.x0 - PLANK_GAP_M, PLANK_M, p.z1 - p.z0 - PLANK_GAP_M),
                    center = Position((p.x0 + p.x1) / 2, -PLANK_M / 2, (p.z0 + p.z1) / 2),
                    materialInstance = finish.oak[p.cut % finish.oak.size],
                )
            }
        }
        // The floor darkens where it meets the wall, as a real corner does.
        ContactShadow(
            size = Size(wall.length + WALL_THICKNESS_M * 2, 0f, WALL_FOOT_SHADOW_M),
            position = Position(wall.length / 2, LINE_LIFT_M / 2, 0f),
            intensity = WALL_FOOT_SHADOW_INTENSITY,
        )
    }
}

/** One plank: [x0]…[x1] along the wall, [z0]…[z1] across, and which of the oak cuts it is. */
internal data class Plank(val x0: Float, val x1: Float, val z0: Float, val z1: Float, val cut: Int)

/** Rows of planks across [floor], each row's joints offset from its neighbour's. */
internal fun plankLayout(floor: RoomFloor): List<Plank> {
    val rows = ceil((floor.z1 - floor.z0) / PLANK_WIDTH_M).toInt().coerceAtLeast(1)
    val out = ArrayList<Plank>()
    for (r in 0 until rows) {
        val z0 = floor.z0 + r * PLANK_WIDTH_M
        val z1 = min(floor.z1, z0 + PLANK_WIDTH_M)
        // A golden-ratio stagger: no two nearby rows line their joints up.
        val shift = (r * GOLDEN_FRACTION % 1f) * PLANK_LENGTH_M
        var x = floor.x0 - shift
        var j = 0
        while (x < floor.x1) {
            val a = max(x, floor.x0)
            val b = min(x + PLANK_LENGTH_M, floor.x1)
            if (b - a > PLANK_GAP_M * 2) out += Plank(a, b, z0, z1, (r * PLANK_CUT_STEP + j) % OAK_CUTS)
            x += PLANK_LENGTH_M
            j++
        }
    }
    return out
}

/**
 * A plaster wall standing on the floor, its back on the far side of the line the scan found:
 * the scan sees the inner face. Its windows are framed and glazed, with a sill on the room side;
 * a skirting runs along its foot. It rises as it is built.
 */
@Composable
private fun SceneScope.RebuiltWall(
    wall: RoomWall,
    openings: List<RoomOpening>,
    inward: Float,
    floorY: Float,
    finish: RoomFinish,
    label: LabelInk,
    eye: () -> Position,
) {
    val g = rememberGrow()
    val pieces = remember(wall, openings) { wallPieces(wall, openings) }
    val length = wall.length
    val height = wall.height
    val back = -inward * WALL_THICKNESS_M / 2
    Node(
        position = Position(wall.from.x, floorY, wall.from.z),
        rotation = Rotation(y = wall.yawDegrees),
        scale = Scale(1f, g.coerceAtLeast(GROW_FLOOR), 1f),
    ) {
        pieces.forEachIndexed { i, p ->
            key("piece-$i") {
                Solid(
                    Size(p.x1 - p.x0, p.y1 - p.y0, WALL_THICKNESS_M),
                    Position((p.x0 + p.x1) / 2, (p.y0 + p.y1) / 2, back),
                    finish.plaster,
                )
                if (p.y0 == 0f) {
                    CubeNode(
                        size = Size(p.x1 - p.x0, SKIRTING_HEIGHT_M, SKIRTING_DEPTH_M),
                        center = Position((p.x0 + p.x1) / 2, SKIRTING_HEIGHT_M / 2, inward * SKIRTING_DEPTH_M / 2),
                        materialInstance = finish.skirting,
                    )
                }
            }
        }
        openings.forEach { o -> key(o.id) { OpeningFrame(o, inward, finish) } }
    }
    // Labels in world space, outside the rotated node: a billboard faces the camera on its own.
    val mid = wall.at(length / 2)
    Label(
        "${SceneToolExecutor.fmt(length)} m",
        Position(mid.x, floorY + height * g + WALL_LABEL_LIFT_M, mid.z),
        label,
        eye,
        g,
    )
    val l = length.coerceAtLeast(MIN_PIECE_M)
    val nx = -(wall.to.z - wall.from.z) / l * inward
    val nz = (wall.to.x - wall.from.x) / l * inward
    openings.forEach { o ->
        key("label-${o.id}") {
            val at = wall.at((o.from + o.to) / 2)
            // In front of the glass, on the room side, so the pane does not tint it.
            Label(
                "${SceneToolExecutor.fmt(o.width)} × ${SceneToolExecutor.fmt(o.height)} m",
                Position(
                    at.x + nx * OPENING_LABEL_OUT_M,
                    floorY + (o.sill + o.head) / 2,
                    at.z + nz * OPENING_LABEL_OUT_M,
                ),
                label,
                eye,
                g,
            )
        }
    }
}

/** An opening's aluminium frame in the wall's thickness; a window adds its glass, mullion and sill. */
@Composable
private fun SceneScope.OpeningFrame(o: RoomOpening, inward: Float, finish: RoomFinish) {
    val z = -inward * WALL_THICKNESS_M * FRAME_SET_BACK
    val w = o.width
    val h = o.height
    val cx = (o.from + o.to) / 2
    val cy = (o.sill + o.head) / 2
    val bar = FRAME_PROFILE_M
    Solid(Size(bar, h, FRAME_DEPTH_M), Position(o.from + bar / 2, cy, z), finish.frame)
    Solid(Size(bar, h, FRAME_DEPTH_M), Position(o.to - bar / 2, cy, z), finish.frame)
    Solid(Size(w, bar, FRAME_DEPTH_M), Position(cx, o.head - bar / 2, z), finish.frame)
    if (o.kind != OpeningKind.Window) return
    Solid(Size(w, bar, FRAME_DEPTH_M), Position(cx, o.sill + bar / 2, z), finish.frame)
    if (w > MULLION_FROM_M) Solid(Size(bar * MULLION_SHARE, h, FRAME_DEPTH_M), Position(cx, cy, z), finish.frame)
    CubeNode(size = Size(w - bar, h - bar, GLASS_M), center = Position(cx, cy, z), materialInstance = finish.glass)
    // The sill board: from the frame, across the reveal, a little proud of the plaster.
    val reveal = WALL_THICKNESS_M * FRAME_SET_BACK - FRAME_DEPTH_M / 2
    val depth = reveal + SILL_PROUD_M
    Solid(
        Size(w + SILL_OVERHANG_M, SILL_M, depth),
        Position(cx, o.sill - SILL_M / 2, inward * (SILL_PROUD_M - reveal) / 2),
        finish.skirting,
    )
}

/** A computed dimension: a line, its two ends and its value. */
@Composable
private fun SceneScope.Dimension(
    from: Vec3,
    to: Vec3,
    kind: DimensionKind,
    value: Float,
    inks: RoomInks,
    label: LabelInk,
    eye: () -> Position,
) {
    val g = rememberGrow()
    Segment(from, to, inks.dimension, g, DIMENSION_RADIUS_M)
    SphereNode(
        radius = DIMENSION_END_M,
        position = from.toPosition(),
        materialInstance = inks.dimension,
        scale = Scale(g),
    )
    SphereNode(
        radius = DIMENSION_END_M,
        position = to.toPosition(),
        materialInstance = inks.dimension,
        scale = Scale(g),
    )
    val mid = (from + to) * 0.5f
    val text = when (kind) {
        DimensionKind.Clearance -> "Clearance ${SceneToolExecutor.fmt(value)} m"
        else -> "${SceneToolExecutor.fmt(value)} m"
    }
    // A clearance runs at floor level between two pieces: its label floats above them to stay seen.
    val lift = if (kind == DimensionKind.Clearance) CLEARANCE_LABEL_LIFT_M else DIMENSION_LABEL_LIFT_M
    Label(text, Position(mid.x, mid.y + lift, mid.z), label, eye, g)
}

/** What the model looked at: a ray (and its hit), a probed box, a tried wall. */
@Composable
private fun SceneScope.LookNode(look: Look, g: Float, inks: RoomInks) {
    when (look) {
        is Look.Ray -> {
            Segment(look.from, look.to, if (look.hit) inks.look else inks.lookMiss, g, RAY_RADIUS_M)
            if (look.hit) {
                SphereNode(
                    radius = HIT_RADIUS_M,
                    position = look.to.toPosition(),
                    materialInstance = inks.look,
                    scale = Scale(g.coerceAtLeast(GROW_FLOOR)),
                )
            }
        }
        is Look.Probe -> WireBoxAt(look.box, if (look.ok) inks.look else inks.refused, g)
        is Look.Segment -> Segment(look.from, look.to, if (look.ok) inks.look else inks.refused, g, SEGMENT_RADIUS_M)
    }
}

/** A probed box's wireframe, grown by [g]. */
@Composable
private fun SceneScope.WireBoxAt(box: OrientedBox, edge: MaterialInstance, g: Float) {
    val w = box.size.x
    val h = box.size.y
    val d = box.size.z
    Node(
        position = Position(box.center.x, box.center.y - h / 2, box.center.z),
        rotation = Rotation(y = box.yawDegrees),
        scale = Scale(1f, g.coerceAtLeast(GROW_FLOOR), 1f),
    ) {
        val hw = w / 2
        val hd = d / 2
        for (y in listOf(0f, h)) {
            key("ring-$y") {
                TubeNode(
                    points = listOf(
                        Position(-hw, y, -hd),
                        Position(hw, y, -hd),
                        Position(hw, y, hd),
                        Position(-hw, y, hd),
                    ),
                    radius = EDGE_RADIUS_M,
                    closed = true,
                    materialInstance = edge,
                )
            }
        }
        for ((i, corner) in listOf(-hw to -hd, hw to -hd, hw to hd, -hw to hd).withIndex()) {
            key("post-$i") {
                TubeNode(
                    points = listOf(
                        Position(corner.first, 0f, corner.second),
                        Position(corner.first, h, corner.second),
                    ),
                    radius = EDGE_RADIUS_M,
                    materialInstance = edge,
                )
            }
        }
    }
}

/** A line from [from] to [to], world metres, drawn out from [from] as [g] goes 0 → 1. */
@Composable
private fun SceneScope.Segment(from: Vec3, to: Vec3, ink: MaterialInstance, g: Float, radius: Float) {
    val d = to - from
    Node(position = from.toPosition(), scale = Scale(g.coerceAtLeast(GROW_FLOOR))) {
        TubeNode(
            points = listOf(Position(0f, 0f, 0f), Position(d.x, d.y, d.z)),
            radius = radius,
            caps = true,
            materialInstance = ink,
        )
    }
}

@Composable
private fun SceneScope.FreeRegionSlab(box: OrientedBox, floorY: Float, inks: RoomInks) {
    val g = rememberGrow()
    Node(
        position = Position(box.center.x, floorY + LINE_LIFT_M, box.center.z),
        rotation = Rotation(y = box.yawDegrees),
        scale = Scale(g.coerceAtLeast(GROW_FLOOR), 1f, g.coerceAtLeast(GROW_FLOOR)),
    ) {
        CubeNode(
            size = Size(box.size.x, REGION_THICKNESS_M, box.size.z),
            center = Position(0f, 0f, 0f),
            materialInstance = inks.region,
        )
    }
}

/** A billboard label, the overlay card's colours, sized to its text so it never clips. */
@Composable
private fun SceneScope.Label(text: String, position: Position, ink: LabelInk, eye: () -> Position, g: Float) {
    TextNode(
        text = text,
        fontSize = (LABEL_TEXT_PX / (text.length * LABEL_CHAR_EM)).coerceAtMost(LABEL_MAX_FONT_PX),
        textColor = ink.text,
        backgroundColor = ink.background,
        widthMeters = LABEL_HEIGHT_M * LABEL_ASPECT,
        heightMeters = LABEL_HEIGHT_M,
        position = position,
        scale = Scale(g.coerceAtLeast(GROW_FLOOR)),
        cameraPositionProvider = eye,
    )
}

/** 0 → 1 on the stage's spring, from the first composition of the caller's key. */
@Composable
internal fun rememberGrow(): Float {
    val grow = remember { Animatable(GROW_FLOOR) }
    LaunchedEffect(grow) {
        grow.animateTo(
            1f,
            spring(dampingRatio = SceneViewTokens.Motion.springDampingRatio, stiffness = GROW_STIFFNESS),
        )
    }
    return grow.value
}

private fun Vec3.toPosition() = Position(x, y, z)

private const val REJECTED_LABEL = "Rejected · no points"

private const val LOOK_MISS_ALPHA = 0.4f
private const val REGION_ALPHA = 0.18f

private const val GROW_FLOOR = 0.001f
// Softer than the UI's spring: a wall rising over half a second reads as being built.
private const val GROW_STIFFNESS = 120f
private const val MIN_PIECE_M = 0.01f
private const val EDGE_RADIUS_M = 0.012f
private const val RAY_RADIUS_M = 0.01f
private const val SEGMENT_RADIUS_M = 0.012f
private const val DIMENSION_RADIUS_M = 0.012f
private const val DIMENSION_END_M = 0.02f
private const val HIT_RADIUS_M = 0.035f
private const val LINE_LIFT_M = 0.01f
private const val REGION_THICKNESS_M = 0.01f

/** A plastered block wall: the scan sees its inner face, the rest is behind it. */
internal const val WALL_THICKNESS_M = 0.12f
private const val SKIRTING_HEIGHT_M = 0.08f
private const val SKIRTING_DEPTH_M = 0.012f
private const val FRAME_PROFILE_M = 0.055f
private const val FRAME_DEPTH_M = 0.07f
/** The frame sits this share of the wall's thickness back from the room, as a real window does. */
private const val FRAME_SET_BACK = 0.62f
private const val MULLION_FROM_M = 1.0f
private const val MULLION_SHARE = 0.8f
private const val GLASS_M = 0.008f
private const val SILL_M = 0.025f
private const val SILL_PROUD_M = 0.04f
private const val SILL_OVERHANG_M = 0.08f
private const val OPENING_LABEL_OUT_M = 0.18f

private const val PLANK_WIDTH_M = 0.16f
private const val PLANK_LENGTH_M = 1.2f
private const val PLANK_M = 0.014f
private const val PLANK_GAP_M = 0.003f
private const val SLAB_M = 0.06f
private const val PLANK_CUT_STEP = 2
private const val OAK_CUTS = 3
private const val GOLDEN_FRACTION = 0.618034f
private const val FLOOR_MARGIN_M = 0.4f
private const val DEFAULT_FLOOR_DEPTH_M = 1.5f
private const val WALL_FOOT_SHADOW_M = 0.5f
private const val WALL_FOOT_SHADOW_INTENSITY = 0.4f
private val OAK_GRAIN_OFFSETS = listOf(
    Position(0f, 0.31f, 0.9f),
    Position(0.4f, 1.7f, 0.2f),
    Position(0.9f, 0.6f, 2.3f),
)

private const val DIELECTRIC_REFLECTANCE = 0.5f
private const val SHEEN_ROUGHNESS = 0.6f
private const val FROSTED_ROUGHNESS = 0.4f

private const val KEY_LIGHT_LUX = 9_000f
private const val KEY_ACROSS = 0.5f
private const val KEY_ALONG = 0.55f
private const val SHADOW_MAP_SIZE = 2048
private const val SHADOW_CONSTANT_BIAS = 0.005f
private const val SHADOW_NORMAL_BIAS = 2.5f
private const val AO_INTENSITY = 1f
private const val AO_RADIUS_M = 0.4f

private const val LABEL_HEIGHT_M = 0.2f
private const val LABEL_ASPECT = 4f // TextNode's bitmap is 512 × 128
private const val LABEL_TEXT_PX = 480f
private const val LABEL_CHAR_EM = 0.58f
private const val LABEL_MAX_FONT_PX = 76f
private const val WALL_LABEL_LIFT_M = 0.18f
private const val DIMENSION_LABEL_LIFT_M = 0.14f
private const val CLEARANCE_LABEL_LIFT_M = 1.15f
private const val REJECTED_LABEL_LIFT_M = 0.2f
