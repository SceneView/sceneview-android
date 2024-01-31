package io.github.sceneview.triangulation

import dev.romainguy.kotlin.math.Float2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Kotlin Multiplatform Port of [Earcut](https://github.com/mapbox/earcut)
 *
 * Copyright [earcut-kotlin-multiplatform](https://github.com/Monkey-Maestro/earcut-kotlin-multiplatform)
 *
 * Thanks [Monkey-Maestro](https://github.com/Monkey-Maestro)
 */
object Earcut {

    class Input(val vertices: DoubleArray, val holeIndices: IntArray, val dimensions: Int)

    class VertexIndex(var i: Int, var x: Double, var y: Double) {
        // i = vertex index in coordinates array
        // x, y = vertex coordinates

        // previous and next vertex nodes in a polygon ring
        lateinit var prev: VertexIndex
        lateinit var next: VertexIndex

        // z-order curve value
        var z: Double = 0.0

        // previous and next nodes in z-order
        var nextZ: VertexIndex? = null
        var prevZ: VertexIndex? = null

        // indicates whether this is a steiner point
        var steiner: Boolean = false
    }

    val xComparator = Comparator { a: VertexIndex, b: VertexIndex -> compareValues(a.x, b.x) }

    // lays any 3Dimensional planar polygon out on xy plane
    fun toXY(data: DoubleArray): DoubleArray {
        val normal = normal(data)
        val anyToXYTransform = AnyToXYTransform(normal[0], normal[1], normal[2])
        val result = data.copyOf()
        anyToXYTransform.transform(result)
        return result
    }

    fun triangulate(input: Input): List<Int> {
        return triangulate(input.vertices, input.holeIndices, input.dimensions)
    }

    fun triangulate(path: List<Float2>, holeIndices: List<Int> = listOf()) = triangulate(
        path.flatMap { listOf(it.x.toDouble(), it.y.toDouble()) }.toDoubleArray(),
        holeIndices.toIntArray()
    )

    /**
     * Triangulate a polygon with 3d coordinates with optional hole
     *
     * @param path flat array of vertex coordinates like [x0,y0, x1,y1, x2,y2, ...].
     * @param holeIndices An array of hole indices if any (e.g. [5, 8] for a 12-vertex input would
     * mean one hole with vertices 5–7 and another with 8–11).
     * @param dimensions is the number of coordinates per vertex in the input array (2 by default).
     * Only two are used for triangulation (x and y), and the rest are ignored.
     *
     * @return Each group of three vertex indices in the resulting array forms a triangle.
     * e.g. [1,0,3, 3,2,1]
     */
    fun triangulate(
        path: DoubleArray,
        holeIndices: IntArray = intArrayOf(),
        dimensions: Int = 2
    ): List<Int> {
        val hasHoles = holeIndices.isNotEmpty()
        val outerLen = if (hasHoles) holeIndices[0] * dimensions else path.size
        var outerVertexIndex: VertexIndex? = linkedList(path, 0, outerLen, dimensions, true)
        val triangles: MutableList<Int> = mutableListOf()

        if (outerVertexIndex == null || outerVertexIndex.next === outerVertexIndex.prev) return triangles

        var minX = 0.0
        var minY = 0.0
        var maxX: Double
        var maxY: Double

        var x: Double
        var y: Double

        var invSize = 0.0

        if (hasHoles) outerVertexIndex =
            eliminateHoles(path, holeIndices, outerVertexIndex, dimensions)

        // if the shape is not too simple, we'll use z-order curve hash later; calculate polygon bbox
        if (path.size > 80 * dimensions) {
            maxX = path[0]
            minX = path[0]
            maxY = path[1]
            minY = path[1]

            for (i in dimensions until outerLen step dimensions) {
                x = path[i]
                y = path[i + 1]
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }

            // minX, minY and invSize are later used to transform coords into integers for z-order calculation
            invSize = max(maxX - minX, maxY - minY)
            invSize = if (invSize != 0.0) 32767 / invSize else 0.0
        }

        earcutLinked(outerVertexIndex, triangles, dimensions, minX, minY, invSize, 0)

        return triangles
    }

    private fun normal(vertices: DoubleArray): DoubleArray {
        val ccw = true // counterclockwise normal direction

        val points = arrayListOf<Point3>()
        for (i in 0 until (vertices.size / 3)) {
            points.add(Point3(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2]))
        }

        var m3: Point3 = points[points.size - 2]
        var m2 = points[points.size - 1]

        var c123 = Point3()
        var v32: Point3
        var v12: Point3

        for (i in points.indices) {
            val m1 = points[i]

            v32 = m3 - m2
            v12 = m1 - m2

            c123 = if (!ccw) {
                c123 + v32.cross(v12)
            } else {
                c123 + v12.cross(v32)
            }

            m3 = m2
            m2 = m1
        }

        c123.normalize()

        return doubleArrayOf(c123.x, c123.y, c123.z)
    }

    // create a circular doubly linked list from polygon points in the specified winding order
    private fun linkedList(
        data: DoubleArray,
        start: Int,
        end: Int,
        dim: Int,
        clockwise: Boolean
    ): VertexIndex? {
        var last: VertexIndex? = null

        if (clockwise == (signedArea(data, start, end, dim) > 0)) {
            for (i in start until end step dim) {
                last = insertNode(i, data[i], data[i + 1], last)
            }
        } else {
            for (i in (end - dim) downTo start step dim) {
                last = insertNode(i, data[i], data[i + 1], last)
            }
        }

        if (last != null && equals(last, last.next)) {
            removeNode(last)
            last = last.next
        }

        return last
    }

    // eliminate collinear or duplicate points
    private fun filterPoints(start: VertexIndex?, _end: VertexIndex?): VertexIndex? {
        var end: VertexIndex? = _end

        if (start == null) return start
        if (end == null) end = start

        var p: VertexIndex = start
        var again: Boolean

        do {
            again = false

            if (!p.steiner && (equals(p, p.next) || area(p.prev, p, p.next) == 0.0)) {
                removeNode(p)
                p = p.prev
                end = p.prev
                if (p === p.next) break
                again = true
            } else {
                p = p.next
            }
        } while (again || p !== end)

        return end
    }

    // main ear slicing loop which triangulates a polygon (given as a linked list)
    private fun earcutLinked(
        _ear: VertexIndex?,
        triangles: MutableList<Int>,
        dim: Int,
        minX: Double,
        minY: Double,
        invSize: Double,
        pass: Int
    ) {
        var ear: VertexIndex = _ear ?: return

        // interlink polygon nodes in z-order
        if (pass == 0 && invSize != 0.0) indexCurve(ear, minX, minY, invSize)

        var stop: VertexIndex = ear
        var prev: VertexIndex
        var next: VertexIndex

        // iterate through ears, slicing them one by one
        while (ear.prev !== ear.next) {
            prev = ear.prev
            next = ear.next

            if (if (invSize != 0.0) isEarHashed(ear, minX, minY, invSize) else isEar(ear)) {
                // cut off the triangle
                triangles.add(prev.i / dim or 0)
                triangles.add(ear.i / dim or 0)
                triangles.add(next.i / dim or 0)

                removeNode(ear)

                // skipping the next vertex leads to less sliver triangles
                ear = next.next
                stop = next.next

                continue
            }

            ear = next

            // if we looped through the whole remaining polygon and can't find any more ears
            if (ear === stop) {
                when (pass) {
                    // try filtering points and slicing again
                    0 -> {
                        earcutLinked(
                            filterPoints(ear, null),
                            triangles,
                            dim,
                            minX,
                            minY,
                            invSize,
                            1
                        )
                    }
                    // if this didn't work, try curing all small self-intersections locally
                    1 -> {
                        ear = cureLocalIntersections(filterPoints(ear, null)!!, triangles, dim)!!
                        earcutLinked(ear, triangles, dim, minX, minY, invSize, 2)
                    }
                    // as a last resort, try splitting the remaining polygon into two
                    2 -> {
                        splitEarcut(ear, triangles, dim, minX, minY, invSize)
                    }
                }

                break
            }
        }
    }

    // check whether a polygon node forms a valid ear with adjacent nodes
    private fun isEar(ear: VertexIndex): Boolean {
        val a: VertexIndex = ear.prev
        val b: VertexIndex = ear
        val c: VertexIndex = ear.next

        if (area(a, b, c) >= 0) return false // reflex, can't be an ear

        val ax = a.x
        val bx = b.x
        val cx = c.x
        val ay = a.y
        val by = b.y
        val cy = c.y

        // triangle bbox; min & max are calculated like this for speed
        val x0 = if (ax < bx) (if (ax < cx) ax else cx) else if (bx < cx) bx else cx
        val y0 = if (ay < by) (if (ay < cy) ay else cy) else if (by < cy) by else cy
        val x1 = if (ax > bx) (if (ax > cx) ax else cx) else if (bx > cx) bx else cx
        val y1 = if (ay > by) (if (ay > cy) ay else cy) else if (by > cy) by else cy

        // now make sure we don't have other points inside the potential ear
        var p = c.next
        while (p !== a) {
            if (p.x >= x0 && p.x <= x1 && p.y >= y0 && p.y <= y1 &&
                pointInTriangle(ax, ay, bx, by, cx, cy, p.x, p.y) &&
                area(p.prev, p, p.next) >= 0
            ) {
                return false
            }
            p = p.next
        }

        return true
    }

    private fun isEarHashed(
        ear: VertexIndex,
        minX: Double,
        minY: Double,
        invSize: Double
    ): Boolean {
        val a: VertexIndex = ear.prev
        val b: VertexIndex = ear
        val c: VertexIndex = ear.next

        if (area(a, b, c) >= 0) return false // reflex, can't be an ear

        val ax = a.x
        val bx = b.x
        val cx = c.x
        val ay = a.y
        val by = b.y
        val cy = c.y

        // triangle bbox; min & max are calculated like this for speed
        val x0 = if (ax < bx) (if (ax < cx) ax else cx) else if (bx < cx) bx else cx
        val y0 = if (ay < by) (if (ay < cy) ay else cy) else if (by < cy) by else cy
        val x1 = if (ax > bx) (if (ax > cx) ax else cx) else if (bx > cx) bx else cx
        val y1 = if (ay > by) (if (ay > cy) ay else cy) else if (by > cy) by else cy

        // z-order range for the current triangle bbox;
        val minZ = zOrder(x0, y0, minX, minY, invSize)
        val maxZ = zOrder(x1, y1, minX, minY, invSize)

        var p = ear.prevZ
        var n = ear.nextZ

        // look for points inside the triangle in both directions
        while (p != null && p.z >= minZ && n != null && n.z <= maxZ) {
            if (p.x >= x0 && p.x <= x1 && p.y >= y0 && p.y <= y1 && p !== a && p !== c &&
                pointInTriangle(ax, ay, bx, by, cx, cy, p.x, p.y) && area(p.prev, p, p.next) >= 0
            ) {
                return false
            }
            p = p.prevZ

            if (n.x >= x0 && n.x <= x1 && n.y >= y0 && n.y <= y1 && n !== a && n !== c &&
                pointInTriangle(ax, ay, bx, by, cx, cy, n.x, n.y) && area(n.prev, n, n.next) >= 0
            ) {
                return false
            }
            n = n.nextZ
        }

        // look for remaining points in decreasing z-order
        while (p != null && p.z >= minZ) {
            if (p.x >= x0 && p.x <= x1 && p.y >= y0 && p.y <= y1 && p !== a && p !== c &&
                pointInTriangle(ax, ay, bx, by, cx, cy, p.x, p.y) && area(p.prev, p, p.next) >= 0
            ) {
                return false
            }
            p = p.prevZ
        }

        // look for remaining points in increasing z-order
        while (n != null && n.z <= maxZ) {
            if (n.x >= x0 && n.x <= x1 && n.y >= y0 && n.y <= y1 && n !== a && n !== c &&
                pointInTriangle(ax, ay, bx, by, cx, cy, n.x, n.y) && area(n.prev, n, n.next) >= 0
            ) {
                return false
            }
            n = n.nextZ
        }

        return true
    }

    // go through all polygon nodes and cure small local self-intersections
    private fun cureLocalIntersections(
        _start: VertexIndex,
        triangles: MutableList<Int>,
        dim: Int
    ): VertexIndex? {
        var start: VertexIndex = _start
        var p: VertexIndex = start

        do {
            val a: VertexIndex = p.prev
            val b: VertexIndex = p.next.next

            if (!equals(a, b) && intersects(a, p, p.next, b) && locallyInside(
                    a,
                    b
                ) && locallyInside(
                    b,
                    a
                )
            ) {
                triangles.add(a.i / dim or 0)
                triangles.add(p.i / dim or 0)
                triangles.add(b.i / dim or 0)

                // remove two nodes involved
                removeNode(p)
                removeNode(p.next)

                start = b
                p = b
            }
            p = p.next
        } while (p !== start)

        return filterPoints(p, null)
    }

    // try splitting polygon into two and triangulate them independently
    private fun splitEarcut(
        start: VertexIndex,
        triangles: MutableList<Int>,
        dim: Int,
        minX: Double,
        minY: Double,
        invSize: Double
    ) {
        // look for a valid diagonal that divides the polygon into two
        var a: VertexIndex? = start
        do {
            var b: VertexIndex? = a!!.next.next
            while (b !== a!!.prev) {
                if (a!!.i != b!!.i && isValidDiagonal(a, b)) {
                    // split the polygon in two by the diagonal
                    var c: VertexIndex? = splitPolygon(a, b)

                    // filter collinear points around the cuts
                    a = filterPoints(a, a.next)
                    c = filterPoints(c!!, c.next)

                    // run earcut on each half
                    earcutLinked(a, triangles, dim, minX, minY, invSize, 0)
                    earcutLinked(c, triangles, dim, minX, minY, invSize, 0)
                    return
                }
                b = b.next
            }
            a = a!!.next
        } while (a !== start)
    }

    // link every hole into the outer loop, producing a single-ring polygon without holes
    private fun eliminateHoles(
        data: DoubleArray,
        holeIndices: IntArray,
        _outerVertexIndex: VertexIndex,
        dim: Int
    ): VertexIndex? {
        var outerVertexIndex: VertexIndex? = _outerVertexIndex

        val queue: MutableList<VertexIndex> = mutableListOf()

        var start: Int
        var end: Int
        var list: VertexIndex

        val len: Int = holeIndices.size
        for (i in 0 until len) {
            start = holeIndices[i] * dim
            end = if (i < len - 1) holeIndices[i + 1] * dim else data.size
            list = linkedList(data, start, end, dim, false)!!

            if (list === list.next) list.steiner = true
            queue.add(getLeftmost(list))
        }

        queue.sortWith(xComparator)

        // process holes from left to right
        for (ii in 0 until queue.size) {
            outerVertexIndex = eliminateHole(queue[ii], outerVertexIndex!!)
        }

        return outerVertexIndex
    }

    // find a bridge between vertices that connects hole with an outer ring and link it
    private fun eliminateHole(hole: VertexIndex, outerVertexIndex: VertexIndex): VertexIndex {
        val bridge = findHoleBridge(hole, outerVertexIndex)
        if (bridge == null) {
            return outerVertexIndex
        }

        val bridgeReverse = splitPolygon(bridge, hole)

        // filter collinear points around the cuts
        filterPoints(bridgeReverse, bridgeReverse.next)
        return filterPoints(bridge, bridge.next)!!
    }

    // David Eberly's algorithm for finding a bridge between hole and outer polygon
    private fun findHoleBridge(hole: VertexIndex, outerVertexIndex: VertexIndex): VertexIndex? {
        var p: VertexIndex = outerVertexIndex
        val hx: Double = hole.x
        val hy: Double = hole.y
        var qx = -Double.MAX_VALUE
        var m: VertexIndex? = null

        // find a segment intersected by a ray from the hole's leftmost point to the left;
        // segment's endpoint with lesser x will be potential connection point
        do {
            if (hy <= p.y && hy >= p.next.y && p.next.y != p.y) {
                val x: Double = p.x + (hy - p.y) * (p.next.x - p.x) / (p.next.y - p.y)
                if (x <= hx && x > qx) {
                    qx = x
                    m = if (p.x < p.next.x) p else p.next
                    if (x == hx) return m
                }
            }
            p = p.next
        } while (p !== outerVertexIndex)

        if (m == null) return null

        // look for points inside the triangle of hole point, segment intersection and endpoint;
        // if there are no points found, we have a valid connection;
        // otherwise choose the point of the minimum angle with the ray as connection point

        val stop: VertexIndex = m
        val mx: Double = m.x
        val my: Double = m.y
        var tanMin = Double.MAX_VALUE
        var tan: Double

        p = m

        do {
            if (hx >= p.x && p.x >= mx && hx != p.x &&
                pointInTriangle(
                    if (hy < my) hx else qx,
                    hy,
                    mx,
                    my,
                    if (hy < my) qx else hx,
                    hy,
                    p.x,
                    p.y
                )
            ) {
                tan = abs(hy - p.y) / (hx - p.x) // tangential

                if (locallyInside(p, hole) &&
                    (tan < tanMin || tan == tanMin && (p.x > m!!.x || p.x == m.x && sectorContainsSector(
                        m,
                        p
                    )))
                ) {
                    m = p
                    tanMin = tan
                }
            }

            p = p.next
        } while (p !== stop)

        return m
    }

    // whether sector in vertex m contains sector in vertex p in the same coordinates
    private fun sectorContainsSector(m: VertexIndex, p: VertexIndex): Boolean {
        return area(m.prev, m, p.prev) < 0 && area(p.next, m, m.next) < 0
    }

    // interlink polygon nodes in z-order
    private fun indexCurve(start: VertexIndex, minX: Double, minY: Double, invSize: Double) {
        var p: VertexIndex? = start
        do {
            if (p!!.z == 0.0) p.z = zOrder(p.x, p.y, minX, minY, invSize)
            p.prevZ = p.prev
            p.nextZ = p.next
            p = p.next
        } while (p !== start)

        p.prevZ!!.nextZ = null
        p.prevZ = null

        sortLinked(p)
    }

    // Simon Tatham's linked list merge sort algorithm
// http://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html
    private fun sortLinked(_list: VertexIndex): VertexIndex {
        var list: VertexIndex? = _list
        var p: VertexIndex?
        var q: VertexIndex?
        var e: VertexIndex?
        var tail: VertexIndex?
        var numMerges: Int
        var pSize: Int
        var qSize: Int
        var inSize = 1

        do {
            p = list
            list = null
            tail = null
            numMerges = 0

            while (p != null) {
                numMerges++
                q = p
                pSize = 0

                for (i in 0 until inSize) {
                    pSize++
                    q = q!!.nextZ
                    if (q == null) break
                }
                qSize = inSize

                while (pSize > 0 || (qSize > 0 && q != null)) {
                    if (pSize != 0 && (qSize == 0 || q == null || p!!.z <= q.z)) {
                        e = p
                        p = p!!.nextZ
                        pSize--
                    } else {
                        e = q
                        q = q!!.nextZ
                        qSize--
                    }

                    if (tail != null) tail.nextZ = e else list = e

                    e!!.prevZ = tail
                    tail = e
                }

                p = q
            }

            tail!!.nextZ = null
            inSize *= 2
        } while (numMerges > 1)

        return list!!
    }

    // z-order of a point given coords and inverse of the longer side of data bbox
    private fun zOrder(
        x0: Double,
        y0: Double,
        minX: Double,
        minY: Double,
        invSize: Double
    ): Double {
        // coords are transformed into non-negative 15-bit integer range
        var x = ((x0 - minX) * invSize).toInt() or 0
        var y = ((y0 - minY) * invSize).toInt() or 0

        x = (x or (x shl 8)) and 0x00FF00FF
        x = (x or (x shl 4)) and 0x0F0F0F0F
        x = (x or (x shl 2)) and 0x33333333
        x = (x or (x shl 1)) and 0x55555555

        y = (y or (y shl 8)) and 0x00FF00FF
        y = (y or (y shl 4)) and 0x0F0F0F0F
        y = (y or (y shl 2)) and 0x33333333
        y = (y or (y shl 1)) and 0x55555555

        return (x or (y shl 1)).toDouble()
    }

    // find the leftmost node of a polygon ring
    private fun getLeftmost(start: VertexIndex): VertexIndex {
        var p: VertexIndex = start
        var leftmost: VertexIndex = start

        do {
            if (p.x < leftmost.x || p.x == leftmost.x && p.y < leftmost.y) leftmost = p
            p = p.next
        } while (p !== start)

        return leftmost
    }

    // check if a point lies within a convex triangle
    private fun pointInTriangle(
        ax: Double,
        ay: Double,
        bx: Double,
        by: Double,
        cx: Double,
        cy: Double,
        px: Double,
        py: Double
    ): Boolean {
        return (cx - px) * (ay - py) >= (ax - px) * (cy - py) &&
                (ax - px) * (by - py) >= (bx - px) * (ay - py) &&
                (bx - px) * (cy - py) >= (cx - px) * (by - py)
    }

    // check if a diagonal between two polygon nodes is valid (lies in polygon interior)
    private fun isValidDiagonal(a: VertexIndex, b: VertexIndex): Boolean {
        return a.next.i != b.i && a.prev.i != b.i && !intersectsPolygon(
            a,
            b
        ) && // dones't intersect other edges
                (
                        locallyInside(a, b) && locallyInside(b, a) && middleInside(
                            a,
                            b
                        ) && // locally visible
                                (area(a.prev, a, b.prev) != 0.0 || area(
                                    a,
                                    b.prev,
                                    b
                                ) != 0.0) || // does not create opposite-facing sectors
                                equals(a, b) && area(a.prev, a, a.next) > 0 && area(
                            b.prev,
                            b,
                            b.next
                        ) > 0
                        ) // special zero-length case
    }

    // signed area of a triangle
    private fun area(p: VertexIndex, q: VertexIndex, r: VertexIndex): Double {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)
    }

    // check if two points are equal
    private fun equals(p1: VertexIndex?, p2: VertexIndex?): Boolean {
        if (p1 == null && p2 == null) return true
        if (p1 == null || p2 == null) return false
        return p1.x == p2.x && p1.y == p2.y
    }

    // check if two segments intersect
    private fun intersects(
        p1: VertexIndex,
        q1: VertexIndex,
        p2: VertexIndex,
        q2: VertexIndex
    ): Boolean {
        val o1 = sign(area(p1, q1, p2))
        val o2 = sign(area(p1, q1, q2))
        val o3 = sign(area(p2, q2, p1))
        val o4 = sign(area(p2, q2, q1))

        if (o1 != o2 && o3 != o4) return true // general case

        if (o1 == 0 && onSegment(
                p1,
                p2,
                q1
            )
        ) return true // p1, q1 and p2 are collinear and p2 lies on p1q1
        if (o2 == 0 && onSegment(
                p1,
                q2,
                q1
            )
        ) return true // p1, q1 and q2 are collinear and q2 lies on p1q1
        if (o3 == 0 && onSegment(
                p2,
                p1,
                q2
            )
        ) return true // p2, q2 and p1 are collinear and p1 lies on p2q2
        if (o4 == 0 && onSegment(
                p2,
                q1,
                q2
            )
        ) return true // p2, q2 and q1 are collinear and q1 lies on p2q2

        return false
    }

    // for collinear points p, q, r, check if point q lies on segment pr
    private fun onSegment(p: VertexIndex, q: VertexIndex, r: VertexIndex): Boolean {
        return q.x <= max(p.x, r.x) &&
                q.x >= min(p.x, r.x) &&
                q.y <= max(p.y, r.y) &&
                q.y >= min(p.y, r.y)
    }

    private fun sign(num: Double): Int {
        return if (num > 0) 1 else if (num < 0) -1 else 0
    }

    // check if a polygon diagonal intersects any polygon segments
    private fun intersectsPolygon(a: VertexIndex, b: VertexIndex): Boolean {
        var p: VertexIndex = a
        do {
            if (p.i != a.i && p.next.i != a.i && p.i != b.i && p.next.i != b.i && intersects(
                    p,
                    p.next,
                    a,
                    b
                )
            ) {
                return true
            }
            p = p.next
        } while (p !== a)

        return false
    }

    // check if a polygon diagonal is locally inside the polygon
    private fun locallyInside(a: VertexIndex, b: VertexIndex): Boolean {
        return if (area(a.prev, a, a.next) < 0) {
            area(a, b, a.next) >= 0 && area(a, a.prev, b) >= 0
        } else {
            area(a, b, a.prev) < 0 || area(a, a.next, b) < 0
        }
    }

    // check if the middle point of a polygon diagonal is inside the polygon
    private fun middleInside(a: VertexIndex, b: VertexIndex): Boolean {
        var p: VertexIndex = a
        var inside = false
        val px = (a.x + b.x) / 2
        val py = (a.y + b.y) / 2
        do {
            if (((p.y > py) != (p.next.y > py)) && p.next.y != p.y && (px < (p.next.x - p.x) * (py - p.y) / (p.next.y - p.y) + p.x)) {
                inside = !inside
            }
            p = p.next
        } while (p !== a)

        return inside
    }

    // link two polygon vertices with a bridge; if the vertices belong to the same ring, it splits polygon into two;
// if one belongs to the outer ring and another to a hole, it merges it into a single ring
    private fun splitPolygon(a: VertexIndex, b: VertexIndex): VertexIndex {
        val a2 = VertexIndex(a.i, a.x, a.y)
        val b2 = VertexIndex(b.i, b.x, b.y)
        val an = a.next
        val bp = b.prev

        a.next = b
        b.prev = a

        a2.next = an
        an.prev = a2

        b2.next = a2
        a2.prev = b2

        bp.next = b2
        b2.prev = bp

        return b2
    }

    // create a node and optionally link it with previous one (in a circular doubly linked list)
    private fun insertNode(i: Int, x: Double, y: Double, last: VertexIndex?): VertexIndex {
        val p = VertexIndex(i, x, y)

        if (last == null) {
            p.prev = p
            p.next = p
        } else {
            p.next = last.next
            p.prev = last
            last.next.prev = p
            last.next = p
        }
        return p
    }

    private fun removeNode(p: VertexIndex) {
        p.next.prev = p.prev
        p.prev.next = p.next

        if (p.prevZ != null) p.prevZ!!.nextZ = p.nextZ
        if (p.nextZ != null) p.nextZ!!.prevZ = p.prevZ
    }

    private fun signedArea(data: DoubleArray, start: Int, end: Int, dim: Int): Double {
        var sum = 0.0
        var i = start
        var j = end - dim
        while (i < end) {
            sum += (data[j] - data[i]) * (data[i + 1] + data[j + 1])
            j = i
            i += dim
        }
        return sum
    }

    // return a percentage difference between the polygon area and its triangulation area;
// used to verify correctness of triangulation
    fun deviation(input: Input, triangles: List<Int>): Double {
        return deviation(input.vertices, input.holeIndices, input.dimensions, triangles)
    }

    fun deviation(
        data: DoubleArray,
        holeIndices: IntArray?,
        dim: Int,
        triangles: List<Int>
    ): Double {
        val hasHoles = holeIndices != null && holeIndices.isNotEmpty()
        val outerLen = if (hasHoles) holeIndices!![0] * dim else data.size

        var polygonArea: Double = abs(signedArea(data, 0, outerLen, dim))
        if (hasHoles) {
            val len = holeIndices!!.size
            for (i in 0 until len) {
                val start = holeIndices[i] * dim
                val end = if (i < len - 1) holeIndices[i + 1] * dim else data.size
                polygonArea -= abs(signedArea(data, start, end, dim))
            }
        }

        var trianglesArea = 0.0
        for (i in triangles.indices step 3) {
            val a = triangles[i] * dim
            val b = triangles[i + 1] * dim
            val c = triangles[i + 2] * dim
            trianglesArea += abs((data[a] - data[c]) * (data[b + 1] - data[a + 1]) - (data[a] - data[b]) * (data[c + 1] - data[a + 1]))
        }

        return if (polygonArea == 0.0 && trianglesArea == 0.0) {
            0.0
        } else {
            abs((trianglesArea - polygonArea) / polygonArea)
        }
    }

    // turn a polygon in a multi-dimensional array form (e.g. as in GeoJSON) into a form Earcut accepts
    fun flatten(data: Array<Array<DoubleArray>>): Input {
        val dim: Int = data[0][0].size
        val rVertices = mutableListOf<Double>()
        val rHoles = mutableListOf<Int>()

        var holeIndex = 0

        for (i in data.indices) {
            for (j in data[i].indices) {
                for (d in 0 until dim) {
                    rVertices.add(data[i][j][d])
                }
            }
            if (i > 0) {
                holeIndex += data[i - 1].size
                rHoles.add(holeIndex)
            }
        }

        return Input(rVertices.toDoubleArray(), rHoles.toIntArray(), dim)
    }

    class Point3(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
        fun set(x: Double, y: Double, z: Double) {
            this.x = x
            this.y = y
            this.z = z
        }

        operator fun minus(v2: Point3): Point3 {
            return Point3(x - v2.x, y - v2.y, z - v2.z)
        }

        operator fun plus(v2: Point3): Point3 {
            return Point3(v2.x + x, v2.y + y, v2.z + z)
        }

        operator fun times(sc: Double): Point3 {
            return Point3(x * sc, y * sc, z * sc)
        }

        operator fun div(sc: Double): Point3 {
            return Point3(x / sc, y / sc, z / sc)
        }

        fun normalize() {
            val d = sqrt(x * x + y * y + z * z)
            if (d != 0.0) {
                x /= d
                y /= d
                z /= d
            }
        }

        fun cross(b: Point3): Point3 {
            return Point3(
                y * b.z - b.y * z,
                z * b.x - b.z * x,
                x * b.y - b.x * y
            )
        }
    }

    class AnyToXYTransform(nx: Double, ny: Double, nz: Double) {

        protected var m00 = 0.0
        protected var m01 = 0.0
        protected var m02 = 0.0
        protected var m10 = 0.0
        protected var m11 = 0.0
        protected var m12 = 0.0
        protected var m20 = 0.0
        protected var m21 = 0.0
        protected var m22 = 0.0

        /**
         * normal must be normalized
         *
         * @param nx
         * @param ny
         * @param nz
         */
        fun setSourceNormal(nx: Double, ny: Double, nz: Double) {
            val h: Double
            val f: Double
            val hvx: Double
            val vx: Double = -ny
            val vy: Double = nx
            val c: Double = nz
            h = (1 - c) / (1 - c * c)
            hvx = h * vx
            f = if (c < 0) -c else c
            if (f < 1.0 - 1.0E-4) {
                m00 = c + hvx * vx
                m01 = hvx * vy
                m02 = -vy
                m10 = hvx * vy
                m11 = c + h * vy * vy
                m12 = vx
                m20 = vy
                m21 = -vx
                m22 = c
            } else {
                // if "from" and "to" vectors are nearly parallel
                m00 = 1.0
                m01 = 0.0
                m02 = 0.0
                m10 = 0.0
                m11 = 1.0
                m12 = 0.0
                m20 = 0.0
                m21 = 0.0
                m22 = if (c > 0) {
                    1.0
                } else {
                    -1.0
                }
            }
        }

        /**
         * Assumes source normal is normalized
         */
        init {
            setSourceNormal(nx, ny, nz)
        }

        fun transform(p: Point3) {
            val px: Double = p.x
            val py: Double = p.y
            val pz: Double = p.z
            p.set(
                m00 * px + m01 * py + m02 * pz,
                m10 * px + m11 * py + m12 * pz,
                m20 * px + m21 * py + m22 * pz
            )
        }

        fun transform(data: DoubleArray) {
            for (i in 0 until (data.size / 3)) {
                val point = Point3(data[i * 3], data[i * 3 + 1], data[i * 3 + 2])
                transform(point)
                data[i * 3] = point.x
                data[i * 3 + 1] = point.y
                data[i * 3 + 2] = point.z
            }
        }
    }
}