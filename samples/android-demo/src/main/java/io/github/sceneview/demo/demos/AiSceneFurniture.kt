package io.github.sceneview.demo.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneScope
import io.github.sceneview.demo.ai.scene.OrientedBox
import io.github.sceneview.demo.ai.scene.PlacedItem
import io.github.sceneview.demo.ai.scene.RoomModel
import io.github.sceneview.demo.ai.scene.RoomObject
import io.github.sceneview.demo.ai.scene.RoomWall
import io.github.sceneview.demo.common.rememberFileModelInstance
import io.github.sceneview.demo.hdpack.HdPack
import io.github.sceneview.demo.hdpack.rememberHdPackStore
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.model.model
import io.github.sceneview.rememberModelInstance
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlin.math.hypot
import kotlin.math.min
import io.github.sceneview.node.CubeNode as CubeNodeImpl
import io.github.sceneview.node.CylinderNode as CylinderNodeImpl
import io.github.sceneview.node.SphereNode as SphereNodeImpl

/**
 * The furniture of the AI Scene's rebuilt room. What the scan recognised and what the AI placed
 * are drawn as real products where the HD pack has one — the IKEA desk, the Wishbone chair, the
 * Rivet armchair and arc lamp — scaled into the box the AI fitted, turned to face what they
 * should face. A bed, a radiator and a plant have no catalogue model yet, so they are built here
 * from the room's own finishes; anything else is a thin glass box, the last resort.
 */

/** A model turned [quarterTurns] × 90° about Y, then scaled per box axis ([fitModel]). */
internal data class ModelFit(val quarterTurns: Int, val sx: Float, val sy: Float, val sz: Float)

/** The HD pack ids of the products the room uses. */
internal object RoomProducts {
    const val DESK = "ikea-linnmon-alex-desk"
    const val CHAIR = "wishbone-chair"
    const val ARMCHAIR = "rivet-aiden-armchair"
    const val LAMP = "rivet-arc-floor-lamp"
    val ALL = listOf(DESK, CHAIR, ARMCHAIR, LAMP)
}

/**
 * The on-disk files of [RoomProducts] that are ready. While [wanted], the missing ones are
 * fetched (a few megabytes in all), and appear as they land: until then a built stand-in shows.
 */
@Composable
internal fun rememberRoomProductFiles(wanted: Boolean): Map<String, File> {
    val context = LocalContext.current
    val store = rememberHdPackStore()
    val ready by (store?.readyIds ?: NOTHING_READY).collectAsState()
    LaunchedEffect(store, wanted) {
        if (store != null && wanted) {
            RoomProducts.ALL.filterNot { it in store.readyIds.value }.forEach { HdPack.downloadNow(context, it) }
        }
    }
    return remember(store, ready) {
        RoomProducts.ALL.mapNotNull { id -> store?.readyFile(id)?.let { id to it } }.toMap()
    }
}

private val NOTHING_READY = MutableStateFlow(emptySet<String>())

/** A recognised object: its product, or its built stand-in, standing in its box. */
@Composable
internal fun SceneScope.RoomObjectNode(
    obj: RoomObject,
    front: Pair<Float, Float>?,
    floorY: Float,
    finish: RoomFinish,
    products: Map<String, File>,
    g: Float,
) {
    val box = obj.box
    FloorShadow(box, floorY, g)
    when (obj.category) {
        "desk", "table" -> {
            val desk = rememberFileModelInstance(modelLoader, products[RoomProducts.DESK])
            if (desk != null) {
                FittedModel(desk, box, floorY, front ?: (0f to 1f), matchHeight = false, g = g)
            } else {
                Oriented(box, floorY, front, g) { w, _, d -> Desk(w, d, DESK_HEIGHT_M, finish) }
            }
        }
        "chair", "office_chair" -> {
            val chair = rememberFileModelInstance(modelLoader, products[RoomProducts.CHAIR])
            if (chair != null) {
                FittedModel(chair, box, floorY, front ?: (0f to 1f), matchHeight = false, g = g)
            } else {
                Oriented(box, floorY, front, g) { w, _, d -> Chair(w, d, finish.oak.first(), finish) }
            }
        }
        "bed" -> Oriented(box, floorY, front, g) { w, _, d -> Bed(w, d, finish) }
        "radiator" -> Oriented(box, floorY, front, g) { w, h, d -> Radiator(w, h, d, finish) }
        "plant" -> Oriented(box, floorY, null, g) { w, h, d -> Plant(min(w, d), h, finish) }
        else -> GlassBox(box, floorY, finish, g)
    }
}

