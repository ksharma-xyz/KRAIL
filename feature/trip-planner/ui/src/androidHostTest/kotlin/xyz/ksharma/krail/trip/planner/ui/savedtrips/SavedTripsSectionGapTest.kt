package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
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
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * The space above a section heading on the classic Saved Trips home is one value, whatever stands above the heading.
 *
 * Both headings are the same composable with the same padding, so they only differ when an item above one of them adds space of
 * its own: the last saved trip card used to end with a 16 dp spacer that stacked with the Park & Ride heading's own 16 dp, which
 * made the gap above Park & Ride twice the gap above Saved Trips. The gap is read the same way for both: from the bottom of the
 * element above (the label row, the last card, or the top of the list when nothing is above) to the top of the heading's text.
 * That is the label row's 48 dp touch slot and not the 36 dp pill drawn inside it, since the slot is the element in the list.
 * It is checked at 100 and 200 percent text, with and without labels, with and without Park & Ride rows, with nothing saved,
 * and while the cards are being reordered.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class SavedTripsSectionGapTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = PHONE)
    fun `with labels the gap above Park and Ride equals the gap above Saved Trips at 100 percent text`() {
        setScreen()

        assertEqualGaps(gapAboveSavedTripsFromLabels(), gapAboveParkRideFromLastCard(), "labels, 100 percent")
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `with labels the gap above Park and Ride equals the gap above Saved Trips at 200 percent text`() {
        setScreen(fontScale = 2f)

        assertEqualGaps(gapAboveSavedTripsFromLabels(), gapAboveParkRideFromLastCard(), "labels, 200 percent")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with no labels the gap above Saved Trips, from the title bar, equals the gap above Park and Ride`() {
        setScreen(labels = persistentListOf())

        assertEqualGaps(gapAboveSavedTripsFromListTop(), gapAboveParkRideFromLastCard(), "no labels, 100 percent")
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `with no labels the same holds at 200 percent text`() {
        setScreen(labels = persistentListOf(), fontScale = 2f)

        assertEqualGaps(gapAboveSavedTripsFromListTop(), gapAboveParkRideFromLastCard(), "no labels, 200 percent")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with no Park and Ride rows the gap above the Park and Ride heading is the same`() {
        setScreen(parkRide = persistentListOf())

        assertEqualGaps(gapAboveSavedTripsFromLabels(), gapAboveParkRideFromLastCard(), "no Park and Ride rows")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `while the cards are reordered both gaps are still the same value`() {
        setScreen()
        startReordering()

        assertEqualGaps(
            gapAboveSavedTripsFromListTop(),
            gapAboveParkRideFromLastCard(reordering = true),
            "reordering, 100 percent",
        )
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `while the cards are reordered the same holds at 200 percent text`() {
        setScreen(fontScale = 2f)
        startReordering()

        assertEqualGaps(
            gapAboveSavedTripsFromListTop(),
            gapAboveParkRideFromLastCard(reordering = true),
            "reordering, 200 percent",
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with nothing saved the gap above Park and Ride, from the label row, is the gap Saved Trips had`() {
        val trips = mutableStateOf(TRIPS)
        setScreen(tripsState = trips)
        val reference = gapAboveSavedTripsFromLabels()

        trips.value = emptyList()
        composeRule.waitForIdle()

        assertEqualGaps(
            reference,
            gapAbove("Park & Ride", bounds(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).bottom),
            "nothing saved",
        )
    }

    private fun gapAboveSavedTripsFromLabels(): Dp =
        gapAbove("Saved Trips", bounds(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).bottom)

    // With nothing above the heading, the element above is the title bar, and the list starts where it ends.
    private fun gapAboveSavedTripsFromListTop(): Dp = gapAbove(
        "Saved Trips",
        bounds(TripPlannerTestTags.SAVED_TRIPS_LIST).top,
    )

    private fun gapAboveParkRideFromLastCard(reordering: Boolean = false): Dp =
        gapAbove("Park & Ride", lastCardBottom(reordering))

    /**
     * The bottom of the last saved trip card. While the cards are reordered each one wiggles by a fraction of a degree, which
     * tilts its drawn box and moves its bottom corners by a couple of dp either way, so there the bottom is the card's centre plus
     * half its own height, which a rotation about the centre does not change.
     */
    private fun lastCardBottom(reordering: Boolean): Dp {
        val tag = TripPlannerTestTags.savedTripCard(TRIPS.last().tripId)
        if (!reordering) return bounds(tag).bottom
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val density = node.layoutInfo.density.density
        return ((node.boundsInRoot.center.y + node.size.height / 2f) / density).dp
    }

    /** From the bottom of the element above to the top of the heading's text. */
    private fun gapAbove(heading: String, aboveBottom: Dp): Dp {
        val text = composeRule.onNodeWithText(heading, useUnmergedTree = true).getVisibleBoundsInRoot()
        return text.top - aboveBottom
    }

    private fun assertEqualGaps(savedTrips: Dp, parkRide: Dp, case: String) {
        assertTrue(savedTrips > MIN_GAP, "$case: the gap above Saved Trips is $savedTrips, which is no gap at all")
        assertTrue(
            abs(savedTrips.value - parkRide.value) <= TOLERANCE_DP,
            "$case: the gap above Saved Trips is $savedTrips and above Park & Ride is $parkRide, so they differ by " +
                "${abs(savedTrips.value - parkRide.value)} dp.",
        )
    }

    private fun startReordering() {
        composeRule.onNodeWithTag(
            TripPlannerTestTags.savedTripCard(TRIPS.first().tripId),
        ).performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE).assertExists()
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(
        tag,
        useUnmergedTree = true,
    ).getVisibleBoundsInRoot()

    private fun setScreen(
        fontScale: Float = 1f,
        labels: ImmutableList<StopLabel> = LABELS,
        parkRide: ImmutableList<ParkRideUiState> = persistentListOf(STATION),
        // Trips that change while the screen is up, in place of the fixed ones.
        tripsState: State<List<Trip>>? = null,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PreviewTheme {
                    SavedTripsScreen(
                        savedTripsState = SavedTripsState(
                            isSavedTripsLoading = false,
                            savedTrips = (tripsState?.value ?: TRIPS).toImmutableList(),
                            stopLabels = labels,
                            parkRideUiState = parkRide,
                        ),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val PHONE = "w411dp-h891dp"
        const val NARROW = "w320dp-h1280dp"
        const val TOLERANCE_DP = 0.5f
        val MIN_GAP = 4.dp

        val TRIPS = listOf(
            Trip(
                fromStopId = "200060",
                fromStopName = "Central Station",
                toStopId = "200070",
                toStopName = "Town Hall Station",
            ),
            Trip(
                fromStopId = "200080",
                fromStopName = "Wynyard Station",
                toStopId = "200090",
                toStopName = "Seven Hills Station",
            ),
        )

        val LABELS = persistentListOf(
            StopLabel(emoji = "H", label = "Home", stopId = "200080", stopName = "Wynyard Station"),
            StopLabel(emoji = "W", label = "Work"),
        )

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
