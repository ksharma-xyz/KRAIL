package xyz.ksharma.krail.taj

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * When the font scale changes, the size of the text should also change.
 * This will maintain the aesthetics of the text as well increase text size slowly when font scale.
 */
@Composable
fun Dp.toAdaptiveSize(): Dp {
    val density = LocalDensity.current
    return this.times(density.fontScale)
}

/**
 * When the icons are used in a decorative context, they should be smaller than when they are
 * used in a textual context when font scale changes.
 * This will maintain the aesthetics of the icons as well increase icon size slowly when font scale.
 */
@Composable
fun Dp.toAdaptiveDecorativeIconSize(): Dp {
    val density = LocalDensity.current
    return if (density.fontScale > 1.5f) {
        this.times(density.fontScale * 0.8f)
    } else {
        this.times(density.fontScale)
    }
}
