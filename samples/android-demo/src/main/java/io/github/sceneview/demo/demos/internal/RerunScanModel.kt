package io.github.sceneview.demo.demos.internal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A scan's result: the 3D model the result screen opens on and the export sheet writes first.
 * Kept next to the scan (`model.glb`, then `model.json`, [RerunSessionStore.saveModel]) so the
 * scan reopens on the same model without building it again.
 */
class RerunScanModel(
    val kind: Kind,
    /** The model as one `.glb` ([RerunMeshGlb]), metres, Y up, the scan's world space. */
    val glb: ByteArray,
    /** `[minX, minY, minZ, maxX, maxY, maxZ]` of the geometry: what the result frames. */
    val bounds: FloatArray,
    val triangles: Int,
    val vertices: Int,
    /** The TSDF ran out of memory: part of the space is left out. */
    val budgetReached: Boolean = false,
) {
    init {
        require(bounds.size == BOUNDS) { "six bounds" }
    }

    /** What the model is made of. */
    enum class Kind(val wireName: String) {
        /** A solid surface meshed from the fused depth. */
        Mesh("mesh"),

        /** Meshing failed: the scan's coloured points, each a small square. */
        Points("points"),
        ;

        companion object {
            fun of(wireName: String?): Kind? = entries.firstOrNull { it.wireName == wireName }
        }
    }

    /** `model.json`: everything but the `.glb`, pretty-printed. */
    fun infoJson(): String = pretty.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("version", VERSION)
            put("kind", kind.wireName)
            putJsonArray("bounds") { bounds.forEach { add(JsonPrimitive(it)) } }
            put("triangles", triangles)
            put("vertices", vertices)
            put("budgetReached", budgetReached)
        },
    )

    companion object {
        const val VERSION = 1
        private const val BOUNDS = 6
        private val pretty = Json { prettyPrint = true }
        private val lenient = Json { ignoreUnknownKeys = true; isLenient = true }

        /**
         * `null` when [info] is not a whole `model.json`, was written by a newer app ([VERSION]),
         * or [glb] is not a `.glb`: the scan then builds its model again.
         */
        @Suppress("ReturnCount") // one early return per required key
        fun parse(info: String, glb: ByteArray): RerunScanModel? {
            if (glb.size < GLB_HEADER || !glb.startsWithGlbMagic()) return null
            val root = runCatching { lenient.parseToJsonElement(info) as? JsonObject }.getOrNull() ?: return null
            fun number(key: String) = (root[key] as? JsonPrimitive)?.takeIf { !it.isString }
            val version = number("version")?.intOrNull ?: return null
            if (version !in 1..VERSION) return null
            val kind = Kind.of((root["kind"] as? JsonPrimitive)?.takeIf { it.isString }?.content) ?: return null
            val bounds = (root["bounds"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.floatOrNull }
                ?.takeIf { b -> b.size == BOUNDS && b.all { it.isFinite() } }
                ?.toFloatArray() ?: return null
            return RerunScanModel(
                kind = kind,
                glb = glb,
                bounds = bounds,
                triangles = number("triangles")?.intOrNull?.takeIf { it > 0 } ?: return null,
                vertices = number("vertices")?.intOrNull ?: return null,
                budgetReached = number("budgetReached")?.booleanOrNull ?: false,
            )
        }

        private fun ByteArray.startsWithGlbMagic() =
            this[0] == 'g'.code.toByte() && this[1] == 'l'.code.toByte() &&
                this[2] == 'T'.code.toByte() && this[3] == 'F'.code.toByte()

        private const val GLB_HEADER = 12
    }
}

/**
 * The fallback model when meshing fails: the scan's coloured points, each one a small square
 * lying in its surface (the replay's surfels, [DenseSurfels]), in a mesh [RerunMeshGlb] writes.
 */
object RerunPointsMesh {
    /** Points kept at most: 4 vertices each, ~8 MB of `.glb` at the cap. */
    const val MAX_POINTS = 60_000

    /** Grey for a point the camera never coloured. */
    const val UNCOLOURED = 0xFFB4B4B4.toInt()

