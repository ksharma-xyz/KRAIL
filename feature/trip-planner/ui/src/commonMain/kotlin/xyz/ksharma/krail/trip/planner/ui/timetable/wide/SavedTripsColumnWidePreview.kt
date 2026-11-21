package xyz.ksharma.krail.trip.planner.ui.timetable.wide

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.previewSavedTripsColumnState

// The saved trips column in a wide window, recorded by `TripPlannerUiWideSnapshotTest` on a 1280 dp
// canvas, which is dual pane: the column fills the fixed 480 dp list pane with the same edges as on
// a phone, and the right pane is left empty. Skipped by the phone test, keep both lists in step.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewSavedTripsColumnWide() {
    PreviewTheme { SavedTripsScreen(savedTripsState = previewSavedTripsColumnState) }
}
