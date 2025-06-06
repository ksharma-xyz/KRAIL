package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
fun isFontScaleLessThanThreshold(fontScaleThreshold: Float = 1.8f): Boolean {
    val density = LocalDensity.current
    return density.fontScale < fontScaleThreshold
}
