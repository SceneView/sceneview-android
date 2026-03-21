@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.sceneview.demo.explore.ExploreScreen
import io.github.sceneview.demo.gallery.GalleryScreen
import io.github.sceneview.demo.showcase.ShowcaseScreen
import io.github.sceneview.demo.qa.QAScreen
import io.github.sceneview.demo.theme.SceneViewDemoTheme
import io.github.sceneview.demo.update.InAppUpdateManager
import io.github.sceneview.demo.update.UpdateBanner

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateManager = InAppUpdateManager(this)

        setContent {
            SceneViewDemoTheme {
                SceneViewDemoApp(updateManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateManager.checkForStalledUpdate()
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Explore : Screen("explore", "Explore", Icons.Default.Explore)
    data object Showcase : Screen("showcase", "Showcase", Icons.Default.ViewInAr)
    data object Gallery : Screen("gallery", "Gallery", Icons.Default.PhotoLibrary)
    data object QA : Screen("qa", "QA Tests", Icons.Default.BugReport)
}

@Composable
fun SceneViewDemoApp(updateManager: InAppUpdateManager) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Explore, Screen.Showcase, Screen.Gallery, Screen.QA)

    // Auto-check for updates on launch
    LaunchedEffect(Unit) {
        updateManager.checkForUpdate()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.92f,
                        label = "navScale"
                    )

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                modifier = Modifier.scale(scale)
                            )
                        },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelMedium) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Global update banner — visible across all tabs
            UpdateBanner(updateManager)

            NavHost(
                navController = navController,
                startDestination = Screen.Explore.route,
                modifier = Modifier.weight(1f),
                enterTransition = { fadeIn() + scaleIn(initialScale = 0.96f) },
                exitTransition = { fadeOut() + scaleOut(targetScale = 0.96f) },
                popEnterTransition = { fadeIn() + scaleIn(initialScale = 0.96f) },
                popExitTransition = { fadeOut() + scaleOut(targetScale = 0.96f) }
            ) {
                composable(Screen.Explore.route) {
                    ExploreScreen()
                }
                composable(Screen.Showcase.route) {
                    ShowcaseScreen(updateManager = updateManager)
                }
                composable(Screen.Gallery.route) {
                    GalleryScreen()
                }
                composable(Screen.QA.route) {
                    QAScreen()
                }
            }
        }
    }
}
