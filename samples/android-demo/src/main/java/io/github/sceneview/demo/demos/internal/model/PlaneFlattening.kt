package io.github.sceneview.demo.demos.internal.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/** A plane `normal · p = offset`, unit [nx], [ny], [nz] (+Y up). */
internal data class Plane(val nx: Float, val ny: Float, val nz: Float, val offset: Float) {
    fun distance(x: Float, y: Float, z: Float): Float = nx * x + ny * y + nz * z - offset
}

/** What [PlaneFlattening.flatten] found: the planes, each triangle's plane (-1 for none), and how much it moved. */
internal class Flattening(
    val planes: List<Plane>,
    val facePlane: IntArray,
    val flatArea: Float,
    val totalArea: Float,
    val snappedVertices: Int,
)

/*
 * Makes the room's big flat surfaces — floor, walls, ceiling, worktops — exactly flat. A depth
 * scan meshes a wall as a gentle ripple; the eye reads that as "scanned", not as "a wall". This
 * finds the large near-planar regions of the mesh, fits each one a plane (snapped to gravity
 * when it is within a few degrees of level or plumb), and moves the vertices onto it: onto the
 * plane inside a region, onto the line where two meet, onto the corner where three do. Anything
 * that is not part of a large flat region — a chair, a plant, a lamp — is left as it is.
 */
internal object PlaneFlattening {
    fun flatten(
        mesh: WorkMesh,
        minAreaM2: Float = MIN_PLANE_AREA_M2,
        maxAngleDeg: Float = GROW_ANGLE_DEG,
        maxDistanceM: Float = GROW_DISTANCE_M,
        maxSnapM: Float = MAX_SNAP_M,
    ): Flattening {
        val n = mesh.triangleCount
        val faces = FaceGeometry(mesh.positions, mesh.indices)
        val totalArea = faces.areas.sum()
        if (n == 0) return Flattening(emptyList(), IntArray(0), 0f, 0f, 0)
        val topology = MeshTopology(mesh.vertexCount, mesh.indices)
        val label = grow(topology, faces, cos(Math.toRadians(maxAngleDeg.toDouble())).toFloat(), maxDistanceM)
        var regions = label.max() + 1
        val area = FloatArray(regions)
        for (t in 0 until n) area[label[t]] += faces.areas[t]
        // Small regions are not planes worth flattening: their faces stay free.
        val keepId = IntArray(regions) { -1 }
        var kept = 0
        for (r in 0 until regions) if (area[r] >= minAreaM2) keepId[r] = kept++
        for (t in 0 until n) label[t] = keepId[label[t]]
        regions = kept
        var planes = (0 until regions).map { fit(faces, label, it) }
        // One wall split in two by a cabinet is still one wall: merge regions on the same plane.
        val merged = mergeCoplanar(planes)
        if (merged.second < regions) {
            for (t in 0 until n) if (label[t] >= 0) label[t] = merged.first[label[t]]
            regions = merged.second
            planes = (0 until regions).map { fit(faces, label, it) }
        }
        absorb(topology, faces, label, planes)
        val snapped = snap(mesh, topology, faces, label, planes, maxSnapM)
        var flatArea = 0f
        for (t in 0 until n) if (label[t] >= 0) flatArea += faces.areas[t]
        return Flattening(planes, label, flatArea, totalArea, snapped)
    }

