package io.github.sceneview.demo.demos.internal

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.cross
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.lookAt
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.gesture.zoomedDistanceForPinch
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import io.github.sceneview.verticalFovDegreesForFocalLength
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The third-person camera of the Rerun demo's in-app 3D view (#3950).
 *
 * Two modes, and the switch between them is the whole design:
 *
 * - **Following** (the default, and what a double-tap returns to): the camera frames the session
 *   by itself — it eases towards [home], which the view recomputes as the trail and the planes
 *   grow, and drifts slowly round it. A phone held in one hand, walking a room, gets a view that
 *   keeps up without a finger on it.
 * - **Free**: the first touch takes over. Drag orbits (with inertia), two fingers pan, pinch
 *   zooms. Nothing moves the camera but the user until a double-tap hands it back.
 *
 * Same frame contract as [StudioCameraManipulator]: [update] integrates, [getTransform] converts.
 */
class ArDebugOrbitCamera(
    initialPose: OrbitPose = ArDebugFraming.DEFAULT_POSE,
    /** Slow turntable drift while following. Off in QA, so captures are deterministic. */
    var drift: Boolean = true,
    /**
     * How far above the view's centre the pivot is drawn, as a fraction of the view's height.
     * A full-screen stage with a HUD on top and a filmstrip below has its clear band above the
     * middle: lifting the picture there keeps the room out from under the filmstrip.
     */
    val lift: Float = 0f,
    /** How fast following eases onto [home], per second ([ArDebugFraming.APPROACH_RATE]). */
    val approachRate: Float = ArDebugFraming.APPROACH_RATE,
    /**
     * The orbit a drag across the view's whole width makes, in degrees; `null` keeps the fixed
     * [DEGREES_PER_PIXEL]. 3D Scan's live view gives 180 (#4628): a third of the screen turns it
     * by 60°, the same on every phone, where 0.28°/px turned a Pixel 7a round by 500 px.
     */
    val degreesPerViewWidth: Float? = null,
    /** The elevations a drag can reach, inside [ArDebugFraming.MIN_ELEVATION]…[ArDebugFraming.MAX_ELEVATION]. */
    val elevationRange: ClosedFloatingPointRange<Float> =
        ArDebugFraming.MIN_ELEVATION..ArDebugFraming.MAX_ELEVATION,
    /**
     * The furthest a released drag coasts on, in degrees, `null` for the coast [MAX_SPIN] allows.
     * The fling is kept everywhere; 3D Scan bounds it so a flick cannot spin its cloud away.
     */
    val maxCoastDegrees: Float? = null,
) : CameraGestureDetector.CameraManipulator {

    /**
     * Where a touch that takes the camera over should orbit around, `null` for the pose's own
     * target. 3D Scan follows the phone, but its cloud lies ahead of it: a drag that turned around
     * the phone swung the cloud behind the camera (#4628), so the view sets this to the cloud's
     * centre and [takeOver] moves the orbit's pivot onto it, the picture unchanged.
     */
    var pivot: Position? = null

    /**
     * Told when [following] changes: a touch takes the camera over, a recenter hands it back.
     * The 3D Scan live view shows its Recenter pill off it (#4628).
     */
    var onFollowingChange: ((Boolean) -> Unit)? = null

    /** Current pose. Plain field, not Compose state: it changes every frame. */
    var pose: OrbitPose = initialPose
        private set

    /** Where following mode is heading; set by the view from the content bounds. */
    var home: OrbitPose = initialPose

    /** `true` while the camera frames the session by itself. */
    var following: Boolean = true
        private set(value) {
            if (field == value) return
            field = value
            onFollowingChange?.invoke(value)
        }

    /** Viewport height in pixels, for pan speed and for the view's screen-constant sizes. */
    var viewportHeight: Int = 1
        private set

    /** Viewport width in pixels; with [viewportHeight], the aspect [ArDebugFraming.home] fits. */
    var viewportWidth: Int = 1
        private set

    val aspect: Float get() = viewportWidth.toFloat() / viewportHeight

    /** Set by the view once it has snapped to the first content, so it snaps only once. */
    var hasFramedContent: Boolean = false

    val verticalFovDegrees: Double = verticalFovDegreesForFocalLength(FOCAL_LENGTH_MM)

    private var lastX = 0
    private var lastY = 0
    private var panning = false
    private var grabbing = false
    private var dragAzimuth = 0f
    private var dragElevation = 0f
    private var azimuthVelocity = 0f
    private var elevationVelocity = 0f

    /** Seconds of following, which ramps the drift in instead of starting it at full speed. */
    private var followSeconds = 0f

    /** Where the entrance crane ([playIntro]) started, `null` when none is playing. */
    private var introFrom: OrbitPose? = null
    private var introSeconds = 0f

    /**
     * Holds the crane on its first pose until [releaseIntro]: a scene still hidden behind its
     * loading cover (the GPU warming its shaders) must not spend the entrance where nobody sees it.
     */
    private var introHeld = false

    /** `true` while the entrance crane plays. */
    val introPlaying: Boolean get() = introFrom != null

    /**
     * The map framing: straight down on the room at [ArDebugFraming.MAP_ELEVATION], no drift, so
     * the floor plan reads like a plan. Switching hands the camera back to the automatic framing.
     */
    var overhead: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            recenter()
        }

    /** The elevation the view frames [home] at: the three-quarter view, or the map's. */
    val homeElevation: Float
        get() = if (overhead) ArDebugFraming.MAP_ELEVATION else ArDebugFraming.HOME_ELEVATION

    /**
     * Plays the entrance: the camera jumps to [from] and cranes onto [home] over
     * [ReplayIntro.DURATION_S] — following [home] as it goes, so a view that reframes mid-flight
     * still lands. The first touch cuts it short, like any following. With [held], it waits on its
     * first pose until [releaseIntro].
     */
    fun playIntro(from: OrbitPose, held: Boolean = false) {
        introHeld = held
        following = true
        azimuthVelocity = 0f
        elevationVelocity = 0f
        followSeconds = 0f
        introSeconds = 0f
        introFrom = ArDebugFraming.clamp(from).also { pose = it }
    }

    /** Starts a crane [playIntro] held: the scene is on screen now. */
    fun releaseIntro() {
        introHeld = false
    }

    /** Hands the camera back to the automatic framing — the double-tap and the Recenter button. */
    fun recenter() {
        following = true
        followSeconds = 0f
        azimuthVelocity = 0f
        elevationVelocity = 0f
    }

    /** Puts the camera at [target] at once — the first frame of a view, where easing would lag. */
    fun snapTo(target: OrbitPose) {
        pose = ArDebugFraming.clamp(target)
    }

    override fun setViewport(width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
    }

    override fun getTransform(): Transform {
        val eye = CameraRig.eye(pose)
        // A pedestal move along the camera's own up axis: the picture slides, the orbit does not.
        val drop = liftDrop(pose)
        return Transform(lookAt(eye = eye - drop, target = pose.target - drop, up = Float3(0f, 1f, 0f)))
    }

    override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
        takeOver()
        grabbing = true
        panning = strafe
        lastX = x
        lastY = y
    }

    override fun grabUpdate(x: Int, y: Int) {
        val dx = (x - lastX).toFloat()
        val dy = (y - lastY).toFloat()
        lastX = x
        lastY = y
        if (dx == 0f && dy == 0f) return
        if (panning) {
            pose = pose.copy(target = pannedTarget(dx, dy))
            return
        }
        val perPixel = degreesPerPixel
        val next = clampDrag(
            pose.copy(
                azimuthDegrees = pose.azimuthDegrees - dx * perPixel,
                elevationDegrees = pose.elevationDegrees - dy * perPixel,
            )
        )
        val turned = abs(next.azimuthDegrees - pose.azimuthDegrees) + abs(next.elevationDegrees - pose.elevationDegrees)
        dragAzimuth += next.azimuthDegrees - pose.azimuthDegrees
        dragElevation += next.elevationDegrees - pose.elevationDegrees
        pose = slidTowardsPivot(next, turned)
    }

    /**
     * [next] with its target moved part of the way onto [pivot], in proportion to the [turned]
     * degrees: the touch-down keeps the picture ([repivoted]), and the first
     * [PIVOT_SLIDE_DEGREES] of the drag carry the orbit's centre onto the cloud, so no angle
     * turns it off screen.
     */
    private fun slidTowardsPivot(next: OrbitPose, turned: Float): OrbitPose {
        val centre = pivot ?: return next
        val share = (turned / PIVOT_SLIDE_DEGREES).coerceIn(0f, 1f)
        return next.copy(target = next.target + (centre - next.target) * share)
    }

    override fun grabEnd() {
        grabbing = false
        panning = false
    }

    override fun scrollBegin(x: Int, y: Int, separation: Float) {
        takeOver()
    }

    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
        pose = pose.copy(
            distance = zoomedDistanceForPinch(
                distance = pose.distance,
                prevSeparation = prevSeparation,
                currSeparation = currSeparation,
                minDistance = ArDebugFraming.MIN_DISTANCE,
                maxDistance = ArDebugFraming.MAX_DISTANCE,
            )
        )
    }

    override fun scrollEnd() = Unit

    override fun doubleTapZoom(x: Int, y: Int, zoomIn: Boolean) = recenter()

    override fun update(deltaTime: Float) {
        val dt = deltaTime.takeIf { it.isFinite() && it > 0f && it < 0.25f } ?: return
        if (grabbing && !panning) {
            val spin = maxSpin
            azimuthVelocity = (dragAzimuth / dt).coerceIn(-spin, spin)
            elevationVelocity = (dragElevation / dt).coerceIn(-spin, spin)
        }
        dragAzimuth = 0f
        dragElevation = 0f
        if (grabbing) return

        if (following) {
            val from = introFrom
            if (from != null) {
                if (!introHeld) introSeconds += dt
                val progress = introSeconds / ReplayIntro.DURATION_S
                pose = ArDebugFraming.clamp(ReplayIntro.pose(from, home, progress))
                if (progress >= 1f) introFrom = null
                return
            }
            followSeconds += dt
            if (drift && !overhead) {
                // Ramp in over two seconds so recentering does not lurch into a spin.
                val ramp = (followSeconds / 2f).coerceAtMost(1f)
                home = home.copy(azimuthDegrees = home.azimuthDegrees + DRIFT_DEGREES_PER_SECOND * ramp * dt)
            }
            pose = ArDebugFraming.approach(pose, home, dt, approachRate)
            return
        }

        if (azimuthVelocity != 0f || elevationVelocity != 0f) {
            val next = clampDrag(
                pose.copy(
                    azimuthDegrees = pose.azimuthDegrees + azimuthVelocity * dt,
                    elevationDegrees = pose.elevationDegrees + elevationVelocity * dt,
                )
            )
            // A coast into the pitch limit stops there instead of pressing on it.
            if (next.elevationDegrees != pose.elevationDegrees + elevationVelocity * dt) elevationVelocity = 0f
            val turned = abs(next.azimuthDegrees - pose.azimuthDegrees) +
                abs(next.elevationDegrees - pose.elevationDegrees)
            pose = slidTowardsPivot(next, turned)
            azimuthVelocity = CameraRig.decayInertia(azimuthVelocity, dt)
            elevationVelocity = CameraRig.decayInertia(elevationVelocity, dt)
        }
    }

    private fun takeOver() {
        following = false
        introFrom = null
        azimuthVelocity = 0f
        elevationVelocity = 0f
        // Keep following mode's heading continuous: the home angle becomes the current one, so a
        // recenter later swings back from here rather than from where the drift had got to.
        home = home.copy(azimuthDegrees = pose.azimuthDegrees)
        pivot?.let { pose = repivoted(it) }
    }

    /**
     * The fastest a release coasts at, in degrees per second: [MAX_SPIN], or the speed whose
     * decay ([CameraRig.decayInertia]) covers [maxCoastDegrees] in all.
     */
    val maxSpin: Float
        get() = maxCoastDegrees?.let { minOf(MAX_SPIN, it * -ln(CameraRig.INERTIA_DECAY_PER_SECOND)) } ?: MAX_SPIN

    /** The orbit's degrees per pixel of drag: [degreesPerViewWidth] over the width, or the fixed rate. */
    val degreesPerPixel: Float
        get() = degreesPerViewWidth?.let { it / viewportWidth } ?: DEGREES_PER_PIXEL

    private fun clampDrag(next: OrbitPose): OrbitPose = ArDebugFraming.clamp(next).let {
        it.copy(elevationDegrees = it.elevationDegrees.coerceIn(elevationRange))
    }

    /**
     * The pose that draws exactly the picture [pose] draws, orbiting around the point of the view
     * ray nearest [point] instead of around its own target: same eye, same direction, the target
     * slid along the ray to [point]'s depth (and up by the [lift] that depth draws it with).
     */
    fun repivoted(point: Position): OrbitPose {
        val forward = normalize(pose.target - CameraRig.eye(pose))
        val drawnEye = CameraRig.eye(pose) - liftDrop(pose)
        val depth = dot(point - drawnEye, forward).coerceIn(ArDebugFraming.MIN_DISTANCE, ArDebugFraming.MAX_DISTANCE)
        val moved = pose.copy(distance = depth)
        return moved.copy(target = drawnEye + forward * depth + liftDrop(moved))
    }

    /** How far [lift] moves the drawn camera of [at] along its own up axis. */
    private fun liftDrop(at: OrbitPose): Float3 {
        val metresPerPixel = CameraRig.worldPerPixel(at.distance, verticalFovDegrees, viewportHeight)
        if (lift == 0f || metresPerPixel <= 0f) return Float3()
        val forward = normalize(at.target - CameraRig.eye(at))
        val up = cross(normalize(cross(forward, Float3(0f, 1f, 0f))), forward)
        return up * (lift * viewportHeight * metresPerPixel)
    }

    private fun pannedTarget(dx: Float, dy: Float): Position {
        val metresPerPixel = CameraRig.worldPerPixel(pose.distance, verticalFovDegrees, viewportHeight)
        if (metresPerPixel <= 0f) return pose.target
        val forward = normalize(pose.target - CameraRig.eye(pose))
        val right = normalize(cross(forward, Float3(0f, 1f, 0f)))
        val up = cross(right, forward)
        return pose.target - right * (dx * metresPerPixel) - up * (dy * metresPerPixel)
    }

    companion object {
        /** Same lens as every `SceneView` camera by default. */
        const val FOCAL_LENGTH_MM = 28.0
        const val DEGREES_PER_PIXEL = 0.28f
        const val MAX_SPIN = 360f

        /** The degrees of drag over which the orbit's centre slides onto [pivot]. */
        const val PIVOT_SLIDE_DEGREES = 30f
        const val DRIFT_DEGREES_PER_SECOND = 6f
    }
}

