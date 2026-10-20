package io.github.sceneview.demo.demos

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded._360
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded._3dRotation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.Skybox
import com.google.android.filament.ToneMapper
import com.google.android.filament.utils.KTX1Loader
import io.github.sceneview.DEFAULT_IBL_INTENSITY
import io.github.sceneview.FrameRatePolicy
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.createEnvironment
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.demo.DockItem
import io.github.sceneview.demo.LocalDemoSceneRestingCover
import io.github.sceneview.demo.R
import io.github.sceneview.demo.demoContentPadding
import io.github.sceneview.demo.demos.internal.ArDebugFraming
import io.github.sceneview.demo.demos.internal.ArDebugOrbitCamera
import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.DenseFusion
import io.github.sceneview.demo.demos.internal.DepthBackProjection
import io.github.sceneview.demo.demos.internal.DepthFrame
import io.github.sceneview.demo.demos.internal.DollhouseFraming
import io.github.sceneview.demo.demos.internal.DollhouseOpening
import io.github.sceneview.demo.demos.internal.FramingInsets
import io.github.sceneview.demo.demos.internal.RerunCapturePack
import io.github.sceneview.demo.demos.internal.RerunGlbMesh
import io.github.sceneview.demo.demos.internal.RerunMarchingCubes
import io.github.sceneview.demo.demos.internal.RerunMeshGlb
import io.github.sceneview.demo.demos.internal.RerunPointsMesh
import io.github.sceneview.demo.demos.internal.RerunResultView
import io.github.sceneview.demo.demos.internal.RerunScanModel
import io.github.sceneview.demo.demos.internal.RerunSessionStore
import io.github.sceneview.demo.demos.internal.RerunSyntheticRoom
import io.github.sceneview.demo.demos.internal.RerunTsdf
import io.github.sceneview.demo.demos.internal.ScanAreaCopy
import io.github.sceneview.demo.demos.internal.ScanGround
import io.github.sceneview.demo.demos.internal.ScanGroundMeshes
import io.github.sceneview.demo.demos.internal.ScanPreset
import io.github.sceneview.demo.demos.internal.ScanResultCinematics
import io.github.sceneview.demo.isDemoCompactHeight
import io.github.sceneview.demo.theme.LocalStageChrome
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.theme.SceneViewTokens.Space
import io.github.sceneview.demo.ui.overMediaEdge
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.colorOf
import io.github.sceneview.math.toLinearSpace
import io.github.sceneview.model.model
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberView
import io.github.sceneview.utils.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/*
 * The 3D Scan's result: at the end of a scan, the space as one meshed, coloured 3D model — its raw
 * depth fused into a TSDF ([RerunTsdf]) while it recorded, meshed by marching cubes
 * ([RerunMarchingCubes]) when the scan stops, kept next to the scan ([RerunSessionStore.saveModel])
 * and shown here, still, on the themed stage. Exported as a `.glb` ([RerunMeshGlb]).
 */

/** What a scan's model is built from. */
internal sealed interface RerunModelSource {
    /**
     * The TSDF the recording fused from ARCore's raw depth, frame by frame: the full model.
     * [cloud], the recording's coloured points, is the fallback when meshing finds no surface.
     */
    class Live(tsdf: RerunTsdf, val cloud: DenseCloud?) : RerunModelSource {
        @Volatile
        private var held: RerunTsdf? = tsdf

        /** The fused depth; `null` once [release]d. */
        val tsdf: RerunTsdf? get() = held

        /**
         * Lets the TSDF go (up to [RerunTsdf.MAX_BYTES]) once the model built from it is kept with
         * its scan: reopened, the scan reads that model instead of meshing again.
         */
        fun release() {
            held?.let(RerunLiveModels::forget)
            held = null
        }
    }

    /** A kept scan's surfel cloud, when its raw depth is gone: the scan reopened later. */
    class Surfels(val cloud: DenseCloud) : RerunModelSource

    /**
     * QA only: the ray-cast room, the emulator's stand-in for a camera (it cannot run AR). With
     * [meshFails], meshing is skipped, as if it had found no surface: the points fallback.
     */
    data class Synthetic(val meshFails: Boolean = false) : RerunModelSource

