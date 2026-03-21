@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Explore
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.sceneview.demo.qa.QAScreen
import io.github.sceneview.demo.theme.SceneViewDemoTheme

@Preview(showSystemUi = true, name = "App - Explore Tab")
@Composable
private fun AppExplorePreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        NavigationShellPreview(selectedTab = 0)
    }
}

@Preview(showSystemUi = true, name = "App - Showcase Tab")
@Composable
private fun AppShowcasePreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        NavigationShellPreview(selectedTab = 1)
    }
}

@Preview(showSystemUi = true, name = "App - QA Tab")
@Composable
private fun AppQAPreview() {
    SceneViewDemoTheme(dynamicColor = false) {
        NavigationShellPreview(selectedTab = 2)
    }
}

@Preview(showSystemUi = true, name = "App - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppDarkPreview() {
    SceneViewDemoTheme(darkTheme = true, dynamicColor = false) {
        NavigationShellPreview(selectedTab = 0)
    }
}

@Composable
private fun NavigationShellPreview(selectedTab: Int) {
    var selected by remember { mutableIntStateOf(selectedTab) }
    val tabs = listOf(
        Triple("Explore", Icons.Default.Explore, "Explore"),
        Triple("Showcase", Icons.Default.ViewInAr, "Showcase"),
        Triple("QA Tests", Icons.Default.BugReport, "QA Tests")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEachIndexed { index, (label, icon, _) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        selected = selected == index,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = { selected = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selected) {
                0 -> Text(
                    "Explore Screen\n(3D Scene — not available in preview)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                1 -> Text(
                    "Showcase Screen\n(3D previews — not available in preview)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                2 -> QAScreen()
            }
        }
    }
}
