package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity

// region Previews and snapshots
//
// Each @ScreenshotTest preview is recorded at light 1x, light 2x font and dark, on a Pixel 6 canvas.
// Narrower and wider screens are in the `narrow` and `wide` packages, which use their own canvas.

internal val previewAlert = Disruption(
    id = "metro:preview",
    feed = DisruptionFeed.METRO,
    severity = DisruptionSeverity.NOT_RUNNING,
    headline = "Buses replace metro services between Bankstown and Sydenham",
    detail = "Monday 23 November\n" +
        "From 11:10PM, buses replace metro services between Bankstown and Sydenham.\n" +
        "Services run between Sydenham and Tallawong, to the normal frequency.\n" +
        "The last metro services leave:\n" +
        "- Bankstown at 23:03, arriving Sydenham at 23:24\n" +
        "- Sydenham at 23:15, arriving Bankstown at 23:37",
    startEpochSec = 0L,
    endEpochSec = null,
    moreInfoUrl = "https://transportnsw.info/alerts",
    replacedByBuses = true,
)

// No last services and a longer body, the shape of the early-morning alerts.
internal val previewAlertLong = previewAlert.copy(
    id = "metro:preview-long",
    detail = "Saturday 5 December\n" +
        "From 4AM to 9AM, metro services do not run between Tallawong and Bankstown.\n" +
        "Buses replace services between Tallawong and Chatswood, and between Sydenham and Bankstown only.\n" +
        "Use trains between Epping or Chatswood, the City and Sydenham.\n" +
        "For travel to and from Waterloo use local buses or trains from Redfern.",
)

/** The banner in a column [width] wide, the way a pane of that width draws it. Null fills the canvas. */
@Composable
internal fun PreviewBannerSurface(width: Dp? = null, content: @Composable () -> Unit) {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        val sized = if (width == null) Modifier.fillMaxWidth() else Modifier.width(width)
        Column(modifier = sized.padding(horizontal = KrailTheme.dimensions.spacingL)) { content() }
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewDisruptionBanner() {
    PreviewBannerSurface { DisruptionBanner(disruption = previewAlert) }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewDisruptionBannerExpanded() {
    PreviewBannerSurface { DisruptionBanner(disruption = previewAlert, initialState = InfoTileState.EXPANDED) }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewDisruptionBannerExpandedLongText() {
    PreviewBannerSurface { DisruptionBanner(disruption = previewAlertLong, initialState = InfoTileState.EXPANDED) }
}

// endregion
