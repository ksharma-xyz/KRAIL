package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

@Composable
fun TransportModeInfo(
    transportMode: TransportMode,
    modifier: Modifier = Modifier,
    borderEnabled: Boolean = false,
) {
    // Content alphas should always be 100% for Transport related icons
    CompositionLocalProvider(LocalContentAlpha provides 1f) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportModeIcon(
                transportMode = transportMode,
                displayBorder = borderEnabled,
                size = TransportModeIconSize.Small,
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun TransportModeInfoPreview() {
    PreviewTheme {
        TransportModeInfo(
            transportMode = TransportMode.Bus(),
        )
    }
}

@Preview
@Composable
private fun TransportModeInfoPreview_LargeFont() {
    PreviewTheme(fontScale = 2.0f) {
        TransportModeInfo(
            transportMode = TransportMode.Bus(),
        )
    }
}

// endregion
