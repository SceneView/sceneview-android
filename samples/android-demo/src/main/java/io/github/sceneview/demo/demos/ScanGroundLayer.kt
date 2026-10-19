package io.github.sceneview.demo.demos

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.google.android.filament.Engine
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneScope
import io.github.sceneview.demo.demos.internal.ScanGroundMeshes
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.material.setColor

/**
 * The colours of the ground a scan's result stands on (#4719), from the stage's own: the slab is
 * the stage's card, like the dollhouse's plinth, its edge a shade darker; what the scan did not
 * see is hatched in the guidance amber the live map marks "still to scan" with, so the two read
 * as one map. Every colour opaque, composited over the stage ground: unlit and linear-mapped
 * (`configureForDebug`), each shows at its token's value.
 */
internal data class ScanGroundColors(val slab: Color, val edge: Color, val missing: Color, val hatch: Color) {
    companion object {
        /** How far the slab's edge is shaded from its top: the dollhouse plinth's shade. */
        const val EDGE_SHADE = 0.28f

        /** How much amber a missing tile takes: a tint, so the hatching reads on it. */
        const val MISSING_TINT = 0.2f

        private const val HALF = 0.5f

        fun of(card: Color, ground: Color): ScanGroundColors {
            val slab = card.compositeOver(ground)
            val amber = SceneViewTokens.ArOverlay.accentGuidance
            return ScanGroundColors(
                slab = slab,
                // Darker on a light stage, lighter on a dark one: the slab's side reads in both.
                edge = lerp(slab, if (slab.luminance() < HALF) Color.White else Color.Black, EDGE_SHADE),
                missing = lerp(slab, amber, MISSING_TINT),
                hatch = amber,
            )
        }
    }
}

/** The four layers of [ScanGroundMeshes] and their materials. Main thread, as every Filament call. */
private class ScanGroundNodes(private val engine: Engine, private val materialLoader: MaterialLoader, colors: ScanGroundColors) {
    private val materials = mutableListOf<MaterialInstance>()
    private var colors = colors

    private fun node(color: Color): DebugLayerNode {
        val material = materialLoader.createUnlitColorInstance(color).also {
            it.setCullingMode(Material.CullingMode.NONE)
            materials += it
        }
        return DebugLayerNode(engine, material, PRIORITY)
    }

    val slab = node(colors.slab)
    val edge = node(colors.edge)
    val missing = node(colors.missing)
    val hatch = node(colors.hatch)
    val nodes = listOf(slab, edge, missing, hatch)

    fun upload(meshes: ScanGroundMeshes) {
        slab.upload(meshes.slab)
        edge.upload(meshes.edge)
        missing.upload(meshes.missingFill)
        hatch.upload(meshes.hatch)
    }

    /** A theme switch: the same nodes, repainted. Every colour is opaque, so no material changes kind. */
    fun paint(colors: ScanGroundColors) {
        if (colors == this.colors) return
        this.colors = colors
        materials[0].setColor(colors.slab)
        materials[1].setColor(colors.edge)
        materials[2].setColor(colors.missing)
        materials[3].setColor(colors.hatch)
    }

    fun destroy() = materials.forEach { materialLoader.destroyMaterialInstance(it) }

    companion object {
        const val PRIORITY = 1
    }
}

/**
 * The ground under a scan's result (#4719): [meshes] — the slab, its edge, the hatching of the
 * floor the scan did not see — in [colors], in the scan's own world space next to the model.
 */
@Composable
internal fun SceneScope.ScanGroundLayer(
    meshes: ScanGroundMeshes,
    engine: Engine,
    materialLoader: MaterialLoader,
    colors: ScanGroundColors,
) {
    // Remembered before the node, so Compose releases the materials after the node destroyed the layers.
    val layers = remember(engine, materialLoader, meshes) {
        val started = SystemClock.elapsedRealtimeNanos()
        ScanGroundNodes(engine, materialLoader, colors).also {
            it.upload(meshes)
            // The ground's cost on the main thread, once, and what it adds to every frame.
            Log.i(
                TAG,
                "ground: ${meshes.triangles} triangles, uploaded in " +
                    "${(SystemClock.elapsedRealtimeNanos() - started) / NS_PER_US} µs",
            )
        }
    }
    DisposableEffect(layers) { onDispose { layers.destroy() } }
    SideEffect { layers.paint(colors) }
    key(layers) {
        Node(apply = { layers.nodes.forEach { addChildNode(it) } })
    }
}

private const val TAG = "ScanGround"
private const val NS_PER_US = 1_000L
