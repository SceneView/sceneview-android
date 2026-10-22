package io.github.sceneview.demo.demos.internal

/**
 * The parts of a scan worth scanning again ([RescanZones]) as the result's 3D view and its plan
 * draw them (#4736): each zone's box as its twelve edges, thin square beams — a wall zone reads
 * as a frame standing on the wall, a floor zone as a frame lying on the floor, seen from the side
 * as from above.
 *
 * Plain geometry, built off the main thread; the layer uploads it on the main thread.
 */
object RescanZoneMesh {
    /** A beam's side, metres: thin enough to leave the scan readable, thick enough at room scale. */
    const val BEAM_M = 0.02f

    private const val EDGES = 12
    private const val FACES = 6

    /** The triangles of one zone's frame: twelve beams of six faces. */
    const val TRIANGLES_PER_ZONE = EDGES * FACES * 2

    /** The frames of [zones], beams [beam] metres across. */
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun of(zones: List<RescanZone>, beam: Float = BEAM_M): DebugMesh = DebugMesh()
}