    companion object {
        /** The model [pack] can build: its live TSDF when this process recorded it, else [media]'s surfels. */
        fun of(pack: RerunCapturePack?, media: RerunReplayMedia?): RerunModelSource? {
            val cloud = media?.dense?.cloud?.takeIf { it.count > 0 }
            return RerunLiveModels.find(pack)?.let { Live(it, cloud) }
                ?: cloud?.takeIf { it.normals != null }?.let(::Surfels)
        }
    }
}

/**
 * The recording's TSDF, fed the same raw-depth copies as the surfel map — the depth image's own
 * lens, fresh depth only — on its own worker: a frame that arrives while one integrates is left
 * to the surfel map alone, so neither slows the other.
 */
internal class RerunLiveModel(private val preset: ScanPreset = ScanPreset.ROOM) {
    val tsdf = RerunTsdf(voxelM = preset.modelVoxelM)
    private val busy = AtomicBoolean(false)

    @Volatile
    private var job: Job? = null

    /** Integrates [frame] on [scope]'s Default dispatcher, unless an integration is under way. */
    fun offer(frame: DepthFrame, scope: CoroutineScope) {
        if (!busy.compareAndSet(false, true)) return
        job = scope.launch(Dispatchers.Default) {
            try {
                val step = tsdf.integrate(frame, farM = preset.farM)
                if (tsdf.frames % LOG_EVERY_FRAMES == 1 || step.dropped > 0) {
                    Log.i(
                        TAG,
                        "tsdf: ${tsdf.frames} frames, ${tsdf.blockCount} blocks, " +
                            "${tsdf.bytes / MB} MB of ${tsdf.maxBytes / MB}, dropped ${tsdf.droppedBlocks}",
                    )
                }
            } finally {
                busy.set(false)
            }
        }
    }

    /** Waits for the integration under way. */
    suspend fun join() {
        job?.join()
    }

    /** Keeps this TSDF for [pack], the scan it recorded, so its result can build the model. */
    fun keepFor(pack: RerunCapturePack?) {
        if (pack != null && tsdf.frames > 0) RerunLiveModels.keep(pack, tsdf)
    }
}

/**
 * The last recording's TSDF, kept in memory with the scan it belongs to (one slot: ~50 MB at
 * most). A scan reopened from the list in the same process finds it by its manifest's bytes.
 */
internal object RerunLiveModels {
    @Volatile
    private var kept: Pair<ByteArray, RerunTsdf>? = null

    fun keep(pack: RerunCapturePack, tsdf: RerunTsdf) {
        kept = pack.manifest to tsdf
    }

    fun find(pack: RerunCapturePack?): RerunTsdf? {
        pack ?: return null
        return kept?.takeIf { it.first.contentEquals(pack.manifest) }?.second
    }

    /** Empties the slot when it holds [tsdf]. */
    fun forget(tsdf: RerunTsdf) {
        if (kept?.second === tsdf) kept = null
    }
}

/** A built mesh: its `.glb`, its size, and what building it took. */
internal class RerunModelBuild(
    val glb: ByteArray,
    val bounds: FloatArray,
    val triangles: Int,
    val vertices: Int,
    val buildMs: Long,
    val voxelBytes: Long,
    val budgetReached: Boolean,
)

/** Meshes [source] — fuse (unless done), mesh, write — reporting its share done to [progress]. */
internal fun buildRerunModel(source: RerunModelSource, progress: (Float) -> Unit): RerunModelBuild {
    val started = System.nanoTime()
    val (tsdf, minWeight) = when (source) {
        is RerunModelSource.Live -> checkNotNull(source.tsdf) { "TSDF released" } to LIVE_MIN_WEIGHT
        is RerunModelSource.Surfels -> RerunTsdf().also { t ->
            t.integrateSurfels(source.cloud) { progress(it * FUSE_SHARE) }
        } to RerunMarchingCubes.DEFAULT_MIN_WEIGHT
        is RerunModelSource.Synthetic -> RerunTsdf().also { t ->
            val poses = RerunSyntheticRoom.poses()
            poses.forEachIndexed { i, pose ->
                t.integrate(RerunSyntheticRoom.scene.render(pose))
                progress(FUSE_SHARE * (i + 1) / poses.size)
            }
        } to RerunMarchingCubes.DEFAULT_MIN_WEIGHT
    }
    val fused = System.nanoTime()
    val base = if (source is RerunModelSource.Live) 0f else FUSE_SHARE
    val extraction = RerunMarchingCubes.extractWhole(tsdf, minWeight) { progress(base + (MESH_SHARE - base) * it) }
    val mesh = extraction.mesh
    val glb = if (mesh.triangleCount > 0) RerunMeshGlb.write(mesh) else ByteArray(0)
    progress(1f)
    val done = System.nanoTime()
    Log.i(
        TAG,
        "model (${source::class.simpleName}): ${mesh.triangleCount} triangles, ${mesh.vertexCount} vertices " +
            "(${extraction.rawTriangles} raw, ${extraction.droppedComponents} pieces dropped, " +
            "capped ${extraction.capped}); " +
            "fused ${(fused - started) / NS_PER_MS} ms, meshed ${(done - fused) / NS_PER_MS} ms; " +
            "tsdf ${tsdf.frames} frames, ${tsdf.blockCount} blocks, ${tsdf.bytes / MB} MB, " +
            "dropped ${tsdf.droppedBlocks}; " +
            "glb ${glb.size / KB} KB",
    )
    return RerunModelBuild(
        glb = glb,
        bounds = mesh.bounds(),
        triangles = mesh.triangleCount,
        vertices = mesh.vertexCount,
        buildMs = (done - started) / NS_PER_MS,
        voxelBytes = tsdf.bytes,
        budgetReached = tsdf.budgetReached,
    )
}