    /**
     * Two triangles a point, every `count / maxPoints`-th point when [cloud] holds more than
     * [maxPoints]; a kept square grows so the points left out leave no holes.
     */
    @Suppress("LongMethod")
    fun of(cloud: DenseCloud, voxelM: Float = DenseFusion.VOXEL_M, maxPoints: Int = MAX_POINTS): RerunMesh {
        val stride = maxOf(1, (cloud.count + maxPoints - 1) / maxPoints)
        val n = (cloud.count + stride - 1) / stride
        val half = voxelM * DenseSurfels.HALF_SIDE_VOXELS * sqrt(stride.toFloat())
        val positions = FloatArray(n * QUAD * 3)
        val normals = FloatArray(n * QUAD * 3)
        val colors = IntArray(n * QUAD)
        val indices = IntArray(n * QUAD_INDICES)
        val source = cloud.normals
        for (k in 0 until n) {
            val i = k * stride
            val x = cloud.positions[i * 3]
            val y = cloud.positions[i * 3 + 1]
            val z = cloud.positions[i * 3 + 2]
            val nx = source?.get(i * 3) ?: 0f
            val ny = source?.get(i * 3 + 1) ?: 1f
            val nz = source?.get(i * 3 + 2) ?: 0f
            // Two tangents: any vector not parallel to the normal, crossed twice.
            val ax = if (abs(ny) < PARALLEL) 0f else 1f
            val ay = if (abs(ny) < PARALLEL) 1f else 0f
            var tx = ay * nz
            var ty = -ax * nz
            var tz = ax * ny - ay * nx
            val tl = sqrt(tx * tx + ty * ty + tz * tz).takeIf { it > EPSILON } ?: 1f
            tx = tx / tl * half
            ty = ty / tl * half
            tz = tz / tl * half
            val bx = ny * tz - nz * ty
            val by = nz * tx - nx * tz
            val bz = nx * ty - ny * tx
            // `0` is "no colour" (DenseCloud); a coloured point is opaque, black included.
            val color = cloud.colors[i].takeIf { it ushr ALPHA_SHIFT != 0 } ?: UNCOLOURED
            val v = k * QUAD
            corner(positions, v, x - tx - bx, y - ty - by, z - tz - bz)
            corner(positions, v + 1, x + tx - bx, y + ty - by, z + tz - bz)
            corner(positions, v + 2, x + tx + bx, y + ty + by, z + tz + bz)
            corner(positions, v + 3, x - tx + bx, y - ty + by, z - tz + bz)
            for (c in 0 until QUAD) {
                corner(normals, v + c, nx, ny, nz)
                colors[v + c] = color
            }
            val t = k * QUAD_INDICES
            indices[t] = v
            indices[t + 1] = v + 1
            indices[t + 2] = v + 2
            indices[t + 3] = v
            indices[t + 4] = v + 2
            indices[t + 5] = v + 3
        }
        return RerunMesh(positions, normals, colors, indices)
    }

    private fun corner(into: FloatArray, vertex: Int, x: Float, y: Float, z: Float) {
        into[vertex * 3] = x
        into[vertex * 3 + 1] = y
        into[vertex * 3 + 2] = z
    }

    private const val QUAD = 4
    private const val QUAD_INDICES = 6
    private const val PARALLEL = 0.9f
    private const val EPSILON = 1e-6f
    private const val ALPHA_SHIFT = 24
    private const val LOW = 0.01f
    private const val HIGH = 0.99f

    /**
     * The box the result frames a points model by: per axis, from the 1st to the 99th percentile
     * of its vertices. Points are noisy — a few strays far off, which a min/max box would fit
     * whole, leave the room itself small in the middle of the screen. All zero for no vertex.
     */
    fun framingBounds(mesh: RerunMesh, low: Float = LOW, high: Float = HIGH): FloatArray {
        val n = mesh.vertexCount
        val out = FloatArray(BOUNDS)
        if (n == 0) return out
        val axis = FloatArray(n)
        for (a in 0 until 3) {
            for (v in 0 until n) axis[v] = mesh.positions[v * 3 + a]
            axis.sort()
            out[a] = axis[((n - 1) * low).toInt()]
            out[a + 3] = axis[((n - 1) * high + 0.5f).toInt().coerceAtMost(n - 1)]
        }
        return out
    }

    private const val BOUNDS = 6
}

/**
 * The result screen's view of a model: a fixed three-quarter angle, a little higher than the
 * replay's so the eye looks into the space over its near walls, the geometry's box fitted whole
 * inside the area the chrome leaves free. The export thumbnail is drawn from the same view.
 */
object RerunResultView {
    const val AZIMUTH = ArDebugFraming.HOME_AZIMUTH
    const val ELEVATION = 40f

    fun home(bounds: FloatArray, verticalFovDegrees: Double, aspect: Float, insets: FramingInsets): OrbitPose =
        ArDebugFraming.fit(
            bounds = bounds,
            azimuthDegrees = AZIMUTH,
            elevationDegrees = ELEVATION,
            verticalFovDegrees = verticalFovDegrees,
            aspect = aspect,
            insets = insets,
        )

    /** [model] drawn square, [size] pixels a side, from the screen's view; `null` for an unreadable `.glb`. */
    fun thumbnail(model: RerunScanModel, size: Int, verticalFovDegrees: Double): IntArray? {
        val mesh = RerunGlbMesh.read(model.glb) ?: return null
        val margin = FramingInsets(THUMBNAIL_MARGIN, THUMBNAIL_MARGIN, THUMBNAIL_MARGIN, THUMBNAIL_MARGIN)
        val pose = home(model.bounds, verticalFovDegrees, 1f, margin)
        return RerunModelThumbnail.render(mesh, pose, verticalFovDegrees, size, size)
    }

    /** The thumbnail's breathing room on each side, a share of its width. */
    const val THUMBNAIL_MARGIN = 0.06f
}
