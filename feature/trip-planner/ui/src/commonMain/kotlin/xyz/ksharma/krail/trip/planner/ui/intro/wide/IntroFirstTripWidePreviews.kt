package xyz.ksharma.krail.trip.planner.ui.intro.wide

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewIntroStepTwo
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewTripBothChosen

// Step 2 on a tablet in portrait, single pane, capped to a phone width and centred. Recorded by
// `TripPlannerUiSnapshotTestWideIntro` and skipped by the phone test. Keep both lists in step when adding one here.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroFirstTripWideBothChosen() = PreviewIntroStepTwo(PreviewTripBothChosen)