/**
 * The scan's model: the meshed surface, or — when meshing finds none — the scan's coloured points
 * as small squares ([RerunPointsMesh]). `null` when there is neither. Off the main thread.
 *
 * [progress] is also where a cancelled build stops: it may throw [CancellationException], which
 * goes through untouched — a screen left mid-build stops meshing instead of racing the next one.
 */
internal fun buildScanModel(source: RerunModelSource, progress: (Float) -> Unit): RerunScanModel? {
    val skipMesh = source is RerunModelSource.Synthetic && source.meshFails
    val mesh = if (skipMesh) {
        null
    } else {
        runCatching { buildRerunModel(source) { progress(it * MESH_PART) } }
            .onFailure { if (it is CancellationException) throw it }
            .onFailure { Log.w(TAG, "meshing failed", it) }
            .getOrNull()
            ?.takeIf { it.triangles > 0 }
    }
    if (mesh != null) {
        progress(1f)
        return RerunScanModel(
            kind = RerunScanModel.Kind.Mesh,
            glb = mesh.glb,
            bounds = mesh.bounds,
            triangles = mesh.triangles,
            vertices = mesh.vertices,
            budgetReached = mesh.budgetReached,
        )
    }
    val cloud = when (source) {
        is RerunModelSource.Live -> source.cloud
        is RerunModelSource.Surfels -> source.cloud
        is RerunModelSource.Synthetic -> syntheticCloud()
    }?.takeIf { it.count > 0 }
    val points = cloud?.let { RerunPointsMesh.of(it) }?.takeIf { it.triangleCount > 0 }
    progress(1f)
    points ?: return null
    Log.i(TAG, "model: points fallback, ${cloud.count} points, ${points.triangleCount} triangles")
    return RerunScanModel(
        kind = RerunScanModel.Kind.Points,
        glb = RerunMeshGlb.write(points),
        bounds = RerunPointsMesh.framingBounds(points),
        triangles = points.triangleCount,
        vertices = points.vertexCount,
    )
}

/** QA only: the ray-cast room's depth, fused into coloured points as a recording fuses them. */
internal fun syntheticCloud(): DenseCloud {
    val fusion = DenseFusion()
    RerunSyntheticRoom.frames().forEach { fusion.add(DepthBackProjection.project(it)) }
    return fusion.cloud(minViews = DenseFusion.MIN_VIEWS)
}

/** Where the result screen is: opening the model, showing it, or with none to show. */
private sealed interface ModelState {
    data object Loading : ModelState

    class Ready(val model: RerunScanModel) : ModelState

    data object Failed : ModelState
}

/**
 * A scan's result: its model, full screen and still on the themed stage, opened at a three-quarter
 * angle that fits the geometry whole. Kept with the scan ([store], [id]) — reopened, it shows the
 * same model without building it again; never kept, it is built from [source] and saved.
 *
 * [onModel] hears the model once it is known (`null` when there is none), for the export sheet.
 * [opened] is the kept model when the caller has already read it, so it is not read twice.
 * [floorCovered] is the scan's floor covered in m², its replay's figure (#4734); `null` when its
 * recording cannot be read, and the legend then keys the hatching alone.
 */
