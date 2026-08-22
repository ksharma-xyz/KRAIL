package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
    val maxContentHeight = rememberSheetMaxContentHeight()

    Material3ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // testTagsAsResourceId, again, here.
        //
        // The app sets it once, on KrailNavHost. A ModalBottomSheet does not render inside
        // that subtree: it gets its own window, so the property never reaches it and no
        // testTag inside ANY sheet in this app was published as a resource id. Nothing failed
        // loudly, which is why it went unnoticed: a UI-automation selector for something in a
        // sheet simply never matched, and read as the element not being there.
        //
        // Found when a Maestro flow could not tap a button it could plainly see in the
        // hierarchy dump, sitting there with an empty resource-id next to its own label.
        modifier = modifier.semantics { testTagsAsResourceId = true },
        sheetGesturesEnabled = sheetGesturesEnabled,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
        dragHandle = { WideDragHandle() },
    ) {
        CappedSheetContent(maxContentHeight, content)
    }
}
