package xyz.ksharma.krail.trip.planner.ui.timetable.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.HomeChipRowStates

// The Home, Work and Park & Ride chips on a 320 dp window, recorded by `TripPlannerUiSnapshotTestNarrow`.
// Its 2x font image is the 320 dp at 200 percent frame: whole chips wrapped onto more lines. Skipped by the phone test, keep both lists in step.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewHomeChipRowNarrow() {
    HomeChipRowStates()
}
