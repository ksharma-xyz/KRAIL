package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.core.testing.device.DeviceMatrixScope
import xyz.ksharma.krail.core.testing.device.visibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapActions
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapScreenForTest
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.assertTrue

/**
 * The saved trips home, the classic one and the map home, on every device profile (`docs/testing/DEVICE_PROFILES.md`), with the
 * cards being reordered as well as at rest. The gear, Discover, the trip cards, the Done button, the label row and the search
 * capsule must stand inside the safe area on every device, and the list must scroll clear of the bottom bar. The label row is cut
 * by the side safe area edge and never runs under it, which on the Duo outer screen is the 84 dp strip where the status pill floats.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class SavedTripsDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private fun savedTripsState(reordering: Boolean = false, labels: ImmutableList<StopLabel> = LABELS) = SavedTripsState(
        isSavedTripsLoading = false,
        savedTrips = TRIPS.toImmutableList(),
        parkRideUiState = persistentListOf(STATION),
        stopLabels = labels,
        reorder = if (reordering) ReorderSession.start(TRIPS.map { it.tripId }, rowIndex = 2) else null,
    )

    private fun DeviceMatrixScope.showSavedTrips(
        labels: ImmutableList<StopLabel> = LABELS,
        ai: AiSearchInputUiState = AiSearchInputUiState(),
    ) = setContent(theme = { BareKrailTheme(content = it) }) {
        SavedTripsScreen(savedTripsState = savedTripsState(labels = labels), aiState = ai)
    }

    /** The side edges of the safe area in dp, which the label row and the capsule must stay between. */
    private fun DeviceMatrixScope.safeSides() = profile.insets.left.dp to (profile.widthDp - profile.insets.right).dp

    private fun DeviceMatrixScope.assertInsideTheSides(tag: String, what: String) {
        val (left, right) = safeSides()
        val bounds = onNodeWithTag(tag, useUnmergedTree = true).visibleBoundsInRoot()
        assertTrue(
            bounds.left >= left - TOLERANCE_DP.dp && bounds.right <= right + TOLERANCE_DP.dp,
            "${profile.name}: $what at $bounds runs outside the safe sides $left to $right",
        )
    }

    private fun DeviceMatrixScope.showMapHome(reordering: Boolean) =
        setContent(theme = { BareKrailTheme(content = it) }) {
            HomeMapScreenForTest(
                savedTripsState = savedTripsState(reordering),
                actions = HomeMapActions(),
            )
        }

    @Test
    fun `Saved Trips keeps the gear, the cards, the label row and the search capsule inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "SavedTripsScreen") {
            showSavedTrips()

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.savedTripCard(TRIPS.first().tripId)))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.homeLabelChip("Home"), useUnmergedTree = true))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.homeLabelChip("Work"), useUnmergedTree = true))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true))
            assertInsideTheSides(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS, "the label row")
            assertInsideTheSides(TripPlannerTestTags.SEARCH_ROW_TO, "the search capsule")
            assertScrollEndClearsBottomBar(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST))
        }

    @Test
    fun `Saved Trips keeps the microphone in the capsule pressable on every device`() =
        matrix.forEachDeviceProfile(screen = "SavedTripsScreen") {
            showSavedTrips(ai = AiSearchInputUiState(isFeatureEnabled = true))

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL, useUnmergedTree = true))
            assertInsideTheSides(TripPlannerTestTags.SEARCH_ROW_TO, "the search capsule")
        }

    @Test
    fun `Saved Trips cuts a long label row at the side safe edge, never under the system strip, and every label can be reached`() =
        matrix.forEachDeviceProfile(screen = "SavedTripsScreen") {
            showSavedTrips(labels = MANY_LABELS)

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.homeLabelChip("Home"), useUnmergedTree = true))
            assertInsideTheSides(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS, "the label row")

            // Eight labels with a long name are wider than any window up to ABOUT_ONE_PANE_DP, so the row must scroll there.
            val scroller = onNode(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange) and
                    hasAnyAncestor(hasTestTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS)),
                useUnmergedTree = true,
            )
            val overflows = scroller.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].maxValue() > 0f
            assertTrue(
                overflows || profile.widthDp > ABOUT_ONE_PANE_DP,
                "${profile.name}: the long label row did not overflow, so no pill was cut",
            )

            // The last label scrolls into view, stands inside the safe sides and takes a touch, so the cut hides nothing for good.
            val lastTag = TripPlannerTestTags.homeLabelChip(MANY_LABELS.last().label)
            onNodeWithTag(lastTag, useUnmergedTree = true).performScrollTo()
            rule.waitForIdle()
            assertTouchReaches(onNodeWithTag(lastTag, useUnmergedTree = true))
            assertInsideTheSides(lastTag, "the last label scrolled into view")
            assertInsideTheSides(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS, "the label row after scrolling")
        }

    @Test
    fun `Saved Trips keeps Done and the cards inside the safe area while the cards are reordered`() =
        matrix.forEachDeviceProfile(screen = "SavedTripsScreen") {
            showSavedTrips()
            onNodeWithTag(TripPlannerTestTags.savedTripCard(TRIPS.first().tripId)).performTouchInput { longClick() }
            rule.waitForIdle()

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE))
            assertScrollEndClearsBottomBar(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST))
        }

    @Test
    fun `the map home keeps its chrome, its cards and its search sheet inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "HomeMapScreen") {
            showMapHome(reordering = false)

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS))
        }

    @Test
    fun `the map home keeps Done, the grips and the delete circles inside the safe area while reordering`() =
        matrix.forEachDeviceProfile(screen = "HomeMapScreen") {
            showMapHome(reordering = true)

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE))
        }

    private companion object {
        const val TOLERANCE_DP = 1f

        // The eight labels, one with a long name, come to well over 480 dp of pills, so any window this narrow must scroll the row.
        const val ABOUT_ONE_PANE_DP = 480

        val LABELS = persistentListOf(
            StopLabel(emoji = "H", label = "Home", stopId = "200060", stopName = "Central Station"),
            StopLabel(emoji = "W", label = "Work"),
        )

        val MANY_LABELS = persistentListOf(
            StopLabel(emoji = "H", label = "Home", stopId = "200060", stopName = "Central Station"),
            StopLabel(emoji = "W", label = "Work"),
            StopLabel(emoji = "U", label = "Uni", stopId = "206710", stopName = "Chatswood Station"),
            StopLabel(emoji = "G", label = "Gym"),
            StopLabel(emoji = "P", label = "Grandma's place in Marrickville"),
            StopLabel(emoji = "B", label = "Beach", stopId = "200090", stopName = "Bondi Junction"),
            StopLabel(emoji = "L", label = "Library"),
            StopLabel(emoji = "C", label = "Cafe", stopId = "200100", stopName = "Newtown Station"),
        )

        val TRIPS = (1..SAVED_TRIP_COUNT).map { index ->
            Trip(
                fromStopId = "F$index",
                fromStopName = "From $index Station",
                toStopId = "T$index",
                toStopName = "To $index Station",
            )
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

private const val SAVED_TRIP_COUNT = 8
