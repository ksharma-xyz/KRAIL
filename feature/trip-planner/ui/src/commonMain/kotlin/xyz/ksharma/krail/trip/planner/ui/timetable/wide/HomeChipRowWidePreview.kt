package xyz.ksharma.krail.trip.planner.ui.timetable.wide

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.HomeChipRowStates

// The Home, Work and Park & Ride chips in a wide window, recorded by `TripPlannerUiWideSnapshotTest`
// on a 1280 dp canvas: the row keeps the saved trips column's 560 dp cap and its edges. Skipped by
// the phone test, keep both lists in step.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewHomeChipRowWide() {
    HomeChipRowStates()
}
