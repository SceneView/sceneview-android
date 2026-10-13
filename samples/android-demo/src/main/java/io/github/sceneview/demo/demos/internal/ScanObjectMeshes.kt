package io.github.sceneview.demo.demos.internal

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * What the replay draws round a tapped object ([ScanPick.Found]), as triangles, and where its
 * label goes. Pure, like [ArDebugGeometry]: the JVM tests pin exactly what the GPU receives.
 */
object ScanObjectMeshes {
    /**
     * [box]'s twelve edges as square tubes of [radius], each run [radius] past its two corners so
     * the three that meet there close into one solid corner.
     */
    fun edges(box: ScanBox, radius: Float, into: DebugMesh = DebugMesh(EDGE_VERTICES)): DebugMesh {
        into.clear()
        val corners = box.corners()
        for ((from, to) in ScanBox.EDGES) {
            val a = corners[from]
            val b = corners[to]
            val along = (b - a).normalized() * radius
            ArDebugGeometry.addSegment(into, a - along, b + along, radius)
        }
        return into
    }

    /**
     * The quads of [surfels] cut out of [cloud], the dense layer's own mesh ([DenseSurfels.mesh]:
     * four vertices a surfel, in the cloud's order) — so a tint drawn with them lies exactly on
     * the points it lights, whatever shape the layer gave them. A surfel past the mesh is left out.
     */
    fun surfels(cloud: DebugMesh, surfels: IntArray): DebugMesh {
        val mesh = DebugMesh(maxOf(surfels.size * VERTICES_PER_SURFEL, 1))
        for (surfel in surfels) {
            val first = surfel * VERTICES_PER_SURFEL
            if (surfel < 0 || first + VERTICES_PER_SURFEL > cloud.vertexCount) continue
            val base = mesh.vertexCount
            for (k in 0 until VERTICES_PER_SURFEL) {
                val at = (first + k) * 3
                mesh.vertex(cloud.positions[at], cloud.positions[at + 1], cloud.positions[at + 2])
            }
            mesh.quad(base, base + 1, base + 2, base + 3)
        }
        return mesh
    }

    /**
     * Where a label [height] pixels tall puts its top edge, in the rows [band] the chrome over the
     * scene leaves free: [gap] over [above], the highest point of what it names, while that is in
     * the band; else [gap] under [below], its lowest point — brought back into the band when the
     * object runs out of it, so the label lies across the object's foot, never under the chrome.
     */
    fun labelTop(above: Float, below: Float, height: Int, gap: Int, band: IntRange): Int {
        val lowest = max(band.first, band.last - height)
        val over = (above - gap - height).roundToInt()
        val under = (below + gap).roundToInt()
        return if (over >= band.first) min(over, lowest) else under.coerceIn(band.first, lowest)
    }

    const val VERTICES_PER_SURFEL = 4

    /** Twelve tubes of eight vertices. */
    private const val EDGE_VERTICES = 96
}

/**
 * The pinhole this orbit is drawing through right now, [ArDebugOrbitCamera.getTransform]'s own
 * eye and pedestal drop: where a tap's ray starts, and where a point of the room lands on screen.
 */
fun ArDebugOrbitCamera.scanCamera(): ScanCamera {
    val at = CameraRig.eye(pose)
    val eye = Vec3(at.x, at.y, at.z)
    val forward = (Vec3(pose.target.x, pose.target.y, pose.target.z) - eye).normalized()
    val up = forward.cross(Vec3.Up).normalized().cross(forward)
    val metresPerPixel = CameraRig.worldPerPixel(pose.distance, verticalFovDegrees, viewportHeight)
    val drop = up * (lift * viewportHeight * metresPerPixel)
    return ScanCamera(
        eye = eye - drop,
        forward = forward,
        tanHalfFov = tan(Math.toRadians(verticalFovDegrees / 2.0)).toFloat(),
        width = viewportWidth,
        height = viewportHeight,
    )
}
