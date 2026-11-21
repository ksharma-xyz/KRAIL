package xyz.ksharma.krail.trip.planner.ui.timetable.wide

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.components.ToFromBlockStates

// The To and From block in a wide window, recorded by `TripPlannerUiWideSnapshotTest` on a 1280 dp
// canvas: the bars take the width they are given and keep their edges, columns and 48 dp height.
// Skipped by the phone test, keep both lists in step.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewToFromBlockWide() {
    ToFromBlockStates()
}
