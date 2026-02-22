package xyz.ksharma.krail.core.maps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Banner shown when location permission is permanently denied.
 *
 * Displays below search bar, above map area.
 * Matches provided background color (typically search bar color).
 *
 * Features:
 * - Clear message about permission requirement
 * - "Settings" button to open app settings
 * - Dismissible with close button
 * - Animated enter/exit
 *
 * @param onGoToSettings Callback to open app settings
 * @param modifier Modifier for the banner
 * @param backgroundColor Background color (typically from search bar)
 */
@Composable
fun LocationPermissionBanner(
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = KrailTheme.colors.surface,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color = backgroundColor)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Location Permission Required",
            style = KrailTheme.typography.titleMedium,
        )

        Text(
            text = "Enable location in Settings to see your position on the map.",
            style = KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.softLabel,
            modifier = Modifier.padding(top = 8.dp),
        )

        TextButton(
            onClick = onGoToSettings,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Text(
                text = "Go to Settings",
                style = KrailTheme.typography.labelLarge,
            )
        }
    }
}

// region Previews

@PreviewComponent
@Composable
private fun LocationPermissionBannerPreview() {
    PreviewTheme {
        LocationPermissionBanner(
            onGoToSettings = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

// endregion
