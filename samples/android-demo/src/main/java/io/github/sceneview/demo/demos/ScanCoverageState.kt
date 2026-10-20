package io.github.sceneview.demo.demos

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import io.github.sceneview.demo.demos.internal.ScanAreaCopy
import io.github.sceneview.demo.ui.pressScale
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
 * [coveredSquareMetres] and [coachLine], are Compose state that changes only when their value does.
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
    private var lastHalf = 0f

    /** Bumped when the map has something new to draw: read in the draw phase only. */
    var tick by mutableIntStateOf(0)
        private set

    /**
     * The floor covered so far, in m² (#4717): covered cells times a cell's area, to a tenth.
     * It only grows within the capture: a share of a room whose outline grows with the scan fell
     * as the scan went on, so the map says how much is covered, never of how much.
     */
    var coveredSquareMetres by mutableFloatStateOf(0f)
        private set

    var coachLine by mutableStateOf<ScanCoach.Line?>(null)
        private set

    private var heading: Float? = null
    private var cellsPath: Path? = null
    private var edgesPath: Path? = null
    private var pathsVersion = -1
    private var extentVersion = -1
    private var floorKey = 0
    private var lastSnapshot: FloatArray? = null

    /**
     * [dense] is a v2 scan's dense cloud (flat xyz), of which [denseCount] surfels are found by
     * [frame]'s time: it stands in for the feature points, as it does in the 3D view, so a scan whose
     * room is drawn from depth does not read `Floor 0 % covered` under that room (#4619).
     * [snapshot] is a live depth scan's dense cloud as it stands ([snapshotCount] surfels), each
     * new one binned once on top of the feature points ([CoverageGrid.cover]): live and replay
     * then count the same depth points.
     */
    fun observe(
        frame: ArDebugFrame,
        dense: FloatArray? = null,
        denseCount: Int = 0,
        pose: DebugPose? = frame.camera,
        snapshot: FloatArray? = null,
        snapshotCount: Int = 0,
    ) {
        var grew = if (dense != null) grid.update(dense, denseCount) else grid.update(frame.mapPoints)
        // A live depth scan's dense cloud, a new snapshot every few seconds (#4540): the feature
        // map alone stops at its cap, and the figure froze while the cloud kept growing.
        if (snapshot != null && snapshot !== lastSnapshot) {
            lastSnapshot = snapshot
            grew = grid.cover(snapshot, snapshotCount) || grew
        }
        coachLine = coach.update(frame.time, frame.camera, grew)
        // [pose] is the phone now: the trace's while it records, the live one while paused (#4718).
        val moved = pose != camera
        camera = pose
        trail = frame.trail
        pose?.let { heading = ScanFollow.headingOf(it, heading) }
        headingDegrees = heading ?: 0f
        val floors = frame.planes.filter { it.kind == DebugPlaneKind.Floor && it.vertexCount >= 3 }
        val key = floors.fold(floors.size) { acc, plane -> acc * KEY_PRIME + plane.polygon.contentHashCode() }
        val reshaped = key != floorKey || grid.version != extentVersion
        if (reshaped) {
            floorKey = key
            extentVersion = grid.version
            extent = grid.floorExtent(floors.map { it.polygon })
            val area = (grid.covered * grid.cellMetres * grid.cellMetres * TENTHS).roundToInt() / TENTHS
            if (area > coveredSquareMetres) coveredSquareMetres = area
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
        // Eased in map half-sizes per metre, not pixels: the card opening into the map to
        // complete (#4718) draws the same framing at once, larger, instead of zooming in to it.
        val now = pxPerMetre * lastHalf.let { if (it > 0f) half / it else 1f }
        lastHalf = half
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
        private const val TENTHS = 10f
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
 * covered is outlined, the path walked is drawn under them. [showFloor] adds the area covered,
 * `12.5 m² covered`, which only grows (#4717).
 *
 * A tap opens it into the map to complete (#4718): the same map across the screen, with what
 * each mark means, and a tap closes it again. The card grows and shrinks on `motion-spring`.
 */
@Composable
@Suppress("LongMethod") // one card, two sizes, drawn by one Canvas
internal fun ScanCoverageMap(
    state: ScanCoverageState,
    modifier: Modifier = Modifier,
    showFloor: Boolean = true,
    expanded: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < LUMINANCE_MID
    val scrim = if (dark) ArOverlay.scrimDark else ArOverlay.scrimLight
    val shape = RoundedCornerShape(SceneViewTokens.Radius.lg)
    val mapShape = RoundedCornerShape(SceneViewTokens.Radius.md)
    val area = ScanAreaCopy.area(state.coveredSquareMetres)
    val label = stringResource(R.string.scan_coverage_label, area)
    val toggleLabel = stringResource(if (expanded) R.string.scan_coverage_collapse else R.string.scan_coverage_expand)
    val interaction = remember { MutableInteractionSource() }
    val tappable = onToggle?.let { toggle ->
        Modifier
            .pressScale(interaction)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = toggleLabel,
                onClick = toggle,
            )
    } ?: Modifier
    Column(
        modifier = modifier
            .then(if (expanded) Modifier.widthIn(max = ArOverlay.maxWidth).fillMaxWidth() else Modifier)
            .then(tappable)
            .overMediaEdge(shape)
            .clip(shape)
            .background(scrim, shape)
            .animateContentSize(SceneViewTokens.Motion.spring())
            .padding(if (expanded) Space.md else Space.sm)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .testTag(if (expanded) SCAN_COVERAGE_MAP_EXPANDED_TAG else SCAN_COVERAGE_MAP_TAG),
        horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (expanded) Space.sm else Space.xs),
    ) {
        if (expanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.scan_coverage_title),
                    style = SceneViewTokens.Type.card,
                    color = ArOverlay.onScrim,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.CloseFullscreen,
                    contentDescription = null,
                    tint = ArOverlay.onScrimMuted,
                    modifier = Modifier.size(MapGlyphSize),
                )
            }
        }
        Box {
            val canvas = if (expanded) Modifier.fillMaxWidth().aspectRatio(1f) else Modifier.size(ScanMapSize)
            Canvas(canvas.clip(mapShape).background(ArOverlay.meterTrack, mapShape)) {
                drawMap(state, MapFitMargin.toPx(), MapHatchStep.toPx(), MapEdgeWidth.toPx())
            }
            if (!expanded && onToggle != null) {
                // The map opens: the corner says so, like a picture-in-picture's.
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = null,
                    tint = ArOverlay.onScrim,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Space.xs)
                        .size(MapCornerGlyphSize),
                )
            }
        }
        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
                MapLegend(stringResource(R.string.scan_coverage_covered, area)) { swatch ->
                    drawRect(ArOverlay.accentProgress.copy(alpha = COVERED_ALPHA), size = swatch)
                }
                MapLegend(stringResource(R.string.scan_coverage_missing)) { swatch ->
                    clipRect(right = swatch.width, bottom = swatch.height) {
                        val box = listOf(0f to 0f, swatch.width to 0f, swatch.width to swatch.height, 0f to swatch.height)
                        drawHatch(box, MapHatchStep.toPx(), 1f / MapEdgeWidth.toPx())
                    }
                }
                MapLegend(stringResource(R.string.scan_coverage_you)) { swatch ->
                    drawCircle(ArOverlay.accentProgress, radius = swatch.minDimension / 2f)
                }
            }
            Text(
                text = stringResource(R.string.scan_coverage_hint),
                style = SceneViewTokens.Type.caption,
                color = ArOverlay.onScrimMuted,
            )
        } else if (showFloor) {
            Text(
                text = stringResource(R.string.scan_coverage_area, area),
                style = SceneViewTokens.Type.caption,
                color = ArOverlay.onScrim,
            )
        }
    }
}

