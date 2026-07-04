package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ModalBottomSheet as Material3ModalBottomSheet

/**
 * Android implementation - uses Material3 ModalBottomSheet directly.
 * No reduced motion accessibility issues on Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ModalBottomSheet(
    containerColor: Color,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetGesturesEnabled: Boolean,
    contentWindowInsets: @Composable () -> WindowInsets,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val maxContentHeight = LocalWindowInfo.current.containerDpSize.height -
        statusBarHeight - EXTRA_TOP_MARGIN

    Material3ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetGesturesEnabled = sheetGesturesEnabled,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
        dragHandle = { WideDragHandle() },
    ) {
        CappedSheetContent(maxContentHeight, content)
    }
}

// Edge-to-edge lets the sheet reach behind the status bar otherwise; keeps it flush below,
// plus a little breathing room.
private val EXTRA_TOP_MARGIN = 5.dp
