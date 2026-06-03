package xyz.ksharma.krail.core.maps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Pill shown on the map when location permission has been permanently denied.
 *
 * Visual style and interaction match [MapTimetableDataBadge] so the permission state
 * reads as "yet another map status pill", not as a separate modal:
 *   - Surface background, onSurface text — same colour treatment as the badge.
 *   - Entire pill is a single tap target; no nested buttons, no dismiss.
 *   - One short label, no separators or em dashes.
 *
 * Tapping anywhere on the pill calls [onGoToSettings].
 */
@Composable
fun LocationPermissionBanner(
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .clip(RoundedCornerShape(24.dp))
            .background(color = KrailTheme.colors.surface.copy(alpha = 0.9f))
            .klickable(onClick = onGoToSettings)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Location permission required",
            style = KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.onSurface,
        )
    }
}

// region Previews

@PreviewComponent
@Composable
private fun LocationPermissionBannerPreview() {
    PreviewTheme {
        LocationPermissionBanner(
            onGoToSettings = {},
            modifier = Modifier.padding(KrailTheme.dimensions.spacingXL),
        )
    }
}

// endregion
