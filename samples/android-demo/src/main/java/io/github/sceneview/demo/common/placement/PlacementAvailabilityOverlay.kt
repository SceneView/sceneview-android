package io.github.sceneview.demo.common.placement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.sceneview.ar.ARCoreAvailabilityOverlay
import io.github.sceneview.ar.ARCoreAvailabilityState
import io.github.sceneview.demo.LocalDemoChromeBottomInset
import io.github.sceneview.demo.LocalDemoChromeBottomWidth
import io.github.sceneview.demo.LocalDemoChromeTopInset
import io.github.sceneview.demo.isDemoCompactHeight
import io.github.sceneview.demo.theme.SceneViewTokens

/**
 * The SDK's "Couldn't start AR" / "Install Google Play Services for AR" card, placed for a
 * screen that keeps chrome on while it is up.
 *
 * The SDK centres that card in the scene. Under a [io.github.sceneview.demo.DemoScaffold] with
 * a mode pill the scene runs behind the pill and the dock, so on a phone held sideways the
 * card's "Try again" came up under "Place / Free pose / One call" (#4509). Here the card is
 * centred in what the chrome leaves: between the identity row and the bottom chrome, or — in a
 * short window, where that band is thinner than the card — in the corner beside the centred
 * chrome, exactly as the placement cards do ([coachStackSlot]).
 */
@Composable
internal fun BoxScope.PlacementAvailabilityOverlay(state: ARCoreAvailabilityState) {
    val chromeBottom = LocalDemoChromeBottomInset.current
    val chromeWidth = LocalDemoChromeBottomWidth.current
    val compactHeight = isDemoCompactHeight()
    val chromeTop = LocalDemoChromeTopInset.current
    val gutter = SceneViewTokens.Space.md
    BoxWithConstraints(
        modifier = Modifier
            .matchParentSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val slot = coachStackSlot(
            compactHeight = compactHeight,
            cardShown = true,
            bandWidth = maxWidth,
            chromeWidth = chromeWidth,
            chromeBottom = chromeBottom,
            gutter = gutter,
            // One action, where the placement card lays two side by side: half the room. And
            // the alternative, the band above the pill, cannot hold this card's button at all
            // once the text is large — the corner has the window's whole height, and the
            // card's text scrolls.
            minWidth = SceneViewTokens.ArOverlay.cardMinWidth / 2,
            maxWidth = SceneViewTokens.ArOverlay.maxWidth,
        )
        Box(
            modifier = if (slot.besideChrome) {
                // The SDK card brings its own margin; the corner is the stack's width plus the
                // two gutters `coachStackSlot` set aside around it. Under the identity row:
                // its overflow button sits in this same corner.
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(slot.width + gutter * 2)
                    .fillMaxHeight()
                    .padding(top = chromeTop)
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(top = chromeTop, bottom = chromeBottom)
            },
        ) {
            ARCoreAvailabilityOverlay(state)
        }
    }
}
