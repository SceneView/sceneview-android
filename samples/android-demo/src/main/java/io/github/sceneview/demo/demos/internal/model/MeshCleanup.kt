package io.github.sceneview.demo.demos.internal.model

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A mesh being cleaned: [positions] (metres, three a vertex), [colors] (`0xFFRRGGBB`, sRGB, one a
 * vertex) and [indices] (three a triangle, wound to face the free space the camera saw).
 */
internal class WorkMesh(val positions: FloatArray, val colors: IntArray, val indices: IntArray) {
    val vertexCount: Int get() = colors.size
    val triangleCount: Int get() = indices.size / 3

    /** The triangles [keep] marks, without the vertices none of them uses. */
    fun keepTriangles(keep: BooleanArray): WorkMesh {
        val remap = IntArray(vertexCount) { -1 }
        var kept = 0
        for (t in 0 until triangleCount) if (keep[t]) kept++
        val outIndices = IntArray(kept * 3)
        var n = 0
        var vertices = 0
        for (t in 0 until triangleCount) {
            if (!keep[t]) continue
            for (k in 0 until 3) {
                val v = indices[t * 3 + k]
                if (remap[v] < 0) remap[v] = vertices++
                outIndices[n++] = remap[v]
            }
        }
        val outPositions = FloatArray(vertices * 3)
        val outColors = IntArray(vertices)
        for (v in 0 until vertexCount) {
            val r = remap[v]
            if (r < 0) continue
            outPositions[r * 3] = positions[v * 3]
            outPositions[r * 3 + 1] = positions[v * 3 + 1]
            outPositions[r * 3 + 2] = positions[v * 3 + 2]
            outColors[r] = colors[v]
        }
        return WorkMesh(outPositions, outColors, outIndices)
    }
}

/*
 * The clean-up steps between marching cubes and the final model, each a plain function of a
 * [WorkMesh]. Order matters and is [CleanModel]'s: pieces, doubled surfaces, pieces again,
 * smoothing, planes ([PlaneFlattening]), holes.
 */
internal object MeshCleanup {
    /**
     * Drops the connected pieces whose area is under [minShare] of the largest one's: the
     * floating fragments a scan leaves (a reflection, a passer-by, depth noise), whatever the
     * scan's size. Returns the mesh and how many pieces went.
     */
    fun removeSmallPieces(mesh: WorkMesh, minShare: Float = MIN_PIECE_SHARE): Pair<WorkMesh, Int> {
        if (mesh.triangleCount == 0) return mesh to 0
        val topology = MeshTopology(mesh.vertexCount, mesh.indices)
        val (piece, count) = topology.components()
        val area = DoubleArray(count)
        val faces = FaceGeometry(mesh.positions, mesh.indices)
        for (t in 0 until mesh.triangleCount) area[piece[mesh.indices[t * 3]]] += faces.areas[t].toDouble()
        val largest = area.maxOrNull() ?: 0.0
        val keepPiece = BooleanArray(count) { area[it] >= largest * minShare }
        val dropped = keepPiece.count { !it }
        if (dropped == 0) return mesh to 0
        val keep = BooleanArray(mesh.triangleCount) { keepPiece[piece[mesh.indices[it * 3]]] }
        return mesh.keepTriangles(keep) to dropped
    }

    /** How many connected pieces [mesh] is in. */
    fun pieces(mesh: WorkMesh): Int = MeshTopology(mesh.vertexCount, mesh.indices).components().second