/** Pure framing maths of [ArDebugOrbitCamera]: where "home" is, and how the camera gets there. */
object ArDebugFraming {
    const val MIN_DISTANCE = 0.25f
    const val MAX_DISTANCE = 40f
    const val MIN_ELEVATION = -8f
    const val MAX_ELEVATION = 88f

    /** The three-quarter view from above-behind the Rerun viewer opens a spatial view with. */
    const val HOME_ELEVATION = 32f
    const val HOME_AZIMUTH = 35f

    /** The map's near-vertical view: a floor plan, with just enough tilt to keep depth. */
    const val MAP_ELEVATION = 84f

    /**
     * The fitting distance of the content's bounding sphere, as a multiple of it. Under 1: a room
     * is a flat box, and the sphere around it is much rounder than what the lens actually sees —
     * at 1.15 the room filled barely half of a phone's width.
     */
    const val HOME_MARGIN = 0.92f

    /**
     * The tighter margin of a bundled recording: its bounds are known up front and hold no
     * growth to leave room for, so the room fills the stage between the HUD and the filmstrip.
     * 0.66 ran a recorded room to 8 px of the screen's right edge on a Pixel 7a: 0.72 keeps it
     * inside the 16 dp the chrome keeps from the edges.
     */
    const val REPLAY_MARGIN = 0.72f

