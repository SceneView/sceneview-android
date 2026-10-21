@file:Suppress("UNUSED_PARAMETER", "UnusedParameter")

package io.github.sceneview.demo.demos.internal

// Stubs: the shape of the nearest wall / nearest opening query, without its answers yet.

data class ShellWall(
    val ax: Float,
    val az: Float,
    val bx: Float,
    val bz: Float,
    val nx: Float,
    val nz: Float,
    val confidence: Float? = null,
)

data class ShellOpening(
    val ax: Float,
    val az: Float,
    val bx: Float,
    val bz: Float,
    val bottom: Float,
    val top: Float,
    val confidence: Float? = null,
) {
    val width: Float get() = 0f
    val height: Float get() = 0f

    companion object {
        fun onWall(
            wall: ShellWall,
            from: Float,
            width: Float,
            bottom: Float,
            top: Float,
            confidence: Float? = null,
        ): ShellOpening = ShellOpening(0f, 0f, 0f, 0f, 0f, 0f)

        fun of(box: ScanBox, confidence: Float? = null): ShellOpening = ShellOpening(0f, 0f, 0f, 0f, 0f, 0f)
    }
}

data class NearestWall(
    val wall: ShellWall,
    val normal: Vec3,
    val offset: Float,
    val point: Vec3,
    val distance: Float,
    val fromBehind: Boolean,
)

data class NearestOpening(val opening: ShellOpening, val point: Vec3, val distance: Float) {
    val width: Float get() = opening.width
    val height: Float get() = opening.height
    val confidence: Float? get() = opening.confidence
}

data class RoomNearest(val wall: NearestWall?, val opening: NearestOpening?)

data class RoomShell(
    val walls: List<ShellWall> = emptyList(),
    val openings: List<ShellOpening> = emptyList(),
) {
    fun nearest(pose: DebugPose): RoomNearest = nearest(pose.x, pose.y, pose.z)

    fun nearest(x: Float, y: Float, z: Float): RoomNearest = RoomNearest(null, null)
}
