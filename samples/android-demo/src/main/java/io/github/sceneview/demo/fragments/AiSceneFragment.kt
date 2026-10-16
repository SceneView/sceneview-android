package io.github.sceneview.demo.fragments

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import io.github.sceneview.demo.DemoCategory
import io.github.sceneview.demo.DemoEntry
import io.github.sceneview.demo.DemoStatus
import io.github.sceneview.demo.R
import io.github.sceneview.demo.demos.AiSceneDemo

/**
 * Append-only fragment for the `ai-scene` prototype. Listed in debug builds only
 * (`DEBUG_ONLY_DEMO_IDS`); see [DemoFragment].
 */
object AiSceneFragment : DemoFragment {
    override val entry: DemoEntry = DemoEntry(
        id = "ai-scene",
        titleRes = R.string.demo_ai_scene_title,
        subtitleRes = R.string.demo_ai_scene_subtitle,
        category = DemoCategory.DEV_TOOLS,
        icon = Icons.Rounded.AutoAwesome,
        order = 13,
        addedIn = "4.57.0",
        tags = setOf("ai", "claude", "llm", "tools", "reconstruction", "room", "rerun", "point cloud"),
        status = DemoStatus.InReview,
    )

    @Composable
    override fun Screen(onBack: () -> Unit) {
        AiSceneDemo(onBack)
    }
}