    /** Region growing across edges from the flattest faces first; every face ends in a region. */
    private fun grow(topology: MeshTopology, faces: FaceGeometry, minCos: Float, maxDistance: Float): IntArray {
        val n = faces.count
        val flatness = FloatArray(n)
        for (t in 0 until n) {
            var worst = 1f
            for (k in 0 until 3) {
                val u = topology.neighbours[t * 3 + k]
                worst = if (u < 0) minOf(worst, 0f) else minOf(worst, faces.dot(t, u))
            }
            flatness[t] = worst
        }
        val order = (0 until n).sortedByDescending { flatness[it] }
        val label = IntArray(n) { -1 }
        val queue = IntArray(n)
        var regions = 0
        for (seed in order) {
            if (label[seed] >= 0) continue
            val id = regions++
            var head = 0
            var tail = 0
            queue[tail++] = seed
            label[seed] = id
            var sumNx = 0.0
            var sumNy = 0.0
            var sumNz = 0.0
            var sumCx = 0.0
            var sumCy = 0.0
            var sumCz = 0.0
            var sumA = 0.0
            while (head < tail) {
                val t = queue[head++]
                val a = faces.areas[t].toDouble() + AREA_EPSILON
                sumNx += faces.normals[t * 3] * a
                sumNy += faces.normals[t * 3 + 1] * a
                sumNz += faces.normals[t * 3 + 2] * a
                sumCx += faces.centroids[t * 3] * a
                sumCy += faces.centroids[t * 3 + 1] * a
                sumCz += faces.centroids[t * 3 + 2] * a
                sumA += a
                val length = sqrt(sumNx * sumNx + sumNy * sumNy + sumNz * sumNz)
                if (length <= 0.0) continue
                val nx = (sumNx / length).toFloat()
                val ny = (sumNy / length).toFloat()
                val nz = (sumNz / length).toFloat()
                val cx = (sumCx / sumA).toFloat()
                val cy = (sumCy / sumA).toFloat()
                val cz = (sumCz / sumA).toFloat()
                for (k in 0 until 3) {
                    val u = topology.neighbours[t * 3 + k]
                    if (u < 0 || label[u] >= 0) continue
                    val facing = faces.normals[u * 3] * nx + faces.normals[u * 3 + 1] * ny + faces.normals[u * 3 + 2] * nz
                    if (facing < minCos) continue
                    val off = nx * (faces.centroids[u * 3] - cx) +
                        ny * (faces.centroids[u * 3 + 1] - cy) +
                        nz * (faces.centroids[u * 3 + 2] - cz)
                    if (abs(off) > maxDistance) continue
                    label[u] = id
                    queue[tail++] = u
                }
            }
        }
        return label
    }

    /** The least-squares plane of region [id]'s face centroids, area-weighted, snapped to gravity. */
    private fun fit(faces: FaceGeometry, label: IntArray, id: Int): Plane {
        var a = 0.0
        var cx = 0.0
        var cy = 0.0
        var cz = 0.0
        var mx = 0.0
        var my = 0.0
        var mz = 0.0
        for (t in 0 until faces.count) {
            if (label[t] != id) continue
            val w = faces.areas[t].toDouble() + AREA_EPSILON
            a += w
            cx += faces.centroids[t * 3] * w
            cy += faces.centroids[t * 3 + 1] * w
            cz += faces.centroids[t * 3 + 2] * w
            mx += faces.normals[t * 3] * w
            my += faces.normals[t * 3 + 1] * w
            mz += faces.normals[t * 3 + 2] * w
        }
        cx /= a
        cy /= a
        cz /= a
        val cov = DoubleArray(9)
        for (t in 0 until faces.count) {
            if (label[t] != id) continue
            val w = faces.areas[t].toDouble() + AREA_EPSILON
            val dx = faces.centroids[t * 3] - cx
            val dy = faces.centroids[t * 3 + 1] - cy
            val dz = faces.centroids[t * 3 + 2] - cz
            cov[0] += w * dx * dx
            cov[1] += w * dx * dy
            cov[2] += w * dx * dz
            cov[4] += w * dy * dy
            cov[5] += w * dy * dz
            cov[8] += w * dz * dz
        }
        cov[3] = cov[1]
        cov[6] = cov[2]
        cov[7] = cov[5]
        var normal = smallestEigenvector(cov)
        if (normal[0] * mx + normal[1] * my + normal[2] * mz < 0) normal = doubleArrayOf(-normal[0], -normal[1], -normal[2])
        // Floors, ceilings and worktops are level, walls are plumb: snap the near misses.
        val level = cos(Math.toRadians(GRAVITY_SNAP_DEG))
        val plumb = kotlin.math.sin(Math.toRadians(GRAVITY_SNAP_DEG))
        if (abs(normal[1]) >= level) {
            normal = doubleArrayOf(0.0, if (normal[1] > 0) 1.0 else -1.0, 0.0)
        } else if (abs(normal[1]) <= plumb) {
            val h = sqrt(normal[0] * normal[0] + normal[2] * normal[2])
            normal = doubleArrayOf(normal[0] / h, 0.0, normal[2] / h)
        }
        val offset = normal[0] * cx + normal[1] * cy + normal[2] * cz
        return Plane(normal[0].toFloat(), normal[1].toFloat(), normal[2].toFloat(), offset.toFloat())
    }

