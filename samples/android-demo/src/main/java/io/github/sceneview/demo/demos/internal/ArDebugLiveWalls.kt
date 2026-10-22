package io.github.sceneview.demo.demos.internal

/*
 * 3D Scan's trace handed to the live walls (#4540): the walls themselves know nothing of the
 * trace ([LivePlane]), so Measure and a scan module can feed them ARCore's planes directly.
 */

/**
 * The recorder writes nothing while ARCore has lost tracking, so a trace with no camera pose for
 * longer than this, in seconds, had lost it (or the app was away). A live frame is rebuilt at
 * 20 Hz at most: a frame or two late is still tracking.
 */
const val TRACE_LOST_AFTER_S = 0.25f

/** [this] plane as the live walls read it. */
fun DebugPlane.toLivePlane(): LivePlane = LivePlane(
    id = id,
    kind = when (kind) {
        DebugPlaneKind.Floor -> LivePlane.Kind.Floor
        DebugPlaneKind.Ceiling -> LivePlane.Kind.Ceiling
        DebugPlaneKind.Wall -> LivePlane.Kind.Wall
        DebugPlaneKind.Unknown -> LivePlane.Kind.Other
    },
    polygon = polygon,
)

/** [frame]'s planes as the live walls read them. */
fun livePlanesOf(frame: ArDebugFrame): List<LivePlane> = frame.planes.map { it.toLivePlane() }

/** The walls [frame] shows: its planes, over the floor the trace found, at its time; [tracking] as [LiveWallSource.update]'s. */
fun LiveWallSource.update(frame: ArDebugFrame, tracking: Boolean = true): LiveWallSnapshot =
    update(livePlanesOf(frame), ArDebugGeometry.floorHeight(frame), frame.time, tracking)
