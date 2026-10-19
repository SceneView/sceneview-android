package io.github.sceneview.demo.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.sceneview.demo.R
import io.github.sceneview.demo.demos.internal.ArDebugFrame
import io.github.sceneview.demo.demos.internal.CoverageGrid
import io.github.sceneview.demo.demos.internal.DebugPlaneKind
import io.github.sceneview.demo.demos.internal.DebugPose
import io.github.sceneview.demo.demos.internal.ScanCoach
import io.github.sceneview.demo.demos.internal.ScanFollow
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.theme.SceneViewTokens.ArOverlay
import io.github.sceneview.demo.theme.SceneViewTokens.Space
import io.github.sceneview.demo.ui.overMediaEdge
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * What the scan has, fed frame by frame from the 3D view's `observe` (#4631): the floor
 * [grid], the phone and its trail for the map, and the [coach]'s line. Plain fields for the
 * draw, read only in the draw phase through [tick]; the two figures the chrome shows,
 * [floorPercent] and [coachLine], are Compose state that changes only when their value does.
 */
internal class ScanCoverageState {
    val grid = CoverageGrid()
    val coach = ScanCoach()

    /** The phone now, `null` before the first pose. */
    var camera: DebugPose? = null
        private set

    /** The phone's path so far, flat xyz. */
    var trail: FloatArray = FloatArray(0)
        private set

    /** Heading-up: the azimuth the phone faces, [ScanFollow]'s convention. */
    var headingDegrees = 0f
        private set

    /**
     * The room's floor as far as the scan can tell ([CoverageGrid.floorExtent]): the map draws it
     * hatched, so what lies inside it and is not covered reads as still to scan.
     */
    var extent: List<Pair<Float, Float>> = emptyList()
        private set

    /** The map's scale, pixels per metre, `0` until the first draw fits it ([fitScale]). */
    var pxPerMetre = 0f
        private set

    /** Bumped when the map has something new to draw: read in the draw phase only. */
    var tick by mutableIntStateOf(0)
        private set

    /** `Floor NN % covered`: the covered share of [extent]. */
    var floorPercent by mutableIntStateOf(0)
        private set

    var coachLine by mutableStateOf<ScanCoach.Line?>(null)
        private set

    private var heading: Float? = null
    private var cellsPath: Path? = null
    private var edgesPath: Path? = null
    private var pathsVersion = -1
    private var extentVersion = -1
    private var floorKey = 0

    /**
     * [dense] is a v2 scan's dense cloud (flat xyz), of which [denseCount] surfels are found by
     * [frame]'s time: it stands in for the feature points, as it does in the 3D view, so a scan whose
     * room is drawn from depth does not read `Floor 0 % covered` under that room (#4619).
     */
    fun observe(frame: ArDebugFrame, dense: FloatArray? = null, denseCount: Int = 0) {
        val grew = if (dense != null) grid.update(dense, denseCount) else grid.update(frame.mapPoints)
        coachLine = coach.update(frame.time, frame.camera, grew)
        val moved = frame.camera != camera
        camera = frame.camera
        trail = frame.trail
        frame.camera?.let { pose -> heading = ScanFollow.headingOf(pose, heading) }
        headingDegrees = heading ?: 0f
        val floors = frame.planes.filter { it.kind == DebugPlaneKind.Floor && it.vertexCount >= 3 }
        val key = floors.fold(floors.size) { acc, plane -> acc * KEY_PRIME + plane.polygon.contentHashCode() }
        val reshaped = key != floorKey || grid.version != extentVersion
        if (reshaped) {
            floorKey = key
            extentVersion = grid.version
            extent = grid.floorExtent(floors.map { it.polygon })
            val percent = (grid.coveredShareOf(extent) * PERCENT).roundToInt()
            if (percent != floorPercent) floorPercent = percent
        }
        if (reshaped || moved) tick++
    }

    /** The covered cells as one path, and the outline of what is covered, in world metres (XZ). */
    fun paths(): Pair<Path, Path> {
        val cells = cellsPath
        val edges = edgesPath
        if (cells != null && edges != null && pathsVersion == grid.version) return cells to edges
        val size = grid.cellMetres
        val fill = Path()
        val outline = Path()
        grid.forEachCovered { cx, cz ->
            val x = cx * size
            val z = cz * size
            fill.addRect(androidx.compose.ui.geometry.Rect(x, z, x + size, z + size))
            // An edge whose other side is not covered: the frontier, where to walk next.
            if (!grid.isCovered(cx - 1, cz)) outline.segment(x, z, x, z + size)
            if (!grid.isCovered(cx + 1, cz)) outline.segment(x + size, z, x + size, z + size)
            if (!grid.isCovered(cx, cz - 1)) outline.segment(x, z, x + size, z)
            if (!grid.isCovered(cx, cz + 1)) outline.segment(x, z + size, x + size, z + size)
        }
        cellsPath = fill
        edgesPath = outline
        pathsVersion = grid.version
        return fill to outline
    }