@Composable
@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
internal fun RerunModelScreen(
    source: RerunModelSource?,
    title: String,
    store: RerunSessionStore?,
    id: String?,
    onModel: (RerunScanModel?) -> Unit,
    onBack: () -> Unit,
    onRename: ((String) -> Unit)?,
    onReplay: (() -> Unit)?,
    onViewInAr: (() -> Unit)?,
    onExport: () -> Unit,
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    hintStays: Boolean = false,
    opened: RerunScanModel? = null,
    floorCovered: Float? = null,
) {
    var progress by remember(source, id) { mutableFloatStateOf(0f) }
    val state by produceState<ModelState>(ModelState.Loading, source, id) {
        val kept = opened ?: if (store != null && id != null) {
            withContext(Dispatchers.IO) { runCatching { store.model(id) }.getOrNull() }
        } else {
            null
        }
        val model = kept ?: source?.let { from ->
            withContext(Dispatchers.Default) {
                val job = coroutineContext.job
                val report = { share: Float ->
                    progress = share
                    job.ensureActive()
                }
                runCatching { buildScanModel(from, report) }
                    .onFailure { if (it is CancellationException) throw it }
                    .onFailure { Log.w(TAG, "model build failed", it) }
                    .getOrNull()
            }
        }
        // A model built now is kept with its scan, so the next open reads it instead of meshing again.
        val keepAs = id?.takeIf { kept == null && store != null }
        if (keepAs != null && model != null) {
            val saved = withContext(Dispatchers.IO) {
                runCatching { checkNotNull(store).saveModel(keepAs, model) }
                    .onFailure { Log.w(TAG, "model not saved", it) }
                    .isSuccess
            }
            if (saved) (source as? RerunModelSource.Live)?.release()
        }
        value = if (model != null) ModelState.Ready(model) else ModelState.Failed
    }
    val model = (state as? ModelState.Ready)?.model
    LaunchedEffect(state) { if (state != ModelState.Loading) onModel(model) }
    // Created before the SceneView, so released after its node (Compose forgets in reverse).
    // On the main thread, as every Filament call (CLAUDE.md).
    val instance = remember(model) {
        model?.let { built ->
            val buffer = ByteBuffer.allocateDirect(built.glb.size).order(ByteOrder.nativeOrder()).put(built.glb)
            buffer.rewind()
            runCatching { modelLoader.createModelInstance(buffer) }.getOrNull()
        }
    }
    DisposableEffect(instance) { onDispose { instance?.let { modelLoader.destroyModel(it.model) } } }
    // The ground it stands on and the floor it missed (#4719), read off the model's own vertices.
    val ground by produceState<GroundBuild?>(null, model) {
        value = model?.let { built -> withContext(Dispatchers.Default) { groundOf(built) } }
    }
    // No drift: the camera holds still once the reveal has landed, until the user moves it.
    val orbit = remember(source, id) { ArDebugOrbitCamera(drift = false) }
    // The reveal, the 3D ↔ Plan crane and the tour (#4631), every one cut short by a touch.
    val cinematics = remember(orbit) { ScanResultCinematics(orbit) }
    SideEffect {
        model?.bounds?.let { b -> cinematics.extent = maxOf(b[3] - b[0], b[5] - b[2]).coerceAtLeast(MIN_EXTENT_M) }
    }
    // Read live by the framing below, so a toggle and the frame that flies to it agree.
    var plan by remember(model) { mutableStateOf(false) }
    var touring by remember(model) { mutableStateOf(false) }
    var shown by remember(model) { mutableStateOf(false) }
    var hintVisible by remember(model) { mutableStateOf(true) }
    LaunchedEffect(shown, hintStays) {
        if (shown && !hintStays) {
            delay(HINT_MS)
            hintVisible = false
        }
    }
    var renaming by remember { mutableStateOf(false) }
    val rename = onRename?.let { { renaming = true } }

    DemoScaffold(
        title = stringResource(R.string.demo_ar_rerun_title),
        onBack = onBack,
        controls = {
            Text(stringResource(R.string.scan_result_about), style = SceneViewTokens.Type.body)
            if (rename != null) {
                TextButton(onClick = rename) { Text(stringResource(R.string.scan_result_rename)) }
            }
        },
        themedStage = true,
        topOverlay = { ScanNameCard(title = title, saved = id != null, onRename = rename) },
        bottomOverlay = {
            when {
                state == ModelState.Loading -> ModelStatusCard(
                    text = stringResource(R.string.scan_result_creating),
                    progress = { progress },
                )
                state == ModelState.Failed -> ModelStatusCard(text = stringResource(R.string.scan_result_failed))
                model?.kind == RerunScanModel.Kind.Points ->
                    ModelStatusCard(text = stringResource(R.string.scan_result_partial))
                else -> Unit
            }
            // Laid out from the start and only faded: the model is framed above this slot, and a
            // hint that came and went would move the model when it goes.
            val hintShown = shown && hintVisible
            val hintAlpha by animateFloatAsState(if (hintShown) 1f else 0f, label = "hint")
            // Laid out from the start too, for the same reason; usable once the model is seen.
            val tourReady = shown && instance != null
            // Side by side, so the legend adds no height over the model.
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val floor = ground?.ground?.takeIf { tourReady && it.seen + it.missing > 0 }
                // Without a readable recording there is no figure, but the hatching still needs its key.
                if (floor != null) FloorLegend(floorCovered?.takeIf { it > 0f }?.let { ScanAreaCopy.area(it) })
                ScanTourPill(
                    touring = touring,
                    enabled = tourReady,
                    onClick = { cinematics.request(ScanResultCinematics.Request.ToggleTour) },
                    modifier = Modifier
                        .alpha(if (tourReady) 1f else 0f)
                        .then(if (tourReady) Modifier else Modifier.clearAndSetSemantics { }),
                )
            }
            ModelHint(
                text = stringResource(R.string.scan_result_hint),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .alpha(hintAlpha)
                    .then(if (hintShown) Modifier else Modifier.clearAndSetSemantics { }),
            )
        },
        dock = listOfNotNull(
            // 3D and Plan are one camera move apart (Matterport's dollhouse ↔ floor plan); the
            // selected one recenters, as the replay's 3D view does.
            DockItem(
                icon = Icons.Rounded._3dRotation,
                label = stringResource(R.string.scan_result_3d_label),
                caption = stringResource(R.string.scan_result_3d),
                onClick = {
                    plan = false
                    cinematics.request(ScanResultCinematics.Request.Recenter)
                },
                enabled = instance != null,
                selected = !plan,
            ),
            DockItem(
                icon = Icons.Rounded.Map,
                label = stringResource(R.string.scan_result_plan_label),
                caption = stringResource(R.string.scan_result_plan),
                onClick = {
                    plan = true
                    cinematics.request(ScanResultCinematics.Request.Recenter)
                },
                enabled = instance != null,
                selected = plan,
            ),
            onReplay?.let {
                DockItem(
                    icon = Icons.Rounded.Replay,
                    label = stringResource(R.string.scan_result_replay_label),
                    caption = stringResource(R.string.scan_result_replay),
                    onClick = it,
                )
            },
            onViewInAr?.let {
                DockItem(
                    icon = Icons.Rounded.ViewInAr,
                    label = stringResource(R.string.scan_place),
                    onClick = it,
                )
            },
        ),
        dockAccent = DockItem(
            icon = Icons.Rounded.IosShare,
            label = stringResource(R.string.scan_result_export_label),
            caption = stringResource(R.string.scan_result_export),
            onClick = onExport,
            enabled = state != ModelState.Loading,
        ),
    ) {
        val chrome = LocalStageChrome.current
        val environment = rememberModelEnvironment(engine, chrome.ground)
        val view = rememberView(engine)
        val colorGrading = remember(engine) { ColorGrading.Builder().toneMapper(ToneMapper.Linear()).build(engine) }
        DisposableEffect(colorGrading) { onDispose { engine.destroyColorGrading(colorGrading) } }
        // Before the first frame, not on it: the tone mapping is part of the first picture.
        DisposableEffect(view, colorGrading) {
            view.configureForDebug(colorGrading)
            onDispose { }
        }
        val framing = remember(model, orbit) {
            model?.let { DollhouseFraming(it.bounds, RerunResultView.ELEVATION, orbit.verticalFovDegrees) }
        }
        // The floor plan: the same room, the same turn, straight down from the map's elevation.
        val planFraming = remember(model, orbit) {
            model?.let { DollhouseFraming(it.bounds, ArDebugFraming.MAP_ELEVATION, orbit.verticalFovDegrees) }
        }
        val opening = remember(model, orbit) { DollhouseOpening() }
        BoxWithConstraints(Modifier.fillMaxSize().background(chrome.ground)) {
            // What the chrome leaves of the view at rest — below the name, above the dock, between
            // the window's safe sides — less a gutter: the model opens whole in there (#4492). Held
            // sideways there is no such band: the model takes the safe area, the glass over it.
            val direction = LocalLayoutDirection.current
            val safe = WindowInsets.safeDrawing.asPaddingValues()
            val cover = if (isDemoCompactHeight()) {
                safe
            } else {
                demoContentPadding(
                    cover = LocalDemoSceneRestingCover.current,
                    sceneHeight = maxHeight,
                    left = safe.calculateLeftPadding(direction),
                    right = safe.calculateRightPadding(direction),
                )
            }
            val gutter = Space.md
            val insets = FramingInsets(
                left = (cover.calculateLeftPadding(direction) + gutter) / maxWidth,
                top = (cover.calculateTopPadding() + gutter) / maxHeight,
                right = (cover.calculateRightPadding(direction) + gutter) / maxWidth,
                bottom = (cover.calculateBottomPadding() + gutter) / maxHeight,
            )
            // The view's shape from its constraints: the surface reports its size after composing.
            val aspect = constraints.maxWidth.toFloat() / constraints.maxHeight.coerceAtLeast(1)
            val home = rememberUpdatedState {
                (if (plan) planFraming else framing)?.home(RerunResultView.AZIMUTH, aspect, insets)
            }
            SideEffect { home.value()?.let { opening.aim(orbit, it) } }
            SceneView(
                modifier = Modifier.matchParentSize(),
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                surfaceType = SurfaceType.TextureSurface,
                view = view,
                renderer = rememberRenderer(engine),
                isOpaque = true,
                frameRatePolicy = FrameRatePolicy.Continuous(),
                autoCenterContent = false,
                environment = environment,
                cameraManipulator = cinematics.manipulator,
                onFrame = {
                    val framed = home.value() ?: return@SceneView
                    // The reveal starts on the frame the model is drawn on its framing, and the
                    // view is let to be seen on its first frame, not the still one before it.
                    if (cinematics.frameDrawn(opening, framed, ready = instance != null)) shown = true
                    if (touring != cinematics.touring) touring = cinematics.touring
                },
            ) {
                // The scan in its own world space, metres, Y up: framed by its bounds, not moved.
                instance?.let { ModelNode(modelInstance = it, autoAnimate = false) }
                ground?.let {
                    ScanGroundLayer(it.meshes, engine, materialLoader, ScanGroundColors.current())
                }
            }
            // The ground until the model is drawn on its framing: no pose but that one is seen.
            if (!shown) Box(Modifier.matchParentSize().background(chrome.ground).testTag(RERUN_MODEL_VEIL_TAG))
        }
    }
    if (renaming && onRename != null) {
        RenameDialog(
            title = title,
            onDismiss = { renaming = false },
            onRename = {
                renaming = false
                onRename(it)
            },
        )
    }
}