    /**
     * Removes the hidden sheet of a doubled surface: where a pass drifted, a wall is meshed twice,
     * two sheets a few centimetres apart facing the same way. A triangle whose own normal, cast
     * towards the free space it faces, meets another triangle facing the same way within
     * [maxGapM] is behind that one, where no camera could have seen it: it goes. A thin object
     * seen from both sides (a door, a table top) faces opposite ways and stays. Returns the mesh
     * and how many triangles went.
     */
    fun removeDoubledSurfaces(
        mesh: WorkMesh,
        maxGapM: Float = DOUBLED_GAP_M,
        minCos: Float = DOUBLED_MIN_COS,
    ): Pair<WorkMesh, Int> {
        val n = mesh.triangleCount
        if (n == 0) return mesh to 0
        val faces = FaceGeometry(mesh.positions, mesh.indices)
        val grid = TriangleGrid(mesh.positions, mesh.indices, maxGapM)
        val topology = MeshTopology(mesh.vertexCount, mesh.indices)
        val rings = RingWalk(n)
        val keep = BooleanArray(n) { true }
        var removed = 0
        val p = mesh.positions
        val idx = mesh.indices
        for (t in 0 until n) {
            if (faces.areas[t] <= 0f) continue
            val nx = faces.normals[t * 3]
            val ny = faces.normals[t * 3 + 1]
            val nz = faces.normals[t * 3 + 2]
            val ox = faces.centroids[t * 3] + nx * RAY_START_M
            val oy = faces.centroids[t * 3 + 1] + ny * RAY_START_M
            val oz = faces.centroids[t * 3 + 2] + nz * RAY_START_M
            val hidden = grid.anyAlong(ox, oy, oz, nx, ny, nz, maxGapM) { u ->
                if (u == t || faces.dot(t, u) < minCos) return@anyAlong false
                if (sharesVertex(idx, t, u)) return@anyAlong false
                rayHits(p, idx, u, ox, oy, oz, nx, ny, nz, maxGapM, DOUBLED_MIN_GAP_M) &&
                    !rings.near(topology, idx, t, u, FOLD_RINGS)
            }
            if (hidden) {
                keep[t] = false
                removed++
            }
        }
        if (removed == 0) return mesh to 0
        // The hidden sheet and the sheet in front enclose a thin pocket of "air" that no camera
        // was in: the pocket's far wall faces only the hidden sheet. It goes with it, or it would
        // show as a patch on the back of the wall when the model is turned around.
        val hiddenOnly = BooleanArray(n)
        for (t in 0 until n) {
            if (!keep[t] || faces.areas[t] <= 0f) continue
            val nx = faces.normals[t * 3]
            val ny = faces.normals[t * 3 + 1]
            val nz = faces.normals[t * 3 + 2]
            val ox = faces.centroids[t * 3] + nx * RAY_START_M
            val oy = faces.centroids[t * 3 + 1] + ny * RAY_START_M
            val oz = faces.centroids[t * 3 + 2] + nz * RAY_START_M
            val facesHidden = grid.anyAlong(ox, oy, oz, nx, ny, nz, maxGapM) { u ->
                !keep[u] && faces.dot(t, u) <= -minCos && rayHits(p, idx, u, ox, oy, oz, nx, ny, nz, maxGapM)
            }
            if (facesHidden) hiddenOnly[t] = true
        }
        for (t in 0 until n) {
            if (hiddenOnly[t]) {
                keep[t] = false
                removed++
            }
        }
        removed += dropCutPieces(mesh, topology, keep)
        return mesh.keepTriangles(keep) to removed
    }