    /**
     * The scale that fits [extent] in a [width] × [height] map whose marker sits at
     * (`width / 2`, [markerY]), turned heading-up, with [margin] pixels of air. It zooms out at
     * once so nothing leaves the map, and back in gently so the map does not breathe with each step.
     */
    fun fitScale(width: Float, height: Float, markerY: Float, margin: Float): Float {
        val pose = camera
        val half = min(width, height) / 2f
        var target = half / MIN_HALF_RANGE_M
        if (pose != null && extent.isNotEmpty()) {
            val turn = Math.toRadians(headingDegrees.toDouble())
            val c = cos(turn).toFloat()
            val s = sin(turn).toFloat()
            val side = width / 2f - margin
            val up = markerY - margin
            val down = height - markerY - margin
            for ((x, z) in extent) {
                val dx = x - pose.x
                val dz = z - pose.z
                // Where Compose's rotate(headingDegrees) puts the vertex, relative to the marker.
                val sx = dx * c - dz * s
                val sy = dx * s + dz * c
                if (abs(sx) > EPSILON) target = min(target, side / abs(sx))
                if (sy < -EPSILON) target = min(target, up / -sy)
                if (sy > EPSILON) target = min(target, down / sy)
            }
        }
        target = max(target, half / MAX_HALF_RANGE_M)
        val now = pxPerMetre
        pxPerMetre = if (now == 0f || target < now) target else now + (target - now) * ZOOM_IN_RATE
        return pxPerMetre
    }

    companion object {
        /** The map never zooms in past a metre and a half either side of the phone. */
        const val MIN_HALF_RANGE_M = 1.5f

        /** Nor out past a hall's width. */
        const val MAX_HALF_RANGE_M = 15f

        /** The share of the way back in one draw zooms, so a tighter fit eases in. */
        const val ZOOM_IN_RATE = 0.08f

        private const val EPSILON = 1e-3f
        private const val KEY_PRIME = 31
        private const val PERCENT = 100f
    }
}

private fun Path.segment(x0: Float, z0: Float, x1: Float, z1: Float) {
    moveTo(x0, z0)
    lineTo(x1, z1)
}

/**
 * The live footprint of a scan (#4631): the floor seen from above, heading-up like the 3D view,
 * the phone fixed a little below its middle, the room turning and scaling under it. The room's
 * estimated floor is hatched — still to scan — covered cells are filled over it, the edge of what is
 * covered is outlined, the path walked is drawn under them. [showFloor] adds `Floor NN % covered`.
 */
