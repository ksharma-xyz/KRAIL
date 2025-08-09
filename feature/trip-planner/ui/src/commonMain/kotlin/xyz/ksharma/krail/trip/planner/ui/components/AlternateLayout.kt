package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowHeightSizeClass.Companion.COMPACT

@Composable
fun AlternateLayout(
    modifier: Modifier = Modifier,
    compactContent: @Composable () -> Unit,
    tabletContent: @Composable () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    Box(modifier = modifier) {
        when (windowSizeClass.windowWidthSizeClass) {
            COMPACT -> compactContent()
            else -> tabletContent()
        }
    }
}
