package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Platform-aware Modal Bottom Sheet component for the Taj design system.
 *
 * Automatically handles accessibility concerns like reduced motion on iOS.
 * On platforms where Material3 animations cause crashes (iOS with reduced motion),
 * falls back to a simple overlay without animations.
 *
 * @param onDismissRequest Called when the user dismisses the bottom sheet
 * @param modifier Modifier to be applied to the bottom sheet
 * @param sheetGesturesEnabled Whether swipe gestures are enabled (only applies to Material3 version)
 * @param containerColor Background color of the bottom sheet
 * @param contentWindowInsets Window insets to be applied to the content
 * @param content The content to be displayed inside the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun ModalBottomSheet(
    containerColor: Color,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetGesturesEnabled: Boolean = true,
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets(0, 0, 0, 0) },
    content: @Composable () -> Unit,
)

// Widens the tappable/draggable area beyond M3's default 32.dp visual pill width.
private val DragHandleTouchWidth = 64.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WideDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.width(DragHandleTouchWidth),
        contentAlignment = Alignment.Center,
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

private val EXTRA_TOP_MARGIN = 16.dp

// Floor for the measured status bar inset. Guards against the inset reading as 0 (seen in
// practice inside the Dialog's own composition on both platforms) while still using the real,
// larger value on devices with a notch/Dynamic Island.
private val MIN_STATUS_BAR_HEIGHT = 32.dp

// The sheet's guaranteed top peek: real status bar inset (same API, backed by actual platform
// data on both Android and iOS) plus a fixed margin, so the sheet never sits behind the status
// bar and the peek looks identical on both platforms.
@Composable
internal fun rememberSheetMaxHeight(): Dp {
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
        .coerceAtLeast(MIN_STATUS_BAR_HEIGHT)
    return LocalWindowInfo.current.containerDpSize.height - statusBarHeight - EXTRA_TOP_MARGIN
}

// Caps content (not the sheet's outer modifier, which would corrupt the anchor math's fullHeight
// reference - see docs/investigations) so the peek gap from rememberSheetMaxHeight() actually shows.
@Composable
internal fun CappedSheetContent(maxHeight: Dp, content: @Composable () -> Unit) {
    Box(modifier = Modifier.heightIn(max = maxHeight)) {
        content()
    }
}
