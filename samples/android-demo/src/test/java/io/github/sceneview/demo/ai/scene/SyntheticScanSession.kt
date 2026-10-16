package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.DenseFusion
import io.github.sceneview.demo.demos.internal.DepthBackProjection
import io.github.sceneview.demo.demos.internal.ReplayLens
import io.github.sceneview.demo.demos.internal.RerunSyntheticRoom

/**
 * The synthetic room — 4.0 × 3.6 m, 2.6 m high, a sofa, a table, a cabinet and a ball — fused
 * from its depth frames into a dense cloud with normals, then wrapped as a session. The ground
 * truth lives in [RerunSyntheticRoom]; the reconstruction tests measure against it.
 */
object SyntheticScanSession {

    val cloud: DenseCloud by lazy {
        val fusion = DenseFusion()
        RerunSyntheticRoom.frames().forEach { fusion.add(DepthBackProjection.project(it)) }
        fusion.cloud(minViews = DenseFusion.MIN_VIEWS)
    }

    val session: SpatialSession by lazy {
        SpatialSession(
            points = cloud.positions,
            colors = cloud.colors,
            planes = emptyList(),
            keyframes = emptyList(),
            lens = ReplayLens.Default,
            floorY = 0f,
            poseTimes = FloatArray(0),
            poses = emptyList(),
            normals = cloud.normals,
        )
    }
}
