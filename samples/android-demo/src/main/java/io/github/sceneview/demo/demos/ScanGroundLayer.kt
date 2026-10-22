package io.github.sceneview.demo.demos

import android.os.SystemClock
import android.util.Log
import androidx.compose.material3.MaterialTheme
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
import io.github.sceneview.demo.demos.internal.ArDebugGeometry
import io.github.sceneview.demo.demos.internal.ArDebugStyle
import io.github.sceneview.demo.demos.internal.DebugLayer
import io.github.sceneview.demo.demos.internal.DebugMesh
import io.github.sceneview.demo.demos.internal.ScanGround
import io.github.sceneview.demo.demos.internal.ScanGroundMeshes
import io.github.sceneview.demo.theme.LocalStageChrome
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.material.setColor

/**
 * The colours of the ground a scan's result stands on (#4719), from the stage's own: the slab is
 * the stage's card, like the dollhouse's plinth.
 *
 * - On a light stage the edge is a shade darker than the slab.
 * - On a dark stage the card is near-black like the ground, so the slab would read as a hole in
 *   it: the slab takes the theme's `surface-container-high`, its edge `outline-variant`, so both
 *   lift off the dark ground.
 *
 * The floor the scan did not see is not marked on it: holes read as the slab, and the parts worth
 * scanning again have their own pill (#4736). Every colour opaque, composited over what it lies
 * on: unlit and linear-mapped (`configureForDebug`), each shows at its token's value, and a theme
 * switch repaints the same materials without changing their kind.
 */
internal data class ScanGroundColors(val slab: Color, val edge: Color) {
    companion object {
        /** How far the slab's edge is shaded from its top: the dollhouse plinth's shade. */
        const val EDGE_SHADE = 0.28f

        private const val HALF = 0.5f

        /**
         * The colours on the stage of [card] over [ground]; [container] and [outline] are the
         * theme's `surface-container-high` and `outline-variant`, used on a dark stage only.
         */
        fun of(card: Color, ground: Color, container: Color, outline: Color): ScanGroundColors {
            val stage = card.compositeOver(ground)
            val dark = stage.luminance() < HALF
            val slab = if (dark) container.compositeOver(ground) else stage
            return ScanGroundColors(
                slab = slab,
                edge = if (dark) outline.compositeOver(slab) else lerp(slab, Color.Black, EDGE_SHADE),
            )
        }

        /** The colours of the stage and theme of the moment. */
        @Composable
        fun current(): ScanGroundColors {
            val chrome = LocalStageChrome.current
            val scheme = MaterialTheme.colorScheme
            return of(chrome.card, chrome.ground, scheme.surfaceContainerHigh, scheme.outlineVariant)
        }
    }
}

/** The two layers of [ScanGroundMeshes] and their materials. Main thread, as every Filament call. */
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
    val nodes = listOf(slab, edge)

    fun upload(meshes: ScanGroundMeshes) {
        slab.upload(meshes.slab)
        edge.upload(meshes.edge)
    }

    /** A theme switch: the same nodes, repainted. Every colour is opaque, so no material changes kind. */
    fun paint(colors: ScanGroundColors) {
        if (colors == this.colors) return
        this.colors = colors
        materials[0].setColor(colors.slab)
        materials[1].setColor(colors.edge)
    }

    fun destroy() = materials.forEach { materialLoader.destroyMaterialInstance(it) }

    companion object {
        const val PRIORITY = 1
    }
}

/**
 * The ground under a scan's result (#4719): [meshes] — the slab and its edge — in [colors], in
 * the scan's own world space next to the model.
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

/**
 * The replay's floor grid under a scan's result (QA 2026-10-22): the grid of the replay's 3D view
 * — Polycam's, Reality Composer's — round the slab, fading out with the distance from the room, so
 * the result shows the room's scale as the replay does. Just under the slab's top: the slab hides
 * it inside the room, the grid carries on outside. In the stage's grid colours, light and dark.
 *
 * [style] sizes the lines for the camera the result opens on.
 */
@Composable
internal fun SceneScope.ScanGridLayer(
    ground: ScanGround,
    style: ArDebugStyle,
    engine: Engine,
    materialLoader: MaterialLoader,
) {
    val chrome = LocalStageChrome.current
    val layers = remember(engine, materialLoader) { ScanGridNodes(engine, materialLoader) }
    DisposableEffect(layers) { onDispose { layers.destroy() } }
    remember(layers, ground, style) { layers.upload(ground, style) }
    SideEffect { layers.paint(chrome.debug.gridMinor, chrome.debug.gridMajor) }
    key(layers) {
        Node(apply = { layers.nodes.forEach { addChildNode(it) } })
    }
}

/** The grid's two layers on `replay_grid.mat`, as the replay draws them. Main thread. */
private class ScanGridNodes(engine: Engine, private val materialLoader: MaterialLoader) {
    private val material = materialLoader.createMaterial(GRID_MATERIAL)
    private val minorMaterial = materialLoader.createInstance(material)
    private val majorMaterial = materialLoader.createInstance(material)
    private val minor = DebugLayerNode(engine, minorMaterial, ScanGroundNodes.PRIORITY)
    private val major = DebugLayerNode(engine, majorMaterial, ScanGroundNodes.PRIORITY)
    val nodes = listOf(minor, major)
    private var colors: Pair<Color, Color>? = null

    fun upload(ground: ScanGround, style: ArDebugStyle) {
        val bounds = floatArrayOf(
            ground.originX, ground.floorY, ground.originZ,
            ground.originX + ground.columns * ground.cellM, ground.floorY, ground.originZ + ground.rows * ground.cellM,
        )
        val meshes = HashMap<DebugLayer, DebugMesh>()
        val y = ground.floorY - ScanGroundMeshes.SLAB_DROP_M - UNDER_SLAB_M
        ArDebugGeometry.buildStage(bounds, y, style) { meshes.getOrPut(it) { DebugMesh() } }
        minor.upload(meshes[DebugLayer.GridMinor])
        major.upload(meshes[DebugLayer.GridMajor])
        val fade = ArDebugGeometry.gridFade(bounds)
        listOf(minorMaterial, majorMaterial).forEach {
            it.setParameter("fade", fade.centreX, fade.centreZ, fade.start, fade.end)
        }
    }

    fun paint(minorColor: Color, majorColor: Color) {
        if (colors == minorColor to majorColor) return
        colors = minorColor to majorColor
        minorMaterial.setColor(minorColor)
        majorMaterial.setColor(majorColor)
    }

    /** After the nodes left the scene: the instances, then their material. */
    fun destroy() {
        materialLoader.destroyMaterialInstance(minorMaterial)
        materialLoader.destroyMaterialInstance(majorMaterial)
        materialLoader.destroyMaterial(material)
    }

    companion object {
        const val GRID_MATERIAL = "materials/replay_grid.filamat"

        /** How far under the slab's top the grid lies, so the slab hides it with no z-fighting. */
        const val UNDER_SLAB_M = 0.005f
    }
}