    /**
     * After the hidden sheets went, what hung between them — the pocket's thin side walls — is a
     * piece whose rim is mostly where triangles were cut away. Such a piece, small next to the
     * largest one, goes too; a wall that merely lost a patch behind a picture keeps. Returns
     * how many triangles went.
     */
    private fun dropCutPieces(mesh: WorkMesh, topology: MeshTopology, keep: BooleanArray): Int {
        val n = mesh.triangleCount
        val idx = mesh.indices
        val parent = IntArray(mesh.vertexCount) { it }
        fun root(v: Int): Int {
            var r = v
            while (parent[r] != r) r = parent[r]
            var x = v
            while (parent[x] != r) {
                val next = parent[x]
                parent[x] = r
                x = next
            }
            return r
        }
        for (t in 0 until n) {
            if (!keep[t]) continue
            val a = root(idx[t * 3])
            val b = root(idx[t * 3 + 1])
            if (a != b) parent[a] = b
            val c = root(idx[t * 3 + 2])
            val r = root(b)
            if (c != r) parent[c] = r
        }
        val faces = FaceGeometry(mesh.positions, idx)
        val area = HashMap<Int, Float>()
        val cut = HashMap<Int, Int>()
        val rim = HashMap<Int, Int>()
        val triangles = HashMap<Int, Int>()
        for (t in 0 until n) {
            if (!keep[t]) continue
            val piece = root(idx[t * 3])
            area.merge(piece, faces.areas[t], Float::plus)
            triangles.merge(piece, 1, Int::plus)
            for (k in 0 until 3) {
                val u = topology.neighbours[t * 3 + k]
                if (u >= 0 && keep[u]) continue
                rim.merge(piece, 1, Int::plus)
                if (u >= 0) cut.merge(piece, 1, Int::plus)
            }
        }
        val largest = area.values.maxOrNull() ?: return 0
        val drop = area.keys.filter { piece ->
            val edges = rim[piece] ?: 0
            val cutEdges = cut[piece] ?: 0
            // Mostly cut rim, and narrow: the rim is long next to the piece's size.
            edges > 0 && cutEdges * 2 >= edges && cutEdges * CUT_PIECE_TRIANGLES_PER_EDGE >= (triangles[piece] ?: 0) &&
                area.getValue(piece) < largest * CUT_PIECE_SHARE
        }.toHashSet()
        if (drop.isEmpty()) return 0
        var dropped = 0
        for (t in 0 until n) {
            if (keep[t] && root(idx[t * 3]) in drop) {
                keep[t] = false
                dropped++
            }
        }
        return dropped
    }

    /**
     * Smooths depth noise out of the surface without rounding its edges: each triangle's normal
     * is replaced by a blend of its neighbours' that ignores the ones turned away from it
     * (a bilateral filter on normals), then the vertices move to fit the filtered normals. A
     * wall's ripple goes; the crease where it meets the floor stays sharp. In place.
     */
    fun smooth(
        mesh: WorkMesh,
        normalPasses: Int = SMOOTH_NORMAL_PASSES,
        vertexPasses: Int = SMOOTH_VERTEX_PASSES,
        sharpness: Float = SMOOTH_NORMAL_SIGMA,
    ) {
        val n = mesh.triangleCount
        if (n == 0) return
        val topology = MeshTopology(mesh.vertexCount, mesh.indices)
        val faces = FaceGeometry(mesh.positions, mesh.indices)
        val idx = mesh.indices
        val spacing = meanEdge(mesh)
        val inverseSpatial = 1f / (2f * spacing * spacing * SMOOTH_SPATIAL_EDGES * SMOOTH_SPATIAL_EDGES)
        val inverseRange = 1f / (2f * sharpness * sharpness)
        val ring = RingCollector(n)
        var normals = faces.normals.copyOf()
        var next = FloatArray(normals.size)
        repeat(normalPasses) {
            for (t in 0 until n) {
                var sx = 0f
                var sy = 0f
                var sz = 0f
                ring.collect(topology, idx, t) { u ->
                    val dx = faces.centroids[u * 3] - faces.centroids[t * 3]
                    val dy = faces.centroids[u * 3 + 1] - faces.centroids[t * 3 + 1]
                    val dz = faces.centroids[u * 3 + 2] - faces.centroids[t * 3 + 2]
                    val ex = normals[u * 3] - normals[t * 3]
                    val ey = normals[u * 3 + 1] - normals[t * 3 + 1]
                    val ez = normals[u * 3 + 2] - normals[t * 3 + 2]
                    val w = faces.areas[u] *
                        exp(-(dx * dx + dy * dy + dz * dz) * inverseSpatial) *
                        exp(-(ex * ex + ey * ey + ez * ez) * inverseRange)
                    sx += normals[u * 3] * w
                    sy += normals[u * 3 + 1] * w
                    sz += normals[u * 3 + 2] * w
                }
                val length = sqrt(sx * sx + sy * sy + sz * sz)
                if (length > 0f) {
                    next[t * 3] = sx / length
                    next[t * 3 + 1] = sy / length
                    next[t * 3 + 2] = sz / length
                } else {
                    next[t * 3] = normals[t * 3]
                    next[t * 3 + 1] = normals[t * 3 + 1]
                    next[t * 3 + 2] = normals[t * 3 + 2]
                }
            }
            val swap = normals
            normals = next
            next = swap
        }
        val p = mesh.positions
        val moved = FloatArray(p.size)
        repeat(vertexPasses) {
            faces.update()
            for (v in 0 until mesh.vertexCount) {
                val start = topology.vertexStart[v]
                val end = topology.vertexStart[v + 1]
                if (end == start) continue
                var mx = 0f
                var my = 0f
                var mz = 0f
                for (i in start until end) {
                    val t = topology.vertexTriangles[i]
                    val nx = normals[t * 3]
                    val ny = normals[t * 3 + 1]
                    val nz = normals[t * 3 + 2]
                    val along = nx * (faces.centroids[t * 3] - p[v * 3]) +
                        ny * (faces.centroids[t * 3 + 1] - p[v * 3 + 1]) +
                        nz * (faces.centroids[t * 3 + 2] - p[v * 3 + 2])
                    mx += nx * along
                    my += ny * along
                    mz += nz * along
                }
                val count = (end - start).toFloat()
                moved[v * 3] = mx / count
                moved[v * 3 + 1] = my / count
                moved[v * 3 + 2] = mz / count
            }
            for (i in p.indices) p[i] += moved[i]
        }
    }

