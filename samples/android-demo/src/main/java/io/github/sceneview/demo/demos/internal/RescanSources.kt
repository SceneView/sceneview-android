package io.github.sceneview.demo.demos.internal

/**
 * The inputs of [RescanZones] read off what a saved scan has today (#4736): its dense cloud, the
 * room survey drawn from it, and the ground under its model.
 *
 * - **Walls** come from [RoomSurvey]: a wall it did not see is inferred, and a seen wall keeps its
 *   unseen stretches. The survey draws its walls on the page (longer side along x, top-left corner
 *   at the origin); [walls] lays them back into the scan frame by fitting the page's wall lines to
 *   the cloud's wall surfels, along the survey's own axes.
 * - **Holes** are the connected pieces of the floor [ScanGround] found missing, each as its
 *   bounding rectangle with its true area.
 * - **Coverage** is the cloud's surface over each [COVERAGE_CELL_M] cell, from just under the
 *   floor to [COVERAGE_TOP_M] (or [COVERAGE_TOP_SHARE] of the room's height), so a ceiling never
 *   counts as floor seen. A surfel weighs its area: `4^level` finest voxels.
 *
 * Every function is pure arithmetic: off the main thread.
 */
object RescanSources {
    /** A coverage cell's side, metres. */
    const val COVERAGE_CELL_M = 0.25f

    /** How far under the floor the coverage starts, metres: the floor's own scatter. */
    const val COVERAGE_FLOOR_BAND_M = 0.1f

    /** The highest the coverage reads above the floor, metres. */
    const val COVERAGE_TOP_M = 2.0f

    /** …and at most this share of the room's height, so a low ceiling stays out. */
    const val COVERAGE_TOP_SHARE = 0.8f

    /** The most cells a coverage grid has on a side: a stray point does not make it huge. */
    const val MAX_COVERAGE_CELLS = 400

    /** How closely a surfel's normal follows a survey axis to be read as a wall's, |cos|. */
    const val WALL_NORMAL = 0.9f

    /** The page fit's histogram bin, metres. */
    const val FIT_BIN_M = 0.01f

    /** The page fit's histogram smoothing, bins on each side. */
    const val FIT_SMOOTH_BINS = 2

    /** How far from a wall a wall surfel may lie to count as seeing it, metres. */
    const val WALL_REACH_M = 0.1f

    /** The step along a wall the page fit compares sightings on, metres. */
    const val WALL_STEP_M = 0.1f

    /** The band of a room's height wall surfels are read in, from the floor, metres. */
    const val WALL_LOW_M = 0.2f

    /** The zones of a scan whose dense points are [cloud] and whose model stands on [ground]. */
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun zones(cloud: DenseCloud, ground: ScanGround?): List<RescanZone> = emptyList()

    /**
     * [survey]'s walls in the scan frame. The page is the survey's (u, v) frame — `u = x·cos(yaw) +
     * z·sin(yaw)`, `v = −x·sin(yaw) + z·cos(yaw)` — turned a quarter when the room is longer along
     * v, and shifted to its corner: two rigid moves, the turn and the shift unknown. Each turn is
     * tried; its shift along u and along v is the one that lays the page's wall lines on the most
     * wall surfels (1 cm bins); of the two, the one whose walls are seen where the survey says they
     * were seen, and unseen where it says they were not, is kept.
     */
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun walls(survey: RoomSurvey, positions: FloatArray, normals: FloatArray, count: Int): List<RescanWall> = emptyList()

    /**
     * The holes in [ground]'s floor: each connected piece (side by side) of its missing cells, as
     * its bounding rectangle with the piece's own area.
     */
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun holes(ground: ScanGround): List<RescanHole> = emptyList()

    /** How densely [cloud] covers the floor plane, read from [floorY] up into a room [height] high. */
    @Suppress("UnusedParameter", "UNUSED_PARAMETER", "FunctionOnlyReturningConstant")
    fun coverage(cloud: DenseCloud, floorY: Float, height: Float): RescanCoverage? = null
}
