package io.github.sceneview.demo.demos.internal

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.lookTowards
import io.github.sceneview.math.Position
import kotlin.math.abs
import kotlin.math.sqrt

/*
 * What Measure (#4732) adds on top of the shared live walls: the reticle landing on a wall's foot,
 * labels that read the right way up, why a point is refused, and what the add button does next.
 * Pure, so each rule runs in a JVM test.
 */

/** The reticle on a recognised wall: [position], its foot under the aim, on the wall [a]–[b]. */
data class MeasureWallFoot(val position: Position, val a: Position, val b: Position)

/**
 * Where the camera ray meets one of the recognised [walls] (standing on [floorY], [height] tall),
 * dropped to the wall's foot at [level]: the point a wall measure wants, without aiming at the
 * seam by hand. `null` when the ray reaches the floor before any wall — the user aims at the floor
 * in front of it — or misses every wall.
 */
fun measureLiveWallFoot(
    origin: Position,
    direction: Position,
    walls: List<LiveWall>,
    floorY: Float,
    height: Float,
    level: Float = floorY,
): MeasureWallFoot? {
    var best: MeasureWallFoot? = null
    var bestT = Float.MAX_VALUE
    for (wall in walls) {
        val ex = wall.bx - wall.ax
        val ez = wall.bz - wall.az
        val length = sqrt(ex * ex + ez * ez)
        if (length < WALL_MIN_LENGTH_M) continue
        val nx = -ez / length
        val nz = ex / length
        val denominator = direction.x * nx + direction.z * nz
        if (abs(denominator) < PARALLEL) continue
        val t = ((wall.ax - origin.x) * nx + (wall.az - origin.z) * nz) / denominator
        if (t <= 0f || t >= bestT) continue
        val hx = origin.x + direction.x * t
        val hy = origin.y + direction.y * t
        val hz = origin.z + direction.z * t
        val along = ((hx - wall.ax) * ex + (hz - wall.az) * ez) / length
        if (along < -SPAN_SLACK_M || along > length + SPAN_SLACK_M) continue
        if (hy < floorY - BELOW_FLOOR_SLACK_M || hy > floorY + height + ABOVE_TOP_SLACK_M) continue
        val s = (along / length).coerceIn(0f, 1f)
        bestT = t
        best = MeasureWallFoot(
            position = Position(wall.ax + ex * s, level, wall.az + ez * s),
            a = Position(wall.ax, level, wall.az),
            b = Position(wall.bx, level, wall.bz),
        )
    }
    val wallFoot = best ?: return null
    // Aiming at the floor in front of a wall is a floor point, not the wall's foot.
    val floorT = if (direction.y < -PARALLEL) (floorY - origin.y) / direction.y else Float.MAX_VALUE
    return wallFoot.takeIf { floorT <= 0f || floorT >= bestT - FLOOR_FIRST_SLACK_M }
}

/**
 * The feet of the recognised [walls] on [level], as snap targets: each foot an edge, its two ends
 * corners — the room's corners where two walls meet, already squared by [PlaneWallSource].
 */
fun measureLiveWallTargets(walls: List<LiveWall>, level: Float): MeasureSnapContext = MeasureSnapContext(
    corners = walls.flatMap { listOf(Position(it.ax, level, it.az), Position(it.bx, level, it.bz)) },
    edges = walls.map { Position(it.ax, level, it.az) to Position(it.bx, level, it.bz) },
)

/**
 * The orientation of a length label at [label] seen from [eye]: its front (local `+Z`) toward the
 * eye and its top along the screen's up ([cameraUp], the camera's world `+Y`), so the text reads
 * the right way up whatever the angle — looking straight down on a floor segment included, where
 * a world-up billboard flips (#4732). `null` with no direction to face.
 */
fun measureLabelQuaternion(label: Position, eye: Position, cameraUp: Float3): Quaternion? {
    val tx = label.x - eye.x
    val ty = label.y - eye.y
    val tz = label.z - eye.z
    val lengthSq = tx * tx + ty * ty + tz * tz
    if (!(lengthSq > MIN_FACING_SQ)) return null
    val inv = 1f / sqrt(lengthSq)
    val fx = tx * inv
    val fy = ty * inv
    val fz = tz * inv
    // The screen's up, with whatever of it runs along the line of sight taken out.
    val along = cameraUp.x * fx + cameraUp.y * fy + cameraUp.z * fz
    val ux = cameraUp.x - fx * along
    val uy = cameraUp.y - fy * along
    val uz = cameraUp.z - fz * along
    val upLength = sqrt(ux * ux + uy * uy + uz * uz)
    if (!(upLength > MIN_UP)) return null
    return lookTowards(
        eye = label,
        forward = Float3(tx, ty, tz),
        up = Float3(ux / upLength, uy / upLength, uz / upLength),
    ).toQuaternion()
}

/** Why the add button would do nothing right now — said on screen instead of a silent refusal. */
enum class MeasureRefusal {
    /** The camera lost its place: no point can be anchored. */
    NotTracking,

    /** Nothing under the reticle: no surface, depth or feature point. */
    NoSurface,

    /** Raising a wall, and the aim is off it: there is no height to read. */
    OffWall,

    /** ARCore would not anchor the point; a retry usually works. */
    AnchorFailed,
}

/** The [MeasureRefusal] for this frame, `null` when the add button would act. */
fun measureRefusal(tracking: Boolean, phase: MeasurePhase, aimFound: Boolean, heightFound: Boolean): MeasureRefusal? =
    when {
        !tracking -> MeasureRefusal.NotTracking
        phase == MeasurePhase.Raising -> if (heightFound) null else MeasureRefusal.OffWall
        !aimFound -> MeasureRefusal.NoSurface
        else -> null
    }

/** What the add button does next, which its caption says. */
enum class MeasureAction {
    /** The first point of the first measure. */
    Start,

    /** Another point on the measure in progress. */
    Continue,

    /** Back on the first point: closes the shape. */
    Close,

    /** Locks the rising walls. */
    SetHeight,

    /** The last measure is done: the next point starts another one beside it. */
    StartNew,
}

/** The add button's next action on [board], with the reticle on the first point when [closes]. */
fun <P> measureAction(board: MeasureBoard<P>, closes: Boolean): MeasureAction = when {
    board.phase == MeasurePhase.Raising -> MeasureAction.SetHeight
    board.active != null && closes -> MeasureAction.Close
    board.active != null -> MeasureAction.Continue
    board.figures.any { it.mode == board.mode && it.points.isNotEmpty() } -> MeasureAction.StartNew
    else -> MeasureAction.Start
}

private const val WALL_MIN_LENGTH_M = 0.05f
private const val PARALLEL = 1e-4f
private const val SPAN_SLACK_M = 0.1f
private const val BELOW_FLOOR_SLACK_M = 0.05f
private const val ABOVE_TOP_SLACK_M = 0.3f
private const val FLOOR_FIRST_SLACK_M = 0.02f
private const val MIN_FACING_SQ = 1e-8f
private const val MIN_UP = 1e-3f
