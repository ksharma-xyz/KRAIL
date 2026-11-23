package xyz.ksharma.krail.trip.planner.ui.intro.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewIntroThemeStep
import xyz.ksharma.krail.trip.planner.ui.intro.PreviewThemeNoneState
import xyz.ksharma.krail.trip.planner.ui.intro.previewThemeChosenState

// The theme screen on a 320dp wide screen, the smallest the app supports. Recorded by
// `TripPlannerUiSnapshotTestNarrowIntro` on a 320 x 1280dp canvas, so the screen at 2x font is not cut off at the
// bottom. Skipped by the phone test. Keep both lists in step when adding one here.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroThemeNarrowNoneChosen() = PreviewIntroThemeStep(PreviewThemeNoneState)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroThemeNarrowMetroChosen() = PreviewIntroThemeStep(previewThemeChosenState(KrailThemeStyle.Metro))

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroThemeNarrowBusChosen() = PreviewIntroThemeStep(previewThemeChosenState(KrailThemeStyle.Bus))
