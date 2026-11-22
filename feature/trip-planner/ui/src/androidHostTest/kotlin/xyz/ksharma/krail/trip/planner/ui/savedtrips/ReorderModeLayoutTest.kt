package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapActions
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapScreenForTest
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.PARK_RIDE_ROW_KEY
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reorder mode of `docs/MAP_HOME_UX.md` (rules 31 and 38) on the real home screen: the chrome is
 * gone, one dark Done sits 16 dp from the top and the right edge, every trip card has a grip centred
 * on its top edge and a delete circle, the Park & Ride row is one outlined block with a wider grip
 * and no delete circle, and back cancels.
 *
 * Each test runs in a window of its own width (density 1, so a dp is a pixel) and reads where the
 * real nodes landed. The session is state the ViewModel holds, so the screen is given one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [REORDER_TEST_SDK], manifest = Config.NONE)
class ReorderModeLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    private val events = mutableListOf<SavedTripUiEvent>()

    // region chrome

    @Test
    @Config(qualifiers = PHONE)
    fun `the logo, the gear, the chips and the search row are gone while reordering`() {
        setScreen(reordering = true)

        composeRule.onNodeWithText("KRAIL").assertDoesNotExist()
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS).assertCountEquals(0)
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW).assertCountEquals(0)
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_ROW).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `outside reorder mode the chrome is there and Done is not`() {
        setScreen(reordering = false)

        composeRule.onNodeWithText("KRAIL").assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW).assertExists()
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `Done is one 48 dp button 16 dp from the top and the right edge`() = assertDonePlace(
        window = 411.dp,
        scale = 1f,
    )

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `Done keeps its place at 320 dp and 200 percent text`() = assertDonePlace(window = 320.dp, scale = 2f)

    private fun assertDonePlace(window: Dp, scale: Float) {
        setScreen(reordering = true, fontScale = scale)

        val done = bounds(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE)
        assertNear("Done right edge", window - 16.dp, done.right)
        assertNear("Done top edge", 16.dp, done.top)
        assertTrue(done.height >= 48.dp - TOLERANCE, "Done is at least 48 dp tall but is ${done.height}")
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the stack takes the height the search row held while reordering`() {
        setScreen(reordering = true, trips = MANY_TRIPS)
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST).performScrollToIndex(MANY_TRIPS.size)
        composeRule.waitForIdle()

        val last = bounds(TripPlannerTestTags.savedTripCard(MANY_TRIPS.last().tripId))
        assertTrue(last.bottom > 891.dp - 100.dp, "The last card ends at ${last.bottom}, near the bottom of the window")
    }

    // endregion

    // region grips, delete circle, block

    @Test
    @Config(qualifiers = PHONE)
    fun `every trip card has a grip centred on its top edge`() {
        setScreen(reordering = true)

        TRIPS.forEach { trip ->
            val card = bounds(TripPlannerTestTags.savedTripCard(trip.tripId))
            val grip = bounds(TripPlannerTestTags.Reorder.grip(trip.tripId))
            assertNear("grip centre x on ${trip.tripId}", (card.left + card.right) / 2, (grip.left + grip.right) / 2)
            assertNear("grip centre y on the top edge of ${trip.tripId}", card.top, (grip.top + grip.bottom) / 2)
            assertNear("grip width", 32.dp, grip.width)
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the Park and Ride row sits in an outlined block with its own wider grip`() {
        setScreen(reordering = true)

        val block = bounds(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_BLOCK)
        val grip = bounds(TripPlannerTestTags.Reorder.grip(PARK_RIDE_ROW_KEY))
        assertNear("block left", 16.dp, block.left)
        assertNear("block right", 411.dp - 16.dp, block.right)
        assertNear("block grip centre x", (block.left + block.right) / 2, (grip.left + grip.right) / 2)
        assertNear("block grip centre y", block.top, (grip.top + grip.bottom) / 2)
        assertNear("the row's grip is wider than a trip's", 64.dp, grip.width)
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(STATION.stopId)).assertExists()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a trip card has the 24 dp delete circle in a 48 dp target, overhanging the top right corner by 6 dp`() {
        setScreen(reordering = true)

        val card = bounds(TripPlannerTestTags.savedTripCard(TRIPS[0].tripId))
        val target = bounds(TripPlannerTestTags.Reorder.delete(TRIPS[0].tripId))
        assertNear("delete target width", 48.dp, target.width)
        assertNear("delete target height", 48.dp, target.height)
        // The circle's centre is 6 dp in from the card's right edge and 6 dp down from its top.
        assertNear("delete centre x", card.right - 6.dp, (target.left + target.right) / 2)
        assertNear("delete centre y", card.top + 6.dp, (target.top + target.bottom) / 2)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a station carries no delete circle and no touch of its own in reorder mode`() {
        setScreen(reordering = true)

        composeRule.onAllNodesWithTag(TripPlannerTestTags.Reorder.delete(PARK_RIDE_ROW_KEY)).assertCountEquals(0)
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(STATION.stopId)).performClick()
        assertTrue(events.none { it is SavedTripUiEvent.ParkRideCardClick }, "A tile cannot be opened while reordering")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the delete circle takes a tap and stages the trip`() {
        setScreen(reordering = true)

        composeRule.onNodeWithTag(TripPlannerTestTags.Reorder.delete(TRIPS[1].tripId)).performClick()

        assertEquals(SavedTripUiEvent.ReorderRemove(TRIPS[1].tripId), events.last())
    }

    // endregion

    // region entering, leaving

    @Test
    @Config(qualifiers = PHONE)
    fun `a long press on a trip card begins reorder mode with that card`() {
        setScreen(reordering = false)

        composeRule.onNodeWithTag(TripPlannerTestTags.savedTripCard(TRIPS[1].tripId))
            .performTouchInput { longClick() }

        assertEquals(SavedTripUiEvent.EnterReorder(TRIPS[1].tripId), events.last())
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a long press on a Park and Ride tile begins reorder mode with the row`() {
        setScreen(reordering = false)

        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(STATION.stopId))
            .performTouchInput { longClick() }

        assertEquals(SavedTripUiEvent.EnterReorder(PARK_RIDE_ROW_KEY), events.last())
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a short tap on a tile still opens it and does not begin reorder mode`() {
        setScreen(reordering = false)

        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(STATION.stopId)).performClick()

        assertTrue(events.none { it is SavedTripUiEvent.EnterReorder })
        assertTrue(events.any { it is SavedTripUiEvent.ParkRideCardClick })
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `Done saves`() {
        setScreen(reordering = true)

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE).performClick()

        assertEquals(SavedTripUiEvent.ReorderDone, events.last())
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the system back cancels and does not save`() {
        setScreen(reordering = true)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(SavedTripUiEvent.ReorderCancel, events.last())
        assertTrue(events.none { it == SavedTripUiEvent.ReorderDone })
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `back does nothing to the session when it is not reordering`() {
        setScreen(reordering = false)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertTrue(events.none { it == SavedTripUiEvent.ReorderCancel })
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a rotation keeps the edit because the screen holds none of it`() {
        restorationTester.setContent { PreviewTheme { Screen(reordering = true, fontScale = 1f, trips = TRIPS) } }
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.Reorder.grip(TRIPS[0].tripId)).assertExists()
    }

    // endregion

    private fun setScreen(reordering: Boolean, fontScale: Float = 1f, trips: List<Trip> = TRIPS) {
        composeRule.setContent { PreviewTheme { Screen(reordering, fontScale, trips) } }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Screen(reordering: Boolean, fontScale: Float, trips: List<Trip>) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            HomeMapScreenForTest(
                savedTripsState = SavedTripsState(
                    isSavedTripsLoading = false,
                    savedTrips = persistentListOf(*trips.toTypedArray()),
                    parkRideUiState = persistentListOf(STATION),
                    stopLabels = persistentListOf(
                        StopLabel(emoji = "H", label = "Home"),
                        StopLabel(emoji = "W", label = "Work"),
                    ),
                    reorder = if (reordering) {
                        ReorderSession.start(trips.map { it.tripId }, rowIndex = 2)
                    } else {
                        null
                    },
                ),
                actions = HomeMapActions(onEvent = { events += it }),
            )
        }
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(abs(expected.value - actual.value) <= TOLERANCE.value, "$what should be $expected but is $actual.")
    }

    private companion object {
        const val PHONE = "w411dp-h891dp"
        val TOLERANCE = 1.5.dp

        val TRIPS = listOf(
            Trip(
                fromStopId = "200060",
                fromStopName = "Central Station",
                toStopId = "200070",
                toStopName = "Town Hall Station",
            ),
            Trip(
                fromStopId = "207710",
                fromStopName = "Parramatta Station",
                toStopId = "206710",
                toStopName = "Chatswood Station",
            ),
            Trip(
                fromStopId = "200080",
                fromStopName = "Wynyard Station",
                toStopId = "200090",
                toStopName = "Redfern Station",
            ),
        )

        val MANY_TRIPS = (1..12).map { n ->
            Trip(fromStopId = "30$n", fromStopName = "From $n", toStopId = "40$n", toStopName = "To $n")
        }

        val STATION = ParkRideUiState(
            stopId = "2153478",
            stopName = "Bella Vista",
            facilities = persistentSetOf(
                ParkRideUiState.ParkRideFacilityDetail(
                    spotsAvailable = 120,
                    totalSpots = 774,
                    facilityName = "Bella Vista Car Park",
                    percentageFull = 84,
                    stopId = "2153478",
                    timeText = "10:00 AM",
                    facilityId = "31",
                ),
            ),
        )
    }
}

private const val REORDER_TEST_SDK = 34
