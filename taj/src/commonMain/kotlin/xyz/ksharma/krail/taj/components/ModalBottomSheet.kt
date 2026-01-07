package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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
    modifier: Modifier,
    sheetGesturesEnabled: Boolean = true,
    onDismissRequest: () -> Unit,
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets(0, 0, 0, 0) },
    content: @Composable () -> Unit,
)