/** A catalogue item the AI placed, at its catalogue size. */
@Composable
internal fun SceneScope.RoomItemNode(
    item: PlacedItem,
    model: RoomModel,
    floorY: Float,
    finish: RoomFinish,
    products: Map<String, File>,
    g: Float,
) {
    val box = item.box
    val front = 0f to 1f // place_item turns an item's +Z towards what it faces
    when (item.category) {
        "armchair" -> {
            FloorShadow(box, floorY, g)
            val hd = rememberFileModelInstance(modelLoader, products[RoomProducts.ARMCHAIR])
            val bundled = rememberModelInstance(modelLoader, BUNDLED_ARMCHAIR)
            (hd ?: bundled)?.let { FittedModel(it, box, floorY, front, matchHeight = true, g = g) }
        }
        "floor_lamp" -> {
            // An arc lamp reaches over the seat it lights: it turns to the nearest armchair.
            val seat = model.items.filter { it.category == "armchair" }
                .minByOrNull { hypot(it.box.center.x - box.center.x, it.box.center.z - box.center.z) }
            val toward = seat?.let { box.localDirectionTo(it.box.center.x, it.box.center.z) } ?: front
            val lamp = rememberFileModelInstance(modelLoader, products[RoomProducts.LAMP])
            ContactShadow(
                size = Size(LAMP_BASE_SHADOW_M, 0f, LAMP_BASE_SHADOW_M),
                position = Position(box.center.x, floorY, box.center.z),
                intensity = CONTACT_SHADOW_INTENSITY * g,
            )
            if (lamp != null) {
                ArcLamp(lamp, box, floorY, toward, g)
            } else {
                Oriented(box, floorY, toward, g) { _, h, _ -> FloorLamp(h, finish) }
            }
        }
        "rug" -> Oriented(box, floorY, front, g) { w, _, d ->
            Solid(Size(w, RUG_THICKNESS_M, d), Position(0f, RUG_THICKNESS_M / 2, 0f), finish.item(item.material))
        }
        "bookshelf" -> {
            FloorShadow(box, floorY, g)
            Oriented(box, floorY, front, g) { w, h, d -> Bookshelf(w, h, d, finish.item(item.material)) }
        }
        "side_table" -> {
            FloorShadow(box, floorY, g)
            Oriented(box, floorY, front, g) { w, h, d -> SideTable(min(w, d), h, finish.item(item.material), finish) }
        }
        else -> GlassBox(box, floorY, finish, g)
    }
}

/** [instance] scaled into [box], turned so its front is [front] (box frame). */
@Composable
private fun SceneScope.FittedModel(
    instance: ModelInstance,
    box: OrientedBox,
    floorY: Float,
    front: Pair<Float, Float>,
    matchHeight: Boolean,
    g: Float,
) {
    val halfExtent = remember(instance) { instance.model.boundingBox.halfExtent }
    val fit = remember(halfExtent, box, front, matchHeight) { fitModel(halfExtent, box, front, matchHeight) }
    val grow = g.coerceAtLeast(GROW_FLOOR_FURNITURE)
    Node(
        position = Position(box.center.x, floorY, box.center.z),
        rotation = Rotation(y = box.yawDegrees),
        scale = Scale(fit.sx * grow, fit.sy * grow, fit.sz * grow),
    ) {
        Node(rotation = Rotation(y = fit.quarterTurns * QUARTER_TURN_DEG)) {
            ModelNode(modelInstance = instance, centerOrigin = Position(0f, -1f, 0f), apply = { isShadowCaster = true })
        }
    }
}

