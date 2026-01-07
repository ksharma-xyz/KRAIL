package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import androidx.compose.material3.ModalBottomSheet as Material3ModalBottomSheet

/**
 * iOS implementation - checks for reduced motion and uses appropriate implementation.
 *
 * When reduced motion is enabled: Uses a simple overlay without animations (prevents crashes)
 * When reduced motion is disabled: Uses Material3 ModalBottomSheet with full animations
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalForeignApi::class)
@Composable
actual fun ModalBottomSheet(
    containerColor: Color,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetGesturesEnabled: Boolean,
    contentWindowInsets: @Composable () -> WindowInsets,
    content: @Composable () -> Unit,
) {
    val isReducedMotionEnabled = UIAccessibilityIsReduceMotionEnabled()

    if (isReducedMotionEnabled) {
        SimpleBottomSheetOverlay(
            onDismissRequest = onDismissRequest,
            containerColor = containerColor,
            modifier = modifier,
            content = content,
        )
    } else {
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
}

const val scrimTransparencyAlpha = 0.6f

/**
 * Simple bottom sheet overlay without animations.
 * Mimics Material3 ModalBottomSheet appearance but without any animations.
 * Used as a fallback when reduced motion is enabled on iOS.
 */
@Composable
private fun SimpleBottomSheetOverlay(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Scrim (background overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(scrimTransparencyAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
        )

        // Bottom sheet content - positioned at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(containerColor)
                .navigationBarsPadding(),
        ) {
            // Drag Handle - always visible, not part of scrollable content
            DragHandle(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // Scrollable content area below the drag handle
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Drag handle component that mimics Material3's bottom sheet drag handle.
 * Clickable to dismiss the bottom sheet.
 */
@Composable
private fun DragHandle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(vertical = 32.dp)
            .background(color = KrailTheme.colors.surface)
            .klickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(KrailTheme.colors.bottomSheetDragHandle),
        )
    }
}
