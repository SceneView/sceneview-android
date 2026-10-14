package io.github.sceneview.demo.ui.home

import io.github.sceneview.animation.LightCues
import io.github.sceneview.utils.GlRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

/**
 * The home hero's night circuit, as pure functions of time.
 *
 * Everything the stage writes to Filament per frame comes out of here: where the camera is
 * ([nightCameraPose]) and how bright every lamp of the circuit is ([nightCueLevels]). Both
 * read **film time**: the seconds since the scene's first textured frame, counted by a
 * [HeroClock] that only advances on presented frames. A scroll, a page swipe, a background
 * trip or a parked loop leave it exactly where it was, so coming back resumes the film from
 * the same frame (#3949).
 *
 * The camera and the start sequence follow the website's scene frame for frame
 * (`circuit.js`, the same shots and the same cue times), so the two surfaces tell one story.
 *
 * See `HomeHeroFlightTest`.
 */

/**
 * Rendering tier: what the stage turns on. A device starts at [forDevice]'s pick and steps
 * down one rung at a time while [HeroFrameBudget] says the frames are late; it never climbs
 * back, the way the website's scene does it.
 */
internal enum class HeroTier(
    /** Screen-space reflections: the wet asphalt mirrors the car, the gantry and the neon. */
    val reflections: Boolean,
    /** The key light's shadow under the car. */
    val shadows: Boolean,
    /** Bloom: the neon, the start lights and the headlights bleed into the night. */
    val bloom: Boolean,
    /** Filament's dynamic resolution: fewer pixels when a frame runs late. */
    val dynamicResolution: Boolean,
    /** The venue's two big point lights (garage, start gantry), on top of the car's own. */
    val venueLights: Boolean,
    /** SceneView's `RenderQuality.Performance` preset, rather than `Default`. */
    val performancePreset: Boolean,
) {
    Light(false, false, false, true, false, true),
    Standard(false, false, true, true, true, false),
    High(false, true, true, false, true, false),
    Cinematic(true, true, true, false, true, false),
    ;

    /** The next rung down, or null at the bottom. */
    val lower: HeroTier? get() = entries.getOrNull(ordinal - 1)

    companion object {
        /**
         * `ActivityManager.isLowRamDevice` starts at the bottom; everything else at the top,
         * or at the rung this install already [settled] on, whichever is lower.
         */
        fun forDevice(lowRam: Boolean, settled: HeroTier? = null): HeroTier {
            val pick = if (lowRam) Light else Cinematic
            return if (settled != null && settled < pick) settled else pick
        }
    }
}

/**
 * Says when the stage cannot hold its frame rate. Each measure starts with a settle (shader
 * compiles, texture uploads) of [settleFrames] presented frames or [settleSeconds], whichever
 * ends first, then times a window of up to [windowFrames] frames: when its frames run slower
 * than [minFps], it returns true once, and the next measure starts with a fresh settle, so
 * the tier just dropped to gets its own compiles before it is judged.
 *
 * Both ends are bounded in time as well as in frames: a window of at least
 * [MIN_JUDGED_FRAMES] closes as soon as it has run longer than [windowFrames] frames at
 * [minFps] would. A GPU that draws a rung at 2 fps (the Pixel 4a's Adreno 618 with shadows
 * or screen-space reflections) leaves it in seconds, not the minute and a half 105 frames
 * would take. A window is judged on its fastest three quarters: a rung's late compiles and
 * uploads are a few long frames, a rung the GPU cannot draw is all of them. A window under
 * a third of the floor sets [farBelow]: one rung down rarely wins a factor of three back.
 *
 * The floor is 30 fps, not 60: the ladder escapes the rungs a GPU cannot draw, it does not
 * trade bloom for the last frames (the Pixel 4a holds 40 to 50 on [HeroTier.Standard] and
 * no more on Light).
 */