    /**
     * Closes the small holes only: a border loop of at most [maxEdges] edges spanning at most
     * [maxSpanM] gets a fan of triangles around its centre, coloured as its rim. A large opening
     * — a doorway, a part of the room nobody scanned — is left open rather than invented.
     * Returns the mesh and how many holes were closed.
     */
    fun fillSmallHoles(
        mesh: WorkMesh,
        maxEdges: Int = MAX_HOLE_EDGES,
        maxSpanM: Float = MAX_HOLE_SPAN_M,
    ): Pair<WorkMesh, Int> {
        if (mesh.triangleCount == 0) return mesh to 0
        val topology = MeshTopology(mesh.vertexCount, mesh.indices)
        val idx = mesh.indices
        val next = IntArray(mesh.vertexCount) { -1 }
        val outgoing = IntArray(mesh.vertexCount)
        for (t in 0 until mesh.triangleCount) {
            for (k in 0 until 3) {
                if (topology.neighbours[t * 3 + k] >= 0) continue
                val a = idx[t * 3 + k]
                val b = idx[t * 3 + (k + 1) % 3]
                if (hasEdge(topology, idx, b, a)) continue // a non-manifold edge, not a border
                next[a] = b
                outgoing[a]++
            }
        }
        val visited = BooleanArray(mesh.vertexCount)
        val loops = ArrayList<IntArray>()
        val loop = IntArray(maxEdges + 1)
        for (start in 0 until mesh.vertexCount) {
            if (next[start] < 0 || visited[start] || outgoing[start] != 1) continue
            var length = 0
            var v = start
            var closed = false
            while (length <= maxEdges) {
                if (outgoing[v] != 1 || visited[v]) break
                visited[v] = true
                loop[length++] = v
                v = next[v]
                if (v == start) {
                    closed = true
                    break
                }
            }
            if (!closed || length < 3 || length > maxEdges) continue
            if (span(mesh.positions, loop, length) > maxSpanM) continue
            loops += loop.copyOf(length)
        }
        if (loops.isEmpty()) return mesh to 0
        val added = loops.size
        val extraTriangles = loops.sumOf { if (it.size == 3) 1 else it.size }
        val positions = mesh.positions.copyOf(mesh.positions.size + added * 3)
        val colors = mesh.colors.copyOf(mesh.vertexCount + added)
        val indices = idx.copyOf(idx.size + extraTriangles * 3)
        var vertex = mesh.vertexCount
        var write = idx.size
        for (rim in loops) {
            if (rim.size == 3) {
                // The new triangle runs the rim the other way, as a neighbour would.
                indices[write++] = rim[2]
                indices[write++] = rim[1]
                indices[write++] = rim[0]
                continue
            }
            var x = 0f
            var y = 0f
            var z = 0f
            for (v in rim) {
                x += positions[v * 3]
                y += positions[v * 3 + 1]
                z += positions[v * 3 + 2]
            }
            positions[vertex * 3] = x / rim.size
            positions[vertex * 3 + 1] = y / rim.size
            positions[vertex * 3 + 2] = z / rim.size
            colors[vertex] = averageColor(mesh.colors, rim)
            for (i in rim.indices) {
                val a = rim[i]
                val b = rim[(i + 1) % rim.size]
                indices[write++] = b
                indices[write++] = a
                indices[write++] = vertex
            }
            vertex++
        }
        return WorkMesh(positions, colors, indices) to added
    }