    /**
     * Gives the faces no region took — noise spikes inside a wall, its last centimetres before
     * a corner — to the plane next to them when they lie close to it, so the wall flattens
     * whole instead of keeping pock-marks.
     */
    private fun absorb(topology: MeshTopology, faces: FaceGeometry, label: IntArray, planes: List<Plane>) {
        val minCos = cos(Math.toRadians(ABSORB_ANGLE_DEG)).toFloat()
        val queue = IntArray(faces.count)
        var tail = 0
        for (t in 0 until faces.count) if (label[t] >= 0) queue[tail++] = t
        var head = 0
        while (head < tail) {
            val t = queue[head++]
            val plane = planes[label[t]]
            for (k in 0 until 3) {
                val u = topology.neighbours[t * 3 + k]
                if (u < 0 || label[u] >= 0) continue
                val facing = faces.normals[u * 3] * plane.nx + faces.normals[u * 3 + 1] * plane.ny +
                    faces.normals[u * 3 + 2] * plane.nz
                // A face turned over (|facing| near 1, negative) lying in the plane is the scan's noise
                // folding over itself: it belongs to the plane, and unfolds there.
                if (abs(facing) < minCos) continue
                val d = plane.distance(faces.centroids[u * 3], faces.centroids[u * 3 + 1], faces.centroids[u * 3 + 2])
                if (abs(d) > ABSORB_DISTANCE_M) continue
                label[u] = label[t]
                queue[tail++] = u
            }
        }
    }

    /** Groups planes within [MERGE_ANGLE_DEG] and [MERGE_DISTANCE_M]: the new id of each, and the count. */
    private fun mergeCoplanar(planes: List<Plane>): Pair<IntArray, Int> {
        val parent = IntArray(planes.size) { it }
        fun root(i: Int): Int {
            var r = i
            while (parent[r] != r) r = parent[r]
            return r
        }
        val minCos = cos(Math.toRadians(MERGE_ANGLE_DEG)).toFloat()
        for (i in planes.indices) {
            for (j in i + 1 until planes.size) {
                val p = planes[i]
                val q = planes[j]
                val facing = p.nx * q.nx + p.ny * q.ny + p.nz * q.nz
                if (facing < minCos || abs(p.offset - q.offset) > MERGE_DISTANCE_M) continue
                val a = root(i)
                val b = root(j)
                if (a != b) parent[a] = b
            }
        }
        val id = IntArray(planes.size) { -1 }
        val rootId = IntArray(planes.size) { -1 }
        var count = 0
        for (i in planes.indices) {
            val r = root(i)
            if (rootId[r] < 0) rootId[r] = count++
            id[i] = rootId[r]
        }
        return id to count
    }

    /** Moves each vertex of a flat region onto its plane, its two planes' line or three planes' corner. */
    @Suppress("LongParameterList")
    private fun snap(
        mesh: WorkMesh,
        topology: MeshTopology,
        faces: FaceGeometry,
        label: IntArray,
        planes: List<Plane>,
        maxSnap: Float,
    ): Int {
        val p = mesh.positions
        val ids = IntArray(MAX_PLANES_AT_VERTEX)
        val weight = FloatArray(MAX_PLANES_AT_VERTEX)
        // What may slide afterwards: on one plane (1), along two planes' line (2), nothing (0).
        val slide = ByteArray(mesh.vertexCount)
        val slideA = IntArray(mesh.vertexCount)
        val slideB = IntArray(mesh.vertexCount)
        var snapped = 0
        for (v in 0 until mesh.vertexCount) {
            var count = 0
            var free = false
            for (i in topology.vertexStart[v] until topology.vertexStart[v + 1]) {
                val t = topology.vertexTriangles[i]
                val id = label[t]
                if (id < 0) {
                    free = true
                    continue
                }
                var slot = -1
                for (s in 0 until count) if (ids[s] == id) slot = s
                if (slot < 0 && count < MAX_PLANES_AT_VERTEX) {
                    slot = count++
                    ids[slot] = id
                    weight[slot] = 0f
                }
                if (slot >= 0) weight[slot] += faces.areas[t] + AREA_EPSILON.toFloat()
            }
            if (count == 0) continue
            sortByWeight(ids, weight, count)
            val x = p[v * 3]
            val y = p[v * 3 + 1]
            val z = p[v * 3 + 2]
            val lineTarget = if (count == 2) line(planes[ids[0]], planes[ids[1]], x, y, z) else null
            val target = when {
                count >= 3 -> corner(planes[ids[0]], planes[ids[1]], planes[ids[2]])
                    ?: line(planes[ids[0]], planes[ids[1]], x, y, z)
                else -> lineTarget
            } ?: onto(planes[ids[0]], x, y, z)
            val dx = target[0] - x
            val dy = target[1] - y
            val dz = target[2] - z
            if (dx * dx + dy * dy + dz * dz > maxSnap * maxSnap) {
                // Too far for a corner or an edge: the region's own plane is the safe move.
                val fallback = onto(planes[ids[0]], x, y, z)
                val d = planes[ids[0]].distance(x, y, z)
                if (abs(d) > maxSnap) continue
                p[v * 3] = fallback[0]
                p[v * 3 + 1] = fallback[1]
                p[v * 3 + 2] = fallback[2]
            } else {
                p[v * 3] = target[0]
                p[v * 3 + 1] = target[1]
                p[v * 3 + 2] = target[2]
                if (!free && count == 1) {
                    slide[v] = 1
                    slideA[v] = ids[0]
                } else if (!free && count == 2 && lineTarget != null) {
                    slide[v] = 2
                    slideA[v] = ids[0]
                    slideB[v] = ids[1]
                }
            }
            snapped++
        }
        relax(mesh, topology, planes, slide, slideA, slideB)
        return snapped
    }

