package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

/**
 * Will return a color based on the transport mode and the current theme (light / dark).
 *
 * @throws IllegalArgumentException if the provided string is not a valid
 * hexadecimal color code.
 *
 * @return A Compose Color object representing the provided hex color code.
 */
@Composable
internal fun transportModeBackgroundColor(transportMode: TransportMode): Color {
    return if (isAppInDarkMode()) {
        transportMode.colorCode.hexToComposeColor().copy(alpha = 0.45f)
    } else {
        transportMode.colorCode.hexToComposeColor().copy(alpha = 0.15f)
    }
}

@Composable
internal fun themeBackgroundColor(theme: KrailThemeStyle): Color {
    return if (isAppInDarkMode()) {
        theme.hexColorCode.hexToComposeColor().copy(alpha = 0.45f)
    } else {
        theme.hexColorCode.hexToComposeColor().copy(alpha = 0.15f)
    }
}
