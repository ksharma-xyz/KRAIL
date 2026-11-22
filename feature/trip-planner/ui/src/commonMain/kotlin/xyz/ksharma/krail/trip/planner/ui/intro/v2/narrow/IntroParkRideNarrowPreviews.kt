package xyz.ksharma.krail.trip.planner.ui.intro.v2.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewAskState
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewIntroStepOne
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewTallAddedState
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewTallCardContentState

// A 320dp wide screen, the smallest the app supports. Recorded by `TripPlannerUiSnapshotTestNarrowIntro` on a
// 320 x 1280dp canvas, so the step at 2x font is not cut off at the bottom. Skipped by the phone test. Keep both
// lists in step when adding one here.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideNarrowAsk() = PreviewIntroStepOne(PreviewAskState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideNarrowCardContent() = PreviewIntroStepOne(PreviewTallCardContentState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroParkRideNarrowAdded() = PreviewIntroStepOne(PreviewTallAddedState)