    /**
     * Projected onto its plane, a noisy surface keeps its noise sideways: where the scan folded
     * over itself, a triangle now lies flipped in the plane — a dark speck, z-fighting its
     * neighbours. A few rounds of in-plane averaging (along the line, for a crease) spread the
     * vertices evenly again and unfold them; corners and the rim of each region hold still.
     */
    @Suppress("LongParameterList")
    private fun relax(
        mesh: WorkMesh,
        topology: MeshTopology,
        planes: List<Plane>,
        slide: ByteArray,
        slideA: IntArray,
        slideB: IntArray,
    ) {
        val p = mesh.positions
        val idx = mesh.indices
        val next = FloatArray(p.size)
        repeat(RELAX_PASSES) {
            for (v in 0 until mesh.vertexCount) {
                if (slide[v].toInt() == 0) continue
                var sx = 0f
                var sy = 0f
                var sz = 0f
                var count = 0
                for (i in topology.vertexStart[v] until topology.vertexStart[v + 1]) {
                    val t = topology.vertexTriangles[i]
                    for (k in 0 until 3) {
                        val u = idx[t * 3 + k]
                        if (u == v) continue
                        // Along a crease, only the crease's own vertices pull.
                        if (slide[v].toInt() == 2 && (slide[u].toInt() == 1)) continue
                        sx += p[u * 3]
                        sy += p[u * 3 + 1]
                        sz += p[u * 3 + 2]
                        count++
                    }
                }
                if (count == 0) {
                    next[v * 3] = p[v * 3]
                    next[v * 3 + 1] = p[v * 3 + 1]
                    next[v * 3 + 2] = p[v * 3 + 2]
                    continue
                }
                val ax = sx / count
                val ay = sy / count
                val az = sz / count
                val target = if (slide[v].toInt() == 1) {
                    onto(planes[slideA[v]], ax, ay, az)
                } else {
                    line(planes[slideA[v]], planes[slideB[v]], ax, ay, az) ?: floatArrayOf(p[v * 3], p[v * 3 + 1], p[v * 3 + 2])
                }
                next[v * 3] = target[0]
                next[v * 3 + 1] = target[1]
                next[v * 3 + 2] = target[2]
            }
            for (v in 0 until mesh.vertexCount) {
                if (slide[v].toInt() == 0) continue
                p[v * 3] = next[v * 3]
                p[v * 3 + 1] = next[v * 3 + 1]
                p[v * 3 + 2] = next[v * 3 + 2]
            }
        }
    }

    private fun sortByWeight(ids: IntArray, weight: FloatArray, count: Int) {
        for (i in 1 until count) {
            var j = i
            while (j > 0 && weight[j] > weight[j - 1]) {
                val w = weight[j]
                weight[j] = weight[j - 1]
                weight[j - 1] = w
                val id = ids[j]
                ids[j] = ids[j - 1]
                ids[j - 1] = id
                j--
            }
        }
    }

    private fun onto(plane: Plane, x: Float, y: Float, z: Float): FloatArray {
        val d = plane.distance(x, y, z)
        return floatArrayOf(x - plane.nx * d, y - plane.ny * d, z - plane.nz * d)
    }

    /** The nearest point to (x, y, z) on the line where [a] and [b] meet; null when they are near parallel. */
    private fun line(a: Plane, b: Plane, x: Float, y: Float, z: Float): FloatArray? {
        // Direction of the line, then the point of the line nearest to p: solve with the third
        // plane through p perpendicular to the line.
        val dx = a.ny * b.nz - a.nz * b.ny
        val dy = a.nz * b.nx - a.nx * b.nz
        val dz = a.nx * b.ny - a.ny * b.nx
        val length = sqrt(dx * dx + dy * dy + dz * dz)
        if (length < MIN_CREASE_SIN) return null
        val c = Plane(dx / length, dy / length, dz / length, (dx * x + dy * y + dz * z) / length)
        return corner(a, b, c)
    }