/** One mark of the open map and what it means: a 12 dp swatch, then its caption. */
@Composable
private fun MapLegend(text: String, swatch: androidx.compose.ui.graphics.drawscope.DrawScope.(Size) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        Canvas(Modifier.size(MapSwatchSize)) { swatch(size) }
        Text(text = text, style = SceneViewTokens.Type.caption, color = ArOverlay.onScrim)
    }
}

/** The map itself, whatever the card's size: fitted, heading-up, you below the middle. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMap(
    state: ScanCoverageState,
    fitMargin: Float,
    hatchStep: Float,
    edgeWidth: Float,
) {
    // Read here only: a new cell or a step redraws the map, never recomposes it.
    state.tick
    val camera = state.camera
    // You stay put, below the middle like a navigation app's arrow: most of the map is
    // ahead of you, where you are about to scan.
    val centre = Offset(size.width / 2f, size.height * MARKER_HEIGHT)
    val pxPerMetre = state.fitScale(size.width, size.height, centre.y, fitMargin)
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
                                drawHatch(extent, gridMetres(hatchStep / pxPerMetre), pxPerMetre / edgeWidth)
                            }
                        }
                        drawPath(
                            floor,
                            ArOverlay.onScrimMuted,
                            style = Stroke(
                                width = edgeWidth / pxPerMetre,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(hatchStep / MAP_DASH_PER_HATCH / pxPerMetre, hatchStep / MAP_DASH_PER_HATCH / pxPerMetre),
                                ),
                            ),
                        )
                    }
                    drawTrail(state.trail, ArOverlay.onScrimMuted, edgeWidth * 2f / pxPerMetre)
                    drawPath(cells, ArOverlay.accentProgress.copy(alpha = COVERED_ALPHA))
                    drawPath(
                        edges,
                        ArOverlay.onScrim,
                        style = Stroke(width = edgeWidth / pxPerMetre, cap = StrokeCap.Square),
                    )
                }
            }
        }
    }
    if (camera != null) {
        // You: a disc with the heading's arrow, pointing up. A quarter of the short side at
        // most, so the open map draws it larger than the corner one.
        val r = (size.minDimension * MARKER_SHARE).coerceIn(MARKER_MIN_RADIUS * edgeWidth, MARKER_MAX_RADIUS * edgeWidth)
        drawCircle(ArOverlay.onScrim, radius = r + edgeWidth, center = centre)
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

/**
 * The floor still to scan, as plan paper (#4540, "a dimensioned grid"): a square grid over
 * [extent]'s box, [step] apart and anchored on whole multiples of it, so it stays put as the room
 * grows; every [MAJOR_EVERY]th line a shade stronger, the way a plan marks its metres. The caller
 * clips it to the floor.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHatch(
    extent: List<Pair<Float, Float>>,
    step: Float,
    unitsPerLine: Float,
) {
    val minX = extent.minOf { it.first }
    val maxX = extent.maxOf { it.first }
    val minZ = extent.minOf { it.second }
    val maxZ = extent.maxOf { it.second }
    val minor = ArOverlay.onScrimMuted.copy(alpha = HATCH_ALPHA)
    val major = ArOverlay.onScrimMuted.copy(alpha = GRID_MAJOR_ALPHA)
    // One edge width wide, whatever the units the caller draws in.
    val width = 1f / unitsPerLine
    var i = kotlin.math.floor(minX / step).toInt()
    while (i * step <= maxX) {
        val color = if (Math.floorMod(i, MAJOR_EVERY) == 0) major else minor
        drawLine(color, Offset(i * step, minZ), Offset(i * step, maxZ), strokeWidth = width)
        i++
    }
    var j = kotlin.math.floor(minZ / step).toInt()
    while (j * step <= maxZ) {
        val color = if (Math.floorMod(j, MAJOR_EVERY) == 0) major else minor
        drawLine(color, Offset(minX, j * step), Offset(maxX, j * step), strokeWidth = width)
        j++
    }
}

/**
 * The map's grid pitch in metres: the coverage cell's, so each square is one cell to cover, or a
 * metre when the map is drawn too small for cells to read ([minStep] metres apart at least).
 */
