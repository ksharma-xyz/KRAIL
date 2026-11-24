package xyz.ksharma.krail.core.testing.device

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.roundToInt

/**
 * Reads `WindowInsets.safeDrawing` from inside the composition, in dp, and hands it to [report] after every composition. The
 * runner compares it with the profile's insets, so it can tell the insets reached the screen and are not zero because nothing
 * was dispatched.
 */
@Composable
internal fun ReportSafeDrawing(report: (SystemInsets) -> Unit) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val safeDrawing = WindowInsets.safeDrawing
    val insets = with(density) {
        SystemInsets(
            left = safeDrawing.getLeft(density, direction).toDp().value.roundToInt(),
            top = safeDrawing.getTop(density).toDp().value.roundToInt(),
            right = safeDrawing.getRight(density, direction).toDp().value.roundToInt(),
            bottom = safeDrawing.getBottom(density).toDp().value.roundToInt(),
        )
    }
    SideEffect { report(insets) }
}
