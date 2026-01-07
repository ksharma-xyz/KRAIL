package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ModalBottomSheet as Material3ModalBottomSheet

/**
 * Android implementation - uses Material3 ModalBottomSheet directly.
 * No reduced motion accessibility issues on Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetGesturesEnabled: Boolean,
    containerColor: Color,
    contentWindowInsets: @Composable () -> WindowInsets,
    content: @Composable () -> Unit,
) {
    Material3ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetGesturesEnabled = sheetGesturesEnabled,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
    ) {
        content()
    }
}

