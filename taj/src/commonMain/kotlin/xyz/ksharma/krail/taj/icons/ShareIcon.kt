package xyz.ksharma.krail.taj.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Returns the platform-appropriate share icon painter.
 * - Android: ic_android_share (the 3-node share icon)
 * - iOS:     ic_ios_share     (box with arrow pointing up)
 */
@Composable
expect fun rememberShareIconPainter(): Painter
