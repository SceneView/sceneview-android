@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.sceneview.demo.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.sceneview.demo.R
import io.github.sceneview.demo.common.DemoModalBottomSheet
import io.github.sceneview.demo.demos.internal.ReconstructionMethod
import io.github.sceneview.demo.theme.SceneViewTokens

/*
 * The end of a scan's choice of reconstruction (#4630): the two the phone can run, what each
 * gives, and how long the filled one takes against the other, as the benchmark measured it.
 */

/** Whether [source] can be filled: [ReconstructionMethod.Poisson] needs the scan's surfels. */
internal fun canFill(source: RerunModelSource): Boolean = when (source) {
    is RerunModelSource.Live -> source.cloud?.normals != null
    is RerunModelSource.Surfels -> source.cloud.normals != null
    is RerunModelSource.Synthetic -> true
}

/**
 * The sheet that asks how to finish the model: [ReconstructionChoices] in the demo's sheet.
 * [onFinish] gets the method picked; dismissing it finishes nothing.
 */
@Composable
internal fun ReconstructionSheet(
    canFill: Boolean,
    initial: ReconstructionMethod,
    onFinish: (ReconstructionMethod) -> Unit,
    onDismiss: () -> Unit,
) {
    DemoModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = SceneViewTokens.Radius.xl, topEnd = SceneViewTokens.Radius.xl),
    ) {
        ReconstructionChoices(canFill, initial, onFinish, Modifier.navigationBarsPadding())
    }
}

/** The sheet's content: the two methods, one picked, and the button that finishes with it. */
@Composable
internal fun ReconstructionChoices(
    canFill: Boolean,
    initial: ReconstructionMethod,
    onFinish: (ReconstructionMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picked by remember(canFill, initial) {
        mutableStateOf(if (canFill) initial else ReconstructionMethod.Fused)
    }
    Column(
        modifier.fillMaxWidth().padding(SceneViewTokens.Space.md),
        verticalArrangement = Arrangement.spacedBy(SceneViewTokens.Space.sm),
    ) {
        Text(stringResource(R.string.scan_finish_method_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.scan_finish_method_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.selectableGroup()) {
            Choice(
                method = ReconstructionMethod.Fused,
                title = stringResource(R.string.scan_finish_method_fused),
                detail = stringResource(R.string.scan_finish_method_fused_detail),
                selected = picked == ReconstructionMethod.Fused,
                enabled = true,
                onPick = { picked = it },
            )
            Choice(
                method = ReconstructionMethod.Poisson,
                title = stringResource(R.string.scan_finish_method_filled),
                detail = stringResource(
                    if (canFill) R.string.scan_finish_method_filled_detail else R.string.scan_finish_method_filled_unavailable,
                ),
                selected = picked == ReconstructionMethod.Poisson,
                enabled = canFill,
                onPick = { picked = it },
            )
        }
        Button(
            onClick = { onFinish(picked) },
            modifier = Modifier.fillMaxWidth().padding(top = SceneViewTokens.Space.xs).testTag(FINISH_TAG),
        ) { Text(stringResource(R.string.scan_finish_method_finish)) }
    }
}

@Composable
private fun Choice(
    method: ReconstructionMethod,
    title: String,
    detail: String,
    selected: Boolean,
    enabled: Boolean,
    onPick: (ReconstructionMethod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = { onPick(method) })
            .padding(vertical = SceneViewTokens.Space.sm)
            .testTag(choiceTag(method)),
        verticalAlignment = Alignment.Top,
    ) {
        // Selection is the Row's: the button itself is inert.
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.padding(start = SceneViewTokens.Space.sm)) {
            val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
            Text(title, style = MaterialTheme.typography.titleMedium, color = color)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal const val FINISH_TAG = "reconstruction-finish"

internal fun choiceTag(method: ReconstructionMethod) = "reconstruction-${method.name.lowercase()}"

private const val DISABLED_ALPHA = 0.38f
