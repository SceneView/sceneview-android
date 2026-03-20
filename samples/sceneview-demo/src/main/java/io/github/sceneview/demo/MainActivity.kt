package io.github.sceneview.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.sceneview.demo.showcase.ShowcaseScreen
import io.github.sceneview.demo.qa.QAScreen
import io.github.sceneview.demo.update.InAppUpdateManager

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateManager = InAppUpdateManager(this)

        setContent {
            SceneViewDemoApp(updateManager)
        }
    }

    override fun onResume() {
        super.onResume()
        updateManager.checkForStalledUpdate()
    }
}

sealed class Screen(val route: String, val label: String) {
    data object Showcase : Screen("showcase", "Showcase")
    data object QA : Screen("qa", "QA Tests")
}

@Composable
fun SceneViewDemoApp(updateManager: InAppUpdateManager) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Showcase, Screen.QA)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.Showcase -> Icons.Default.ViewInAr
                                    Screen.QA -> Icons.Default.BugReport
                                },
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
        NavHost(
            navController = navController,
            startDestination = Screen.Showcase.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Showcase.route) {
                ShowcaseScreen(updateManager = updateManager)
            }
            composable(Screen.QA.route) {
                QAScreen()
            }
        }
    }
}
