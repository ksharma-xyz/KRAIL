package xyz.ksharma.krail.trip.planner.ui.timetable.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.trip.planner.ui.timetable.DisruptionBanner
import xyz.ksharma.krail.trip.planner.ui.timetable.PreviewBannerSurface
import xyz.ksharma.krail.trip.planner.ui.timetable.previewAlert
import xyz.ksharma.krail.trip.planner.ui.timetable.previewAlertLong

// A 320dp wide screen, the smallest the app supports. Recorded by `TripPlannerUiNarrowSnapshotTest` on a
// 320 x 1280dp canvas: at 2x font the card is taller than a Pixel 6 canvas and the bottom was cut off.
// Skipped by the phone test. Keep both lists in step when adding one here.

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewDisruptionBannerNarrow() {
    PreviewBannerSurface { DisruptionBanner(disruption = previewAlert, initialState = InfoTileState.EXPANDED) }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewDisruptionBannerNarrowLongText() {
    PreviewBannerSurface { DisruptionBanner(disruption = previewAlertLong, initialState = InfoTileState.EXPANDED) }
}
