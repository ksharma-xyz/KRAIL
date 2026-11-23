package xyz.ksharma.krail.trip.planner.ui.intro.narrow

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.trip.planner.ui.intro.finale.PREVIEW_ENTERING_MS
import xyz.ksharma.krail.trip.planner.ui.intro.finale.PREVIEW_HOLD_MS
import xyz.ksharma.krail.trip.planner.ui.intro.finale.PreviewFinaleFrame

// The finale on a 320dp wide screen, the smallest the app supports, where the wordmark scales down to fit and the 2x
// font preview is the one that has to. Recorded by `TripPlannerUiSnapshotTestNarrowIntro` on a 320 x 1280dp canvas.
// Skipped by the phone test. Keep both lists in step when adding one here.

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleNarrowEntering() = PreviewFinaleFrame(PREVIEW_ENTERING_MS)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleNarrowHold() = PreviewFinaleFrame(PREVIEW_HOLD_MS)
