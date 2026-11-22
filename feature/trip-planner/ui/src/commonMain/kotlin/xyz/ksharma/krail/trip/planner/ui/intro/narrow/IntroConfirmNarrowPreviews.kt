package xyz.ksharma.krail.trip.planner.ui.intro.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewConfirmLongNames
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewIntroConfirmStep

// The confirm screen on a 320dp wide screen, the smallest the app supports. Recorded by
// `TripPlannerUiSnapshotTestNarrowIntro` on a 320 x 1280dp canvas, so the screen at 2x font is not cut off at the
// bottom. Skipped by the phone test. Keep both lists in step when adding one here.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroConfirmNarrowLongNames() = PreviewIntroConfirmStep(PreviewConfirmLongNames)