    /** The mean edge length of [mesh]: the scale its filters work at. */
    fun meanEdge(mesh: WorkMesh): Float {
        val p = mesh.positions
        var total = 0.0
        var count = 0
        val step = max(1, mesh.triangleCount / EDGE_SAMPLES)
        var t = 0
        while (t < mesh.triangleCount) {
            for (k in 0 until 3) {
                val a = mesh.indices[t * 3 + k] * 3
                val b = mesh.indices[t * 3 + (k + 1) % 3] * 3
                val dx = p[a] - p[b]
                val dy = p[a + 1] - p[b + 1]
                val dz = p[a + 2] - p[b + 2]
                total += sqrt(dx * dx + dy * dy + dz * dz)
                count++
            }
            t += step
        }
        return if (count == 0) 0f else (total / count).toFloat()
    }

    private fun hasEdge(topology: MeshTopology, idx: IntArray, a: Int, b: Int): Boolean {
        for (i in topology.vertexStart[a] until topology.vertexStart[a + 1]) {
            val t = topology.vertexTriangles[i]
            for (k in 0 until 3) if (idx[t * 3 + k] == a && idx[t * 3 + (k + 1) % 3] == b) return true
        }
        return false
    }

    private fun span(p: FloatArray, loop: IntArray, length: Int): Float {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        for (i in 0 until length) {
            val v = loop[i] * 3
            minX = min(minX, p[v])
            minY = min(minY, p[v + 1])
            minZ = min(minZ, p[v + 2])
            maxX = max(maxX, p[v])
            maxY = max(maxY, p[v + 1])
            maxZ = max(maxZ, p[v + 2])
        }
        val dx = maxX - minX
        val dy = maxY - minY
        val dz = maxZ - minZ
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun averageColor(colors: IntArray, of: IntArray): Int {
        var r = 0
        var g = 0
        var b = 0
        for (v in of) {
            r += colors[v] shr 16 and 0xFF
            g += colors[v] shr 8 and 0xFF
            b += colors[v] and 0xFF
        }
        val n = of.size
        return (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
    }

    private fun sharesVertex(idx: IntArray, t: Int, u: Int): Boolean {
        for (i in 0 until 3) for (j in 0 until 3) if (idx[t * 3 + i] == idx[u * 3 + j]) return true
        return false
    }

    /** Möller–Trumbore: whether the ray from o along d meets triangle [u] within ([minT], [maxT]]. */
    @Suppress("LongParameterList")
    internal fun rayHits(
        p: FloatArray,
        idx: IntArray,
        u: Int,
        ox: Float,
        oy: Float,
        oz: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        maxT: Float,
        minT: Float = 0f,
    ): Boolean {
        val a = idx[u * 3] * 3
        val b = idx[u * 3 + 1] * 3
        val c = idx[u * 3 + 2] * 3
        val e1x = p[b] - p[a]
        val e1y = p[b + 1] - p[a + 1]
        val e1z = p[b + 2] - p[a + 2]
        val e2x = p[c] - p[a]
        val e2y = p[c + 1] - p[a + 1]
        val e2z = p[c + 2] - p[a + 2]
        val hx = dy * e2z - dz * e2y
        val hy = dz * e2x - dx * e2z
        val hz = dx * e2y - dy * e2x
        val det = e1x * hx + e1y * hy + e1z * hz
        if (abs(det) < PARALLEL) return false
        val inv = 1f / det
        val sx = ox - p[a]
        val sy = oy - p[a + 1]
        val sz = oz - p[a + 2]
        val bu = (sx * hx + sy * hy + sz * hz) * inv
        if (bu < 0f || bu > 1f) return false
        val qx = sy * e1z - sz * e1y
        val qy = sz * e1x - sx * e1z
        val qz = sx * e1y - sy * e1x
        val bv = (dx * qx + dy * qy + dz * qz) * inv
        if (bv < 0f || bu + bv > 1f) return false
        val t = (e2x * qx + e2y * qy + e2z * qz) * inv
        return t > minT && t <= maxT
    }

    /** A piece under this share of the largest piece's area is debris. */
    const val MIN_PIECE_SHARE = 0.005f

    /** Doubled sheets further apart than this are two real surfaces, not one meshed twice. */
    const val DOUBLED_GAP_M = 0.12f

    /** Sheets closer than this are one surface's noise folding over itself, not a second pass. */
    const val DOUBLED_MIN_GAP_M = 0.03f

    /** A piece left hanging by the doubled-sheet removal goes when under this share of the largest. */
    const val CUT_PIECE_SHARE = 0.1f
    private const val CUT_PIECE_TRIANGLES_PER_EDGE = 8

    /** A same-facing sheet this few triangle rings away is the surface folding over, not a second pass. */
    private const val FOLD_RINGS = 5

    /** Two sheets face the same way when their normals are within ~35°. */
    const val DOUBLED_MIN_COS = 0.82f

    /** Largest hole closed: a loop of this many edges, ~1.5 cm each... */
    const val MAX_HOLE_EDGES = 40

    /** ...spanning no more than this. */
    const val MAX_HOLE_SPAN_M = 0.25f

    const val SMOOTH_NORMAL_PASSES = 3
    const val SMOOTH_VERTEX_PASSES = 8

    /** Normals further apart than about this (|n − n'|, ~20°) barely blend: the edge keeps. */
    const val SMOOTH_NORMAL_SIGMA = 0.35f

    /** The spatial reach of the normal filter, in mean edges. */
    private const val SMOOTH_SPATIAL_EDGES = 1.5f
    private const val RAY_START_M = 0.002f
    private const val PARALLEL = 1e-12f
    private const val EDGE_SAMPLES = 20_000
}

/** Whether two triangles are within a few rings of each other across shared vertices: a reusable walk. */
internal class RingWalk(triangles: Int) {
    private val stamp = IntArray(triangles)
    private var round = 0
    private var frontier = IntArray(INITIAL)
    private var next = IntArray(INITIAL)

    fun near(topology: MeshTopology, idx: IntArray, from: Int, to: Int, rings: Int): Boolean {
        round++
        var size = 1
        frontier[0] = from
        stamp[from] = round
        repeat(rings) {
            var nextSize = 0
            for (f in 0 until size) {
                val t = frontier[f]
                for (k in 0 until 3) {
                    val v = idx[t * 3 + k]
                    for (i in topology.vertexStart[v] until topology.vertexStart[v + 1]) {
                        val u = topology.vertexTriangles[i]
                        if (stamp[u] == round) continue
                        if (u == to) return true
                        stamp[u] = round
                        if (nextSize == next.size) next = next.copyOf(nextSize * 2)
                        next[nextSize++] = u
                    }
                }
            }
            val swap = frontier
            frontier = next
            next = swap
            size = nextSize
            if (size == 0) return false
        }
        return false
    }

    private companion object {
        const val INITIAL = 256
    }
}

/** The triangles sharing a vertex with a triangle, each once: a reusable collector. */
internal class RingCollector(triangles: Int) {
    private val stamp = IntArray(triangles) { -1 }

    inline fun collect(topology: MeshTopology, idx: IntArray, t: Int, visit: (Int) -> Unit) {
        for (k in 0 until 3) {
            val v = idx[t * 3 + k]
            for (i in topology.vertexStart[v] until topology.vertexStart[v + 1]) {
                val u = topology.vertexTriangles[i]
                if (mark(u, t)) visit(u)
            }
        }
    }

    /** Marks [u] seen for [t]; `false` when it already was. */
    fun mark(u: Int, t: Int): Boolean {
        if (stamp[u] == t) return false
        stamp[u] = t
        return true
    }
}

/**
 * Triangles bucketed into a uniform grid of [cell]-metre cells, for the short rays the doubled-
 * surface test casts. Each triangle is listed in every cell its bounding box touches; a sorted
 * array of (cell, triangle) keys, no hash map.
 */
internal class TriangleGrid(positions: FloatArray, indices: IntArray, private val cell: Float) {
    private val keys: LongArray

    init {
        val n = indices.size / 3
        var entries = LongArray(n * 2)
        var count = 0
        for (t in 0 until n) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE
            for (k in 0 until 3) {
                val v = indices[t * 3 + k] * 3
                minX = min(minX, positions[v])
                minY = min(minY, positions[v + 1])
                minZ = min(minZ, positions[v + 2])
                maxX = max(maxX, positions[v])
                maxY = max(maxY, positions[v + 1])
                maxZ = max(maxZ, positions[v + 2])
            }
            for (cz in cellOf(minZ)..cellOf(maxZ)) {
                for (cy in cellOf(minY)..cellOf(maxY)) {
                    for (cx in cellOf(minX)..cellOf(maxX)) {
                        if (count == entries.size) entries = entries.copyOf(entries.size * 2)
                        entries[count++] = (cellKey(cx, cy, cz) shl TRIANGLE_BITS) or t.toLong()
                    }
                }
            }
        }
        keys = entries.copyOf(count)
        keys.sort()
    }

    /** Whether [test] holds for a triangle in a cell the segment from o along d, [length] long, touches. */
    @Suppress("LongParameterList")
    inline fun anyAlong(
        ox: Float,
        oy: Float,
        oz: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        length: Float,
        test: (Int) -> Boolean,
    ): Boolean {
        val ex = ox + dx * length
        val ey = oy + dy * length
        val ez = oz + dz * length
        for (cz in cellOf(min(oz, ez))..cellOf(max(oz, ez))) {
            for (cy in cellOf(min(oy, ey))..cellOf(max(oy, ey))) {
                for (cx in cellOf(min(ox, ex))..cellOf(max(ox, ex))) {
                    val key = cellKey(cx, cy, cz)
                    var i = firstOf(key)
                    while (i < size && (keyAt(i) ushr TRIANGLE_BITS) == key) {
                        if (test((keyAt(i) and TRIANGLE_MASK).toInt())) return true
                        i++
                    }
                }
            }
        }
        return false
    }

    val size: Int get() = keys.size

    fun keyAt(i: Int): Long = keys[i]

    fun cellOf(v: Float): Int = floor(v / cell).toInt().coerceIn(-CELL_HALF, CELL_HALF - 1)

    fun cellKey(cx: Int, cy: Int, cz: Int): Long =
        ((cx + CELL_HALF).toLong() shl (2 * CELL_BITS)) or
            ((cy + CELL_HALF).toLong() shl CELL_BITS) or
            (cz + CELL_HALF).toLong()

    /** The first entry of cell [key], or where it would be. */
    fun firstOf(key: Long): Int {
        val target = key shl TRIANGLE_BITS
        var lo = 0
        var hi = keys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keys[mid] < target) lo = mid + 1 else hi = mid
        }
        return lo
    }

    companion object {
        const val TRIANGLE_BITS = 23
        const val TRIANGLE_MASK = (1L shl TRIANGLE_BITS) - 1
        const val CELL_BITS = 13
        const val CELL_HALF = 1 shl (CELL_BITS - 1)
    }
}
