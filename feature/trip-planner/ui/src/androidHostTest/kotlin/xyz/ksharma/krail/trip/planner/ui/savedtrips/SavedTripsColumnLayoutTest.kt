package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapScreenForTest
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Rule 45 of `docs/MAP_HOME_UX.md` on the real screen: trips are one column, at most 560 dp
 * wide and centred, and the trip cards, the Park & Ride row and the bottom search row share
 * its edges at every window size.
 *
 * Each test runs in a window of its own width (density 1, so a dp is a pixel) and reads where
 * the real nodes landed. The expected column is written out per window and not asked of
 * [columnWidthFor], so a wrong cap fails here as well as in the table test. The search row's
 * block sits on the column's edges and its fields, like the cards, one page padding inside them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [COLUMN_TEST_SDK], manifest = Config.NONE)
class SavedTripsColumnLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `a 320 dp window is all column with no gutter`() = assertOneColumn(window = 320.dp, column = 320.dp)

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a phone window is all column with no gutter`() = assertOneColumn(window = 411.dp, column = 411.dp)

    @Test
    @Config(qualifiers = "w559dp-h900dp")
    fun `one dp under the cap is still all column`() = assertOneColumn(window = 559.dp, column = 559.dp)

    @Test
    @Config(qualifiers = "w560dp-h900dp")
    fun `a window exactly the cap is all column`() = assertOneColumn(window = 560.dp, column = 560.dp)

    @Test
    @Config(qualifiers = "w700dp-h1000dp")
    fun `a 700 dp window keeps the column at 560 dp and centred`() =
        assertOneColumn(window = 700.dp, column = 560.dp)

    @Test
    @Config(qualifiers = "w839dp-h1000dp")
    fun `the widest single pane window keeps the column at 560 dp and centred`() =
        assertOneColumn(window = 839.dp, column = 560.dp)

    @Test
    @Config(qualifiers = "w1024dp-h768dp")
    fun `a dual pane window keeps the column inside the 480 dp list pane`() =
        assertOneColumn(window = 1024.dp, column = DUAL_PANE_LIST_WIDTH, pane = DUAL_PANE_LIST_WIDTH)

    private fun assertOneColumn(window: Dp, column: Dp, pane: Dp = window) {
        composeRule.setContent {
            PreviewTheme { HomeMapScreenForTest(savedTripsState = STATE) }
        }
        composeRule.waitForIdle()

        val columnLeft = (pane - column) / 2
        val columnRight = columnLeft + column
        val cardLeft = columnLeft + PAGE_PADDING
        val cardRight = columnRight - PAGE_PADDING
        val at = "in a $window window (pane $pane, column $column)"

        val trip = boundsOfTag(TripPlannerTestTags.savedTripCard(STATE.savedTrips.first().tripId))
        assertEdges("saved trip card $at", trip, cardLeft, cardRight)

        // The Park & Ride row is one block on the column's own edges. Its first tile starts one page
        // padding in, where the cards do, and the row has no heading or card of its own around it.
        assertEdges(
            "Park & Ride row $at",
            boundsOfTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW),
            columnLeft,
            columnRight,
        )
        assertLeft("first Park & Ride tile $at", boundsOfTag(TripPlannerTestTags.parkRideTile(STATION_ID)), cardLeft)

        // The map home's one search pill is on the cards' own edges, so it starts and ends where they do.
        val searchRow = boundsOfTag(TripPlannerTestTags.SEARCH_ROW)
        assertEdges("search pill $at", searchRow, cardLeft, cardRight)

        assertLeft("search row To field $at", boundsOfTag(TripPlannerTestTags.SEARCH_ROW_TO), cardLeft)
    }

    private fun boundsOfTag(tag: String): DpRect =
        composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertEdges(what: String, actual: DpRect, left: Dp, right: Dp) {
        assertLeft(what, actual, left)
        assertNear("$what, right edge", right, actual.right)
    }

    private fun assertLeft(what: String, actual: DpRect, left: Dp) =
        assertNear("$what, left edge", left, actual.left)

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= EDGE_TOLERANCE_DP,
            "$what should be at $expected but is at $actual.",
        )
    }

    private companion object {
        const val STOP_NAME = "Bella Vista"
        const val STATION_ID = "2153478"
        const val EDGE_TOLERANCE_DP = 1f

        /** The page padding every card and field sits inside the column by. */
        val PAGE_PADDING = 16.dp

        val STATE = SavedTripsState(
            isSavedTripsLoading = false,
            savedTrips = persistentListOf(
                Trip(
                    fromStopId = "200060",
                    fromStopName = "Central Station",
                    toStopId = "200070",
                    toStopName = "Town Hall Station",
                ),
            ),
            parkRideUiState = persistentListOf(
                ParkRideUiState(
                    stopId = "2153478",
                    stopName = STOP_NAME,
                    isLoading = false,
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
                ),
            ),
        )
    }
}

private const val COLUMN_TEST_SDK = 34
