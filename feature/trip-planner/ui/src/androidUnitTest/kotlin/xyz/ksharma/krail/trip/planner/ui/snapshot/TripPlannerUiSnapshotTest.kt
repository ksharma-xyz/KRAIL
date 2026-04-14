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
 * - Record: ./gradlew :feature:trip-planner:ui:recordRoborazziDebug
 * - Verify: ./gradlew :feature:trip-planner:ui:verifyRoborazziDebug
 * - Compare: ./gradlew :feature:trip-planner:ui:compareRoborazziDebug
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

    @Test
    fun generateTripPlannerUiScreenshots() {
        generateSnapshots()
    }
}
