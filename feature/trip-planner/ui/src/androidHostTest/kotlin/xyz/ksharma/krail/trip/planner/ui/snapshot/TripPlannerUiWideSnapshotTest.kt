package xyz.ksharma.krail.trip.planner.ui.snapshot

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.snapshot.BaseSnapshotTest

/**
 * Snapshot tests for previews wider than a phone: the dual pane list, a Surface Duo screen and a tablet.
 *
 * `@Preview(widthDp = ...)` does not resize the canvas in these tests, so previews that need more room than
 * a Pixel 6 live in the `wide` package and are recorded here, on a tablet sized canvas.
 *
 * Run commands are the same as [TripPlannerUiSnapshotTest], and this class runs with it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.MediumTablet,
    manifest = Config.NONE,
)
class TripPlannerUiWideSnapshotTest : BaseSnapshotTest() {

    override val packageToScan = "xyz.ksharma.krail.trip.planner.ui.timetable.wide"

    @Test
    fun generateWideScreenshots() {
        generateSnapshots()
    }
}
