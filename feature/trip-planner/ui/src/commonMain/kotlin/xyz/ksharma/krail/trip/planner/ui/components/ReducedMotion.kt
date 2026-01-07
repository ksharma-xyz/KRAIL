package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable

/**
 * Platform-specific check for reduced motion accessibility setting.
 * Returns true if the user has enabled reduced motion in system settings.
 */
@Composable
expect fun isReducedMotionEnabled(): Boolean