internal class HeroFrameBudget(
    private val settleFrames: Int = 45,
    private val windowFrames: Int = 60,
    private val minFps: Float = 30f,
    private val settleSeconds: Double = 1.0,
) {
    private var settled = 0
    private var settleElapsed = 0.0
    private val intervals = DoubleArray(windowFrames)
    private var frames = 0
    private var elapsed = 0.0
    private var previousNanos: Long? = null

    /** Whether the last window flagged ran under a third of [minFps]. */
    var farBelow: Boolean = false
        private set

    /** Forget the window under way: the next frame starts a fresh settle. */
    fun reset() {
        settled = 0
        settleElapsed = 0.0
        frames = 0
        elapsed = 0.0
        previousNanos = null
    }

    /** One presented frame at [nanos]; true when the window it closes was too slow. */
    fun frame(nanos: Long): Boolean {
        val previous = previousNanos
        previousNanos = nanos
        if (previous == null) return false
        val delta = (nanos - previous) / 1_000_000_000.0
        if (settled < settleFrames && settleElapsed < settleSeconds) {
            settled++
            settleElapsed += delta
            return false
        }
        intervals[frames++] = delta
        elapsed += delta
        val overrun = elapsed > windowFrames / minFps && frames >= MIN_JUDGED_FRAMES
        if (!overrun && frames < windowFrames) return false
        intervals.sort(0, frames)
        val kept = (frames * 3 + 3) / 4
        var sum = 0.0
        for (i in 0 until kept) sum += intervals[i]
        val frameTime = sum / kept
        val slow = frameTime > 1.0 / minFps
        frames = 0
        elapsed = 0.0
        if (slow) {
            farBelow = frameTime > 3.0 / minFps
            settled = 0
            settleElapsed = 0.0
        }
        return slow
    }

    private companion object {
        /** The fewest frames a window cut short is judged on: one stall is not a verdict. */
        const val MIN_JUDGED_FRAMES = 4
    }
}
/**
 * What fills the hero stage: the flight, or its picture.
 *
 * Decided once, from the GL implementation, *before* an engine exists. Filament reports a
 * shader program it cannot link as a panic that aborts the process, so on a renderer the
 * flight does not survive, the only safe handling is not to start it (#4411).
 */
internal enum class HeroSurface {
    /** The flight, rendered by SceneView. */
    Live,

    /** One frame of the flight, as a bundled picture: no engine is created. */
    Still;

    companion object {
        /**
         * [Still] on the legacy guest SwiftShader ([GlRenderer.isLegacySwiftShader]), where
         * the app aborted in Filament a second after launch on every start, and when
         * [renderer] is null — no OpenGL ES 3 context at all, so no engine either. [Live]
         * everywhere else, hardware or not: the emulator's own SwiftShader modes fly it.
         */
        fun forRenderer(renderer: GlRenderer?): HeroSurface =
            if (renderer == null || renderer.isLegacySwiftShader) Still else Live
    }
}

/**
 * A clock that only ticks while frames are presented and motion is wanted.
 *
 * [frame] returns the flight time to draw. The first frame after a pause is given no
 * delta at all (`previousNanos` is null), and no single frame is ever handed more than
 * [MAX_FRAME_SECONDS]: a stall in the compositor must not jump the film.
 */
internal class HeroClock(initialSeconds: Double = 0.0) {
    var seconds: Double = initialSeconds
        private set
    private var previousNanos: Long? = null

    fun pause() {
        previousNanos = null
    }

    fun frame(nanos: Long, moving: Boolean): Double {
        if (!moving) {
            pause()
            return seconds
        }
        previousNanos?.let { previous ->
            seconds += ((nanos - previous) / 1_000_000_000.0).coerceIn(0.0, MAX_FRAME_SECONDS)
        }
        previousNanos = nanos
        return seconds
    }

    companion object {
        const val MAX_FRAME_SECONDS = 0.1
    }
}

/**
 * One camera shot of the film: an orbit around the car ([NightFilm.CAR_X]…) at [t] seconds.
 * [angle] 0 puts the eye ahead of the car's nose (+Z); [height] is the eye's absolute world
 * Y; [fov] is vertical, degrees; [lift] raises the point looked at above the car's centre.
 */
