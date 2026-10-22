package io.github.sceneview.demo.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.filament.Engine
import io.github.sceneview.SceneScope
import io.github.sceneview.demo.demos.internal.DebugMesh
import io.github.sceneview.demo.demos.internal.RescanZone
import io.github.sceneview.demo.demos.internal.ScanGround
import io.github.sceneview.loaders.MaterialLoader

/** The pill that lists the parts of a scan worth scanning again (#4736). */
internal const val RERUN_MODEL_RESCAN_TAG = "rerun_model_rescan"

/** One part's "Re-scan this part" in the list. */
internal const val RERUN_MODEL_RESCAN_ACTION_TAG = "rerun_model_rescan_action"

/** The parts of a scan worth scanning again, and the frames the result draws them with. */
internal class RescanBuild(val zones: List<RescanZone>, val mesh: DebugMesh)

/**
 * The parts of [source]'s scan worth scanning again (#4736), [ground] the floor read off its
 * model, or `null` when there are none or no points to read them from. Off the main thread: it
 * reads the whole cloud.
 */
@Suppress("UnusedParameter", "UNUSED_PARAMETER", "FunctionOnlyReturningConstant")
internal fun rescanOf(source: RerunModelSource?, ground: ScanGround?): RescanBuild? = null

/** The parts to scan again framed in the scan's own world space (#4736). */
@Suppress("UnusedParameter", "UNUSED_PARAMETER", "UnusedReceiverParameter")
@Composable
internal fun SceneScope.RescanZoneLayer(mesh: DebugMesh, engine: Engine, materialLoader: MaterialLoader) = Unit

/** How many parts to scan again, a pill beside the tour's. A tap lists them. */
@Suppress("UnusedParameter", "UNUSED_PARAMETER")
@Composable
internal fun RescanZonesPill(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) = Unit

/** The parts to scan again, each with its kind, why, and its own "Re-scan this part". */
@Suppress("UnusedParameter", "UNUSED_PARAMETER")
@Composable
internal fun RescanZonesDialog(zones: List<RescanZone>, onDismiss: () -> Unit) = Unit
