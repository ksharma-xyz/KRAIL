package xyz.ksharma.krail.trip.planner.ui.timetable.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.previewSavedTripsColumnState

// The saved trips column on a 320 dp window, recorded by `TripPlannerUiSnapshotTestNarrow`. Its
// 2x font image is the 320 dp at 200 percent frame: every card and the search row wrap whole items
// and keep the same left and right edges. Skipped by the phone test, keep both lists in step.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewSavedTripsColumnNarrow() {
    PreviewTheme { SavedTripsScreen(savedTripsState = previewSavedTripsColumnState) }
}