/** The scan's name, and where it is kept; a tap renames it. */
@Composable
private fun ScanNameCard(title: String, saved: Boolean, onRename: (() -> Unit)?) {
    val chrome = LocalStageChrome.current
    val shape = RoundedCornerShape(SceneViewTokens.Radius.lg)
    val label = stringResource(R.string.scan_result_rename)
    Box(Modifier.fillMaxWidth().padding(horizontal = Space.md), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .widthIn(max = SceneViewTokens.ArOverlay.maxWidth)
                .heightIn(min = SceneViewTokens.Layout.touchTarget)
                .clip(shape)
                .background(chrome.card, shape)
                .overMediaEdge(shape, chrome.edgeRing, chrome.edgeHalo)
                .then(
                    if (onRename != null) {
                        Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onRename)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = Space.md, vertical = Space.sm)
                .testTag(RERUN_MODEL_NAME_TAG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Column(Modifier.weight(1f, fill = false)) {
                Text(
                    title,
                    style = SceneViewTokens.Type.card,
                    color = chrome.onCard,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (saved) {
                    Text(
                        stringResource(R.string.scan_result_saved),
                        style = SceneViewTokens.Type.caption,
                        color = chrome.onCardMuted,
                    )
                }
            }
            if (onRename != null) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = chrome.onCardMuted,
                    modifier = Modifier.size(Space.lg),
                )
            }
        }
    }
}