@Composable
internal fun ScanCoverageMap(state: ScanCoverageState, modifier: Modifier = Modifier, showFloor: Boolean = true) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < LUMINANCE_MID
    val scrim = if (dark) ArOverlay.scrimDark else ArOverlay.scrimLight
    val shape = RoundedCornerShape(SceneViewTokens.Radius.lg)
    val mapShape = RoundedCornerShape(SceneViewTokens.Radius.md)
    val percent = state.floorPercent
    val label = stringResource(R.string.scan_coverage_label, percent)
    Column(
        modifier = modifier
            .overMediaEdge(shape)
            .clip(shape)
            .background(scrim, shape)
            .padding(Space.sm)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .testTag(SCAN_COVERAGE_MAP_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Canvas(Modifier.size(ScanMapSize).clip(mapShape).background(ArOverlay.meterTrack, mapShape)) {
            // Read here only: a new cell or a step redraws the map, never recomposes it.
            state.tick
            val camera = state.camera
            // You stay put, below the middle like a navigation app's arrow: most of the map is
            // ahead of you, where you are about to scan.
            val centre = Offset(size.width / 2f, size.height * MARKER_HEIGHT)
            val pxPerMetre = state.fitScale(size.width, size.height, centre.y, MapFitMargin.toPx())
            val (cells, edges) = state.paths()
            val extent = state.extent
            val floor = Path()
            extent.forEachIndexed { i, (x, z) -> if (i == 0) floor.moveTo(x, z) else floor.lineTo(x, z) }
            floor.close()
            translate(centre.x, centre.y) {
                // Heading-up: the way the phone faces is the top of the map (see ScanFollow).
                rotate(state.headingDegrees, pivot = Offset.Zero) {
                    scale(pxPerMetre, pxPerMetre, pivot = Offset.Zero) {
                        translate(-(camera?.x ?: 0f), -(camera?.z ?: 0f)) {
                            if (extent.size >= 3) {
                                // The floor still to scan: hatched inside the room's estimated
                                // outline, so a gap reads as "not yet", not as "nothing there".
                                clipPath(floor) {
                                    // Never over what is covered: a covered cell reads solid.
                                    clipPath(cells, ClipOp.Difference) {
                                        drawHatch(extent, MapHatchStep.toPx() / pxPerMetre, pxPerMetre)
                                    }
                                }
                                drawPath(
                                    floor,
                                    ArOverlay.onScrimMuted,
                                    style = Stroke(
                                        width = MapEdgeWidth.toPx() / pxPerMetre,
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(MapDash.toPx() / pxPerMetre, MapDash.toPx() / pxPerMetre),
                                        ),
                                    ),
                                )
                            }
                            drawTrail(state.trail, ArOverlay.onScrimMuted, MapTrailWidth.toPx() / pxPerMetre)
                            drawPath(cells, ArOverlay.accentProgress.copy(alpha = COVERED_ALPHA))
                            drawPath(
                                edges,
                                ArOverlay.onScrim,
                                style = Stroke(width = MapEdgeWidth.toPx() / pxPerMetre, cap = StrokeCap.Square),
                            )
                        }
                    }
                }
            }
            if (camera != null) {
                // You: the live view's marker, small — a disc with the heading's arrow, pointing up.
                val r = MapMarkerSize.toPx() / 2f
                drawCircle(ArOverlay.onScrim, radius = r + MapEdgeWidth.toPx(), center = centre)
                drawCircle(ArOverlay.accentProgress, radius = r, center = centre)
                val arrow = Path().apply {
                    moveTo(centre.x, centre.y - r * ARROW_TIP)
                    lineTo(centre.x + r * ARROW_HALF_WIDTH, centre.y + r * ARROW_BASE)
                    lineTo(centre.x, centre.y + r * ARROW_NOTCH)
                    lineTo(centre.x - r * ARROW_HALF_WIDTH, centre.y + r * ARROW_BASE)
                    close()
                }
                drawPath(arrow, ArOverlay.onAccentProgress)
            }
        }
        if (showFloor) {
            Text(
                text = stringResource(R.string.scan_coverage_floor, percent),
                style = SceneViewTokens.Type.caption,
                color = ArOverlay.onScrim,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrail(
    trail: FloatArray,
    color: androidx.compose.ui.graphics.Color,
    width: Float,
) {
    val points = trail.size / 3
    if (points < 2) return
    val path = Path()
    path.moveTo(trail[0], trail[2])
    for (i in 1 until points) path.lineTo(trail[i * 3], trail[i * 3 + 2])
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Diagonal hatching over [extent]'s box, [step] metres apart; the caller clips it to the floor. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHatch(
    extent: List<Pair<Float, Float>>,
    step: Float,
    pxPerMetre: Float,
) {
    val minX = extent.minOf { it.first }
    val maxX = extent.maxOf { it.first }
    val minZ = extent.minOf { it.second }
    val maxZ = extent.maxOf { it.second }
    val span = (maxX - minX) + (maxZ - minZ)
    val color = ArOverlay.onScrimMuted.copy(alpha = HATCH_ALPHA)
    val width = MapEdgeWidth.toPx() / pxPerMetre
    var d = 0f
    while (d <= span) {
        // Lines at 45°: x + z = minX + minZ + d.
        drawLine(color, Offset(minX + d, minZ), Offset(minX + d - span, minZ + span), strokeWidth = width)
        d += step
    }
}

/** `112 dp`: the map reads a room at a glance and leaves the live view its middle. */
private val ScanMapSize = Space.x4l + Space.md

/** The phone's marker on the map: half the live view's, the map is a quarter of its scale. */
private val MapMarkerSize = Space.md - Space.xs / 2

private val MapEdgeWidth = SceneViewTokens.Glass.edgeWidth
private val MapTrailWidth = SceneViewTokens.Glass.edgeWidth * 2

/** Air between the room's outline and the map's edge. */
private val MapFitMargin = Space.sm

/** The hatching's pitch and the outline's dash: fine enough to read as texture, not as lines. */
private val MapHatchStep = Space.xs + Space.xs / 2
private val MapDash = Space.xs

/** The marker's height in the map: below the middle, most of the map ahead of you. */
private const val MARKER_HEIGHT = 0.7f
private const val HATCH_ALPHA = 0.45f
private const val COVERED_ALPHA = 0.6f
private const val LUMINANCE_MID = 0.5f
private const val ARROW_TIP = 0.62f
private const val ARROW_BASE = 0.5f
private const val ARROW_NOTCH = 0.22f
private const val ARROW_HALF_WIDTH = 0.46f

internal const val SCAN_COVERAGE_MAP_TAG = "scan_coverage_map"
