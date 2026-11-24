package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithText
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorScreen
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions

/**
 * The Plan your trip sheet on every device profile (`docs/testing/DEVICE_PROFILES.md`): Reset, the Leave and Arrive options, the
 * date row with its two chevrons, the time picker and the select button. Everything the rider presses stands inside the safe
 * area, and Reset and both chevrons take a real touch.
 *
 * The screen is composed in [SheetHost], which places it as the sheet does, because the sheet is where the fault was: the sheet's
 * window runs edge to edge, so on the outer screen of the iPhone Duo the content ran under the status pill and Reset opened
 * Control Center. The screen itself pads for nothing, so a test that composed it alone would pass on every profile.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class DateTimeSelectorDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private val selection = DateTimeSelectionItem(
        option = JourneyTimeOptions.ARRIVE,
        hour = 8,
        minute = 30,
        date = LocalDate(2030, 1, 1),
    )

    @Test
    fun `Plan your trip keeps every control inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "DateTimeSelectorScreen") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                SheetHost { DateTimeSelectorScreen(dateTimeSelection = selection) }
            }

            assertNothingInteractiveUnderSystemBars()
            assertScrollEndClearsBottomBar(onNode(hasScrollAction()))
        }

    @Test
    fun `Reset and both date chevrons take a real touch on every device`() =
        matrix.forEachDeviceProfile(screen = "DateTimeSelectorScreen") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                SheetHost { DateTimeSelectorScreen(dateTimeSelection = selection) }
            }

            assertTouchReaches(onNodeWithText("Reset"))
            val chevrons = onAllNodes(DATE_CHEVRON)
            chevrons.assertCountEquals(2)
            assertTouchReaches(chevrons[0])
            assertTouchReaches(chevrons[1])
        }

    private companion object {
        /**
         * The date row's two chevrons: the only controls on the sheet with no text and no description, because they are an
         * image alone. The semantics tree puts every item of the sheet's list side by side, so the date text is not a sibling
         * to find them by.
         */
        val DATE_CHEVRON = hasClickAction() and
            SemanticsMatcher.keyNotDefined(SemanticsProperties.Text) and
            SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription)
    }
}
