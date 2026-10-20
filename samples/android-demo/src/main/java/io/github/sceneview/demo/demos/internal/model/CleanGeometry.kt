package io.github.sceneview.demo.demos.internal.model

import io.github.sceneview.demo.demos.internal.RerunMesh

/** What [CleanGeometry.clean] did to a mesh, for the log and the tests. */
internal data class CleanGeometryStats(
    val trianglesIn: Int,
    val trianglesOut: Int,
    val piecesIn: Int,
    val piecesOut: Int,
    val piecesDropped: Int,
    val doubledTriangles: Int,
    val planes: Int,
    val flatShare: Float,
    val holesFilled: Int,
    val planeList: List<Plane> = emptyList(),
)

/*
 * The geometry half of the clean model: marching cubes' raw surface in, a surface with no
 * floating debris, no doubled sheets, no ripple, flat walls and closed pin-holes out. Colours ride
 * along per vertex. Off the main thread: a room is a few seconds of work.
 */
internal object CleanGeometry {
    fun of(mesh: RerunMesh): WorkMesh = WorkMesh(mesh.positions.copyOf(), mesh.colors.copyOf(), mesh.indices.copyOf())

    fun clean(raw: WorkMesh, progress: (Float) -> Unit = {}): Pair<WorkMesh, CleanGeometryStats> {
        val trianglesIn = raw.triangleCount
        val piecesIn = MeshCleanup.pieces(raw)
        var (mesh, dropped) = MeshCleanup.removeSmallPieces(raw)
        progress(PROGRESS_PIECES)
        val (undoubled, doubled) = MeshCleanup.removeDoubledSurfaces(mesh)
        mesh = undoubled
        if (doubled > 0) {
            // A sheet's leftovers — the few triangles at its rim the test did not reach — go too.
            val (pruned, more) = MeshCleanup.removeSmallPieces(mesh)
            mesh = pruned
            dropped += more
        }
        progress(PROGRESS_DOUBLED)
        MeshCleanup.smooth(mesh)
        progress(PROGRESS_SMOOTH)
        val flattening = PlaneFlattening.flatten(mesh)
        progress(PROGRESS_PLANES)
        val (filled, holes) = MeshCleanup.fillSmallHoles(mesh)
        mesh = filled
        progress(1f)
        val stats = CleanGeometryStats(
            trianglesIn = trianglesIn,
            trianglesOut = mesh.triangleCount,
            piecesIn = piecesIn,
            piecesOut = MeshCleanup.pieces(mesh),
            piecesDropped = dropped,
            doubledTriangles = doubled,
            planes = flattening.planes.size,
            flatShare = if (flattening.totalArea > 0f) flattening.flatArea / flattening.totalArea else 0f,
            holesFilled = holes,
            planeList = flattening.planes,
        )
        return mesh to stats
    }

    private const val PROGRESS_PIECES = 0.1f
    private const val PROGRESS_DOUBLED = 0.3f
    private const val PROGRESS_SMOOTH = 0.6f
    private const val PROGRESS_PLANES = 0.9f
}
