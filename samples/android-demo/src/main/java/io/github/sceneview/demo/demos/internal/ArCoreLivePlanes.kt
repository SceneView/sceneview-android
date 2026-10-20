package io.github.sceneview.demo.demos.internal

import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState

/*
 * ARCore's planes handed to the live walls (#4540, #4732), for any screen that runs a live ARCore
 * session — Measure today, a scan module tomorrow. 3D Scan reads its planes from its trace instead
 * (`ArDebugLiveWalls.kt`); both end up as the same world-space [LivePlane].
 */

/**
 * An ARCore plane boundary, [local] `x, z` pairs in the plane's own frame (`Plane.polygon`), as a
 * world-space polygon, flat `[x0,y0,z0, …]`. [transformPoint] is the plane's `centerPose`
 * `transformPoint`: it takes a local `[x, y, z]` and returns the world one.
 */
fun livePlanePolygon(local: FloatArray, transformPoint: (FloatArray) -> FloatArray): FloatArray {
    val count = local.size / 2
    val world = FloatArray(count * 3)
    val point = FloatArray(3)
    for (i in 0 until count) {
        point[0] = local[i * 2]
        point[1] = 0f
        point[2] = local[i * 2 + 1]
        val w = transformPoint(point)
        world[i * 3] = w[0]
        world[i * 3 + 1] = w[1]
        world[i * 3 + 2] = w[2]
    }
    return world
}

/**
 * The floor the live walls stand on: the lowest upward-facing plane's mean height, `null` before
 * one is tracked. Walls are not drawn floating, so a screen waits for this.
 */
fun livePlanesFloorY(planes: List<LivePlane>): Float? = planes
    .filter { it.kind == LivePlane.Kind.Floor && it.vertexCount >= MIN_POLYGON_VERTICES }
    .minOfOrNull { plane -> (0 until plane.vertexCount).sumOf { plane.polygon[it * 3 + 1].toDouble() }.toFloat() / plane.vertexCount }

/** [this] plane's boundary in world space: `polygon` through `centerPose`. */
fun Plane.worldPolygon(): FloatArray {
    val buffer = polygon.duplicate().apply { rewind() }
    val local = FloatArray(buffer.remaining()).also { buffer.get(it) }
    val pose = centerPose
    return livePlanePolygon(local) { pose.transformPoint(it) }
}

/** Which way [this] plane faces, as the live walls read it. */
val Plane.liveKind: LivePlane.Kind
    get() = when (type) {
        Plane.Type.HORIZONTAL_UPWARD_FACING -> LivePlane.Kind.Floor
        Plane.Type.HORIZONTAL_DOWNWARD_FACING -> LivePlane.Kind.Ceiling
        Plane.Type.VERTICAL -> LivePlane.Kind.Wall
        else -> LivePlane.Kind.Other
    }

/**
 * Every plane ARCore tracks in [this] session right now, merged ones left out, as live planes.
 * [ids] keeps one stable id per ARCore plane across frames (ARCore hands the same `Plane` back,
 * equal to itself), which is what lets [PlaneWallSource] skip a frame where nothing changed.
 */
fun Session.livePlanes(ids: MutableMap<Plane, Int>): List<LivePlane> =
    runCatching { getAllTrackables(Plane::class.java) }.getOrNull().orEmpty()
        .filter { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }
        .mapNotNull { plane ->
            val polygon = plane.worldPolygon()
            if (polygon.size / 3 < MIN_POLYGON_VERTICES) return@mapNotNull null
            LivePlane(ids.getOrPut(plane) { ids.size + 1 }, plane.liveKind, polygon)
        }

private const val MIN_POLYGON_VERTICES = 3
