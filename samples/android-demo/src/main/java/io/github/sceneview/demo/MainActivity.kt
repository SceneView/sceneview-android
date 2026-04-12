package io.github.sceneview.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.sceneview.demo.demos.AnimationDemo
import io.github.sceneview.demo.demos.ARPlacementDemo
import io.github.sceneview.demo.demos.CameraControlsDemo
import io.github.sceneview.demo.demos.EnvironmentDemo
import io.github.sceneview.demo.demos.FogDemo
import io.github.sceneview.demo.demos.GeometryDemo
import io.github.sceneview.demo.demos.LightingDemo
import io.github.sceneview.demo.demos.ModelViewerDemo
import io.github.sceneview.demo.demos.TextDemo
import io.github.sceneview.demo.demos.LinesPathsDemo
import io.github.sceneview.demo.demos.ImageDemo
import io.github.sceneview.demo.demos.BillboardDemo
import io.github.sceneview.demo.demos.VideoDemo
import io.github.sceneview.demo.demos.ViewNodeDemo
import io.github.sceneview.demo.demos.GestureEditingDemo
import io.github.sceneview.demo.demos.CollisionDemo
import io.github.sceneview.demo.theme.SceneViewDemoTheme
import io.github.sceneview.demo.update.InAppUpdateManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateManager = InAppUpdateManager(this)
        setContent {
            SceneViewDemoTheme {
                SceneViewDemoApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateManager.checkForStalledUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateManager.destroy()
    }
}

@Composable
fun SceneViewDemoApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "list",
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        composable("list") {
            DemoListScreen(onDemoClick = { id -> navController.navigate("demo/$id") })
        }
        composable("demo/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val onBack: () -> Unit = { navController.popBackStack() }
            DemoRouter(id = id, onBack = onBack)
        }
    }
}

/**
 * Routes a demo [id] to the corresponding composable.
 * Demos not yet implemented show a placeholder.
 */
@Composable
fun DemoRouter(id: String, onBack: () -> Unit) {
    when (id) {
        // 3D Basics
        "model-viewer" -> ModelViewerDemo(onBack)
        "geometry" -> GeometryDemo(onBack)
        "animation" -> AnimationDemo(onBack)
        // Lighting & Environment
        "lighting" -> LightingDemo(onBack)
        "fog" -> FogDemo(onBack)
        "environment" -> EnvironmentDemo(onBack)
        // Interaction
        "camera-controls" -> CameraControlsDemo(onBack)
        // Content
        "text" -> TextDemo(onBack)
        "lines-paths" -> LinesPathsDemo(onBack)
        "image" -> ImageDemo(onBack)
        "billboard" -> BillboardDemo(onBack)
        "video" -> VideoDemo(onBack)
        // Interaction
        "gesture-editing" -> GestureEditingDemo(onBack)
        "collision" -> CollisionDemo(onBack)
        "view-node" -> ViewNodeDemo(onBack)
        // Augmented Reality
        "ar-placement" -> ARPlacementDemo(onBack)
        // Not yet implemented
        else -> PlaceholderDemo(id = id, onBack = onBack)
    }
}

@Composable
fun PlaceholderDemo(id: String, onBack: () -> Unit) {
    val entry = ALL_DEMOS.find { it.id == id }
    DemoScaffold(
        title = entry?.title ?: id,
        onBack = onBack
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