    /** A room-sized framing for an empty session. */
    val DEFAULT_POSE = OrbitPose(
        target = Position(0f, -0.4f, -0.5f),
        azimuthDegrees = HOME_AZIMUTH,
        elevationDegrees = HOME_ELEVATION,
        distance = 3.2f,
    )

    /** Rate of the exponential approach to home, per second. ~95 % in one second. */
    const val APPROACH_RATE = 3f

    fun clamp(pose: OrbitPose): OrbitPose = pose.copy(
        elevationDegrees = pose.elevationDegrees.coerceIn(MIN_ELEVATION, MAX_ELEVATION),
        distance = pose.distance.takeIf { it.isFinite() }?.coerceIn(MIN_DISTANCE, MAX_DISTANCE)
            ?: DEFAULT_POSE.distance,
    )

    /**
     * The pose that frames [bounds] (`[minX, minY, minZ, maxX, maxY, maxZ]`, or null for an empty
     * session) at [azimuthDegrees], through a lens of [verticalFovDegrees] on a view of
     * [aspect] (width / height). The whole bounding sphere fits the **narrower** field of view,
     * so a tall phone and a small PiP both see everything.
     */
    fun home(
        bounds: FloatArray?,
        azimuthDegrees: Float,
        verticalFovDegrees: Double,
        aspect: Float,
        elevationDegrees: Float = HOME_ELEVATION,
        margin: Float = HOME_MARGIN,
    ): OrbitPose {
        if (bounds == null) {
            return DEFAULT_POSE.copy(azimuthDegrees = azimuthDegrees, elevationDegrees = elevationDegrees)
        }
        val cx = (bounds[0] + bounds[3]) / 2f
        val cy = (bounds[1] + bounds[4]) / 2f
        val cz = (bounds[2] + bounds[5]) / 2f
        val ex = bounds[3] - bounds[0]
        val ey = bounds[4] - bounds[1]
        val ez = bounds[5] - bounds[2]
        // A small session (the phone barely moved) still gets a room-sized view: 0.8 m radius.
        val radius = max(0.8f, sqrt(ex * ex + ey * ey + ez * ez) / 2f)
        val halfVertical = Math.toRadians(verticalFovDegrees / 2.0)
        val halfHorizontal = kotlin.math.atan(kotlin.math.tan(halfVertical) * aspect.coerceIn(0.2f, 5f))
        val halfFov = minOf(halfVertical, halfHorizontal)
        val distance = (radius / sin(halfFov)).toFloat() * margin
        return clamp(
            OrbitPose(
                target = Position(cx, cy, cz),
                azimuthDegrees = azimuthDegrees,
                elevationDegrees = elevationDegrees,
                distance = distance,
            )
        )
    }

