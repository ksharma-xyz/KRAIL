package xyz.ksharma.krail.taj.snapshot

import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.snapshot.BaseSnapshotTest

/**
 * Snapshot tests for Taj Design System components.
 *
 * Run commands:
 * - Record: ./gradlew :taj:testAndroidHostTest -Proborazzi.test.record=true
 * - Verify: ./gradlew :taj:testAndroidHostTest -Proborazzi.test.verify=true
 * - Compare: ./gradlew :taj:testAndroidHostTest -Proborazzi.test.compare=true
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE
)
class TajSnapshotTest : BaseSnapshotTest() {

    // Specify the package to scan for @Preview + @ScreenshotTest annotations
    override val packageToScan = "xyz.ksharma.krail.taj"

    @Test
    fun generateTajDesignSystemScreenshots() {
        generateSnapshots()
    }
}
