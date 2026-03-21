package io.github.sceneview.geometries

import io.github.sceneview.rendering.Vertex

/**
 * Pure geometry data — vertices and triangle indices — with no rendering-engine dependency.
 *
 * Platform-specific code converts this into native vertex/index buffers.
 */
data class GeometryData(
    val vertices: List<Vertex>,
    val indices: List<Int>
)
