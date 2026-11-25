package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.assertEquals

/**
 * Back on the whole screen while there is no map. The list is then the only state there is to draw, and it takes
 * focus by itself, which the flow records as a tap on the bar. The pure flow tests never see that focus, so these
 * run the screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class SearchStopMapsOffBackTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var exits = 0

    // Stops no saved label points at, so the box names them by their stop names.
    private val origin = StopItem(stopName = "Redfern Station", stopId = "stop_redfern")
    private val destination = StopItem(stopName = "Wynyard Station", stopId = "stop_wynyard")

    private fun open(
        fieldType: SearchStopFieldType = SearchStopFieldType.FROM,
        presetFrom: StopItem? = null,
        presetTo: StopItem? = null,
    ) {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithHomeSet().copy(isMapsAvailable = false),
                    fieldType = fieldType,
                    presetFrom = presetFrom,
                    presetTo = presetTo,
                    goBack = { exits++ },
                    onEvent = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun systemBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun buttonBack() {
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_BACK).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `with no map one system back leaves the screen it opened on`() {
        open()

        systemBack()

        assertEquals(1, exits)
    }

    @Test
    fun `with no map one tap on the back button leaves the screen it opened on`() {
        open()

        buttonBack()

        assertEquals(1, exits)
    }

    @Test
    fun `with no map one back leaves after the rider has moved to the other bar`() {
        open(fieldType = SearchStopFieldType.TO)
        composeRule.onNodeWithText(FROM_PLACEHOLDER).performClick()
        composeRule.waitForIdle()

        systemBack()

        assertEquals(1, exits)
    }

    @Test
    fun `with no map one back leaves the single field form`() {
        open(fieldType = SearchStopFieldType.LABEL)

        systemBack()

        assertEquals(1, exits)
    }

    @Test
    fun `with no map and both bars filled one back leaves the box`() {
        open(presetFrom = origin, presetTo = destination)

        systemBack()

        assertEquals(1, exits)
    }

    @Test
    fun `with no map and both bars filled back closes an opened list first, then leaves`() {
        open(presetFrom = origin, presetTo = destination)
        composeRule.onNodeWithText(origin.stopName).performClick()
        composeRule.waitForIdle()

        systemBack()
        assertEquals(0, exits, "The first back closes the list and shows the box again.")
        systemBack()

        assertEquals(1, exits)
    }
}
