package xyz.ksharma.krail.trip.planner.ui.intro.v2.wide

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewIntroStepOne
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewTallCardContentState

// A tablet in portrait, single pane: the step is capped to a phone width and centred on the canvas. Recorded by
// `TripPlannerUiSnapshotTestWideIntro`, and skipped by the phone test. Keep both lists in step when adding one here.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideWideCardContent() = PreviewIntroStepOne(PreviewTallCardContentState)