    /**
     * The closest pose at [azimuthDegrees] and [elevationDegrees] that shows the whole of [bounds]
     * (`[minX, minY, minZ, maxX, maxY, maxZ]`) inside what [insets] leave free of a view of
     * [aspect] (width / height), through a lens of [verticalFovDegrees] (#4492).
     *
     * Where [home] fits a sphere around the content and shortens the result by a margin, this
     * projects the box's eight corners: a room is a flat box seen from above at an angle, and the
     * sphere says nothing of how wide it lands on a tall screen — a squarish room opened with a
     * side past the screen's edge. The distance is the least one at which every corner projects
     * inside the free area, on both axes; the target then slides in the picture plane so the
     * projected box is centred there, since perspective draws the near half of a box larger than
     * the far one and chrome is rarely the same height top and bottom.
     */
    fun fit(
        bounds: FloatArray,
        azimuthDegrees: Float,
        elevationDegrees: Float,
        verticalFovDegrees: Double,
        aspect: Float,
        insets: FramingInsets = FramingInsets.None,
    ): OrbitPose {
        val elevation = elevationDegrees.coerceIn(MIN_ELEVATION, MAX_ELEVATION)
        val azimuth = Math.toRadians(azimuthDegrees.toDouble())
        val tilt = Math.toRadians(elevation.toDouble())
        val sinA = sin(azimuth).toFloat()
        val cosA = cos(azimuth).toFloat()
        val sinE = sin(tilt).toFloat()
        val cosE = cos(tilt).toFloat()
        // The camera's own axes at these angles, as CameraRig.eye and lookAt make them.
        val right = Float3(cosA, 0f, -sinA)
        val up = Float3(-sinA * sinE, cosE, -cosA * sinE)
        val forward = Float3(-cosE * sinA, -sinE, -cosE * cosA)
        val centre = Position(
            (bounds[0] + bounds[3]) / 2f,
            (bounds[1] + bounds[4]) / 2f,
            (bounds[2] + bounds[5]) / 2f,
        )
        // Each corner from the box's centre, along those axes: across, up the picture, and away.
        val across = FloatArray(BOX_CORNERS)
        val upward = FloatArray(BOX_CORNERS)
        val away = FloatArray(BOX_CORNERS)
        for (corner in 0 until BOX_CORNERS) {
            val offset = Float3(
                bounds[if (corner and 1 == 0) 0 else 3],
                bounds[if (corner and 2 == 0) 1 else 4],
                bounds[if (corner and 4 == 0) 2 else 5],
            ) - centre
            across[corner] = dot(offset, right)
            upward[corner] = dot(offset, up)
            away[corner] = dot(offset, forward)
        }
        val tanVertical = tan(Math.toRadians(verticalFovDegrees / 2.0)).toFloat()
        val tanHorizontal = tanVertical * aspect.coerceIn(0.2f, 5f)
        val free = insets.bounded()
        val horizontal = FitAxis(across, away, tanHorizontal, -1f + 2f * free.left, 1f - 2f * free.right)
        val vertical = FitAxis(upward, away, tanVertical, -1f + 2f * free.bottom, 1f - 2f * free.top)
        // Never closer than the nearest corner: every corner stays in front of the lens.
        val nearest = MIN_DISTANCE - away.min()
        val framed = clamp(
            OrbitPose(
                target = centre,
                azimuthDegrees = azimuthDegrees,
                elevationDegrees = elevation,
                distance = maxOf(nearest, horizontal.distance(), vertical.distance()),
            )
        )
        return framed.copy(
            target = centre + right * horizontal.slide(framed.distance) + up * vertical.slide(framed.distance),
        )
    }

