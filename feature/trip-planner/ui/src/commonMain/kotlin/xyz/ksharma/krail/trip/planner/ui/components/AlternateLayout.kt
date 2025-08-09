package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowHeightSizeClass.Companion.COMPACT

@Composable
fun AlternateLayout(
    modifier: Modifier = Modifier,
    compactContent: @Composable BoxScope.() -> Unit,
    tabletContent: @Composable BoxScope.() -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    println("Window Size Class: ${windowSizeClass.windowWidthSizeClass}")

    Box(modifier = modifier.fillMaxSize()) {
        when (windowSizeClass.windowWidthSizeClass) {

            COMPACT -> compactContent()

            else -> tabletContent()
        }
    }
}
