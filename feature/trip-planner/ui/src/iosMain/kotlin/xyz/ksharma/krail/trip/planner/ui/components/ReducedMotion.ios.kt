package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

/**
 * iOS implementation - checks system accessibility setting for reduced motion.
 * Returns true if the user has "Reduce Motion" enabled in iOS Settings > Accessibility.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun isReducedMotionEnabled(): Boolean {
    return UIAccessibilityIsReduceMotionEnabled()
}
