package io.github.sceneview.demo.demos.internal

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * What one [DenseFusion.add] did: voxels created, samples merged into voxels, voxels in all, and
 * of those the [confirmed] ones — seen by at least [DenseFusion.MIN_VIEWS] depth frames, the
 * points a saved scan keeps, so the one figure the scan counts (#4629).
 */
data class DenseFuseStats(val added: Int, val kept: Int, val total: Int, val confirmed: Int)

/**
 * The dense map: samples merged into [voxelM] voxels, deduplicated by a primitive
 * open-addressing hash — a voxel seen again averages its position, colour and normal, each
 * sample weighted by [DenseSamples.weights] (a near, confident, face-on view outweighs a far,
 * doubtful, grazing one) up to [MAX_WEIGHT] full views, so it keeps following a better view, and
 * keeps its best confidence. It also counts the [add] calls — depth frames — that saw it, so
 * [cloud] can leave out a voxel one frame alone saw: the dust a noisy depth leaves in the air.
 *
 * It holds [maxPoints] voxels and never fills up on a scan of a home (#4629): space is cut into
 * [CHUNK_VOXELS]-voxel chunks (64 cm), each at a detail level — level 0 keeps [voxelM] voxels,
 * level L voxels 2^L times as wide. Once the map reaches [COMPACT_SHARE] of [maxPoints], it
 * coarsens by one level the chunks that lose least — broad flat surfaces before detailed ones,
 * fine chunks before coarse ones, the chunks the camera sees now last — merging their voxels,
 * until it is back under [COMPACT_TARGET]. Walls end at 4–8 cm, the furniture stays at 2 cm, and
 * every room scanned stays in. Only with every chunk at [maxLevel] does it drop a new voxel
 * ([saturated]); `maxLevel = 0` is the fixed cap it had before.
 *
 * Voxels keep their insertion order, through a compaction too — a merged voxel takes the place of
 * its first-seen part — so a prefix of the cloud is the map as it stood earlier ([revealCounts]).
 * Single-threaded: one fusion at a time, off the main thread.
 */
@Suppress("TooManyFunctions") // one small step of the voxel bookkeeping each
class DenseFusion(
    val voxelM: Float = VOXEL_M,
    val maxPoints: Int = MAX_POINTS,
    val maxLevel: Int = MAX_LEVEL,
) {
    init {
        require(maxPoints > 0) { "maxPoints $maxPoints" }
        require(maxLevel in 0..LEVEL_LIMIT) { "maxLevel $maxLevel, 0..$LEVEL_LIMIT" }
    }

    var count: Int = 0
        private set

    /** Voxels at least [MIN_VIEWS] depth frames saw: the count of [cloud] with [MIN_VIEWS]. */
    var confirmed: Int = 0
        private set

    /** Times the map coarsened part of itself to make room for more space. */
    var compactions: Int = 0
        private set

    /** Whether the map has dropped a new voxel: every chunk at [maxLevel] and no room left. */
    var saturated: Boolean = false
        private set

    /** The [add] calls so far: the depth frames merged, numbered from 1. */
    val frames: Int get() = view

    private val compactAt = (maxPoints * COMPACT_SHARE).toInt().coerceAtLeast(1)
    private val compactTarget = (maxPoints * COMPACT_TARGET).toInt()

    /** The count at which the next compaction runs: a little past the last one's result. */
    private var nextCompactAt = compactAt

    private var keys = LongArray(INITIAL_SLOTS) { EMPTY }
    private var slots = IntArray(INITIAL_SLOTS)
    private var px = FloatArray(INITIAL_POINTS)
    private var py = FloatArray(INITIAL_POINTS)
    private var pz = FloatArray(INITIAL_POINTS)
    private var nx = FloatArray(INITIAL_POINTS)
    private var ny = FloatArray(INITIAL_POINTS)
    private var nz = FloatArray(INITIAL_POINTS)
    private var r = FloatArray(INITIAL_POINTS)
    private var g = FloatArray(INITIAL_POINTS)
    private var b = FloatArray(INITIAL_POINTS)
    private var weight = FloatArray(INITIAL_POINTS)
    private var colorWeight = FloatArray(INITIAL_POINTS)
    private var confidence = ByteArray(INITIAL_POINTS)
    private var views = IntArray(INITIAL_POINTS)
    private var lastView = IntArray(INITIAL_POINTS)
    private var firstView = IntArray(INITIAL_POINTS)

    /** Each voxel's key ([levelKey]): its cell and its level, what a compaction re-keys. */
    private var voxelKeys = LongArray(INITIAL_POINTS)
    private var chunkOf = IntArray(INITIAL_POINTS)

    // The chunks: their detail level, the last frame that saw them, the voxels they hold.
    private val chunkIndex = LongIntMap()
    private var chunks = 0
    private var chunkLevel = ByteArray(INITIAL_CHUNKS)
    private var chunkSeen = IntArray(INITIAL_CHUNKS)
    private var chunkCount = IntArray(INITIAL_CHUNKS)

    /** The current [add] call, numbered from 1: a voxel's views grow once per call. */
    private var view = 0

    /** Merges [samples] into the map. */
    @Suppress("LoopWithTooManyJumpStatements", "CyclomaticComplexMethod") // one skip per rejected sample
    fun add(samples: DenseSamples): DenseFuseStats {
        view++
        var added = 0
        var kept = 0
        var roomTried = false
        var chunkKey = EMPTY
        var chunk = -1
        for (i in 0 until samples.count) {
            val x = samples.positions[i * 3]
            val y = samples.positions[i * 3 + 1]
            val z = samples.positions[i * 3 + 2]
            if (!x.isFinite() || !y.isFinite() || !z.isFinite()) continue
            val ix = floor(x / voxelM).toInt()
            val iy = floor(y / voxelM).toInt()
            val iz = floor(z / voxelM).toInt()
            val ck = pack(ix shr CHUNK_SHIFT, iy shr CHUNK_SHIFT, iz shr CHUNK_SHIFT)
            if (ck != chunkKey) {
                chunkKey = ck
                chunk = chunkFor(ck)
                chunkSeen[chunk] = view
            }
            var key = levelKey(ix, iy, iz, chunkLevel[chunk].toInt())
            var index = find(key)
            if (index < 0 && count >= maxPoints) {
                // Full mid-frame: make room once, then drop what still does not fit.
                if (!roomTried) {
                    roomTried = true
                    compact()
                    key = levelKey(ix, iy, iz, chunkLevel[chunk].toInt())
                    index = find(key)
                }
                if (index < 0 && count >= maxPoints) {
                    saturated = true
                    continue
                }
            }
            if (index < 0) {
                index = insert(key, chunk)
                added++
            }
            merge(index, x, y, z, samples, i)
            kept++
        }
        if (count >= nextCompactAt) compact()
        return DenseFuseStats(added, kept, count, confirmed)
    }

    /**
     * The map's first [limit] voxels as a cloud — averaged positions, colours, unit normals, and
     * each voxel's detail level once the map has coarsened — leaving out those fewer than
     * [minViews] depth frames saw ([MIN_VIEWS] for a saved scan), and [maxPoints] at most: past
     * it, every n-th voxel, so a capped cloud still covers the whole room — thinner — rather
     * than the corner scanned first.
     */
    fun cloud(limit: Int = count, minViews: Int = 1, maxPoints: Int = Int.MAX_VALUE): DenseCloud {
        val first = limit.coerceIn(0, count)
        val cap = minOf(first, maxPoints.coerceAtLeast(0))
        val stride = if (cap == 0) 1 else (first + cap - 1) / cap
        val kept = IntArray(cap)
        var n = 0
        var i = 0
        while (i < first && n < cap) {
            if (views[i] >= minViews) kept[n++] = i
            i += stride
        }
        val positions = FloatArray(n * 3)
        val normals = FloatArray(n * 3)
        val colors = IntArray(n)
        val confidences = ByteArray(n)
        val levels = if ((0 until n).any { levelOf(voxelKeys[kept[it]]) != 0 }) ByteArray(n) else null
        for (o in 0 until n) {
            val i = kept[o]
            positions[o * 3] = px[i]
            positions[o * 3 + 1] = py[i]
            positions[o * 3 + 2] = pz[i]
            val l = sqrt(nx[i] * nx[i] + ny[i] * ny[i] + nz[i] * nz[i])
            if (l > 1e-6f) {
                normals[o * 3] = nx[i] / l
                normals[o * 3 + 1] = ny[i] / l
                normals[o * 3 + 2] = nz[i] / l
            } else {
                normals[o * 3 + 1] = 1f // opposite views cancelled out: face up
            }
            colors[o] = if (colorWeight[i] == 0f) 0 else {
                (0xFF shl 24) or (channel(r[i]) shl 16) or (channel(g[i]) shl 8) or channel(b[i])
            }
            confidences[o] = confidence[i]
            levels?.set(o, levelOf(voxelKeys[i]).toByte())
        }
        return DenseCloud(positions, colors, normals, confidences, levels)
    }

    /**
     * For each of [atFrames] (ascending [frames] values), how many voxels of [cloud] with
     * [minViews] had been found by that frame: the prefix of the saved cloud a replay shows then.
     * A compaction merges voxels, so a count taken during the scan no longer is that prefix.
     */
    fun revealCounts(atFrames: IntArray, minViews: Int = MIN_VIEWS): IntArray {
        val found = IntArray(count)
        var n = 0
        for (i in 0 until count) if (views[i] >= minViews) found[n++] = firstView[i]
        found.sort(0, n)
        return IntArray(atFrames.size) { upperBound(found, n, atFrames[it]) }
    }

    /** Voxels at each detail level, `[level 0, level 1, …]` up to [maxLevel]: how far it coarsened. */
    fun levelCounts(): IntArray {
        val counts = IntArray(maxLevel + 1)
        for (i in 0 until count) counts[levelOf(voxelKeys[i]).coerceAtMost(maxLevel)]++
        return counts
    }

    private fun merge(index: Int, x: Float, y: Float, z: Float, samples: DenseSamples, i: Int) {
        if (lastView[index] != view) {
            lastView[index] = view
            views[index]++
            if (views[index] == MIN_VIEWS) confirmed++
        }
        val sw = (samples.weights?.get(i) ?: 1f).coerceAtLeast(MIN_SAMPLE_WEIGHT)
        // A running weighted mean; past MAX_WEIGHT a new sample still moves it by sw / MAX_WEIGHT.
        val w = minOf(weight[index] + sw, MAX_WEIGHT)
        weight[index] = w
        val k = sw / w
        px[index] += (x - px[index]) * k
        py[index] += (y - py[index]) * k
        pz[index] += (z - pz[index]) * k
        nx[index] += samples.normals[i * 3] * sw
        ny[index] += samples.normals[i * 3 + 1] * sw
        nz[index] += samples.normals[i * 3 + 2] * sw
        boundNormal(index)
        val c = samples.colors[i]
        if (c != 0) {
            val cw = minOf(colorWeight[index] + sw, MAX_WEIGHT)
            colorWeight[index] = cw
            val ck = sw / cw
            r[index] += ((c shr 16 and 0xFF) - r[index]) * ck
            g[index] += ((c shr 8 and 0xFF) - g[index]) * ck
            b[index] += ((c and 0xFF) - b[index]) * ck
        }
        val conf = samples.confidences[i].toInt() and 0xFF
        if (conf > (confidence[index].toInt() and 0xFF)) confidence[index] = conf.toByte()
    }

    /** Keeps the summed normal bounded so a long-seen voxel still turns with new views. */
    private fun boundNormal(index: Int) {
        val l = abs(nx[index]) + abs(ny[index]) + abs(nz[index])
        if (l > MAX_WEIGHT) {
            val s = MAX_WEIGHT / l
            nx[index] *= s
            ny[index] *= s
            nz[index] *= s
        }
    }

    /**
     * Makes room: coarsens chunks ([coarsen]) and merges their voxels ([rebuild]) until the map
     * is back under [compactTarget], or nothing is left to coarsen.
     */
    private fun compact() {
        val before = count
        var rounds = 0
        while (count > compactTarget && rounds < MAX_ROUNDS && coarsen(count - compactTarget)) {
            rebuild()
            rounds++
        }
        if (count < before) compactions++
        // Not back under the target (all coarse already, or all in view): wait for real growth.
        nextCompactAt = maxOf(compactAt, minOf(count + maxPoints / RETRY_DIVISOR, maxPoints))
    }

    /**
     * Raises the level of the chunks that lose least until the voxels they hold should shrink by
     * [need]; `false` when no chunk can go coarser. A chunk's cost: its level, how far its
     * normals disagree (an edge, a chair, a plant — detail), and whether the camera sees it now.
     */
    private fun coarsen(need: Int): Boolean {
        val candidates = (0 until chunks).filter { chunkLevel[it] < maxLevel && chunkCount[it] >= MIN_CHUNK_VOXELS }
        if (candidates.isEmpty()) return false
        val sx = FloatArray(chunks)
        val sy = FloatArray(chunks)
        val sz = FloatArray(chunks)
        for (i in 0 until count) {
            val l = sqrt(nx[i] * nx[i] + ny[i] * ny[i] + nz[i] * nz[i])
            if (l <= 1e-6f) continue
            val c = chunkOf[i]
            sx[c] += nx[i] / l
            sy[c] += ny[i] / l
            sz[c] += nz[i] / l
        }
        val cost = FloatArray(chunks)
        for (c in candidates) {
            val flatness = sqrt(sx[c] * sx[c] + sy[c] * sy[c] + sz[c] * sz[c]) / chunkCount[c]
            val inView = view - chunkSeen[c] < RECENT_FRAMES
            cost[c] = chunkLevel[c] * LEVEL_COST + (1f - flatness) * DETAIL_COST + if (inView) RECENT_COST else 0f
        }
        var gain = 0f
        for (c in candidates.sortedWith(compareBy<Int> { cost[it] }.thenByDescending { chunkCount[it] })) {
            if (gain >= need) break
            chunkLevel[c] = (chunkLevel[c] + 1).toByte()
            gain += chunkCount[c] * EXPECTED_SHRINK
        }
        return true
    }

    /**
     * Re-keys every voxel at its chunk's level and merges those that now share a cell, in place
     * and in order: a merged voxel stays where its first-seen part was.
     */
    private fun rebuild() {
        keys.fill(EMPTY)
        chunkCount.fill(0, 0, chunks)
        var w = 0
        var nowConfirmed = 0
        for (i in 0 until count) {
            val c = chunkOf[i]
            val key = coarser(voxelKeys[i], chunkLevel[c].toInt())
            val j = find(key)
            if (j >= 0) {
                val wasConfirmed = views[j] >= MIN_VIEWS
                absorb(j, i)
                if (!wasConfirmed && views[j] >= MIN_VIEWS) nowConfirmed++
                continue
            }
            if (w != i) move(i, w)
            voxelKeys[w] = key
            place(key, w)
            chunkCount[c]++
            if (views[w] >= MIN_VIEWS) nowConfirmed++
            w++
        }
        count = w
        confirmed = nowConfirmed
    }

    /** Merges voxel [i] into voxel [j], weighted the way [merge] weighs samples. */
    private fun absorb(j: Int, i: Int) {
        val ws = weight[j] + weight[i]
        if (ws > 0f) {
            val k = weight[i] / ws
            px[j] += (px[i] - px[j]) * k
            py[j] += (py[i] - py[j]) * k
            pz[j] += (pz[i] - pz[j]) * k
        }
        weight[j] = minOf(ws, MAX_WEIGHT)
        nx[j] += nx[i]
        ny[j] += ny[i]
        nz[j] += nz[i]
        boundNormal(j)
        val cs = colorWeight[j] + colorWeight[i]
        if (colorWeight[i] > 0f) {
            val k = colorWeight[i] / cs
            r[j] += (r[i] - r[j]) * k
            g[j] += (g[i] - g[j]) * k
            b[j] += (b[i] - b[j]) * k
        }
        colorWeight[j] = minOf(cs, MAX_WEIGHT)
        if ((confidence[i].toInt() and 0xFF) > (confidence[j].toInt() and 0xFF)) confidence[j] = confidence[i]
        // Below MIN_VIEWS each side is one frame: equal last ids are the same observation,
        // different ids make the merged voxel confirmed. Past it, keep the known bound instead
        // of guessing how much the older frame histories overlap.
        views[j] = if (views[j] < MIN_VIEWS && views[i] < MIN_VIEWS && lastView[j] != lastView[i]) {
            views[j] + views[i]
        } else {
            maxOf(views[j], views[i])
        }
        lastView[j] = maxOf(lastView[j], lastView[i])
        firstView[j] = minOf(firstView[j], firstView[i])
    }

    private fun move(from: Int, to: Int) {
        px[to] = px[from]; py[to] = py[from]; pz[to] = pz[from]
        nx[to] = nx[from]; ny[to] = ny[from]; nz[to] = nz[from]
        r[to] = r[from]; g[to] = g[from]; b[to] = b[from]
        weight[to] = weight[from]
        colorWeight[to] = colorWeight[from]
        confidence[to] = confidence[from]
        views[to] = views[from]
        lastView[to] = lastView[from]
        firstView[to] = firstView[from]
        chunkOf[to] = chunkOf[from]
    }

    private fun chunkFor(key: Long): Int {
        val known = chunkIndex.get(key)
        if (known >= 0) return known
        if (chunks == chunkLevel.size) {
            val size = chunks * 2
            chunkLevel = chunkLevel.copyOf(size)
            chunkSeen = chunkSeen.copyOf(size)
            chunkCount = chunkCount.copyOf(size)
        }
        chunkIndex.put(key, chunks)
        return chunks++
    }

    private fun find(key: Long): Int {
        val mask = keys.size - 1
        var slot = mix(key) and mask
        while (true) {
            val k = keys[slot]
            if (k == EMPTY) return -1
            if (k == key) return slots[slot]
            slot = (slot + 1) and mask
        }
    }

    /** Hashes voxel [index] under [key]; the table has room, [insert] saw to it. */
    private fun place(key: Long, index: Int) {
        val mask = keys.size - 1
        var slot = mix(key) and mask
        while (keys[slot] != EMPTY) slot = (slot + 1) and mask
        keys[slot] = key
        slots[slot] = index
    }

    private fun insert(key: Long, chunk: Int): Int {
        if ((count + 1) * 2 > keys.size) rehash(keys.size * 2)
        if (count == px.size) grow(minOf(px.size * 2, maxOf(maxPoints, px.size + 1)))
        place(key, count)
        voxelKeys[count] = key
        chunkOf[count] = chunk
        chunkCount[chunk]++
        weight[count] = 0f
        colorWeight[count] = 0f
        confidence[count] = 0
        views[count] = 0
        lastView[count] = 0
        firstView[count] = view
        px[count] = 0f; py[count] = 0f; pz[count] = 0f
        nx[count] = 0f; ny[count] = 0f; nz[count] = 0f
        r[count] = 0f; g[count] = 0f; b[count] = 0f
        return count++
    }

    private fun rehash(size: Int) {
        val oldKeys = keys
        val oldSlots = slots
        keys = LongArray(size) { EMPTY }
        slots = IntArray(size)
        for (s in oldKeys.indices) {
            if (oldKeys[s] != EMPTY) place(oldKeys[s], oldSlots[s])
        }
    }

    private fun grow(size: Int) {
        px = px.copyOf(size); py = py.copyOf(size); pz = pz.copyOf(size)
        nx = nx.copyOf(size); ny = ny.copyOf(size); nz = nz.copyOf(size)
        r = r.copyOf(size); g = g.copyOf(size); b = b.copyOf(size)
        weight = weight.copyOf(size)
        colorWeight = colorWeight.copyOf(size)
        confidence = confidence.copyOf(size)
        views = views.copyOf(size)
        lastView = lastView.copyOf(size)
        firstView = firstView.copyOf(size)
        voxelKeys = voxelKeys.copyOf(size)
        chunkOf = chunkOf.copyOf(size)
    }

    companion object {
        /** The design's surfel size: 2 cm. */
        const val VOXEL_M = 0.02f

        /** 500 k surfels: a room at full detail, 6 MB of SVPC. A phone gets [budgetFor] its heap. */
        const val MAX_POINTS = 500_000

        /** Detail levels a chunk may coarsen to: 2, 4, then 8 cm voxels. */
        const val MAX_LEVEL = 2

        /** The most levels a key holds: 2 cm × 2⁷. */
        const val LEVEL_LIMIT = 7

        /** A chunk is 32 voxels a side at level 0: 64 cm. */
        const val CHUNK_VOXELS = 32

        /** The share of [maxPoints] at which the map starts to coarsen. */
        const val COMPACT_SHARE = 0.8f

        /** The share of [maxPoints] a compaction brings the map back under. */
        const val COMPACT_TARGET = 0.65f

        /** A voxel's weight stops growing at this many full-weight views. */
        const val MAX_WEIGHT = 32f

        /**
         * Depth frames that must see a voxel for a saved scan to keep it: a voxel only one frame
         * saw is most often that frame's noise.
         */
        const val MIN_VIEWS = 2

        /** The fewest voxels [budgetFor] gives, and the most. */
        const val MIN_BUDGET = 250_000
        const val MAX_BUDGET = 600_000

        /**
         * Heap bytes per voxel of budget: a voxel takes ~100 bytes with its hash slots, so the map
         * may grow to a fifth of the app's heap.
         */
        const val HEAP_BYTES_PER_POINT = 512L

        /**
         * The voxels a map gets on a phone whose app heap is [maxHeapBytes]
         * (`Runtime.maxMemory()`): a fifth of it, within [MIN_BUDGET]..[MAX_BUDGET].
         */
        fun budgetFor(maxHeapBytes: Long): Int =
            (maxHeapBytes / HEAP_BYTES_PER_POINT).coerceIn(MIN_BUDGET.toLong(), MAX_BUDGET.toLong()).toInt()

        private const val CHUNK_SHIFT = 5
        private const val LEVEL_SHIFT = 60
        private const val AXIS_BITS = 20
        private const val AXIS_MASK = (1L shl AXIS_BITS) - 1
        private const val SIGN_SHIFT = Int.SIZE_BITS - AXIS_BITS

        // What a chunk costs to coarsen ([coarsen]): per level it has, for its detail, in view.
        private const val LEVEL_COST = 1f
        private const val DETAIL_COST = 2f
        private const val RECENT_COST = 4f

        /** Frames (~2 s of depth) a chunk counts as in view after the last one that saw it. */
        private const val RECENT_FRAMES = 20

        /** A chunk this sparse is not worth coarsening: it frees nothing. */
        private const val MIN_CHUNK_VOXELS = 8

        /** The share of a chunk's voxels one level frees on a surface: 4 cells become 1. */
        private const val EXPECTED_SHRINK = 0.7f
        private const val MAX_ROUNDS = 4

        /** After a compaction, the next waits for 5 % of [maxPoints] of growth. */
        private const val RETRY_DIVISOR = 20

        /** The least a sample counts, so a weight never divides by zero. */
        private const val MIN_SAMPLE_WEIGHT = 1e-4f

        private const val INITIAL_SLOTS = 1 shl 14
        private const val INITIAL_POINTS = 1 shl 13
        private const val INITIAL_CHUNKS = 1 shl 8

        /** Never a key: [levelKey] leaves the sign bit clear. */
        private const val EMPTY = -1L

        /** Cells ([a], [b], [c]) on 20 bits each: ±10 km of 2 cm voxels. */
        private fun pack(a: Int, b: Int, c: Int): Long =
            ((a.toLong() and AXIS_MASK) shl (2 * AXIS_BITS)) or ((b.toLong() and AXIS_MASK) shl AXIS_BITS) or
                (c.toLong() and AXIS_MASK)

        /** The voxel at [level] holding level-0 cell ([ix], [iy], [iz]). */
        private fun levelKey(ix: Int, iy: Int, iz: Int, level: Int): Long =
            pack(ix shr level, iy shr level, iz shr level) or (level.toLong() shl LEVEL_SHIFT)

        private fun levelOf(key: Long): Int = (key ushr LEVEL_SHIFT).toInt()

        /** [key]'s voxel at [level], its parent when that is coarser; [key] itself otherwise. */
        private fun coarser(key: Long, level: Int): Long {
            val from = levelOf(key)
            if (level <= from) return key
            val s = level - from
            return pack(axis(key, 2 * AXIS_BITS) shr s, axis(key, AXIS_BITS) shr s, axis(key, 0) shr s) or
                (level.toLong() shl LEVEL_SHIFT)
        }

        /** The signed 20-bit cell at bit [shift] of [key]. */
        private fun axis(key: Long, shift: Int): Int =
            ((key ushr shift) and AXIS_MASK).toInt() shl SIGN_SHIFT shr SIGN_SHIFT

        private fun upperBound(sorted: IntArray, n: Int, value: Int): Int {
            var lo = 0
            var hi = n
            while (lo < hi) {
                val mid = (lo + hi) ushr 1
                if (sorted[mid] <= value) lo = mid + 1 else hi = mid
            }
            return lo
        }

        private fun mix(key: Long): Int {
            var h = key * -0x61c8864680b583ebL
            h = h xor (h ushr 29)
            return h.toInt() and Int.MAX_VALUE
        }

        private fun channel(v: Float) = (v + 0.5f).toInt().coerceIn(0, 255)
    }
}
