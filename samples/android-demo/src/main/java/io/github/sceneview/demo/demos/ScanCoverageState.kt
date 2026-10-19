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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import io.github.sceneview.demo.demos.internal.DebugPose
import io.github.sceneview.demo.demos.internal.ScanCoach
import io.github.sceneview.demo.demos.internal.ScanFollow
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.theme.SceneViewTokens.ArOverlay
import io.github.sceneview.demo.theme.SceneViewTokens.Space
import io.github.sceneview.demo.ui.overMediaEdge
import kotlin.math.hypot
import kotlin.math.max
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

    /** The map's half-width, in metres: grows with the scan, so nothing covered leaves the map. */
    var halfRangeMetres = MIN_HALF_RANGE_M
        private set

    /** Bumped when the map has something new to draw: read in the draw phase only. */
    var tick by mutableIntStateOf(0)
        private set

    /** `Floor NN %`: the covered share of the scan's footprint. */
    var floorPercent by mutableIntStateOf(0)
        private set

    var coachLine by mutableStateOf<ScanCoach.Line?>(null)
        private set

    private var heading: Float? = null
    private var cellsPath: Path? = null
    private var edgesPath: Path? = null
    private var pathsVersion = -1

    fun observe(frame: ArDebugFrame) {
        val grew = grid.update(frame.mapPoints)
        coachLine = coach.update(frame.time, frame.camera, grew)
        val moved = frame.camera != camera
        camera = frame.camera
        trail = frame.trail
        frame.camera?.let { pose -> heading = ScanFollow.headingOf(pose, heading) }
        headingDegrees = heading ?: 0f
        if (grew) {
            // The map holds the farthest covered corner from the phone, never shrinking under it.
            halfRangeMetres = max(halfRangeMetres, reachOf(frame.camera) * RANGE_MARGIN)
        }
        if (grew || moved) tick++
        val percent = (grid.coveredShare * PERCENT).roundToInt()
        if (percent != floorPercent) floorPercent = percent
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

    private fun reachOf(camera: DebugPose?): Float {
        val x = camera?.x ?: 0f
        val z = camera?.z ?: 0f
        val size = grid.cellMetres
        val dx = max(x - grid.minCx * size, (grid.maxCx + 1) * size - x)
        val dz = max(z - grid.minCz * size, (grid.maxCz + 1) * size - z)
        return hypot(dx, dz)
    }

    companion object {
        /** A small room's width either side of the phone, before the scan has any. */
        const val MIN_HALF_RANGE_M = 3f

        /** A cell's width of air around what is covered. */
        const val RANGE_MARGIN = 1.08f

        private const val PERCENT = 100f
    }
}

private fun Path.segment(x0: Float, z0: Float, x1: Float, z1: Float) {
    moveTo(x0, z0)
    lineTo(x1, z1)
}

/**
 * The live footprint of a scan (#4631): the floor seen from above, heading-up like the 3D view,
 * the phone at its centre. Covered cells are filled, the edge of what is covered is outlined —
 * where to walk next — and the path walked is drawn under them. [showFloor] adds the
 * `Floor NN %` figure under the map.
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
            val centre = Offset(size.width / 2f, size.height / 2f)
            val pxPerMetre = size.minDimension / 2f / state.halfRangeMetres
            val (cells, edges) = state.paths()
            translate(centre.x, centre.y) {
                // Heading-up: the way the phone faces is the top of the map (see ScanFollow).
                rotate(state.headingDegrees, pivot = Offset.Zero) {
                    scale(pxPerMetre, pxPerMetre, pivot = Offset.Zero) {
                        translate(-(camera?.x ?: 0f), -(camera?.z ?: 0f)) {
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

/** `112 dp`: the map reads a room at a glance and leaves the live view its middle. */
private val ScanMapSize = Space.x4l + Space.md

/** The phone's marker on the map: half the live view's, the map is a quarter of its scale. */
private val MapMarkerSize = Space.md - Space.xs / 2

private val MapEdgeWidth = SceneViewTokens.Glass.edgeWidth
private val MapTrailWidth = SceneViewTokens.Glass.edgeWidth * 2

private const val COVERED_ALPHA = 0.6f
private const val LUMINANCE_MID = 0.5f
private const val ARROW_TIP = 0.62f
private const val ARROW_BASE = 0.5f
private const val ARROW_NOTCH = 0.22f
private const val ARROW_HALF_WIDTH = 0.46f

internal const val SCAN_COVERAGE_MAP_TAG = "scan_coverage_map"
