package io.github.sceneview.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * SceneView Desktop — Compose Desktop application scaffold.
 *
 * Architecture:
 * - Compose Desktop provides the UI framework (window, layout, Material 3)
 * - LWJGL provides the OpenGL context for native rendering
 * - Filament (C++) will render 3D content via JNI into the OpenGL context
 * - sceneview-core (KMP) provides math, collision, geometry, animation
 *
 * Current status: SCAFFOLD — Filament JNI desktop bindings not yet available.
 * The UI framework and LWJGL setup are in place; Filament integration is TODO.
 *
 * To run: ./gradlew :sceneview-desktop:run
 */
fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "SceneView Desktop — 3D Viewer",
        state = windowState,
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            DesktopModelViewer()
        }
    }
}

@Composable
fun DesktopModelViewer() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Placeholder canvas — will be replaced with Filament OpenGL rendering
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw a grid to indicate the 3D viewport area
                val gridSize = 40f
                val gridColor = Color(0xFF2A2A3E)
                for (x in 0..((size.width / gridSize).toInt())) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x * gridSize, 0f),
                        end = Offset(x * gridSize, size.height),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..((size.height / gridSize).toInt())) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y * gridSize),
                        end = Offset(size.width, y * gridSize),
                        strokeWidth = 1f
                    )
                }

                // Center crosshair
                val center = Offset(size.width / 2, size.height / 2)
                drawLine(Color(0xFF4A4A6A), center.copy(x = center.x - 20), center.copy(x = center.x + 20), 2f)
                drawLine(Color(0xFF4A4A6A), center.copy(y = center.y - 20), center.copy(y = center.y + 20), 2f)
            }

            // Info overlay
            Column(
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SceneView Desktop",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Filament renderer — coming soon",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "Windows • macOS • Linux",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Status bar
            Text(
                "LWJGL + OpenGL ready | Filament JNI: pending",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
        }
    }
}
