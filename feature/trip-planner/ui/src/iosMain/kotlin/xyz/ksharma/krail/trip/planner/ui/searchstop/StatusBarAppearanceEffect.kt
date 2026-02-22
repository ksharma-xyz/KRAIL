package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.Composable

// iOS status bar icon color requires overriding preferredStatusBarStyle on the hosting
// UIViewController, which ComposeUIViewController doesn't expose directly.
// This is a no-op until a UIKit-layer solution is implemented (e.g. a custom VC wrapper).
@Composable
actual fun StatusBarAppearanceEffect(lightStatusBar: Boolean) = Unit
