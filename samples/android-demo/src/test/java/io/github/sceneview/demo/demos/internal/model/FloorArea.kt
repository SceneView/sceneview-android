package io.github.sceneview.demo.demos.internal.model

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * A model's floor as the eye reads it: how much of it there is, and how many holes it has.
 *
 * The floor is the faces turned up ([MIN_UP] of their normal along +y) whose centroid lies
 * within [BAND_M] of the floor's height — given, or the height most of the upward area lies at
 * within [SEARCH_M] of the lowest ([height]). Its [area] is theirs projected onto the floor, so a
 * floor rippled by the scan's noise and the same floor made flat weigh the same. Its holes are
 * the loops of its open edges, vertices welded within [WELD_M], less one — the rim around the
 * rest: [innerLoops].
 */
internal class FloorArea private constructor(val height: Float, val area: Float, val innerLoops: Int) {
    companion object {
        /** The floor of the triangle soup [positions] / [indices], at [height] when known. */
        fun of(positions: FloatArray, indices: IntArray, height: Float? = null): FloorArea {
            val count = indices.size / 3
            val up = FloatArray(count)
            val centreY = FloatArray(count)
            val area = FloatArray(count)
            for (t in 0 until count) {
                val a = indices[t * 3] * 3
                val b = indices[t * 3 + 1] * 3
                val c = indices[t * 3 + 2] * 3
                val ux = positions[b] - positions[a]
                val uy = positions[b + 1] - positions[a + 1]
                val uz = positions[b + 2] - positions[a + 2]
                val vx = positions[c] - positions[a]
                val vy = positions[c + 1] - positions[a + 1]
                val vz = positions[c + 2] - positions[a + 2]
                val nx = uy * vz - uz * vy
                val ny = uz * vx - ux * vz
                val nz = ux * vy - uy * vx
                val length = sqrt(nx * nx + ny * ny + nz * nz)
                if (length <= 0f) continue
                area[t] = length / 2f
                up[t] = ny / length
                centreY[t] = (positions[a + 1] + positions[b + 1] + positions[c + 1]) / 3f
            }
            val floorY = height ?: height(up, centreY, area) ?: return FloorArea(Float.NaN, 0f, 0)
            val floor = BooleanArray(count) { up[it] >= MIN_UP && abs(centreY[it] - floorY) <= BAND_M }
            var projected = 0.0
            for (t in 0 until count) if (floor[t]) projected += area[t] * up[t]
            return FloorArea(floorY, projected.toFloat(), innerLoops(positions, indices, floor))
        }

        /**
         * The height the most upward area lies at, in [BIN_M] bins, within [SEARCH_M] of the
         * lowest bin holding [MIN_LOWEST_SHARE] of it: the floor, not a stray under it nor a
         * table on it. `null` with nothing turned up.
         */
        private fun height(up: FloatArray, centreY: FloatArray, area: FloatArray): Float? {
            val bins = HashMap<Long, Double>()
            var total = 0.0
            for (t in up.indices) {
                if (up[t] < MIN_UP) continue
                val bin = (centreY[t] / BIN_M).roundToLong()
                bins[bin] = (bins[bin] ?: 0.0) + area[t]
                total += area[t]
            }
            if (total <= 0.0) return null
            val sorted = bins.keys.sorted()
            val lowest = sorted.first { bins.getValue(it) >= total * MIN_LOWEST_SHARE }
            val reach = (SEARCH_M / BIN_M).toLong()
            val floorBin = sorted.filter { it in lowest..lowest + reach }.maxByOrNull { bins.getValue(it) } ?: lowest
            return floorBin * BIN_M
        }

        /** The loops of the open edges of the faces [kept], less one: the rim. */
        private fun innerLoops(positions: FloatArray, indices: IntArray, kept: BooleanArray): Int {
            // Welded ids: a model split at its seams has its vertices twice in the same place.
            val weld = HashMap<Triple<Long, Long, Long>, Int>()
            val id = IntArray(positions.size / 3) { -1 }
            fun welded(v: Int): Int {
                if (id[v] >= 0) return id[v]
                val key = Triple(
                    (positions[v * 3] / WELD_M).roundToLong(),
                    (positions[v * 3 + 1] / WELD_M).roundToLong(),
                    (positions[v * 3 + 2] / WELD_M).roundToLong(),
                )
                return weld.getOrPut(key) { weld.size }.also { id[v] = it }
            }
            val uses = HashMap<Long, Int>()
            for (t in kept.indices) {
                if (!kept[t]) continue
                for (k in 0 until 3) {
                    val a = welded(indices[t * 3 + k])
                    val b = welded(indices[t * 3 + (k + 1) % 3])
                    if (a == b) continue
                    val key = (minOf(a, b).toLong() shl Int.SIZE_BITS) or maxOf(a, b).toLong()
                    uses[key] = (uses[key] ?: 0) + 1
                }
            }
            val parent = IntArray(weld.size) { it }
            fun root(v: Int): Int {
                var r = v
                while (parent[r] != r) {
                    parent[r] = parent[parent[r]]
                    r = parent[r]
                }
                return r
            }
            val open = uses.filterValues { it == 1 }.keys
            for (key in open) {
                val a = root((key shr Int.SIZE_BITS).toInt())
                val b = root((key and LOW_INT).toInt())
                if (a != b) parent[a] = b
            }
            val edges = HashMap<Int, Int>()
            for (key in open) {
                val r = root((key shr Int.SIZE_BITS).toInt())
                edges[r] = (edges[r] ?: 0) + 1
            }
            return (edges.size - 1).coerceAtLeast(0)
        }

        /** cos 60°: a face of the floor rippled by the scan still faces up. */
        const val MIN_UP = 0.5f
        const val BAND_M = 0.05f
        const val BIN_M = 0.01f
        const val SEARCH_M = 0.5f
        const val MIN_LOWEST_SHARE = 0.01f
        const val WELD_M = 0.001f
        private const val LOW_INT = 0xFFFFFFFFL
    }
}
