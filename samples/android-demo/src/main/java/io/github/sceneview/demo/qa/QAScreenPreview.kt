@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.qa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sceneview.demo.theme.SceneViewDemoTheme

private val sampleTest = QATest(
    id = "cube_render",
    name = "CubeNode Rendering",
    description = "Verifies CubeNode renders with correct geometry",
    category = "Geometry"
)

@Preview(showBackground = true, name = "QATestCard - Idle")
@Composable
private fun QATestCardIdlePreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                QATestCard(test = sampleTest, status = QATestStatus.IDLE, onRun = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "QATestCard - Running")
@Composable
private fun QATestCardRunningPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                QATestCard(test = sampleTest, status = QATestStatus.RUNNING, onRun = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "QATestCard - Pass")
@Composable
private fun QATestCardPassPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                QATestCard(test = sampleTest, status = QATestStatus.PASS, onRun = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "QATestCard - Fail")
@Composable
private fun QATestCardFailPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                QATestCard(test = sampleTest, status = QATestStatus.FAIL, onRun = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "QATestCard - All States")
@Composable
private fun QATestCardAllStatesPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                QATestStatus.entries.forEach { status ->
                    QATestCard(test = sampleTest, status = status, onRun = {})
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "QATestCard - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QATestCardDarkPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                QATestStatus.entries.forEach { status ->
                    QATestCard(test = sampleTest, status = status, onRun = {})
                }
            }
        }
    }
}

@Preview(showSystemUi = true, name = "QAScreen - Full")
@Composable
private fun QAScreenPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        QAScreen()
    }
}