/** What the model screen is doing, or why it shows less than a surface. */
@Composable
private fun ModelStatusCard(text: String, progress: (() -> Float)? = null) {
    val chrome = LocalStageChrome.current
    OverlayCard(testTag = RERUN_MODEL_CARD_TAG) {
        Text(text, style = SceneViewTokens.Type.card, color = chrome.onCard)
        if (progress != null) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = chrome.accent,
                trackColor = chrome.track,
                drawStopIndicator = {},
            )
        }
    }
}

/** The ground of a model (#4719): where it stands and which of its floor it saw. */
private class GroundBuild(val ground: ScanGround, val meshes: ScanGroundMeshes)

/** [model]'s ground, `null` when its vertices cannot be read or stand on nothing. Off the main thread. */
private fun groundOf(model: RerunScanModel): GroundBuild? = runCatching {
    val started = System.nanoTime()
    val mesh = RerunGlbMesh.read(model.glb) ?: return@runCatching null
    val ground = ScanGround.of(mesh.positions, mesh.normals, mesh.vertexCount) ?: return@runCatching null
    GroundBuild(ground, ScanGroundMeshes.of(ground)).also {
        Log.i(
            TAG,
            "ground: floor y ${"%.2f".format(ground.floorY)} m, ${ground.columns}x${ground.rows} cells of " +
                "${(ground.cellM * CM_PER_M).toInt()} cm, ${ground.seen} seen, ${ground.missing} missing, " +
                "${it.meshes.triangles} triangles in ${(System.nanoTime() - started) / NS_PER_MS} ms",
        )
    }
}.onFailure { Log.w(TAG, "ground failed", it) }.getOrNull()

