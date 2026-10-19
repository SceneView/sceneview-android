package io.github.sceneview.demo.demos.internal

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

/*
 * The Rerun demo's Record mode: a room scanned live with ARCore and rebuilt in the same 3D replay
 * as the bundled session — the camera's path, a photo every few steps, the feature points in the
 * colours the camera saw them in, and the planes. Nothing leaves the phone.
 *
 * Everything here is pure Kotlin, tested on the JVM: which poses earn a photo, how a world point
 * lands on ARCore's camera image and what colour it finds there, how that image is turned upright
 * for a thumbnail, and how the photos are packed for the replay. The Android side (RerunScan.kt)
 * only hands it ARCore's buffers.
 */

/**
 * Pinhole intrinsics of ARCore's CPU camera image (`Camera.getImageIntrinsics()`), in the
 * image's own orientation — the sensor's, landscape on a phone.
 */
data class ScanIntrinsics(
    val fx: Float,
    val fy: Float,
    val cx: Float,
    val cy: Float,
    val width: Int,
    val height: Int,
) {
    val isUsable: Boolean get() = fx > 0f && fy > 0f && width > 0 && height > 0

    /** Whether pixel coordinates ([u], [v]) fall on the image. */
    fun contains(u: Float, v: Float): Boolean = u >= 0f && v >= 0f && u < width && v < height
}

/**
 * A `YUV_420_888` camera image, read in place: the luma plane at full size, the two chroma planes
 * at half size in both directions, each with its own row and pixel strides (the chroma planes of
 * an NV21 buffer share memory and have a pixel stride of 2).
 */
class YuvFrame(
    val width: Int,
    val height: Int,
    private val y: ByteBuffer,
    private val yRowStride: Int,
    private val u: ByteBuffer,
    private val v: ByteBuffer,
    private val uvRowStride: Int,
    private val uvPixelStride: Int,
) {
    /** The colour at pixel ([px], [py]), `0xFFRRGGBB`; coordinates are clamped to the image. */
    fun argb(px: Int, py: Int): Int {
        val x = px.coerceIn(0, width - 1)
        val yy = py.coerceIn(0, height - 1)
        val luma = y.get(yy * yRowStride + x).toInt() and 0xFF
        val chroma = (yy shr 1) * uvRowStride + (x shr 1) * uvPixelStride
        return ScanColor.yuvToArgb(luma, u.get(chroma).toInt() and 0xFF, v.get(chroma).toInt() and 0xFF)
    }
}

