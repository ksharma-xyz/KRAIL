package xyz.ksharma.krail.core.maps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun LocationPermissionBanner(
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)

    Row(
        modifier = modifier
            .clip(shape)
            .background(color = KrailTheme.colors.onSurface, shape = shape)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Location off —",
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.surface,
        )

        Text(
            text = "Open Settings",
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.surface,
            modifier = Modifier.klickable(onClick = onGoToSettings),
        )

        Text(
            text = "×",
            style = KrailTheme.typography.titleSmall,
            color = KrailTheme.colors.surface.copy(alpha = 0.6f),
            modifier = Modifier.klickable(onClick = onDismiss),
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
            onDismiss = {},
            modifier = Modifier.padding(KrailTheme.dimensions.spacingXL),
        )
    }
}

// endregion