/**
 * The arc lamp: its base on the item's spot, its arc reaching towards [toward]. The model's base
 * sits off its bounding box's centre, at the back of the arc, so it is shifted forward by that much.
 */
@Composable
private fun SceneScope.ArcLamp(
    instance: ModelInstance,
    box: OrientedBox,
    floorY: Float,
    toward: Pair<Float,
    Float>,
    g: Float,
) {
    val halfExtent = remember(instance) { instance.model.boundingBox.halfExtent }
    val s = box.size.y / (2f * halfExtent[1]).coerceAtLeast(EPSILON) * g.coerceAtLeast(GROW_FLOOR_FURNITURE)
    val angle = Math.toDegrees(kotlin.math.atan2(toward.first, toward.second).toDouble()).toFloat()
    Node(
        position = Position(box.center.x, floorY, box.center.z),
        rotation = Rotation(y = box.yawDegrees + angle),
        scale = Scale(s),
    ) {
        ModelNode(
            modelInstance = instance,
            centerOrigin = Position(0f, -1f, 0f),
            position = Position(0f, 0f, ARC_LAMP_BASE_OFFSET * halfExtent[2]),
            apply = { isShadowCaster = true },
        )
    }
}

/**
 * A built piece in [box]'s frame, turned so its +Z is [front] (or the box's own +Z): the content
 * gets the width, height and depth seen from that front, and stands on y = 0.
 */
@Composable
private fun SceneScope.Oriented(
    box: OrientedBox,
    floorY: Float,
    front: Pair<Float, Float>?,
    g: Float,
    content: @Composable SceneScope.(w: Float, h: Float, d: Float) -> Unit,
) {
    val turns = front?.let { quarterTurnsFor(it) } ?: 0
    val odd = turns % 2 == 1
    val w = if (odd) box.size.z else box.size.x
    val d = if (odd) box.size.x else box.size.z
    Node(
        position = Position(box.center.x, floorY, box.center.z),
        rotation = Rotation(y = box.yawDegrees + turns * QUARTER_TURN_DEG),
        scale = Scale(g.coerceAtLeast(GROW_FLOOR_FURNITURE)),
    ) {
        content(w, box.size.y, d)
    }
}

/** The soft pool of shade under something standing on the floor. */
@Composable
private fun SceneScope.FloorShadow(box: OrientedBox, floorY: Float, g: Float) {
    ContactShadow(
        size = Size(box.size.x * SHADOW_SPREAD, 0f, box.size.z * SHADOW_SPREAD),
        position = Position(box.center.x, floorY, box.center.z),
        rotation = Rotation(y = box.yawDegrees),
        intensity = CONTACT_SHADOW_INTENSITY * g,
    )
}

