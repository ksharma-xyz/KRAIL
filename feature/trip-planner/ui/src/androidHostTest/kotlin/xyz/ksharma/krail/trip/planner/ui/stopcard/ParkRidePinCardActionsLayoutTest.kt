package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Go here on the Park & Ride pin card, on real nodes (`docs/MAP_HOME_UX.md` rules 13, 21 and 40): one whole
 * button under another at every width and text size, Go here the primary when the station has a stop and
 * Add to home or Remove from home the outlined one, and a station with no stop showing the home action alone.
 *
 * Each test runs in a window of its own size at density 1, so a dp is a pixel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [PIN_ACTIONS_TEST_SDK], manifest = Config.NONE)
class ParkRidePinCardActionsLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val picked = mutableListOf<StopItem>()
    private var toggled = 0

    @Test
    @Config(qualifiers = PHONE)
    fun `a station with a stop reads Go here under Remove from home, both whole and 48 dp tall`() {
        showCard(goHereStopId = "2155384")

        assertEquals("Go here", textOf(StopCard.PICK))
        assertEquals("Remove from home", textOf(StopCard.HOME_ACTION))
        assertTrue(
            bounds(StopCard.PICK).top >= bounds(StopCard.HOME_ACTION).bottom,
            "Go here sits under Remove from home.",
        )
        assertTrue(bounds(StopCard.PICK).height >= 48.dp - TOLERANCE)
        assertTrue(bounds(StopCard.HOME_ACTION).height >= 48.dp - TOLERANCE)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `go here hands the station on as a stop, named as the station, and Add to home does not`() {
        showCard(goHereStopId = "2155384")

        composeRule.onNodeWithTag(StopCard.PICK).performClick()

        assertEquals(listOf(StopItem(stopId = "2155384", stopName = "Tallawong")), picked)
        assertEquals(0, toggled)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a station with no stop has no Go here and its one action is the primary`() {
        showCard(goHereStopId = null)

        composeRule.onAllNodesWithTag(StopCard.PICK).assertCountEquals(0)
        composeRule.onNodeWithTag(StopCard.HOME_ACTION).assertHasClickAction().performClick()
        assertEquals(1, toggled)
        assertTrue(picked.isEmpty())
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a start search reads Start here`() {
        showCard(goHereStopId = "2155384", role = PickRole.Start)

        assertEquals("Start here", textOf(StopCard.PICK))
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the home action reads Add to home when the station is not on home`() {
        showCard(goHereStopId = "2155384", isOnHome = false)

        assertEquals("Add to home", textOf(StopCard.HOME_ACTION))
    }

    @Test
    @Config(qualifiers = "w320dp-h900dp")
    fun `at 320 dp and 200 percent text both buttons are the card's full width, one under the other`() {
        showCard(goHereStopId = "2155384", fontScale = 2f)

        val pick = bounds(StopCard.PICK)
        val home = bounds(StopCard.HOME_ACTION)
        assertNear("the two are as wide as each other", home.width, pick.width)
        assertTrue(pick.top >= home.bottom - TOLERANCE, "Go here is under Remove from home.")
        assertTrue(
            pick.right <= bounds(StopCard.CARD).right && pick.left >= bounds(StopCard.CARD).left,
            "Go here is cut.",
        )
    }

    private fun showCard(
        goHereStopId: String?,
        role: PickRole = PickRole.Destination,
        isOnHome: Boolean = true,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            WithFontScale(fontScale) {
                PreviewTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                            ParkRidePinCard(
                                tile = TALLAWONG.toPinCardTile(emptyList()),
                                isOnHome = isOnHome,
                                onToggleHome = { toggled++ },
                                onDirectionsClick = {},
                                onClose = {},
                                pickAction = parkRidePickActionFor(TALLAWONG.stationName, goHereStopId, role),
                                onPick = { picked += it },
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
            content = content,
        )
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun textOf(tag: String): String =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE.value,
            "$what should be $expected but is $actual.",
        )
    }

    private companion object {
        const val PHONE = "w411dp-h891dp"
        val TOLERANCE = 1.5.dp

        val TALLAWONG = ParkRideStationPickerItem(
            stationId = "2155384",
            stationName = "Tallawong",
            stopName = "Tallawong Station",
            carParkNames = listOf("Tallawong P1"),
            mappings = listOf(ParkRideMapping("2155384", "26", "Tallawong P1")),
        )
    }
}

private const val PIN_ACTIONS_TEST_SDK = 34
