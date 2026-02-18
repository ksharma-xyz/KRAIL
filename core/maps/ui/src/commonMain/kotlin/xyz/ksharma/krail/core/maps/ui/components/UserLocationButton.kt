package xyz.ksharma.krail.core.maps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Pure UI component displaying a circular location indicator button.
 *
 * Features:
 * - Outer circle (28dp) with theme color
 * - Inner circle (20dp) with white border
 * - No business logic - just UI and click handling
 *
 * Usage:
 * ```kotlin
 * UserLocationButton(
 *     onClick = { /* Handle location logic here */ }
 * )
 * ```
 *
 * @param onClick Callback when button is clicked
 * @param modifier Modifier to be applied to the component
 */
@Composable
fun UserLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeColor by LocalThemeColor.current
    val themeComposeColor = themeColor.hexToComposeColor()

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .klickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Outer circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(themeComposeColor)
        )

        // Inner solid circle with white border
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(themeComposeColor)
                .border(width = 3.dp, color = Color.White, shape = CircleShape)
        )
    }
}

@PreviewComponent
@Composable
private fun UserLocationButtonPreview() {
    PreviewTheme {
        UserLocationButton(onClick = {})
    }
}
