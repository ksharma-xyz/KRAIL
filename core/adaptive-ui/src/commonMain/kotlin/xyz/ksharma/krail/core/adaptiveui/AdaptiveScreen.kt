package xyz.ksharma.krail.core.adaptiveui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember

@Composable
fun AdaptiveScreenContent(
    singlePaneContent: @Composable () -> Unit = {},
    dualPaneContent: @Composable () -> Unit = {},
) {
    val singlePaneContent = remember { movableContentOf { singlePaneContent() } }
    val dualPaneContent = remember { movableContentOf { dualPaneContent() } }
    val adaptiveLayoutInfo = rememberAdaptiveLayoutInfo()
    if (adaptiveLayoutInfo.shouldShowDualPane) {
        dualPaneContent()
    } else {
        singlePaneContent()
    }
}
