package io.github.sceneview.demo.demos.internal

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.lookTowards
import io.github.sceneview.math.Position
import kotlin.math.abs
import kotlin.math.sqrt

/*
 * What Measure (#4732) adds on top of the shared live walls: the reticle telling a wall's foot,
 * labels that read the right way up, why a point is refused, and what the add button does next.
 * Pure, so each rule runs in a JVM test.
 */

/** A point on a recognised wall's foot: [position], on the wall [a]–[b] at the point's level. */
data class MeasureWallFoot(val position: Position, val a: Position, val b: Position)

/**
 * The recognised wall whose foot [point] stands on, within [tolerance] of it in plan, or `null`.
 * Display only: the reticle draws the seam and says "Wall foot", while the point itself stays
 * where ARCore measured and the snapper put it — the live walls are squared and extended, so
 * they never move a point (#4750 review). The nearest wall wins.
 */
fun measureWallFootAt(point: Position, walls: List<LiveWall>, tolerance: Float = WALL_FOOT_NEAR_M): MeasureWallFoot? {
    var best: MeasureWallFoot? = null
    var bestDistance = tolerance
    for (wall in walls) {
        val ex = wall.bx - wall.ax
        val ez = wall.bz - wall.az
        val length = sqrt(ex * ex + ez * ez)
        if (length < WALL_MIN_LENGTH_M) continue
        val along = ((point.x - wall.ax) * ex + (point.z - wall.az) * ez) / length
        if (along < -SPAN_SLACK_M || along > length + SPAN_SLACK_M) continue
        val distance = abs((point.x - wall.ax) * ez - (point.z - wall.az) * ex) / length
        if (distance > bestDistance) continue
        bestDistance = distance
        best = MeasureWallFoot(
            position = point,
            a = Position(wall.ax, point.y, wall.az),
            b = Position(wall.bx, point.y, wall.bz),
        )
    }
    return best
}

/**
 * The wall height shown while raising, from this frame's [read] and the [shown] one: `null` as
 * soon as the aim leaves the wall — no stale height to lock (#4751) — and [shown] kept while the
 * read stays within [epsilon] of it, so the walls don't jitter.
 */
fun measureRaisingHeight(read: Float?, shown: Float?, epsilon: Float): Float? = when {
    read == null -> null
    shown != null && abs(read - shown) <= epsilon -> shown
    else -> read
}

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
private const val SPAN_SLACK_M = 0.1f
private const val WALL_FOOT_NEAR_M = 0.1f
private const val MIN_FACING_SQ = 1e-8f
private const val MIN_UP = 1e-3f
