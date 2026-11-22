package xyz.ksharma.krail.trip.planner.ui.snapshot

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.snapshot.BaseSnapshotTest

/**
 * Snapshot tests for the Intro previews on a tablet sized canvas, where the step is capped to a phone width.
 *
 * Runs with [TripPlannerUiSnapshotTest]; see it for the run commands. The name sorts after
 * [TripPlannerUiSnapshotTest] for the reason given on [TripPlannerUiSnapshotTestNarrow].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.MediumTablet,
    manifest = Config.NONE,
)
class TripPlannerUiSnapshotTestWideIntro : BaseSnapshotTest() {

    override val packageToScan = "xyz.ksharma.krail.trip.planner.ui.intro.v2.wide"

    @Test
    fun generateWideIntroScreenshots() {
        generateSnapshots()
    }
}