/** A made bed, head at −Z: upholstered base and headboard, mattress, duvet, a throw, pillows. */
@Composable
private fun SceneScope.Bed(w: Float, d: Float, f: RoomFinish) {
    val head = -d / 2
    val foot = d / 2
    Solid(Size(w - PLINTH_INSET_M, BED_PLINTH_M, d - PLINTH_INSET_M), Position(0f, BED_PLINTH_M / 2, 0f), f.frame)
    Solid(
        Size(w, BED_BASE_TOP_M - BED_PLINTH_M, d - HEADBOARD_DEPTH_M),
        Position(0f, (BED_BASE_TOP_M + BED_PLINTH_M) / 2, HEADBOARD_DEPTH_M / 2),
        f.headboard,
    )
    Solid(
        Size(w + HEADBOARD_OVERHANG_M, HEADBOARD_HEIGHT_M, HEADBOARD_DEPTH_M),
        Position(0f, HEADBOARD_HEIGHT_M / 2, head + HEADBOARD_DEPTH_M / 2),
        f.headboard,
    )
    val mattressTop = BED_BASE_TOP_M + MATTRESS_M
    val mattressFrom = head + HEADBOARD_DEPTH_M
    Solid(
        Size(w - MATTRESS_INSET_M, MATTRESS_M, foot - mattressFrom),
        Position(0f, BED_BASE_TOP_M + MATTRESS_M / 2, (mattressFrom + foot) / 2),
        f.pillow,
    )
    // The duvet drapes over the mattress's sides from the foot to under the pillows.
    val duvetFrom = mattressFrom + (foot - mattressFrom) * (1f - DUVET_COVER)
    Solid(
        Size(w + DUVET_OVERHANG_M, MATTRESS_M + DUVET_THICKNESS_M, foot - duvetFrom + DUVET_OVERHANG_M / 2),
        Position(
            0f,
            mattressTop + DUVET_THICKNESS_M - (MATTRESS_M + DUVET_THICKNESS_M) / 2,
            (duvetFrom + foot + DUVET_OVERHANG_M / 2) / 2,
        ),
        f.linen,
    )
    Solid(
        Size(w + DUVET_OVERHANG_M + THROW_OVERHANG_M, THROW_THICKNESS_M + DUVET_THICKNESS_M, THROW_DEPTH_M),
        Position(
            0f,
            mattressTop + (THROW_THICKNESS_M + DUVET_THICKNESS_M) / 2,
            foot - THROW_DEPTH_M / 2 - THROW_FROM_FOOT_M,
        ),
        f.throwFabric,
    )
    val pillows = if (w >= DOUBLE_BED_M) 2 else 1
    val pillowW = min(PILLOW_MAX_W_M, (w - MATTRESS_INSET_M * 2) / pillows - PILLOW_GAP_M)
    repeat(pillows) { i ->
        key("pillow-$i") {
            val x = if (pillows == 1) 0f else (i - 0.5f) * (pillowW + PILLOW_GAP_M)
            SphereNode(
                radius = 0.5f,
                materialInstance = f.pillow,
                position = Position(
                    x,
                    mattressTop + PILLOW_HEIGHT_M / 2,
                    mattressFrom + PILLOW_DEPTH_M / 2 + PILLOW_GAP_M,
                ),
                scale = Scale(pillowW, PILLOW_HEIGHT_M, PILLOW_DEPTH_M),
                apply = SPHERE_CASTER,
            )
        }
    }
}

/** A steel panel radiator facing +Z, on two valve pipes, its convector fins on the front. */
@Composable
private fun SceneScope.Radiator(w: Float, h: Float, d: Float, f: RoomFinish) {
    val top = h.coerceIn(RADIATOR_MIN_TOP_M, RADIATOR_MAX_TOP_M)
    val panelH = top - RADIATOR_FLOOR_GAP_M
    val panelD = min(d, RADIATOR_DEPTH_M)
    val midY = RADIATOR_FLOOR_GAP_M + panelH / 2
    Solid(Size(w, panelH, panelD), Position(0f, midY, 0f), f.radiator)
    val inner = w - RADIATOR_END_M * 2
    val fins = (inner / RADIATOR_FIN_PITCH_M).toInt().coerceAtLeast(1)
    repeat(fins) { i ->
        key("fin-$i") {
            Solid(
                Size(RADIATOR_FIN_W_M, panelH - RADIATOR_END_M, RADIATOR_FIN_D_M),
                Position(-inner / 2 + (i + 0.5f) * inner / fins, midY, panelD / 2 + RADIATOR_FIN_D_M / 2),
                f.radiator,
            )
        }
    }
    for (side in listOf(-1f, 1f)) {
        key("pipe-$side") {
            Rod(Position(side * (w / 2 - RADIATOR_END_M), 0f, 0f), RADIATOR_FLOOR_GAP_M, PIPE_RADIUS_M, f.radiator)
        }
    }
}

