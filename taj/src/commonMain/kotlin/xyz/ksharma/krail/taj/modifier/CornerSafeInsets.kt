package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val CORNER_CLEARANCE = 16.dp

/**
 * The top system-bar inset, raised to [CORNER_CLEARANCE] only when content would otherwise sit
 * in the top-leading corner of the display.
 *
 * Most screens never need the raise: a phone in portrait has a status bar taller than the
 * clearance, and a phone in landscape has a side inset that already moves content off the
 * corner. It exists for iPhone Duo's outer display, where the status bar and camera sit on the
 * right edge, so the top inset is zero and so is the leading one, and a title drawn at the top
 * would sit inside the rounded corner.
 *
 * Returned as [WindowInsets] so `windowInsetsPadding` still subtracts anything a parent already
 * consumed: a parent that padded the status bar leaves nothing here to add.
 */
@Composable
fun WindowInsets.Companion.cornerSafeTop(): WindowInsets {
    val top = WindowInsets.systemBars.only(WindowInsetsSides.Top)
    return if (needsCornerClearance()) top.union(WindowInsets(top = CORNER_CLEARANCE)) else top
}

/**
 * [cornerSafeTop] as a plain [Dp], for components that take a top padding value rather than a
 * modifier, such as a map's ornament offset.
 */
@Composable
fun cornerSafeTopPadding(): Dp {
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.systemBars.getTop(density).toDp() }
    return if (needsCornerClearance()) maxOf(top, CORNER_CLEARANCE) else top
}

@Composable
private fun needsCornerClearance(): Boolean {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.systemBars
    val top = with(density) { insets.getTop(density).toDp() }
    val leadingPx = if (layoutDirection == LayoutDirection.Ltr) {
        insets.getLeft(density, layoutDirection)
    } else {
        insets.getRight(density, layoutDirection)
    }
    val leading = with(density) { leadingPx.toDp() }
    return top < CORNER_CLEARANCE && leading < CORNER_CLEARANCE
}
