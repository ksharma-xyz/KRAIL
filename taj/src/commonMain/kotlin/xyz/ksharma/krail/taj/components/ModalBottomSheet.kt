package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.modifier.sideSafeInsets
import xyz.ksharma.krail.taj.modifier.sideSafeWindowInsets

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

// BottomSheetDefaults.DragHandle(): 4.dp pill + 22.dp vertical padding (top and bottom) = 48.dp
// total, rendered above our content and not itself capped - must be subtracted here too, or its
// height silently eats the whole peek margin.
private val DRAG_HANDLE_HEIGHT = 48.dp

// The sheet's guaranteed top peek: real status bar inset (same API, backed by actual platform
// data on both Android and iOS) plus a fixed margin, so the sheet never sits behind the status
// bar and the peek looks identical on both platforms. Returns the CONTENT cap (drag handle height
// already subtracted) since the drag handle renders above content and isn't itself constrained.
@Composable
internal fun rememberSheetMaxContentHeight(): Dp {
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
        .coerceAtLeast(MIN_STATUS_BAR_HEIGHT)
    val maxSheetHeight = LocalWindowInfo.current.containerDpSize.height - statusBarHeight - EXTRA_TOP_MARGIN
    return maxSheetHeight - DRAG_HANDLE_HEIGHT
}

/**
 * What a [ModalBottomSheet] needs from the window to lay its content out: the height the content may take, the side insets
 * and how much room a centred sheet already has at each side. Made by [rememberSheetFrame].
 */
@Stable
class SheetFrame internal constructor(
    internal val maxHeight: Dp,
    internal val sideInsets: WindowInsets,
    internal val clearOnEachSide: Dp,
)

/**
 * Reads the window for a sheet. Both platform sheets call it in their own composition, before the sheet's window opens, and
 * hand the result to [SheetContent]: that is the composition every screen reads its insets in, so the numbers do not depend on
 * what the sheet's own composition reports (the status bar floor above exists because that composition has read 0).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSheetFrame(): SheetFrame {
    val maxHeight = rememberSheetMaxContentHeight()
    val sideInsets = sideSafeWindowInsets()
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    val clearOnEachSide = ((windowWidth - BottomSheetDefaults.SheetMaxWidth) / 2).coerceAtLeast(0.dp)
    return remember(maxHeight, sideInsets, clearOnEachSide) { SheetFrame(maxHeight, sideInsets, clearOnEachSide) }
}

/**
 * What a [ModalBottomSheet] puts its content in, on both platforms.
 *
 * It caps the content's height, so the peek gap from [rememberSheetMaxContentHeight] shows, and keeps the content out of the
 * sides of the window that belong to the system. The sheet's window runs edge to edge (Compose Multiplatform opens it with
 * `usePlatformInsets = false`, so it is not shrunk to the safe area), the Material 3 sheet pads its content by the vertical
 * insets only, and every caller passes empty `contentWindowInsets`. A sheet narrower than its cap
 * (`BottomSheetDefaults.SheetMaxWidth`) fills the window, so on the outer screen of the iPhone Duo its content ran under the
 * status pill, where the system takes the touch: the Reset button of the Plan your trip sheet opened Control Center.
 *
 * The sheet itself still draws edge to edge, and only its content is inset. A sheet in a window wider than the cap is centred
 * and has room at each side already, so [Modifier.sideSafeInsets] is told how much and pads only the part of a side inset past
 * it: none on an iPhone in landscape or on an Android phone with a cutout, so the sheet is not pushed off centre there. Sheet
 * content must not call `sideSafeInsets()` again, and needs no `contentWindowInsets` for the sides.
 *
 * Public so a host test can compose a sheet body exactly as the sheet does: the sheet's own window cannot be shown in one.
 *
 * @param frame What the window gives the sheet. Pass the one made before the sheet's window opened. The default reads it here,
 * which is right outside a sheet, in a test for instance.
 */
@Composable
fun SheetContent(
    modifier: Modifier = Modifier,
    frame: SheetFrame = rememberSheetFrame(),
    content: @Composable () -> Unit,
) {
    // Caps content (not the sheet's outer modifier, which would corrupt the anchor math's fullHeight
    // reference - see docs/investigations) so the peek gap from rememberSheetMaxContentHeight() actually shows.
    Box(
        modifier = modifier
            .heightIn(max = frame.maxHeight)
            .sideSafeInsets(clearOnEachSide = frame.clearOnEachSide, sides = frame.sideInsets),
    ) {
        content()
    }
}
