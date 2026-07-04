package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

// Caps content (not the sheet's outer modifier, which would corrupt the anchor math's fullHeight
// reference - see docs/investigations) so a top peek always shows. Each platform computes its
// own maxHeight - iOS estimates a fraction of the window, Android measures the actual status bar
// inset - since the two platforms need different top-peek strategies.
@Composable
internal fun CappedSheetContent(maxHeight: Dp, content: @Composable () -> Unit) {
    Box(modifier = Modifier.heightIn(max = maxHeight)) {
        content()
    }
}
