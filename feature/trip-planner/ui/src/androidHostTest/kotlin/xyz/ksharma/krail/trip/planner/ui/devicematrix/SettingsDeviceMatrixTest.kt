package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.settings.SettingsScreen
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryScreen
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryState

/**
 * Settings and Our story on every device profile (`docs/testing/DEVICE_PROFILES.md`): every control stands inside the safe area
 * and the back arrow, which the title bar pads for the system itself, takes a real touch. Both lists scroll, so their last row
 * must clear the bottom bar once scrolled to the end.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class SettingsDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    @Test
    fun `Settings keeps every control inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "SettingsScreen") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                SettingsScreen(appVersion = "1.0.0", showDebugConfig = true, onBackClick = {})
            }
            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithContentDescription("Back"))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SETTINGS_ITEM_THEME))
            assertScrollEndClearsBottomBar(onNode(hasScrollAction()))
        }

    @Test
    fun `Our story keeps its back arrow inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "OurStoryScreen") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                OurStoryScreen(
                    state = OurStoryState(isLoading = false, story = OUR_STORY, disclaimer = DISCLAIMER),
                    onBackClick = {},
                )
            }
            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithContentDescription("Back"))
            assertScrollEndClearsBottomBar(onNode(hasScrollAction()))
        }

    private companion object {
        const val OUR_STORY = "KRAIL started as a small way to see the next train without opening a big app. " +
            "It grew one rider's request at a time, and it is still made the same way."
        const val DISCLAIMER = "KRAIL is not affiliated with any transport operator."
    }
}