internal data class NightShot(
    val t: Float,
    val angle: Float,
    val height: Float,
    val radius: Float,
    val fov: Float,
    val lift: Float,
)

/** The film's fixed numbers: the car, the shots and the loop. World units are metres. */
internal object NightFilm {
    /** The point the camera orbits: the car's centre, nose towards +Z. */
    const val CAR_X = 0f
    const val CAR_Y = 0.62f
    const val CAR_Z = 0.25f

    /** One lap of the camera; the fifth shot of the website is the first one, one turn on. */
    const val LOOP_SECONDS = 14f

    /** The website's shots (`circuit.js` SHOTS), but the last: the loop closes on the first. */
    val shots: List<NightShot> = listOf(
        NightShot(t = 0f, angle = 0.62f, height = 0.92f, radius = 6.5f, fov = 30f, lift = 0f),
        NightShot(t = 3.6f, angle = 1.5f, height = 0.5f, radius = 7.3f, fov = 28f, lift = -0.04f),
        NightShot(t = 7.2f, angle = 2.8f, height = 1.15f, radius = 7.1f, fov = 30f, lift = 0.06f),
        NightShot(t = 10.6f, angle = 4.75f, height = 0.62f, radius = 7.2f, fov = 29f, lift = 0f),
    )

    /** Extra turn, radians, once the band has scrolled all the way out. */
    const val SCROLL_TURN = 0.9f

    /** Fraction of the distance the scroll pushes in by, at the end of its travel. */
    const val SCROLL_PUSH = 0.22f

    /** How far the eye sinks towards the asphalt over the scroll, metres. */
    const val SCROLL_SINK = 0.25f

    /** The eye never goes below this height: the wet asphalt is at 0. */
    const val MIN_EYE_HEIGHT = 0.25f
}

private const val TWO_PI = (2.0 * PI).toFloat()

/** Shot [k] of the endless loop: index modulo the shots, one turn and one lap per wrap. */
private fun loopShot(k: Int): NightShot {
    val n = NightFilm.shots.size
    val lap = floor(k.toFloat() / n).toInt()
    val shot = NightFilm.shots[k - lap * n]
    return if (lap == 0) {
        shot
    } else {
        shot.copy(t = shot.t + lap * NightFilm.LOOP_SECONDS, angle = shot.angle + lap * TWO_PI)
    }
}

/**
 * The camera shot at [seconds] of film: the website's Catmull-Rom between shots (cubic
 * Hermite, tangents from the neighbours, scaled for uneven spacing), made periodic so the
 * fourteenth second flows into the first with no seam.
 */
internal fun nightShotAt(seconds: Float): NightShot {
    val loop = NightFilm.LOOP_SECONDS
    val lap = floor(seconds / loop)
    val local = seconds - lap * loop
    val shots = NightFilm.shots
    var i = 0
    while (i < shots.size - 1 && local >= shots[i + 1].t) i++
    val a = loopShot(i)
    val b = loopShot(i + 1)
    val p = loopShot(i - 1)
    val q = loopShot(i + 2)
    val span = b.t - a.t
    val u = ((local - a.t) / span).coerceIn(0f, 1f)
    val u2 = u * u
    val u3 = u2 * u
    val h00 = 2f * u3 - 3f * u2 + 1f
    val h10 = u3 - 2f * u2 + u
    val h01 = -2f * u3 + 3f * u2
    val h11 = u3 - u2
    fun hermite(pick: (NightShot) -> Float): Float {
        val m0 = (pick(b) - pick(p)) / (b.t - p.t) * span
        val m1 = (pick(q) - pick(a)) / (q.t - a.t) * span
        return h00 * pick(a) + h10 * m0 + h01 * pick(b) + h11 * m1
    }
    return NightShot(
        t = seconds,
        angle = hermite { it.angle },
        height = hermite { it.height },
        radius = hermite { it.radius },
        fov = hermite { it.fov },
        lift = hermite { it.lift },
    )
}

