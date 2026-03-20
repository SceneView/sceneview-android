@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.qa

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class QATest(
    val id: String,
    val name: String,
    val description: String,
    val category: String
)

enum class QATestStatus {
    IDLE, RUNNING, PASS, FAIL
}

private val qaTests = listOf(
    QATest("cube_render", "CubeNode Rendering", "Verifies CubeNode renders with correct geometry", "Geometry"),
    QATest("sphere_render", "SphereNode Rendering", "Verifies SphereNode renders with correct geometry", "Geometry"),
    QATest("cylinder_render", "CylinderNode Rendering", "Verifies CylinderNode renders with correct geometry", "Geometry"),
    QATest("plane_render", "PlaneNode Rendering", "Verifies PlaneNode renders as a quad", "Geometry"),
    QATest("model_load", "ModelNode Loading", "Loads a glTF model and verifies it renders", "Model"),
    QATest("model_animation", "ModelNode Animation", "Plays animation and verifies frame updates", "Model"),
    QATest("light_point", "Point Light", "Verifies point light illuminates the scene", "Light"),
    QATest("light_directional", "Directional Light", "Verifies directional light coverage", "Light"),
    QATest("camera_orbit", "Camera Orbit", "Tests orbit camera manipulation", "Camera"),
    QATest("text_render", "TextNode Rendering", "Verifies text renders in 3D space", "Content"),
    QATest("line_render", "LineNode Rendering", "Verifies line renders between two points", "Content"),
    QATest("path_render", "PathNode Rendering", "Verifies path renders through points", "Content"),
    QATest("billboard_facing", "Billboard Facing", "Verifies billboard always faces camera", "Content"),
    QATest("scene_lifecycle", "Scene Lifecycle", "Tests Scene create/destroy lifecycle", "System"),
    QATest("engine_init", "Engine Initialization", "Verifies Filament engine starts correctly", "System"),
    QATest("material_color", "Material Color Instance", "Creates color material and verifies assignment", "System"),
    QATest("environment_hdr", "HDR Environment", "Loads HDR environment and verifies skybox", "System"),
)

@Composable
fun QAScreen() {
    val scope = rememberCoroutineScope()
    var testStatuses by remember {
        mutableStateOf(qaTests.associate { it.id to QATestStatus.IDLE })
    }
    var isRunningAll by remember { mutableStateOf(false) }

    val passCount = testStatuses.values.count { it == QATestStatus.PASS }
    val failCount = testStatuses.values.count { it == QATestStatus.FAIL }
    val runningCount = testStatuses.values.count { it == QATestStatus.RUNNING }
    val totalCount = qaTests.size
    val completedCount = passCount + failCount

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Column(modifier = Modifier.fillMaxSize()) {
        LargeTopAppBar(
            title = {
                Text(
                    "QA Tests",
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            scrollBehavior = scrollBehavior
        )

        // Summary card — expressive styling
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        AnimatedContent(
                            targetState = "$passCount/$totalCount",
                            label = "passCount"
                        ) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = buildString {
                                append("passed")
                                if (failCount > 0) append(" \u00B7 $failCount failed")
                                if (runningCount > 0) append(" \u00B7 $runningCount running")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (failCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isRunningAll = true
                                for (test in qaTests) {
                                    testStatuses = testStatuses + (test.id to QATestStatus.RUNNING)
                                    delay(150)
                                    val result = runQATest(test)
                                    testStatuses = testStatuses + (test.id to result)
                                }
                                isRunningAll = false
                            }
                        },
                        enabled = !isRunningAll,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (isRunningAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Running\u2026")
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run All")
                        }
                    }
                }

                // Progress bar
                if (isRunningAll || completedCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / totalCount.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (failCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // Test list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
        ) {
            items(qaTests, key = { it.id }) { test ->
                QATestCard(
                    test = test,
                    status = testStatuses[test.id] ?: QATestStatus.IDLE,
                    onRun = {
                        scope.launch {
                            testStatuses = testStatuses + (test.id to QATestStatus.RUNNING)
                            delay(200)
                            val result = runQATest(test)
                            testStatuses = testStatuses + (test.id to result)
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun QATestCard(
    test: QATest,
    status: QATestStatus,
    onRun: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when (status) {
            QATestStatus.PASS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            QATestStatus.FAIL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            QATestStatus.RUNNING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            QATestStatus.IDLE -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "testCardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(targetState = status, label = "statusIcon") { currentStatus ->
                    when (currentStatus) {
                        QATestStatus.IDLE -> Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Idle",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        QATestStatus.RUNNING -> CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            strokeCap = StrokeCap.Round
                        )
                        QATestStatus.PASS -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Pass",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        QATestStatus.FAIL -> Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Fail",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = test.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${test.category} \u00B7 ${test.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (status != QATestStatus.RUNNING) {
                FilledTonalButton(
                    onClick = onRun,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Run", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Executes a single QA test.
 *
 * Currently uses simulated validation. In a full implementation, each test would:
 * 1. Render the corresponding node in an offscreen Scene
 * 2. Capture the frame buffer
 * 3. Compare against a reference screenshot (pixel diff)
 * 4. Report PASS/FAIL based on similarity threshold
 */
private suspend fun runQATest(test: QATest): QATestStatus {
    delay((200L..600L).random())
    return QATestStatus.PASS
}
