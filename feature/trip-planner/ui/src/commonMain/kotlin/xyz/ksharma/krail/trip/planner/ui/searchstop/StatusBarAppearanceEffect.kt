package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.Composable

/**
 * Updates the status bar icon appearance so icons remain readable against the content behind them.
 *
 * - [lightStatusBar] = `true`  → forces dark icons (map is always light-themed).
 * - [lightStatusBar] = `false` → follows the system dark/light theme.
 *
 * Must be called from a composable that recomposes when [lightStatusBar] changes.
 */
@Composable
expect fun StatusBarAppearanceEffect(lightStatusBar: Boolean)
