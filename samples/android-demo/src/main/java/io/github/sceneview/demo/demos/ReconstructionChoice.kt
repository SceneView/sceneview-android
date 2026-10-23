package io.github.sceneview.demo.demos

import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.DenseFusion
import io.github.sceneview.demo.demos.internal.DepthBackProjection
import io.github.sceneview.demo.demos.internal.ReconstructionMethod
import io.github.sceneview.demo.demos.internal.RerunTsdf
import io.github.sceneview.demo.demos.internal.ScreenedPoisson
import io.github.sceneview.demo.demos.internal.model.TexturedSyntheticRoom

/*
 * The end of a scan's reconstruction, when it is not the fused TSDF (#4630): [ScreenedPoisson]
 * over the scan's surfels, its field handed to the model's meshing as a TSDF.
 */

/**
 * [source]'s surface by [method], as a TSDF to mesh: `null` for [ReconstructionMethod.Fused] —
 * the model fuses as it always has — and when Poisson has nothing to work from (no surfels
 * with normals) or no grid fits its budget; the model then fuses too.
 */
internal fun reconstructedTsdf(source: RerunModelSource, method: ReconstructionMethod, progress: (Float) -> Unit): RerunTsdf? {
    if (method != ReconstructionMethod.Poisson) return null
    val cloud = when (source) {
        is RerunModelSource.Live -> source.cloud
        is RerunModelSource.Surfels -> source.cloud
        is RerunModelSource.Synthetic -> texturedSyntheticCloud()
    } ?: return null
    return ScreenedPoisson.reconstruct(cloud, progress = progress)?.tsdf
}

/** QA only: the textured synthetic room's surfels, as its recording keeps them. */
private fun texturedSyntheticCloud(): DenseCloud {
    val fusion = DenseFusion()
    TexturedSyntheticRoom.frames().forEach { fusion.add(DepthBackProjection.project(it)) }
    return fusion.cloud(minViews = DenseFusion.MIN_VIEWS)
}