/** A potted plant: a terracotta pot and a spiral of broad leaves on thin stems. */
@Composable
private fun SceneScope.Plant(span: Float, h: Float, f: RoomFinish) {
    val potR = (span * POT_SHARE).coerceIn(POT_MIN_R_M, POT_MAX_R_M)
    val potH = potR * POT_ASPECT
    CylinderNode(
        radius = potR,
        height = potH,
        center = Position(0f, potH / 2, 0f),
        materialInstance = f.pot,
        apply = CYLINDER_CASTER,
    )
    CylinderNode(
        radius = potR + POT_RIM_M,
        height = POT_RIM_H_M,
        center = Position(0f, potH - POT_RIM_H_M / 2, 0f),
        materialInstance = f.pot,
        apply = CYLINDER_CASTER,
    )
    CylinderNode(
        radius = potR - POT_RIM_M,
        height = SOIL_M,
        center = Position(0f, potH, 0f),
        materialInstance = f.headboard,
    )
    val top = h.coerceIn(PLANT_MIN_H_M, PLANT_MAX_H_M)
    val reach = (span / 2).coerceIn(LEAF_MIN_REACH_M, LEAF_MAX_REACH_M)
    repeat(LEAVES) { i ->
        key("leaf-$i") {
            val t = i / (LEAVES - 1f)
            val y = potH + (top - potH) * (LEAF_LOW + (1f - LEAF_LOW) * t) - LEAF_LENGTH_M * LEAF_TIP_DROP
            val r = reach * (1f - LEAF_INNER * t)
            // Low leaves droop outward, high ones reach up.
            val tilt = LEAF_LOW_TILT_DEG + (LEAF_HIGH_TILT_DEG - LEAF_LOW_TILT_DEG) * t
            Node(rotation = Rotation(y = i * GOLDEN_ANGLE_DEG)) {
                TubeNode(
                    points = listOf(Position(0f, potH, 0f), Position(0f, y, r - LEAF_LENGTH_M * LEAF_STEM_SHARE)),
                    radius = STEM_RADIUS_M,
                    materialInstance = f.leaf,
                )
                SphereNode(
                    radius = 0.5f,
                    materialInstance = if (i % 2 == 0) f.leaf else f.leafLight,
                    position = Position(0f, y, r),
                    rotation = Rotation(x = tilt),
                    scale = Scale(
                        LEAF_WIDTH_M * (1f - LEAF_SHRINK * t),
                        LEAF_THICKNESS_M,
                        LEAF_LENGTH_M * (1f - LEAF_SHRINK * t),
                    ),
                    apply = SPHERE_CASTER,
                )
            }
        }
    }
}

/** A plain desk facing +Z: an oak top on four dark legs. */
@Composable
private fun SceneScope.Desk(w: Float, d: Float, height: Float, f: RoomFinish) {
    Solid(Size(w, TOP_M, d), Position(0f, height - TOP_M / 2, 0f), f.oak.first())
    Legs(w - LEG_INSET_M * 2, d - LEG_INSET_M * 2, height - TOP_M, f.frame)
}

/** A plain chair facing +Z: seat, back at −Z, four legs. */
@Composable
private fun SceneScope.Chair(w: Float, d: Float, wood: MaterialInstance, f: RoomFinish) {
    val sw = min(w, CHAIR_SEAT_M)
    val sd = min(d, CHAIR_SEAT_M)
    Solid(Size(sw, TOP_M, sd), Position(0f, CHAIR_SEAT_H_M - TOP_M / 2, 0f), wood)
    Solid(Size(sw, CHAIR_BACK_H_M, TOP_M), Position(0f, CHAIR_SEAT_H_M + CHAIR_BACK_H_M / 2, -sd / 2 + TOP_M / 2), wood)
    Legs(sw - LEG_INSET_M, sd - LEG_INSET_M, CHAIR_SEAT_H_M - TOP_M, f.frame)
}

@Composable
private fun SceneScope.Legs(spanX: Float, spanZ: Float, height: Float, material: MaterialInstance) {
    for ((i, corner) in listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).withIndex()) {
        key("leg-$i") {
            Solid(
                Size(LEG_M, height, LEG_M),
                Position(corner.first * spanX / 2, height / 2, corner.second * spanZ / 2),
                material,
            )
        }
    }
}

