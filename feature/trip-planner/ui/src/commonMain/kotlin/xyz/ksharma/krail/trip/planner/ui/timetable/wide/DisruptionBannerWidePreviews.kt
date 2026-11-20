package xyz.ksharma.krail.trip.planner.ui.timetable.wide

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.trip.planner.ui.timetable.DisruptionBanner
import xyz.ksharma.krail.trip.planner.ui.timetable.PreviewBannerSurface
import xyz.ksharma.krail.trip.planner.ui.timetable.previewAlert
import xyz.ksharma.krail.trip.planner.ui.timetable.previewAlertLong

// Previews wider than a phone. This package is recorded by `TripPlannerUiWideSnapshotTest` on a tablet
// sized canvas, and skipped by the phone test. Keep both lists in step when adding one here.

/** The fixed width of the list pane in the dual pane layout, where the timetable lives on a tablet. */
@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewDisruptionBannerListPane() {
    PreviewBannerSurface(width = DUAL_PANE_LIST_WIDTH) {
        DisruptionBanner(disruption = previewAlert, initialState = InfoTileState.EXPANDED)
    }
}

/** One screen of a Surface Duo, single pane. */
@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewDisruptionBannerDuo() {
    PreviewBannerSurface(width = 540.dp) {
        DisruptionBanner(disruption = previewAlertLong, initialState = InfoTileState.EXPANDED)
    }
}

/** A tablet in portrait, single pane: the card fills the canvas. */
@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewDisruptionBannerTablet() {
    PreviewBannerSurface {
        DisruptionBanner(disruption = previewAlert, initialState = InfoTileState.EXPANDED)
    }
}