/** One frame of the camera. Positions are metres, the field of view vertical degrees. */
internal data class NightCameraPose(
    val eyeX: Float,
    val eyeY: Float,
    val eyeZ: Float,
    val targetX: Float,
    val targetY: Float,
    val targetZ: Float,
    val verticalFovDegrees: Float,
) {
    /** The lens that gives [verticalFovDegrees] on Filament's 24 mm-high sensor. */
    val focalLengthMillimetres: Double
        get() = 12.0 / tan(Math.toRadians(verticalFovDegrees / 2.0))
}

/**
 * The camera at [film] seconds.
 *
 * @param scroll  How far the featured band has scrolled out, 0 (at rest) → 1 (gone): the
 *                camera turns a little further round the car, pushes in and sinks, so the
 *                scroll itself reads as a camera move.
 * @param aspect  Width / height of the area the car is framed in. A narrow one backs the
 *                camera off, as the website does: `clamp(1.55 / aspect, 1.2, 1.9)`.
 * @param tiltX   Smoothed device tilt, −1 → 1, left → right: a few degrees round the car.
 * @param tiltY   Smoothed device tilt, −1 → 1, back → forward: a few centimetres up or down.
 * @param motion  False under reduced motion: the first shot, held, whatever the scroll.
 */
internal fun nightCameraPose(
    film: Float,
    scroll: Float = 0f,
    aspect: Float = 1f,
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    motion: Boolean = true,
): NightCameraPose {
    val shot = nightShotAt(if (motion) film else 0f)
    val s = if (motion) scroll.coerceIn(0f, 1f) else 0f
    val eased = s * s * (3f - 2f * s)
    val zoom = (1.55f / aspect.coerceAtLeast(0.1f)).coerceIn(1.2f, 1.9f)
    val angle = shot.angle + NightFilm.SCROLL_TURN * eased + tiltX * 0.08f
    val radius = shot.radius * zoom * (1f - NightFilm.SCROLL_PUSH * eased)
    val height = max(
        NightFilm.MIN_EYE_HEIGHT,
        shot.height - NightFilm.SCROLL_SINK * eased + tiltY * 0.08f,
    )
    return NightCameraPose(
        eyeX = NightFilm.CAR_X + sin(angle) * radius,
        eyeY = height,
        eyeZ = NightFilm.CAR_Z + cos(angle) * radius,
        targetX = NightFilm.CAR_X,
        targetY = NightFilm.CAR_Y + shot.lift,
        targetZ = NightFilm.CAR_Z,
        verticalFovDegrees = shot.fov,
    )
}

/**
 * When each lamp of the circuit does what, in film seconds. The start procedure (A2–A5)
 * follows the website's `circuit.js` to the frame and repeats on every
 * [NightFilm.LOOP_SECONDS] lap of the camera; the venue's power-up and the car's wake-up
 * (A0, A0b) are this app's own and play once. Names follow the sound cue sheet
 * (`reperes-son.md`) and the website's porting notes.
 */
internal object NightCues {
    /** A0 — the venue powers up, one group after the other: (start, duration, seed). */
    val GARAGE = Triple(0.0f, 0.7f, 11)
    val WINDOWS = Triple(0.15f, 0.9f, 12)
    val LED_WARM = Triple(0.2f, 1.6f, 20)
    val CITY = Triple(0.3f, 1.0f, 13)
    val SEATS = Triple(0.45f, 0.6f, 14)
    val LED = Triple(0.6f, 0.6f, 15)
    val SCREENS = Triple(0.9f, 0.9f, 16)
    val FLOOD = Triple(1.2f, 0.5f, 17)

    /** A0b — the car wakes: two hazard blinks, then the headlights fade in. */
    const val WAKE = 0.5f
    const val WAKE_BLINK_PERIOD = 0.45f
    const val WAKE_BLINKS = 2
    const val HEADLIGHTS_ON = 1.0f
    const val HEADLIGHTS_FADE = 0.4f

