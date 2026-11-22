package xyz.ksharma.krail.trip.planner.ui.intro.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewDestinationNoMatch
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewDestinationSuggestions
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewDestinationTyped
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewIntroStopStep
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewOriginNearby

// The destination and origin screens on a 320dp wide screen, the smallest the app supports. Recorded by
// `TripPlannerUiSnapshotTestNarrowIntro` on a 320 x 1280dp canvas, so the screen at 2x font is not cut off at the
// bottom. Skipped by the phone test. Keep both lists in step when adding one here.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroDestinationNarrowSuggestions() = PreviewIntroStopStep(PreviewDestinationSuggestions)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroDestinationNarrowTyped() = PreviewIntroStopStep(PreviewDestinationTyped)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroDestinationNarrowNoMatch() = PreviewIntroStopStep(PreviewDestinationNoMatch)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroOriginNarrowNearby() = PreviewIntroStopStep(PreviewOriginNearby)
