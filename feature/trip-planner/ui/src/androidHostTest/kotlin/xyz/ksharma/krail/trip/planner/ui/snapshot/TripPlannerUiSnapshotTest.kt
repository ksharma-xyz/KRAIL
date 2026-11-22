package xyz.ksharma.krail.trip.planner.ui.snapshot

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.snapshot.BaseSnapshotTest

/**
 * Snapshot tests for the Trip Planner UI module.
 *
 * Run commands:
 * - Record: ./gradlew :feature:trip-planner:ui:testAndroidHostTest -Proborazzi.test.record=true
 * - Verify: ./gradlew :feature:trip-planner:ui:testAndroidHostTest -Proborazzi.test.verify=true
 * - Compare: ./gradlew :feature:trip-planner:ui:testAndroidHostTest -Proborazzi.test.compare=true
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class TripPlannerUiSnapshotTest : BaseSnapshotTest() {

    override val packageToScan = "xyz.ksharma.krail.trip.planner.ui"

    /**
     * `MapUiState.Loading` renders a Material `CircularProgressIndicator`, which animates
     * forever. Which frame of the sweep the capture lands on is decided by the animation
     * clock, so re-recording produces a different 106x106 patch in the middle of the pane
     * every time and the golden can never be matched twice. Same class of preview as taj's
     * `PreviewLoadingDotsPill_Visible`.
     */
    override val excludedPreviewNames = setOf(
        "PreviewMapStopSelectionPane_Loading",
        // Recorded on a tablet sized canvas by TripPlannerUiWideSnapshotTest.
        "PreviewDisruptionBannerListPane",
        "PreviewDisruptionBannerDuo",
        "PreviewDisruptionBannerTablet",
        // Recorded on a 320dp canvas by TripPlannerUiSnapshotTestNarrow.
        "PreviewDisruptionBannerNarrow",
        "PreviewDisruptionBannerNarrowLongText",
        // Recorded on a 320dp canvas and on a tablet sized canvas by the two Intro snapshot classes.
        "PreviewIntroParkRideNarrowAsk",
        "PreviewIntroParkRideNarrowCardContent",
        "PreviewIntroParkRideNarrowAdded",
        "PreviewIntroParkRideWideCardContent",
        "PreviewIntroFirstTripNarrowPrefilled",
        "PreviewIntroFirstTripNarrowBothChosen",
        "PreviewIntroFirstTripWideBothChosen",
    )

    @Test
    fun generateTripPlannerUiScreenshots() {
        generateSnapshots()
    }
}
