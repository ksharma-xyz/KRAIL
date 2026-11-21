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
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
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
    startEpochSec = 1_795_436_400L,
    endEpochSec = 1_795_446_000L,
    moreInfoUrl = "https://transportnsw.info/alerts",
    replacedByBuses = true,
)

// A morning window on another day, for an alert that says the metro does not run and names no buses.
internal val previewAlertLong = previewAlert.copy(
    id = "metro:preview-long",
    headline = "Metro services do not run between Tallawong and Bankstown",
    startEpochSec = 1_796_403_600L,
    endEpochSec = 1_796_421_600L,
    replacedByBuses = false,
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

// The planner's own alert for the line, copied from the live trip reply on 2026-11-21.
private val previewLineAlert = ServiceAlert(
    heading = "Buses replace trains between Cronulla, Waterfall and Central",
    message = "<div><strong>Saturday 21 and Sunday 22 November</strong></div>" +
        "<ul><li>Buses replace trains between Cronulla, Waterfall and Central.</li>" +
        "<li>Trains run between Central and Bondi Junction, to a changed timetable.</li></ul>",
    isLineAlert = true,
    url = "https://transportnsw.info/alerts/details#/ems-81168",
)

// Replacement buses in the results and nothing attached to them: the title only, no expand.
@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewReplacementBusesBanner() {
    PreviewBannerSurface { ReplacementBusesBanner(disruption = null, lineAlert = previewLineAlert) }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewReplacementBusesBannerLineAlertExpanded() {
    PreviewBannerSurface {
        ReplacementBusesBanner(disruption = null, lineAlert = previewLineAlert, initialState = InfoTileState.EXPANDED)
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewReplacementBusesBannerMetroAlertExpanded() {
    PreviewBannerSurface {
        ReplacementBusesBanner(disruption = previewAlert, lineAlert = null, initialState = InfoTileState.EXPANDED)
    }
}

// endregion