    /** Where three planes meet (Cramer's rule); null when they do not meet in one point. */
    private fun corner(a: Plane, b: Plane, c: Plane): FloatArray? {
        val det = a.nx * (b.ny * c.nz - b.nz * c.ny) -
            a.ny * (b.nx * c.nz - b.nz * c.nx) +
            a.nz * (b.nx * c.ny - b.ny * c.nx)
        if (abs(det) < MIN_CORNER_DET) return null
        // p = (d1 (b × c) + d2 (c × a) + d3 (a × b)) / det
        val bcX = b.ny * c.nz - b.nz * c.ny
        val bcY = b.nz * c.nx - b.nx * c.nz
        val bcZ = b.nx * c.ny - b.ny * c.nx
        val caX = c.ny * a.nz - c.nz * a.ny
        val caY = c.nz * a.nx - c.nx * a.nz
        val caZ = c.nx * a.ny - c.ny * a.nx
        val abX = a.ny * b.nz - a.nz * b.ny
        val abY = a.nz * b.nx - a.nx * b.nz
        val abZ = a.nx * b.ny - a.ny * b.nx
        val px = (a.offset * bcX + b.offset * caX + c.offset * abX) / det
        val py = (a.offset * bcY + b.offset * caY + c.offset * abY) / det
        val pz = (a.offset * bcZ + b.offset * caZ + c.offset * abZ) / det
        if (!px.isFinite() || !py.isFinite() || !pz.isFinite()) return null
        return floatArrayOf(px, py, pz)
    }

    /** The eigenvector of the smallest eigenvalue of the symmetric 3×3 [m] (row-major), by Jacobi rotations. */
    internal fun smallestEigenvector(m: DoubleArray): DoubleArray {
        val a = m.copyOf()
        val v = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
        repeat(JACOBI_SWEEPS) {
            for ((p, q) in arrayOf(0 to 1, 0 to 2, 1 to 2)) {
                val apq = a[p * 3 + q]
                if (abs(apq) < JACOBI_EPSILON) continue
                val app = a[p * 3 + p]
                val aqq = a[q * 3 + q]
                val theta = (aqq - app) / (2 * apq)
                val t = (if (theta >= 0) 1.0 else -1.0) / (abs(theta) + sqrt(theta * theta + 1))
                val c = 1 / sqrt(t * t + 1)
                val s = t * c
                for (k in 0 until 3) {
                    val akp = a[k * 3 + p]
                    val akq = a[k * 3 + q]
                    a[k * 3 + p] = c * akp - s * akq
                    a[k * 3 + q] = s * akp + c * akq
                }
                for (k in 0 until 3) {
                    val apk = a[p * 3 + k]
                    val aqk = a[q * 3 + k]
                    a[p * 3 + k] = c * apk - s * aqk
                    a[q * 3 + k] = s * apk + c * aqk
                }
                for (k in 0 until 3) {
                    val vkp = v[k * 3 + p]
                    val vkq = v[k * 3 + q]
                    v[k * 3 + p] = c * vkp - s * vkq
                    v[k * 3 + q] = s * vkp + c * vkq
                }
            }
        }
        var best = 0
        for (i in 1 until 3) if (a[i * 3 + i] < a[best * 3 + best]) best = i
        val x = v[best]
        val y = v[3 + best]
        val z = v[6 + best]
        val length = sqrt(x * x + y * y + z * z)
        return doubleArrayOf(x / length, y / length, z / length)
    }

    /** A flat region smaller than this is furniture detail, not a surface to flatten. */
    const val MIN_PLANE_AREA_M2 = 0.25f

    /** A face joins a region when its normal is within this of the region's... */
    const val GROW_ANGLE_DEG = 18f

    /** ...and its centre within this of the region's plane. */
    const val GROW_DISTANCE_M = 0.03f

    /** No vertex moves further than this to reach its plane, edge or corner. */
    const val MAX_SNAP_M = 0.05f

    /** Within this of level or plumb, a plane is made exactly level or plumb. */
    const val GRAVITY_SNAP_DEG = 4.0

    private const val RELAX_PASSES = 12
    private const val ABSORB_ANGLE_DEG = 45.0
    private const val ABSORB_DISTANCE_M = 0.02f
    private const val MERGE_ANGLE_DEG = 5.0
    private const val MERGE_DISTANCE_M = 0.025f
    private const val MAX_PLANES_AT_VERTEX = 6
    private const val MIN_CREASE_SIN = 0.34f // planes meeting at under ~20° do not make a crease
    private const val MIN_CORNER_DET = 0.2f
    private const val AREA_EPSILON = 1e-9
    private const val JACOBI_SWEEPS = 12
    private const val JACOBI_EPSILON = 1e-18
}
