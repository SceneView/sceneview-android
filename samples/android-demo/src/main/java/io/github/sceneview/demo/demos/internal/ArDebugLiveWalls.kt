package io.github.sceneview.demo.demos.internal

/*
 * 3D Scan's trace handed to the live walls (#4540): the walls themselves know nothing of the
 * trace ([LivePlane]), so Measure and a scan module can feed them ARCore's planes directly.
 */

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

/** The walls [frame] shows: its planes, over the floor the trace found, at its time. */
fun LiveWallSource.update(frame: ArDebugFrame): LiveWallSnapshot =
    update(livePlanesOf(frame), ArDebugGeometry.floorHeight(frame), frame.time)
