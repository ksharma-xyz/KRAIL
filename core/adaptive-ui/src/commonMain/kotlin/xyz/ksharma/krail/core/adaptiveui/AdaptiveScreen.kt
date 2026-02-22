package xyz.ksharma.krail.core.adaptiveui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import xyz.ksharma.krail.core.log.log

@Composable
fun AdaptiveScreenContent(
    singlePaneContent: @Composable () -> Unit = {},
    dualPaneContent: @Composable () -> Unit = {},
) {
    val singlePaneContent = remember { movableContentOf { singlePaneContent() } }
    val dualPaneContent = remember { movableContentOf { dualPaneContent() } }
    val adaptiveLayoutInfo = rememberAdaptiveLayoutInfo()

    SideEffect {
        log("[ADAPTIVE] pane=${if (adaptiveLayoutInfo.shouldShowDualPane) "DUAL" else "SINGLE"}")
    }

    if (adaptiveLayoutInfo.shouldShowDualPane) {
        dualPaneContent()
    } else {
        singlePaneContent()
    }
}