    /**
     * A2 — the start gantry. Lit as the lap opens (the website's poster), dark at
     * [REDS_OFF], then one column a second from [REDS_ON], each rising over [COLUMN_RISE].
     */
    const val REDS_OFF = 0.9f
    const val REDS_ON = 1.6f
    const val COLUMN_STEP = 1.0f
    const val COLUMN_RISE = 0.15f
    const val COLUMNS = 5

    /** A3 — lights out: every column at once. */
    const val LIGHTS_OUT = 7.0f

    /**
     * A2' — the gantry relights in a sweep before the lap closes, so the next lap opens on
     * the same lit gantry with no jump.
     */
    const val REDS_BACK = 12.4f
    const val REDS_BACK_STEP = 0.3f
    const val REDS_BACK_RISE = 0.2f

    /** A4 — two high-beam pulses at lights out, each a [FLASH_LENGTH] bump. */
    const val FLASH_FIRST = 7.0f
    const val FLASH_SECOND = 7.45f
    const val FLASH_LENGTH = 0.38f

    /** At a pulse's peak: the spots (1e7 → 2.4e7), the lamp glass, the cones in the haze. */
    const val FLASH_LIGHT_PEAK = 2.4f
    const val FLASH_LAMP_PEAK = 3.2f
    const val FLASH_BEAM_PEAK = 3.5f

    /** A5 — the brakes, held while the camera passes over the tail. */
    const val BRAKE_ON = 7.0f
    const val BRAKE_RISE = 0.35f
    const val BRAKE_OFF = 9.6f
    const val BRAKE_FALL = 0.6f

    /** Pressed: the tail light (6e5 → 2.6e6) and the lamp glass. */
    const val BRAKE_LIGHT_PEAK = 2.6e6f / 6e5f
    const val BRAKE_LAMP_PEAK = 5f

    /** A6 — the tower's beacon: on 0.12 s every 1.2 s, down to 0.12 of its glow between. */
    const val BEACON_PERIOD = 1.2f
    const val BEACON_ON = 0.12f
    const val BEACON_FLOOR = 0.12f

    /** A7 — the pit neon struggles a second time on every lap. */
    const val NEON_STRUGGLE = 9.0f
    const val NEON_STRUGGLE_LENGTH = 0.5f

    /** A6b — the SCENEVIEW screens stutter once in every this many seconds. */
    const val SCREEN_GLITCH_EVERY = 7f
}

/**
 * How bright every lamp is, each a multiplier of the GLB's own emissive strength (or of a
 * light's base intensity): 1 is the scene as authored, 0 dark. The car's lamps go above 1
 * when they flash or brake.
 */
internal class NightCueLevels {
    var garage = 1f
    var windows = 1f
    var city = 1f
    var seats = 1f
    var led = 1f
    var ledWarm = 1f

    /** `Light board` and `Light banner`, the two SCENEVIEW screens. */
    var screens = 1f

    /** The floodlights — and the key light and the image-based light that stand for them. */
    var flood = 1f
    var beacon = 1f

    /** The five start columns, left to right. */
    val startColumns = FloatArray(NightCues.COLUMNS) { 1f }

    /** The red point light over the gantry: the share of columns lit. */
    var startLight = 1f

    /** The two headlight spots. */
    var headlights = 1f

    /** The headlight glass (`Headlight` material). */
    var headlightLamps = 1f

    /** The headlight cones in the haze (`Beam` material's opacity). */
    var beams = 1f

    /** The indicators (`Signallight`). */
    var signals = 1f

    /** The tail point light. */
    var brake = 1f

    /** The brake lamp glass (`Brakelight`). */
    var brakeLamps = 1f
}