/**
 * How much of the floor the scan covers, and the hatching that marks the rest: the live map's
 * own words and swatch, so the result reads as the map the scan was filling in. Without [area] (a
 * recording that cannot be read) it keys the hatching alone.
 */
@Composable
private fun FloorLegend(area: String?) {
    val chrome = LocalStageChrome.current
    val shape = RoundedCornerShape(SceneViewTokens.Radius.full)
    val label = area?.let { stringResource(R.string.scan_coverage_label, it) }
        ?: stringResource(R.string.scan_coverage_label_hatched)
    val colors = ScanGroundColors.current()
    Row(
        modifier = Modifier
            .heightIn(min = SceneViewTokens.Layout.touchTarget)
            .clip(shape)
            .background(chrome.card, shape)
            .overMediaEdge(shape, chrome.edgeRing, chrome.edgeHalo)
            .padding(horizontal = Space.md, vertical = Space.sm)
            .clearAndSetSemantics { contentDescription = label }
            .testTag(RERUN_MODEL_FLOOR_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        val swatch = RoundedCornerShape(SceneViewTokens.Radius.xs)
        // On a dark stage the hole's veil and the amber are close in tone, and three fine stripes
        // melt into one beige disc at this size: the swatch takes the dark ground's own look —
        // fewer, bolder stripes, framed by the hole's `outline-variant` edge.
        val lines = if (colors.dark) DARK_HATCH_LINES else HATCH_LINES
        val share = if (colors.dark) DARK_HATCH_STROKE else HATCH_STROKE
        val ring = Modifier.border(SceneViewTokens.Layout.selectedOutlineWidth, colors.edge, swatch)
        Canvas(Modifier.size(Space.md).clip(swatch).then(if (colors.dark) ring else Modifier)) {
            drawRect(colors.missing)
            val step = size.width / lines
            val stroke = step * share
            for (i in -lines..lines) {
                val x = i * step
                drawLine(colors.hatch, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stroke)
            }
        }
        Text(
            text = area?.let { stringResource(R.string.scan_coverage_floor, it) }
                ?: stringResource(R.string.scan_coverage_hatched),
            style = SceneViewTokens.Type.caption,
            color = chrome.onCard,
        )
    }
}

/** The one-line gesture hint, a pill that leaves after a few seconds. */
@Composable
private fun ModelHint(text: String, modifier: Modifier = Modifier) {
    val chrome = LocalStageChrome.current
    val shape = RoundedCornerShape(SceneViewTokens.Radius.full)
    Text(
        text = text,
        style = SceneViewTokens.Type.caption,
        color = chrome.onCard,
        modifier = modifier
            .padding(top = Space.sm)
            .clip(shape)
            .background(chrome.card, shape)
            .overMediaEdge(shape, chrome.edgeRing, chrome.edgeHalo)
            .padding(horizontal = Space.md, vertical = Space.sm)
            .testTag(RERUN_MODEL_HINT_TAG),
    )
}

/**
 * Tour, and Stop while it plays (#4631): a pill in the stage's chrome over the model, beside
 * which the hint is read — the one camera move the user asks for, never one that starts alone.
 */
@Composable
private fun ScanTourPill(touring: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chrome = LocalStageChrome.current
    val shape = RoundedCornerShape(SceneViewTokens.Radius.full)
    val label = stringResource(R.string.scan_result_tour_label)
    Row(
        modifier = modifier
            .heightIn(min = SceneViewTokens.Layout.touchTarget)
            .clip(shape)
            .background(chrome.card, shape)
            .overMediaEdge(shape, chrome.edgeRing, chrome.edgeHalo)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm)
            .testTag(RERUN_MODEL_TOUR_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Icon(
            imageVector = if (touring) Icons.Rounded.Stop else Icons.Rounded._360,
            contentDescription = null,
            tint = chrome.onCard,
            modifier = Modifier.size(SceneViewTokens.Layout.dockIconSize),
        )
        Text(
            text = stringResource(if (touring) R.string.scan_result_tour_stop else R.string.scan_result_tour),
            style = SceneViewTokens.Type.caption,
            color = chrome.onCard,
        )
    }
}

@Composable
private fun RenameDialog(title: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var draft by remember { mutableStateOf(title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scan_result_rename_title)) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(NAME_MAX_LENGTH) },
                singleLine = true,
                label = { Text(stringResource(R.string.scan_result_rename_field)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(draft.trim()) }, enabled = draft.isNotBlank()) {
                Text(stringResource(R.string.scan_result_rename_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.scan_result_cancel)) } },
    )
}

