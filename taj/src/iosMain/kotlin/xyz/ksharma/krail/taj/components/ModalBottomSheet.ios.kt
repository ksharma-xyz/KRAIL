package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
    modifier: Modifier,
    sheetGesturesEnabled: Boolean,
    onDismissRequest: () -> Unit,
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
        modifier = modifier.fillMaxSize()
    ) {
        // Manual Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.6f))
                .klickable(onClick = onDismissRequest)
        )

        // Bottom sheet content
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(containerColor)
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Prevent clicks from dismissing */ }
                    )
            ) {
                content()
            }
        }
    }
}