/** Colour conversion of the camera's frames. */
object ScanColor {
    /**
     * Full-range BT.601 YUV → `0xFFRRGGBB`: what the camera HAL's `YUV_420_888` carries, the
     * conversion `YuvImage` → JPEG applies too. Fixed point, 16 fractional bits.
     */
    @Suppress("MagicNumber") // the BT.601 coefficients
    fun yuvToArgb(y: Int, u: Int, v: Int): Int {
        val d = u - 128
        val e = v - 128
        val r = (y + ((91_881 * e) shr 16)).coerceIn(0, 255)
        val g = (y - ((22_554 * d + 46_802 * e) shr 16)).coerceIn(0, 255)
        val b = (y + ((116_130 * d) shr 16)).coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Averages `0xFFRRGGBB` colours channel by channel. */
    fun average(colors: IntArray, count: Int = colors.size): Int {
        if (count <= 0) return 0
        var r = 0
        var g = 0
        var b = 0
        for (i in 0 until count) {
            val c = colors[i]
            r += c shr 16 and 0xFF
            g += c shr 8 and 0xFF
            b += c and 0xFF
        }
        return (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
    }
}

/**
 * Where things land on ARCore's camera image, and what the scan takes from it.
 *
 * Two poses are in play. `Camera.getPose()` ([sensor]) is the physical camera, its +X to the
 * right of the image and +Y up it, looking down -Z: a point projects onto the CPU image with
 * the intrinsics as they are. `Camera.getDisplayOrientedPose()` ([display]) is the same camera
 * turned to the screen — upright in portrait — and it is the pose the trail and the frustums use.
 * A photo meant to sit in a frustum is therefore resampled from the sensor image through the
 * rotation between the two.
 */
object ScanProjection {

    /** The world point ([x], [y], [z]) in the local frame of a camera at [pose]. */
    fun toLocal(pose: DebugPose, x: Float, y: Float, z: Float): Vec3 =
        DebugPose(0f, 0f, 0f, -pose.qx, -pose.qy, -pose.qz, pose.qw).rotate(x - pose.x, y - pose.y, z - pose.z)

    /**
     * Pixel coordinates `(u, v)` of the world point on the image of a camera at [sensor], packed
     * in a [FloatArray] of two; `null` behind the camera or outside the image.
     */
    fun project(intrinsics: ScanIntrinsics, sensor: DebugPose, x: Float, y: Float, z: Float): FloatArray? {
        val p = toLocal(sensor, x, y, z)
        if (p.z > -MIN_DEPTH_M) return null
        val u = intrinsics.cx + intrinsics.fx * p.x / -p.z
        val v = intrinsics.cy - intrinsics.fy * p.y / -p.z
        return if (intrinsics.contains(u, v)) floatArrayOf(u, v) else null
    }

    /**
     * One colour per point of the flat `[x,y,z, …]` [points], read from the camera [image] a
     * camera at [sensor] took; `0` for a point the image does not show. Each colour is the mean
     * of the pixel and its four neighbours two pixels out, so sensor noise does not paint a
     * point with one hot pixel.
     */
    fun colors(image: YuvFrame, intrinsics: ScanIntrinsics, sensor: DebugPose, points: FloatArray): IntArray {
        val count = points.size / 3
        val out = IntArray(count)
        val taps = IntArray(TAPS.size / 2)
        for (i in 0 until count) {
            val uv = project(intrinsics, sensor, points[i * 3], points[i * 3 + 1], points[i * 3 + 2]) ?: continue
            val px = uv[0].toInt()
            val py = uv[1].toInt()
            for (k in taps.indices) taps[k] = image.argb(px + TAPS[k * 2], py + TAPS[k * 2 + 1])
            out[i] = ScanColor.average(taps)
        }
        return out
    }

    /**
     * The frustum proportions of the camera as the screen holds it: the sensor image's half
     * extents at one metre, swapped when the display turns the image a quarter.
     */
    fun displayLens(intrinsics: ScanIntrinsics, sensor: DebugPose, display: DebugPose): ReplayLens {
        val halfW = intrinsics.width / 2f / intrinsics.fx
        val halfH = intrinsics.height / 2f / intrinsics.fy
        return if (isQuarterTurn(sensor, display)) ReplayLens(halfH, halfW) else ReplayLens(halfW, halfH)
    }

    /** Whether the screen's X axis runs along the image's Y axis — portrait on a phone. */
    fun isQuarterTurn(sensor: DebugPose, display: DebugPose): Boolean {
        val right = display.rotate(1f, 0f, 0f)
        val sensorRotation = DebugPose(0f, 0f, 0f, sensor.qx, sensor.qy, sensor.qz, sensor.qw)
        val inSensor = toLocal(sensorRotation, right.x, right.y, right.z)
        return abs(inSensor.y) > abs(inSensor.x)
    }

    /** Output size of an upright photo whose long side is [longSide]: portrait when the lens is. */
    fun photoSize(lens: ReplayLens, longSide: Int): Pair<Int, Int> {
        val aspect = lens.halfWidthPerDepth / lens.halfHeightPerDepth
        return if (aspect <= 1f) {
            (longSide * aspect).roundToInt().coerceAtLeast(1) to longSide
        } else {
            longSide to (longSide / aspect).roundToInt().coerceAtLeast(1)
        }
    }

    /**
     * The camera [image] as the screen held it: [outWidth] × [outHeight] `0xFFRRGGBB` pixels,
     * row-major, framed by [lens] around the [display] camera, each pixel the mean of a 2×2
     * grid of samples of the sensor image. What falls outside the sensor image is black.
     *
     * The ray through an output pixel is linear in its coordinates and a rotation keeps it so:
     * the sensor-space ray is `origin + x·stepX + y·stepY`, three vectors rotated once, and each
     * sample costs one division.
     */
    @Suppress("LongParameterList") // the image, its camera, the two poses and the output size
    fun uprightImage(
        image: YuvFrame,
        intrinsics: ScanIntrinsics,
        sensor: DebugPose,
        display: DebugPose,
        lens: ReplayLens,
        outWidth: Int,
        outHeight: Int,
    ): IntArray {
        val out = IntArray(outWidth * outHeight)
        val sx = 2f * lens.halfWidthPerDepth / outWidth
        val sy = -2f * lens.halfHeightPerDepth / outHeight
        val origin = sensorRay(sensor, display, -lens.halfWidthPerDepth, lens.halfHeightPerDepth, -1f)
        val stepX = sensorRay(sensor, display, sx, 0f, 0f)
        val stepY = sensorRay(sensor, display, 0f, sy, 0f)
        val samples = IntArray(4)
        for (oy in 0 until outHeight) {
            for (ox in 0 until outWidth) {
                var n = 0
                for (sub in 0 until 4) {
                    val fx = ox + SUB[sub * 2]
                    val fy = oy + SUB[sub * 2 + 1]
                    val color = sample(
                        image,
                        intrinsics,
                        origin.x + stepX.x * fx + stepY.x * fy,
                        origin.y + stepX.y * fx + stepY.y * fy,
                        origin.z + stepX.z * fx + stepY.z * fy,
                    )
                    if (color != NO_SAMPLE) samples[n++] = color
                }
                out[oy * outWidth + ox] = if (n == 0) OPAQUE_BLACK else ScanColor.average(samples, n)
            }
        }
        return out
    }

    /**
     * The colour the sensor ray ([rx], [ry], [rz]) finds on [image], or [NO_SAMPLE] when it
     * points behind the lens or off the image. A real colour is opaque, so never `0`.
     */
    private fun sample(image: YuvFrame, intrinsics: ScanIntrinsics, rx: Float, ry: Float, rz: Float): Int {
        if (rz > -MIN_DEPTH_M) return NO_SAMPLE
        val u = intrinsics.cx + intrinsics.fx * rx / -rz
        val v = intrinsics.cy - intrinsics.fy * ry / -rz
        return if (intrinsics.contains(u, v)) image.argb(u.toInt(), v.toInt()) else NO_SAMPLE
    }

    /** The display-camera vector ([x], [y], [z]) in the sensor camera's frame. */
    private fun sensorRay(sensor: DebugPose, display: DebugPose, x: Float, y: Float, z: Float): Vec3 {
        val world = display.rotate(x, y, z)
        return toLocal(DebugPose(0f, 0f, 0f, sensor.qx, sensor.qy, sensor.qz, sensor.qw), world.x, world.y, world.z)
    }

    /** Nothing nearer the lens than this projects: ARCore's own near plane is further out. */
    private const val MIN_DEPTH_M = 0.01f

    private const val OPAQUE_BLACK = 0xFF000000.toInt()
    private const val NO_SAMPLE = 0

    /** The colour taps: the pixel, then two pixels left, right, up and down. */
    private val TAPS = intArrayOf(0, 0, -2, 0, 2, 0, 0, -2, 0, 2)

    /** 2×2 sub-pixel grid, in output pixels. */
    private val SUB = floatArrayOf(0.25f, 0.25f, 0.75f, 0.25f, 0.25f, 0.75f, 0.75f, 0.75f)
}

/**
 * Which camera poses earn a photo: the first one, then any that moved [minStepM] or turned
 * [minTurnDegrees] from the last photo's — a photo every few steps and every glance, never two
 * of the same view. Stops at [maxPhotos] so a long scan cannot fill the memory.
 */
class KeyframeGate(
    private val minStepM: Float = MIN_STEP_M,
    minTurnDegrees: Float = MIN_TURN_DEGREES,
    private val maxPhotos: Int = MAX_PHOTOS,
) {
    /** cos(½·turn): two unit quaternions `turn` apart have `|dot|` equal to it. */
    private val minTurnDot = cos(Math.toRadians(minTurnDegrees / 2.0)).toFloat()
    private var last: DebugPose? = null

    var count: Int = 0
        private set

    val isFull: Boolean get() = count >= maxPhotos

    /** Whether a photo taken from [pose] would be kept. Does not commit: see [accept]. */
    fun wants(pose: DebugPose): Boolean {
        if (isFull) return false
        val previous = last ?: return true
        val dx = pose.x - previous.x
        val dy = pose.y - previous.y
        val dz = pose.z - previous.z
        if (dx * dx + dy * dy + dz * dz >= minStepM * minStepM) return true
        val dot = abs(pose.qx * previous.qx + pose.qy * previous.qy + pose.qz * previous.qz + pose.qw * previous.qw)
        return dot < minTurnDot
    }

    /** Records that a photo was taken from [pose]. */
    fun accept(pose: DebugPose) {
        last = pose
        count++
    }

    companion object {
        const val MIN_STEP_M = 0.15f
        const val MIN_TURN_DEGREES = 10f

        /** 300 photos of 240×320: about 5 MB of JPEG and 23 MB of thumbnails. */
        const val MAX_PHOTOS = 300
    }
}

/**
 * The scan's photos, packed the way the bundled replay ships its own: one archive of JPEGs and
 * the span of each, so the replay reads a scan and the showcase through the same code.
 */
object ScanArchive {
    /** Where photo [index] (0-based) lives in the scan: `scan/frame-0001.jpg` for the first. */
    fun photoPath(index: Int): String = String.format(Locale.US, "scan/frame-%04d.jpg", index + 1)

    /** Concatenates [photos] (path → bytes, in order) and indexes each. */
    fun pack(photos: List<Pair<String, ByteArray>>): Pair<ByteArray, Map<String, MediaSpan>> {
        val out = ByteArrayOutputStream(photos.sumOf { it.second.size })
        val spans = LinkedHashMap<String, MediaSpan>()
        for ((path, bytes) in photos) {
            if (bytes.isEmpty()) continue
            spans[path] = MediaSpan(out.size(), bytes.size)
            out.write(bytes)
        }
        return out.toByteArray() to spans
    }
}


/** Copy of the Record mode, pure so a test holds it to what the brief promised. */
object ScanCopy {
    /** The promise under the Start button, and in the sheet. */
    const val PRIVACY = "Saved on your phone. Nothing leaves it unless you share it."

    const val IDLE_TITLE = "3D Scan"
    const val IDLE_DETAIL = "Move the phone slowly around a space. $PRIVACY"
    const val WAITING = "Move slowly to get started."

    // The landing (#4627): one name for the feature, "3D Scan", on every surface.
    const val LANDING_TITLE = "3D Scan"
    const val LANDING_BODY = "Move around a space and watch it take shape."

    /** The landing's one primary action. */
    const val RECORD = "Start scanning"
    const val WATCH_SAMPLE = "See an example"
    const val OPEN_FILE = "Open scan"
    const val SESSIONS_TITLE = "Your scans"
    const val ON_THIS_PHONE = "On this phone"
    const val SESSIONS_EMPTY_TITLE = "No scans yet"
    const val SESSIONS_EMPTY = "Scans you make are kept here, on this phone, until you delete them. " +
        "You can also open a .svscan scan file."
    const val OPENING_FILE = "Opening scan…"
    const val SHARE_SCAN = "Share scan file"
    const val DELETE = "Delete"
    const val DELETE_DETAIL = "It is removed from this phone. Files you already shared are not affected."
    const val OPEN_FAILED = "This scan could not be opened."
    const val SAVE_FAILED = "Couldn't save your scan"
    const val SAVE_FAILED_DETAIL = "Your scan is still here, not saved yet. Try again, or discard it."

    /** `Delete "Room · Sep 28, 2:32 PM"?` */
    fun deleteTitle(title: String): String = "Delete “$title”?"

    /** Why [file] did not open, in the user's words. */
    fun importFailure(failure: RerunImportFailure, file: String): String {
        val name = "“$file”"
        return when (failure) {
            is RerunImportFailure.Unsupported -> "$name is not a SceneView scan file or a Rerun recording."
            is RerunImportFailure.Empty -> "$name holds no camera path, points or surfaces to replay."
            is RerunImportFailure.RrdCompressed ->
                "$name is a compressed Rerun recording. Save it again from Rerun without compression."
            is RerunImportFailure.RrdNewerVersion ->
                "$name comes from a newer version of Rerun than this app can read."
            is RerunImportFailure.Unreadable -> "$name could not be read. It may be damaged or cut short."
        }
    }

    /** A kept session's figures: `4.2 m · 3.8k points · 42 photos · 0:18`, as on iOS. */
    fun figures(pathMetres: Float, points: Int, photos: Int, seconds: Float): String {
        val parts = mutableListOf(
            ArDebugFormat.distance(pathMetres),
            "${ArDebugFormat.compactCount(points)} ${label(points, "point", "points")}",
        )
        if (photos > 0) parts += "${ArDebugFormat.count(photos)} ${label(photos, "photo", "photos")}"
        if (seconds.isFinite() && seconds > 0f) parts += ArDebugFormat.clock(seconds)
        return parts.joinToString(" · ")
    }

    const val LOADING = "Opening your scan…"
    const val REPLAY_TITLE = "Your scan"
    const val SAMPLE_TITLE = "Example scan"
    const val FULL = "Photo limit reached — points and path keep recording."

    // The capture lifecycle (#4627): Scanning ⇄ Paused, Finish opens a review that Keep scanning
    // leaves for the same capture; only Save scan ends it.
    const val SCANNING = "Scanning"
    const val PAUSED = "Paused"
    const val PAUSE = "Pause"
    const val RESUME = "Resume"
    const val FINISH = "Finish"
    const val COACH_SCANNING = "Move slowly sideways"
    const val COACH_PAUSED = "Nothing is captured while paused."

    // ScanCoach's lines (#4631): short, and said only while their cause holds.
    const val COACH_SLOWER = "Move slower"
    const val COACH_NOTHING_NEW = "Point at the floor or a wall, 1 to 3 m away"

    // The dense map past its budget (#4629): said once while it merges, never as a limit.
    const val ADJUSTING_DETAIL = "Adjusting detail for a larger space"
    const val SCAN_FULL = "This scan is full. Save it to keep what you have."
    const val TRACKING_LOST = "Scan paused. Point at an area you already scanned."
    const val REVIEW_TITLE = "Review your scan"
    const val REVIEW_DETAIL = "Keep scanning to add more, or save it on this phone."
    const val EMPTY_TITLE = "Nothing captured yet"
    const val EMPTY_DETAIL = "Move the phone slowly around the space, then tap Finish again."
    const val KEEP_SCANNING = "Keep scanning"
    const val SAVE_SCAN = "Save scan"
    const val SAVING = "Saving scan…"
    const val TRY_AGAIN = "Try again"
    const val DISCARD = "Discard"
    const val CANCEL = "Cancel"
    const val LEAVE_TITLE = "Leave this scan?"
    const val LEAVE_DETAIL = "Save it on this phone, or discard what you captured."
    const val LEAVE_EMPTY_DETAIL = "Nothing captured yet. Keep scanning, or discard it and leave."
    const val DISCARD_TITLE = "Discard this scan?"
    const val DISCARD_DETAIL = "It was never saved, so it can't be recovered."

    /** The top line of the capture: `Scanning · 0:42`, `Paused · 0:42`. */
    fun status(phase: ScanPhase, clock: String): String =
        "${if (phase == ScanPhase.Scanning) SCANNING else PAUSED} · $clock"

    /** The capture's one count, from zero and never against a cap: `124,300 points captured`. */
    fun pointsCaptured(points: Int): String {
        val n = points.coerceAtLeast(0)
        return "${ArDebugFormat.count(n)} ${label(n, "point", "points")} captured"
    }

    /** What the review says about the capture so far: `124,300 points · 42 photos · 0:42`. */
    fun reviewFigures(points: Int, photos: Int, clock: String): String {
        val parts = mutableListOf("${ArDebugFormat.count(points.coerceAtLeast(0))} ${label(points, "point", "points")}")
        if (photos > 0) parts += "${ArDebugFormat.count(photos)} ${label(photos, "photo", "photos")}"
        parts += clock
        return parts.joinToString(" · ")
    }

    /**
     * The one coaching line under the capture, or `null` when a card speaks instead: tracking
     * loss first, then the dense map's [detail] when it is not [ScanDetail.Fine], then the photo
     * limit, then the [coach]'s line (#4631), then the plain advice.
     */
    fun coaching(
        phase: ScanPhase,
        trackingLost: Boolean,
        photoLimitReached: Boolean,
        detail: ScanDetail = ScanDetail.Fine,
        coach: ScanCoach.Line? = null,
    ): String? = when (phase) {
        ScanPhase.Scanning -> when {
            trackingLost -> TRACKING_LOST
            detail == ScanDetail.Full -> SCAN_FULL
            detail == ScanDetail.Adjusting -> ADJUSTING_DETAIL
            photoLimitReached -> FULL
            coach == ScanCoach.Line.Slower -> COACH_SLOWER
            coach == ScanCoach.Line.NothingNew -> COACH_NOTHING_NEW
            else -> COACH_SCANNING
        }
        ScanPhase.Paused -> COACH_PAUSED
        else -> null
    }

    /** `1` → `1 photo`, `12` → `12 photos`: the figure's label follows its value. */
    fun label(count: Int, one: String, many: String): String = if (count == 1) one else many

    /**
     * A figure of the Record screen, in full up to `9,999` and compact past it (`12k`), so three
     * of them in the display size fit a phone's width side by side.
     */
    fun figure(value: Int): String =
        if (value < COMPACT_FROM) ArDebugFormat.count(value.coerceAtLeast(0)) else ArDebugFormat.compactCount(value)

    private const val COMPACT_FROM = 10_000
}
