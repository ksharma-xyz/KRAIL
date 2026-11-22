package xyz.ksharma.krail.trip.planner.ui.intro.v2.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewIntroStepTwo
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewTripBothChosen
import xyz.ksharma.krail.trip.planner.ui.intro.v2.PreviewTripPrefilled

// Step 2 on a 320dp wide screen, recorded by `TripPlannerUiSnapshotTestNarrowIntro` and skipped by the phone
// test. Keep both lists in step when adding one here.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroFirstTripNarrowPrefilled() = PreviewIntroStepTwo(PreviewTripPrefilled)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroFirstTripNarrowBothChosen() = PreviewIntroStepTwo(PreviewTripBothChosen)
