package xyz.ksharma.krail.info.tiles.ui.snapshot

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.snapshot.BaseSnapshotTest

/**
 * Snapshot tests for the InfoTile previews.
 *
 * Run commands:
 * - Record: ./gradlew :info-tile:ui:recordRoborazziAndroidHostTest
 * - Verify: ./gradlew :info-tile:ui:verifyRoborazziAndroidHostTest
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class InfoTileSnapshotTest : BaseSnapshotTest() {

    override val packageToScan = "xyz.ksharma.krail.info.tiles.ui"

    @Test
    fun generateInfoTileScreenshots() {
        generateSnapshots()
    }
}
