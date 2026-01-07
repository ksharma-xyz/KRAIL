package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable

/**
 * Android implementation - reduced motion is generally not an issue on Android.
 * Returns false to allow animations.
 */
@Composable
actual fun isReducedMotionEnabled(): Boolean {
    // Android handles reduced motion gracefully, so we can always use Material3
    return false
}
