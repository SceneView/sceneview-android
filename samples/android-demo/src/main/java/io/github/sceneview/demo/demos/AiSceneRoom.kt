package io.github.sceneview.demo.demos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneScope
import io.github.sceneview.demo.ai.scene.DimensionKind
import io.github.sceneview.demo.ai.scene.Look
import io.github.sceneview.demo.ai.scene.OrientedBox
import io.github.sceneview.demo.ai.scene.PlacedItem
import io.github.sceneview.demo.ai.scene.RoomModel
import io.github.sceneview.demo.ai.scene.RoomOpening
import io.github.sceneview.demo.ai.scene.RoomWall
import io.github.sceneview.demo.ai.scene.SceneToolExecutor
import io.github.sceneview.demo.demos.internal.Vec3
import io.github.sceneview.demo.theme.DebugPalette
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.rememberModelInstance

/**
 * The AI Scene's 3D layer, drawn over the recorded cloud by [ArDebugSceneView]'s content slot:
 * the room the model rebuilt (walls with their openings cut, the furniture it fitted), what it
 * looked at while doing so (rays, probed boxes, tried walls), and the catalogue items it placed
 * with their computed dimensions. Every size comes from [RoomModel]; nothing here measures.
 */

/** The unlit inks of the room layer, taken from the stage's own debug palette (`DESIGN.md`). */
internal class RoomInks(
    val wallFill: MaterialInstance,
    val wallEdge: MaterialInstance,
    val opening: MaterialInstance,
    val objectFill: MaterialInstance,
    val objectEdge: MaterialInstance,
    val look: MaterialInstance,
    val lookMiss: MaterialInstance,
    val refused: MaterialInstance,
    val item: MaterialInstance,
    val itemFill: MaterialInstance,
    val region: MaterialInstance,
    val dimension: MaterialInstance,
    val lampShade: MaterialInstance,
    val lampPole: MaterialInstance,
) {
    val all: List<MaterialInstance>
        get() = listOf(
            wallFill, wallEdge, opening, objectFill, objectEdge, look, lookMiss, refused,
            item, itemFill, region, dimension, lampShade, lampPole,
        )

    companion object {
        /** On the main thread, as every Filament call. Destroy [all] after the SceneView. */
        fun of(loader: MaterialLoader, palette: DebugPalette, danger: Color): RoomInks {
            fun ink(color: Color) = loader.createUnlitColorInstance(color)
            return RoomInks(
                wallFill = ink(palette.wallOutline.copy(alpha = WALL_FILL_ALPHA)),
                wallEdge = ink(palette.wallOutline),
                opening = ink(palette.floorOutline),
                objectFill = ink(palette.otherOutline.copy(alpha = OBJECT_FILL_ALPHA)),
                objectEdge = ink(palette.otherOutline),
                look = ink(palette.livePoint),
                lookMiss = ink(palette.livePoint.copy(alpha = LOOK_MISS_ALPHA)),
                refused = ink(danger),
                item = ink(palette.anchor),
                itemFill = ink(palette.anchor.copy(alpha = OBJECT_FILL_ALPHA)),
                region = ink(palette.anchor.copy(alpha = REGION_ALPHA)),
                dimension = ink(palette.floorOutline),
                lampShade = ink(palette.anchor),
                lampPole = ink(palette.otherOutline),
            )
        }
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
 * The room layer. [lookFade] fades the looks out once a request is over (1 shown, 0 gone);
 * [showModel] hides the rebuilt room and its items to compare with the bare cloud.
 */
@Composable
internal fun SceneScope.AiRoomLayer(
    model: RoomModel,
    looks: List<Look>,
    lookFade: Float,
    showModel: Boolean,
    floorY: Float,
    inks: RoomInks,
    label: LabelInk,
    eye: () -> Position,
) {
    if (showModel) {
        model.regions.forEach { region -> key("region-${region.id}") { FreeRegionSlab(region.box, floorY, inks) } }
        model.walls.forEach { wall ->
            key("wall-${wall.id}") {
                RebuiltWall(wall, model.openings.filter { it.wallId == wall.id }, floorY, inks, label, eye)
            }
        }
        model.objects.forEach { obj -> key("object-${obj.id}") { FittedBox(
            obj.box,
            inks.objectFill,
            inks.objectEdge,
            rememberGrow(),
        ) } }
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
        model.items.forEach { item -> key("item-${item.id}") { PlacedItemNode(item, floorY, inks) } }
        model.dimensions.forEach { dim ->
            key("dimension-${dim.id}") {
                val g = rememberGrow()
                Segment(dim.from, dim.to, inks.dimension, g, DIMENSION_RADIUS_M)
                SphereNode(
                    radius = DIMENSION_END_M,
                    position = dim.from.toPosition(),
                    materialInstance = inks.dimension,
                    scale = Scale(g),
                )
                SphereNode(
                    radius = DIMENSION_END_M,
                    position = dim.to.toPosition(),
                    materialInstance = inks.dimension,
                    scale = Scale(g),
                )
                val mid = (dim.from + dim.to) * 0.5f
                val text = when (dim.kind) {
                    DimensionKind.Clearance -> "Clearance ${SceneToolExecutor.fmt(dim.value)} m"
                    else -> "${SceneToolExecutor.fmt(dim.value)} m"
                }
                Label(text, Position(mid.x, mid.y + DIMENSION_LABEL_LIFT_M, mid.z), label, eye, g)
            }
        }
    }
    if (lookFade > 0f) {
        looks.forEachIndexed { i, look ->
            key("look-$i") {
                val g = rememberGrow() * lookFade
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
                    is Look.Probe -> FittedBox(look.box, null, if (look.ok) inks.look else inks.refused, g)
                    is Look.Segment -> Segment(
                        look.from,
                        look.to,
                        if (look.ok) inks.look else inks.refused,
                        g,
                        SEGMENT_RADIUS_M,
                    )
                }
            }
        }
    }
}

/** A wall standing on the floor, its pieces filled and its outline and openings traced. */
@Composable
private fun SceneScope.RebuiltWall(
    wall: RoomWall,
    openings: List<RoomOpening>,
    floorY: Float,
    inks: RoomInks,
    label: LabelInk,
    eye: () -> Position,
) {
    val g = rememberGrow()
    val pieces = remember(wall, openings) { wallPieces(wall, openings) }
    val length = wall.length
    val height = wall.height
    Node(
        position = Position(wall.from.x, floorY, wall.from.z),
        rotation = Rotation(y = wall.yawDegrees),
        scale = Scale(1f, g, 1f),
    ) {
        pieces.forEach { p ->
            CubeNode(
                size = Size(p.x1 - p.x0, p.y1 - p.y0, WALL_THICKNESS_M),
                center = Position((p.x0 + p.x1) / 2, (p.y0 + p.y1) / 2, 0f),
                materialInstance = inks.wallFill,
            )
        }
        TubeNode(
            points = rectangle(0f, length, 0f, height),
            radius = EDGE_RADIUS_M,
            closed = true,
            materialInstance = inks.wallEdge,
        )
        openings.forEach { o ->
            key(o.id) {
                TubeNode(
                    points = rectangle(o.from, o.to, o.sill, o.head),
                    radius = EDGE_RADIUS_M,
                    closed = true,
                    materialInstance = inks.opening,
                )
            }
        }
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
    openings.forEach { o ->
        key("label-${o.id}") {
            val at = wall.at((o.from + o.to) / 2)
            Label(
                "${SceneToolExecutor.fmt(o.width)} × ${SceneToolExecutor.fmt(o.height)} m",
                Position(at.x, floorY + (o.sill + o.head) / 2, at.z),
                label,
                eye,
                g,
            )
        }
    }
}

/** A box on the floor: an optional translucent [fill] and its [edge] wireframe, grown by [g]. */
@Composable
private fun SceneScope.FittedBox(box: OrientedBox, fill: MaterialInstance?, edge: MaterialInstance, g: Float) {
    val w = box.size.x
    val h = box.size.y
    val d = box.size.z
    Node(
        position = Position(box.center.x, box.center.y - h / 2, box.center.z),
        rotation = Rotation(y = box.yawDegrees),
        scale = Scale(1f, g.coerceAtLeast(GROW_FLOOR), 1f),
    ) {
        if (fill != null) {
            CubeNode(size = Size(w, h, d), center = Position(0f, h / 2, 0f), materialInstance = fill)
        }
        WireBox(w, h, d, edge)
    }
}

@Composable
private fun SceneScope.WireBox(w: Float, h: Float, d: Float, edge: MaterialInstance) {
    val hw = w / 2
    val hd = d / 2
    for (y in listOf(0f, h)) {
        key("ring-$y") {
            TubeNode(
                points = listOf(Position(-hw, y, -hd), Position(hw, y, -hd), Position(hw, y, hd), Position(-hw, y, hd)),
                radius = EDGE_RADIUS_M,
                closed = true,
                materialInstance = edge,
            )
        }
    }
    for ((i, corner) in listOf(-hw to -hd, hw to -hd, hw to hd, -hw to hd).withIndex()) {
        key("post-$i") {
            TubeNode(
                points = listOf(Position(corner.first, 0f, corner.second), Position(corner.first, h, corner.second)),
                radius = EDGE_RADIUS_M,
                materialInstance = edge,
            )
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

/** A catalogue item at its catalogue size: the armchair as a model, the lamp built, others boxed. */
@Composable
private fun SceneScope.PlacedItemNode(item: PlacedItem, floorY: Float, inks: RoomInks) {
    val g = rememberGrow().coerceAtLeast(GROW_FLOOR)
    val box = item.box
    val w = box.size.x
    val h = box.size.y
    val d = box.size.z
    Node(
        position = Position(box.center.x, floorY, box.center.z),
        rotation = Rotation(y = box.yawDegrees),
        scale = Scale(g),
    ) {
        TubeNode(
            points = listOf(
                Position(-w / 2, LINE_LIFT_M, -d / 2),
                Position(w / 2, LINE_LIFT_M, -d / 2),
                Position(w / 2, LINE_LIFT_M, d / 2),
                Position(-w / 2, LINE_LIFT_M, d / 2),
            ),
            radius = EDGE_RADIUS_M,
            closed = true,
            materialInstance = inks.item,
        )
        when (item.category) {
            ARMCHAIR -> {
                val chair = rememberModelInstance(modelLoader, ARMCHAIR_MODEL)
                if (chair != null) {
                    ModelNode(
                        modelInstance = chair,
                        scaleToUnits = maxOf(w, h, d),
                        centerOrigin = Position(0f, -1f, 0f),
                    )
                } else {
                    CubeNode(size = Size(w, h, d), center = Position(0f, h / 2, 0f), materialInstance = inks.itemFill)
                }
            }
            FLOOR_LAMP -> {
                CylinderNode(
                    radius = w / 2 * LAMP_BASE_RATIO,
                    height = LAMP_BASE_M,
                    center = Position(0f, LAMP_BASE_M / 2, 0f),
                    materialInstance = inks.lampPole,
                )
                CylinderNode(
                    radius = LAMP_POLE_RADIUS_M,
                    height = h - LAMP_SHADE_M,
                    center = Position(0f, (h - LAMP_SHADE_M) / 2, 0f),
                    materialInstance = inks.lampPole,
                )
                CylinderNode(
                    radius = w / 2,
                    height = LAMP_SHADE_M,
                    center = Position(0f, h - LAMP_SHADE_M / 2, 0f),
                    materialInstance = inks.lampShade,
                )
            }
            else -> {
                CubeNode(size = Size(w, h, d), center = Position(0f, h / 2, 0f), materialInstance = inks.itemFill)
                WireBox(w, h, d, inks.item)
            }
        }
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
private fun rememberGrow(): Float {
    val grow = remember { Animatable(GROW_FLOOR) }
    LaunchedEffect(grow) {
        grow.animateTo(
            1f,
            spring(dampingRatio = SceneViewTokens.Motion.springDampingRatio, stiffness = GROW_STIFFNESS),
        )
    }
    return grow.value
}

private fun rectangle(x0: Float, x1: Float, y0: Float, y1: Float) =
    listOf(Position(x0, y0, 0f), Position(x1, y0, 0f), Position(x1, y1, 0f), Position(x0, y1, 0f))

private fun Vec3.toPosition() = Position(x, y, z)

private const val ARMCHAIR = "armchair"
private const val FLOOR_LAMP = "floor_lamp"
private const val ARMCHAIR_MODEL = "models/khronos_sheen_chair.glb"
private const val REJECTED_LABEL = "Rejected · no points"

private const val WALL_FILL_ALPHA = 0.16f
private const val OBJECT_FILL_ALPHA = 0.14f
private const val LOOK_MISS_ALPHA = 0.4f
private const val REGION_ALPHA = 0.18f

private const val GROW_FLOOR = 0.001f
// Softer than the UI's spring: a wall rising over half a second reads as being built.
private const val GROW_STIFFNESS = 120f
private const val MIN_PIECE_M = 0.01f
private const val WALL_THICKNESS_M = 0.04f
private const val EDGE_RADIUS_M = 0.012f
private const val RAY_RADIUS_M = 0.01f
private const val SEGMENT_RADIUS_M = 0.012f
private const val DIMENSION_RADIUS_M = 0.012f
private const val DIMENSION_END_M = 0.02f
private const val HIT_RADIUS_M = 0.035f
private const val LINE_LIFT_M = 0.01f
private const val REGION_THICKNESS_M = 0.01f
private const val LAMP_BASE_RATIO = 0.9f
private const val LAMP_BASE_M = 0.03f
private const val LAMP_POLE_RADIUS_M = 0.022f
private const val LAMP_SHADE_M = 0.28f

private const val LABEL_HEIGHT_M = 0.2f
private const val LABEL_ASPECT = 4f // TextNode's bitmap is 512 × 128
private const val LABEL_TEXT_PX = 480f
private const val LABEL_CHAR_EM = 0.58f
private const val LABEL_MAX_FONT_PX = 76f
private const val WALL_LABEL_LIFT_M = 0.18f
private const val DIMENSION_LABEL_LIFT_M = 0.14f
private const val REJECTED_LABEL_LIFT_M = 0.2f