    /**
     * One screen axis of [fit]: each corner's offset [along] it and its depth [away] past the
     * target, the tangent of half the field of view on this axis ([tanHalf]), and the free band
     * in normalised device coordinates, from [low] to [high] (-1 and 1 are the view's edges).
     */
    private class FitAxis(
        private val along: FloatArray,
        private val away: FloatArray,
        private val tanHalf: Float,
        private val low: Float,
        private val high: Float,
    ) {
        /**
         * The least distance at which the corners fit the band, wherever the target slides to.
         * Corner `i` stays under [high] when the slide is at least `along[i] - high·tan·(d + away[i])`
         * and corner `j` over [low] when it is at most `along[j] - low·tan·(d + away[j])`: a slide
         * exists once every such floor is under every such ceiling, which is linear in `d`.
         */
        fun distance(): Float {
            var distance = 0f
            for (i in along.indices) for (j in along.indices) {
                val needed = (along[i] - along[j] + tanHalf * (low * away[j] - high * away[i])) /
                    ((high - low) * tanHalf)
                distance = max(distance, needed)
            }
            return distance
        }

        /**
         * How far the target moves along this axis to centre the corners in the band at [distance].
         * On the axis that set the distance there is one slide that fits; on the other there is a
         * range, and the one that puts the corners' two extremes the same way from the band's
         * edges is found by halving it — both extremes move the same way as the target slides.
         */
        fun slide(distance: Float): Float {
            var floor = -Float.MAX_VALUE
            var ceiling = Float.MAX_VALUE
            for (i in along.indices) {
                floor = max(floor, along[i] - high * tanHalf * (distance + away[i]))
                ceiling = minOf(ceiling, along[i] - low * tanHalf * (distance + away[i]))
            }
            if (floor >= ceiling) return (floor + ceiling) / 2f
            repeat(CENTRING_STEPS) {
                val middle = (floor + ceiling) / 2f
                var least = Float.MAX_VALUE
                var most = -Float.MAX_VALUE
                for (i in along.indices) {
                    val drawn = (along[i] - middle) / (tanHalf * (distance + away[i]))
                    least = minOf(least, drawn)
                    most = max(most, drawn)
                }
                if (least + most > low + high) floor = middle else ceiling = middle
            }
            return (floor + ceiling) / 2f
        }
    }

