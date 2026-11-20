package xyz.ksharma.krail.trip.planner.ui.snapshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.snapshot.BaseSnapshotTest

/**
 * Snapshot tests for previews on a 320dp wide screen, the smallest the app supports.
 *
 * The canvas is 320 x 1280dp so a card at 2x font is not cut off at the bottom. Runs with
 * [TripPlannerUiSnapshotTest]; see it for the run commands.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w320dp-h1280dp-xxhdpi",
    manifest = Config.NONE,
)
class TripPlannerUiNarrowSnapshotTest : BaseSnapshotTest() {

    override val packageToScan = "xyz.ksharma.krail.trip.planner.ui.timetable.narrow"

    @Test
    fun generateNarrowScreenshots() {
        generateSnapshots()
    }
}
