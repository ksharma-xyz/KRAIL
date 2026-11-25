package xyz.ksharma.krail.trip.planner.ui.parkride.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.asThemeInk
import xyz.ksharma.krail.taj.themeGroundColor
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.rememberMapLetterPainter

internal val ParkRideMarkerSize = 30.dp
private val MarkerCornerRadius = 8.dp
private val MarkerLetterSize = 22.dp

/**
 * The fill of the map's `P` logo: the rider's theme colour [theme], made only as deep as a white letter of this size needs
 * to stand out on it (3 to 1, the figure for large bold text and for graphics). A theme that already has it is unchanged.
 */
internal fun parkRideMarkerFill(theme: Color): Color =
    theme.asThemeInk(darkMode = false, minContrast = ContrastAnalyzer.UI_COMPONENT_CONTRAST_AA)

/**
 * The app's parking logo as a map marker: a large white bold `P` on a rounded square of the theme colour, with a white
 * edge that lifts it off the map. It is the same in light and dark mode, because the map under it is.
 */
@Composable
internal fun rememberParkRideMarkerPainter(theme: Color = themeGroundColor()): Painter = rememberMapLetterPainter(
    letter = "P",
    fill = remember(theme) { parkRideMarkerFill(theme) },
    letterSize = MarkerLetterSize,
    corner = MarkerCornerRadius,
)

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewParkRideMapMarkers() {
    PreviewTheme {
        val dim = KrailTheme.dimensions
        FlowRow(
            modifier = Modifier.padding(dim.spacingXL),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXL),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
        ) {
            KrailThemeStyle.entries.forEach { style ->
                Image(
                    painter = rememberParkRideMarkerPainter(style.hexColorCode.hexToComposeColor()),
                    contentDescription = null,
                    modifier = Modifier.size(ParkRideMarkerSize),
                )
            }
        }
    }
}

// endregion