    private const val BOX_CORNERS = 8
    private const val CENTRING_STEPS = 24

    /**
     * One frame of the ease from [pose] to [home]: an exponential approach, which is frame-rate
     * independent and retargets smoothly — home moves every time the trail grows.
     */
    fun approach(
        pose: OrbitPose,
        home: OrbitPose,
        deltaSeconds: Float,
        rate: Float = APPROACH_RATE,
    ): OrbitPose {
        val fraction = 1f - exp(-rate * deltaSeconds)
        val next = CameraRig.lerp(pose, home, fraction)
        // Snap when close, so an idle view reaches a fixed point (render-on-demand can rest).
        val angleSettled = abs(CameraRig.shortestDelta(next.azimuthDegrees, home.azimuthDegrees)) < 0.01f &&
            abs(next.elevationDegrees - home.elevationDegrees) < 0.01f
        val targetOffset = abs(next.target.x - home.target.x) + abs(next.target.y - home.target.y) +
            abs(next.target.z - home.target.z)
        val reachSettled = abs(next.distance - home.distance) < 1e-4f && targetOffset < 1e-4f
        return if (angleSettled && reachSettled) home else next
    }
}

/**
 * What covers a view, as fractions of its width ([left], [right]) and of its height ([top],
 * [bottom]) measured inwards from each edge: the chrome [ArDebugFraming.fit] keeps the content
 * clear of.
 */
data class FramingInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {
    /**
     * These insets, never negative, and leaving at least [MIN_FREE] of each axis free: two bands
     * that add up to more than that are cut back in proportion.
     */
    fun bounded(): FramingInsets {
        val (boundedLeft, boundedRight) = boundedPair(left, right)
        val (boundedTop, boundedBottom) = boundedPair(top, bottom)
        return FramingInsets(boundedLeft, boundedTop, boundedRight, boundedBottom)
    }

    private fun boundedPair(first: Float, second: Float): Pair<Float, Float> {
        val a = first.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val b = second.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val covered = a + b
        val most = 1f - MIN_FREE
        return if (covered <= most) a to b else a * most / covered to b * most / covered
    }

    companion object {
        val None = FramingInsets()

        /** The least share of a view's width or height a framing is given. */
        const val MIN_FREE = 0.2f
    }
}
