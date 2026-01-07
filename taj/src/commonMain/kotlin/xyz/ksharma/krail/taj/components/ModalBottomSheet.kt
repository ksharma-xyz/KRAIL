package xyz.ksharma.krail.taj.components
)
    content: @Composable () -> Unit,
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets(0, 0, 0, 0) },
    containerColor: Color = Color.Unspecified,
    sheetGesturesEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
expect fun ModalBottomSheet(
@Composable
@OptIn(ExperimentalMaterial3Api::class)
 */
 * @param content The content to be displayed inside the bottom sheet
 * @param contentWindowInsets Window insets to be applied to the content
 * @param containerColor Background color of the bottom sheet
 * @param sheetGesturesEnabled Whether swipe gestures are enabled (only applies to Material3 version)
 * @param modifier Modifier to be applied to the bottom sheet
 * @param onDismissRequest Called when the user dismisses the bottom sheet
 * 
 * falls back to a simple overlay without animations.
 * On platforms where Material3 animations cause crashes (iOS with reduced motion),
 * Automatically handles accessibility concerns like reduced motion on iOS.
 * 
 * Platform-aware Modal Bottom Sheet component for the Taj design system.
/**

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.WindowInsets


