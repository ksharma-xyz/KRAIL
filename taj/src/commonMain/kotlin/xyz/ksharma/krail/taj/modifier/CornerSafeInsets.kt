package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val CORNER_CLEARANCE = 16.dp

/**
 * Extra top padding to add *after* `statusBarsPadding()` so content stays out of the top-leading
 * display corner. Zero everywhere except where the status bar inset is too small to do that job.
 *
 * Most screens never need it: a phone in portrait has a status bar taller than the clearance,
 * and a phone in landscape has a side inset that already moves content off the corner. It exists
 * for iPhone Duo's outer display, where the status bar and camera sit on the right edge, so the
 * top inset is zero and so is the leading one, and a title drawn at the top would sit inside the
 * rounded corner.
 *
 * A separate padding rather than a replacement inset on purpose: screens such as Timetable apply
 * `statusBarsPadding()` on a parent, and a child's `statusBarsPadding()` is what that parent's
 * consumption is subtracted from. Replacing it with `windowInsetsPadding` put a second status
 * bar's worth of space above the title.
 */
@Composable
fun cornerClearanceTop(): Dp {
    val statusTop = statusBarTop()
    return if (needsCornerClearance(statusTop)) CORNER_CLEARANCE - statusTop else 0.dp
}

/**
 * The status bar's height, or [CORNER_CLEARANCE] where [cornerClearanceTop] applies, as a plain
 * [Dp] for components that take a top offset rather than a modifier, such as a map's ornaments.
 */
@Composable
fun cornerSafeTopPadding(): Dp {
    val statusTop = statusBarTop()
    return if (needsCornerClearance(statusTop)) CORNER_CLEARANCE else statusTop
}

@Composable
private fun statusBarTop(): Dp {
    val density = LocalDensity.current
    return with(density) { WindowInsets.statusBars.getTop(density).toDp() }
}

@Composable
private fun needsCornerClearance(statusTop: Dp): Boolean {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.systemBars
    val leadingPx = if (layoutDirection == LayoutDirection.Ltr) {
        insets.getLeft(density, layoutDirection)
    } else {
        insets.getRight(density, layoutDirection)
    }
    val leading = with(density) { leadingPx.toDp() }
    return statusTop < CORNER_CLEARANCE && leading < CORNER_CLEARANCE
}