@Composable
private fun rememberModelEnvironment(engine: Engine, ground: androidx.compose.ui.graphics.Color): Environment {
    val context = androidx.compose.ui.platform.LocalContext.current
    return rememberEnvironment(engine, key = ground) {
        val stage = colorOf(ground).toLinearSpace()
        val bundle = KTX1Loader.createIndirectLight(
            engine,
            context.assets.readBuffer("environments/neutral/neutral_ibl.ktx"),
        )
        createEnvironment(
            engine = engine,
            indirectLight = bundle.indirectLight?.also { it.intensity = DEFAULT_IBL_INTENSITY },
            skybox = Skybox.Builder().color(stage.x, stage.y, stage.z, 1f).build(engine),
            // The bundle's cubemap leaves with the environment, after the light (#4358).
            textures = listOfNotNull(bundle.cubemap),
        )
    }
}

internal const val RERUN_MODEL_CARD_TAG = "rerun_model_card"
internal const val RERUN_MODEL_NAME_TAG = "rerun_model_name"
internal const val RERUN_MODEL_HINT_TAG = "rerun_model_hint"
internal const val RERUN_MODEL_TOUR_TAG = "rerun_model_tour"
internal const val RERUN_MODEL_VEIL_TAG = "rerun_model_veil"
internal const val RERUN_MODEL_FLOOR_TAG = "rerun_model_floor"

/** The legend's swatch: this many hatching lines across it, each this share of their spacing. */
private const val HATCH_LINES = 3
private const val HATCH_STROKE = 0.35f
private const val DARK_HATCH_LINES = 2
private const val DARK_HATCH_STROKE = 0.42f
private const val CM_PER_M = 100f

/** The smallest scan the tour pushes into, metres: an object on a table. */
private const val MIN_EXTENT_M = 0.2f

private const val TAG = "RerunModel"
private const val LOG_EVERY_FRAMES = 30
private const val MB = 1024L * 1024L
private const val KB = 1024
private const val NS_PER_MS = 1_000_000L

/** A live TSDF voxel meshes once two medium-confidence views (or one sure one) agree on it. */
private const val LIVE_MIN_WEIGHT = 1f
private const val FUSE_SHARE = 0.5f
private const val MESH_SHARE = 0.95f

/** The mesh's share of the progress bar; the points fallback, when needed, takes the rest. */
private const val MESH_PART = 0.9f

/** How long the gesture hint stays once the model is on screen. */
private const val HINT_MS = 4_000L
private const val NAME_MAX_LENGTH = 80