/** The stand-in floor lamp while the arc lamp downloads: brass base and stem, linen shade. */
@Composable
private fun SceneScope.FloorLamp(h: Float, f: RoomFinish) {
    CylinderNode(
        radius = LAMP_BASE_R_M,
        height = LAMP_BASE_H_M,
        center = Position(0f, LAMP_BASE_H_M / 2, 0f),
        materialInstance = f.brass,
        apply = CYLINDER_CASTER,
    )
    Rod(Position(0f, 0f, 0f), h - LAMP_SHADE_H_M, LAMP_STEM_R_M, f.brass)
    CylinderNode(
        radius = LAMP_SHADE_R_M,
        height = LAMP_SHADE_H_M,
        center = Position(0f, h - LAMP_SHADE_H_M / 2, 0f),
        materialInstance = f.shade,
        apply = CYLINDER_CASTER,
    )
}

/** A bookshelf facing +Z: two sides, a back, and evenly spaced shelves. */
@Composable
private fun SceneScope.Bookshelf(w: Float, h: Float, d: Float, wood: MaterialInstance) {
    for (side in listOf(-1f, 1f)) {
        key("side-$side") { Solid(Size(TOP_M, h, d), Position(side * (w - TOP_M) / 2, h / 2, 0f), wood) }
    }
    Solid(Size(w, h, BACK_M), Position(0f, h / 2, -d / 2 + BACK_M / 2), wood)
    val shelves = (h / SHELF_PITCH_M).toInt().coerceAtLeast(2)
    repeat(shelves + 1) { i ->
        key("shelf-$i") { Solid(Size(w, TOP_M, d), Position(0f, TOP_M / 2 + i * (h - TOP_M) / shelves, 0f), wood) }
    }
}

/** A round pedestal side table. */
@Composable
private fun SceneScope.SideTable(span: Float, h: Float, wood: MaterialInstance, f: RoomFinish) {
    CylinderNode(
        radius = span / 2,
        height = TOP_M,
        center = Position(0f, h - TOP_M / 2, 0f),
        materialInstance = wood,
        apply = CYLINDER_CASTER,
    )
    Rod(Position(0f, 0f, 0f), h - TOP_M, LAMP_STEM_R_M * 2, f.frame)
    CylinderNode(
        radius = span * PEDESTAL_SHARE,
        height = LAMP_BASE_H_M,
        center = Position(0f, LAMP_BASE_H_M / 2, 0f),
        materialInstance = f.frame,
    )
}

/** The last resort: a thin frosted-glass box, for what has no product and no built stand-in. */
@Composable
private fun SceneScope.GlassBox(box: OrientedBox, floorY: Float, f: RoomFinish, g: Float) {
    FloorShadow(box, floorY, g)
    Node(
        position = Position(box.center.x, floorY, box.center.z),
        rotation = Rotation(y = box.yawDegrees),
        scale = Scale(1f, g.coerceAtLeast(GROW_FLOOR_FURNITURE), 1f),
    ) {
        CubeNode(
            size = Size(box.size.x, box.size.y, box.size.z),
            center = Position(0f, box.size.y / 2, 0f),
            materialInstance = f.frosted,
        )
    }
}

/** A shadow-casting box. */
@Composable
internal fun SceneScope.Solid(size: Size, center: Position, material: MaterialInstance) {
    CubeNode(size = size, center = center, materialInstance = material, apply = CUBE_CASTER)
}

/** An upright rod standing on [base]. */
@Composable
private fun SceneScope.Rod(base: Position, height: Float, radius: Float, material: MaterialInstance) {
    CylinderNode(
        radius = radius,
        height = height,
        center = Position(base.x, base.y + height / 2, base.z),
        materialInstance = material,
        apply = CYLINDER_CASTER,
    )
}

private val CUBE_CASTER: CubeNodeImpl.() -> Unit = { isShadowCaster = true }
private val SPHERE_CASTER: SphereNodeImpl.() -> Unit = { isShadowCaster = true }
private val CYLINDER_CASTER: CylinderNodeImpl.() -> Unit = { isShadowCaster = true }

private const val BUNDLED_ARMCHAIR = "models/khronos_sheen_chair.glb"
private const val EPSILON = 1e-4f
private const val GROW_FLOOR_FURNITURE = 0.001f
private const val QUARTER_TURN_DEG = 90f