/** The website's `ramp`: a smoothstep from 0 at [from] to 1 at [from] + [length]. */
private fun ramp(t: Float, from: Float, length: Float): Float {
    val x = ((t - from) / length).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** The website's `bump`: up and down once, a half sine over [length] seconds from [at]. */
private fun bump(t: Float, at: Float, length: Float): Float {
    val u = (t - at) / length
    return if (u <= 0f || u >= 1f) 0f else sin(u * PI.toFloat())
}

/** Seconds into the current lap of the film. */
internal fun nightLap(film: Float): Float =
    film - floor(film / NightFilm.LOOP_SECONDS) * NightFilm.LOOP_SECONDS

/** The warm neon's flicker once a lap, a different stutter each lap; 1 outside the window. */
private fun neonStruggle(film: Float, lap: Float): Float {
    val end = NightCues.NEON_STRUGGLE + NightCues.NEON_STRUGGLE_LENGTH
    if (lap < NightCues.NEON_STRUGGLE || lap >= end) return 1f
    val lapIndex = floor(film / NightFilm.LOOP_SECONDS).toInt()
    return LightCues.ignition(lap, NightCues.NEON_STRUGGLE, NightCues.NEON_STRUGGLE_LENGTH, seed = 40 + lapIndex)
        .coerceAtLeast(0.15f)
}

/**
 * Every lamp at [film] seconds, written into [into] (one per stage, so a frame allocates
 * nothing). Under reduced motion ([motion] false) every lamp is simply on, as authored.
 */
internal fun nightCueLevels(
    film: Float,
    motion: Boolean = true,
    into: NightCueLevels = NightCueLevels(),
): NightCueLevels = into.apply {
    if (!motion) {
        garage = 1f; windows = 1f; city = 1f; seats = 1f; led = 1f; ledWarm = 1f
        screens = 1f; flood = 1f; beacon = 1f
        startColumns.fill(1f)
        startLight = 1f; headlights = 1f; headlightLamps = 1f; beams = 1f; signals = 1f
        brake = 1f; brakeLamps = 1f
        return@apply
    }
    val t = film
    val lap = nightLap(t)
    fun ignite(cue: Triple<Float, Float, Int>) = LightCues.ignition(t, cue.first, cue.second, cue.third)
    garage = ignite(NightCues.GARAGE)
    windows = ignite(NightCues.WINDOWS)
    city = ignite(NightCues.CITY)
    seats = ignite(NightCues.SEATS)
    led = ignite(NightCues.LED)
    ledWarm = ignite(NightCues.LED_WARM) * neonStruggle(t, lap)
    screens = ignite(NightCues.SCREENS) *
        LightCues.glitch(t, everySeconds = NightCues.SCREEN_GLITCH_EVERY, seed = NightCues.SCREENS.third)
    flood = ignite(NightCues.FLOOD)
    beacon = NightCues.BEACON_FLOOR + (1f - NightCues.BEACON_FLOOR) * LightCues.blink(
        t,
        periodSeconds = NightCues.BEACON_PERIOD,
        duty = NightCues.BEACON_ON / NightCues.BEACON_PERIOD,
        softness = 0.02f,
    )

    // The start procedure runs on the camera's lap.
    var lit = 0f
    for (i in 0 until NightCues.COLUMNS) {
        val column = when {
            lap < NightCues.REDS_OFF -> 1f
            lap < NightCues.LIGHTS_OUT ->
                ramp(lap, NightCues.REDS_ON + i * NightCues.COLUMN_STEP, NightCues.COLUMN_RISE)
            lap < NightCues.REDS_BACK -> 0f
            else -> ramp(lap, NightCues.REDS_BACK + i * NightCues.REDS_BACK_STEP, NightCues.REDS_BACK_RISE)
        }
        startColumns[i] = column
        lit += column
    }
    startLight = lit / NightCues.COLUMNS

    val on = LightCues.fadeIn(t, NightCues.HEADLIGHTS_ON, NightCues.HEADLIGHTS_FADE)
    val pulse = max(
        bump(lap, NightCues.FLASH_FIRST, NightCues.FLASH_LENGTH),
        bump(lap, NightCues.FLASH_SECOND, NightCues.FLASH_LENGTH),
    )
    headlights = on * (1f + (NightCues.FLASH_LIGHT_PEAK - 1f) * pulse)
    headlightLamps = on * (1f + (NightCues.FLASH_LAMP_PEAK - 1f) * pulse)
    beams = on * (1f + (NightCues.FLASH_BEAM_PEAK - 1f) * pulse)

    val wakeEnd = NightCues.WAKE + NightCues.WAKE_BLINKS * NightCues.WAKE_BLINK_PERIOD
    signals = if (t >= NightCues.WAKE && t < wakeEnd) {
        2.5f * LightCues.blink(t - NightCues.WAKE, NightCues.WAKE_BLINK_PERIOD, duty = 0.5f)
    } else {
        1f
    }

    val pressed = ramp(lap, NightCues.BRAKE_ON, NightCues.BRAKE_RISE) *
        (1f - ramp(lap, NightCues.BRAKE_OFF, NightCues.BRAKE_FALL))
    brake = 1f + (NightCues.BRAKE_LIGHT_PEAK - 1f) * pressed
    brakeLamps = 1f + (NightCues.BRAKE_LAMP_PEAK - 1f) * pressed
}

/**
 * Device tilt, smoothed and re-centred.
 *
 * Raw gravity is fed by the sensor at whatever cadence it likes; [update] runs once per
 * presented frame with the elapsed time. Two low-pass filters: a quick one (τ ≈ 0.15 s)
 * that follows the hand, and a slow one (τ ≈ 4 s) that learns the resting posture. The
 * output is their difference, so a phone held at any angle settles at zero and only a
 * *change* of tilt steers the gaze — nobody reads the home screen holding it flat.
 */
internal class HeroTilt(
    private val fastTau: Float = 0.15f,
    private val slowTau: Float = 4f,
    private val gain: Float = 0.25f,
) {
    private var rawX = 0f
    private var rawY = 0f
    private var fastX = 0f
    private var fastY = 0f
    private var slowX = 0f
    private var slowY = 0f
    private var primed = false

    /** −1 → 1, left → right. */
    var x: Float = 0f
        private set

    /** −1 → 1, back → forward. */
    var y: Float = 0f
        private set

    /**
     * Gravity vector from `Sensor.TYPE_GRAVITY`, any thread; only the latest is kept. The
     * component along the phone's long axis (Y) carries no lean, so it is not needed.
     */
    fun feed(gravityX: Float, gravityZ: Float) {
        // In portrait, gravity along +X means the phone leans left, along +Z it lies flat.
        rawX = (-gravityX / EARTH_GRAVITY).coerceIn(-1f, 1f)
        rawY = (gravityZ / EARTH_GRAVITY).coerceIn(-1f, 1f)
    }

    fun update(deltaSeconds: Float) {
        if (!primed) {
            fastX = rawX; fastY = rawY; slowX = rawX; slowY = rawY
            primed = true
        }
        val dt = deltaSeconds.coerceIn(0f, 0.25f)
        val kf = 1f - kotlin.math.exp(-dt / fastTau)
        val ks = 1f - kotlin.math.exp(-dt / slowTau)
        fastX += (rawX - fastX) * kf
        fastY += (rawY - fastY) * kf
        slowX += (fastX - slowX) * ks
        slowY += (fastY - slowY) * ks
        x = ((fastX - slowX) * gain / SETTLE_RANGE).coerceIn(-1f, 1f)
        y = ((fastY - slowY) * gain / SETTLE_RANGE).coerceIn(-1f, 1f)
    }

    fun reset() {
        primed = false
        x = 0f
        y = 0f
    }

    private companion object {
        const val EARTH_GRAVITY = 9.80665f
        /** Tilt (as a fraction of g) that maps to full deflection, before [gain]. */
        const val SETTLE_RANGE = 0.12f
    }
}