private fun gridMetres(minStep: Float): Float =
    if (CoverageGrid.CELL_METRES >= minStep) CoverageGrid.CELL_METRES else GRID_MAJOR_METRES

/** `112 dp`: the map reads a room at a glance and leaves the live view its middle. */
private val ScanMapSize = Space.x4l + Space.md

private val MapEdgeWidth = SceneViewTokens.Glass.edgeWidth

/** The glyphs of the map card: the open map's close mark, the corner one's open mark. */
private val MapGlyphSize = Space.lg - Space.xs
private val MapCornerGlyphSize = Space.md - Space.xs / 2

/** A legend mark of the open map: 12 dp. */
private val MapSwatchSize = Space.sm + Space.xs

/** Air between the room's outline and the map's edge. */
private val MapFitMargin = Space.sm

/** The hatching's pitch, and the outline's dash at two thirds of it: texture, not lines. */
private val MapHatchStep = Space.xs + Space.xs / 2
private const val MAP_DASH_PER_HATCH = 1.5f

/** The marker's radius: a share of the map's short side, between 5 and 12 edge widths (dp). */
private const val MARKER_SHARE = 0.045f
private const val MARKER_MIN_RADIUS = 5f
private const val MARKER_MAX_RADIUS = 12f

/** The marker's height in the map: below the middle, most of the map ahead of you. */
private const val MARKER_HEIGHT = 0.7f
private const val HATCH_ALPHA = 0.3f

/** The grid's metre lines: every fourth line of a 25 cm grid, a shade stronger. */
private const val MAJOR_EVERY = 4
private const val GRID_MAJOR_ALPHA = 0.6f
private const val GRID_MAJOR_METRES = 1f
private const val COVERED_ALPHA = 0.6f
private const val LUMINANCE_MID = 0.5f
private const val ARROW_TIP = 0.62f
private const val ARROW_BASE = 0.5f
private const val ARROW_NOTCH = 0.22f
private const val ARROW_HALF_WIDTH = 0.46f

internal const val SCAN_COVERAGE_MAP_TAG = "scan_coverage_map"
internal const val SCAN_COVERAGE_MAP_EXPANDED_TAG = "scan_coverage_map_expanded"