/** The arc lamp's base, measured on the GLB, sits this far behind its bounding box's centre (× half depth). */
private const val ARC_LAMP_BASE_OFFSET = 0.81f
private const val LAMP_BASE_SHADOW_M = 0.5f

private const val SHADOW_SPREAD = 1.45f
private const val CONTACT_SHADOW_INTENSITY = 0.5f

private const val DESK_HEIGHT_M = 0.74f
private const val TOP_M = 0.03f
private const val BACK_M = 0.012f
private const val LEG_M = 0.035f
private const val LEG_INSET_M = 0.05f
private const val CHAIR_SEAT_M = 0.46f
private const val CHAIR_SEAT_H_M = 0.46f
private const val CHAIR_BACK_H_M = 0.36f
private const val SHELF_PITCH_M = 0.36f
private const val PEDESTAL_SHARE = 0.32f
private const val RUG_THICKNESS_M = 0.008f

private const val BED_PLINTH_M = 0.06f
private const val PLINTH_INSET_M = 0.1f
private const val BED_BASE_TOP_M = 0.3f
private const val MATTRESS_M = 0.2f
private const val MATTRESS_INSET_M = 0.04f
private const val HEADBOARD_HEIGHT_M = 1.05f
private const val HEADBOARD_DEPTH_M = 0.08f
private const val HEADBOARD_OVERHANG_M = 0.04f
private const val DUVET_COVER = 0.72f
private const val DUVET_THICKNESS_M = 0.05f
private const val DUVET_OVERHANG_M = 0.04f
private const val THROW_OVERHANG_M = 0.02f
private const val THROW_THICKNESS_M = 0.015f
private const val THROW_DEPTH_M = 0.45f
private const val THROW_FROM_FOOT_M = 0.08f
private const val DOUBLE_BED_M = 1.3f
private const val PILLOW_MAX_W_M = 0.68f
private const val PILLOW_GAP_M = 0.05f
private const val PILLOW_HEIGHT_M = 0.13f
private const val PILLOW_DEPTH_M = 0.4f

private const val RADIATOR_MIN_TOP_M = 0.5f
private const val RADIATOR_MAX_TOP_M = 0.78f
private const val RADIATOR_FLOOR_GAP_M = 0.14f
private const val RADIATOR_DEPTH_M = 0.07f
private const val RADIATOR_END_M = 0.04f
private const val RADIATOR_FIN_PITCH_M = 0.05f
private const val RADIATOR_FIN_W_M = 0.022f
private const val RADIATOR_FIN_D_M = 0.012f
private const val PIPE_RADIUS_M = 0.012f

private const val POT_SHARE = 0.34f
private const val POT_MIN_R_M = 0.1f
private const val POT_MAX_R_M = 0.2f
private const val POT_ASPECT = 1.5f
private const val POT_RIM_M = 0.012f
private const val POT_RIM_H_M = 0.035f
private const val SOIL_M = 0.012f
private const val PLANT_MIN_H_M = 0.7f
private const val PLANT_MAX_H_M = 1.3f
private const val LEAF_MIN_REACH_M = 0.16f
private const val LEAF_MAX_REACH_M = 0.3f
private const val LEAVES = 13
private const val LEAF_LOW = 0.45f
private const val LEAF_INNER = 0.45f
private const val LEAF_TIP_DROP = 0.1f
private const val LEAF_STEM_SHARE = 0.45f
private const val LEAF_LOW_TILT_DEG = 28f
private const val LEAF_HIGH_TILT_DEG = -32f
private const val LEAF_WIDTH_M = 0.2f
private const val LEAF_LENGTH_M = 0.27f
private const val LEAF_THICKNESS_M = 0.014f
private const val LEAF_SHRINK = 0.35f
private const val STEM_RADIUS_M = 0.005f
private const val GOLDEN_ANGLE_DEG = 137.5f

private const val LAMP_BASE_R_M = 0.14f
private const val LAMP_BASE_H_M = 0.025f
private const val LAMP_STEM_R_M = 0.012f
private const val LAMP_SHADE_R_M = 0.17f
private const val LAMP_SHADE_H_M = 0.26f
