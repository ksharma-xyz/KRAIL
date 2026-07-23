package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.assertEquals

/**
 * Compose UI tests for [savedTripsListBody]'s section visibility.
 *
 * The rule under test: a rider with nothing saved gets no Saved Trips section at all (no
 * heading, no placeholder row), while Park & Ride always renders so the feature stays
 * reachable before a first trip is saved.
 *
 * Card contents, reordering and Park & Ride expansion/polling are out of scope here and
 * covered by [SavedTripViewModelsTest] and the snapshot suite.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SavedTripsListBodyTest {

    @get:Rule
    val composeRule = createComposeRule()

    // region Empty saved trips

    @Test
    fun noSavedTrips_savedTripsHeadingIsNotShown() {
        composeRule.setContent { ListBody(state = emptyState) }

        composeRule.onNodeWithText(SAVED_TRIPS_TITLE).assertDoesNotExist()
    }

    @Test
    fun noSavedTrips_placeholderRowIsNotShown() {
        composeRule.setContent { ListBody(state = emptyState) }

        composeRule.onNodeWithText(EMPTY_PLACEHOLDER).assertDoesNotExist()
    }

    @Test
    fun noSavedTrips_parkRideSectionStillRenders() {
        composeRule.setContent { ListBody(state = emptyState) }

        composeRule.onNodeWithText(PARK_RIDE_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(ADD_FIRST_PARK_RIDE).assertIsDisplayed()
    }

    @Test
    fun noSavedTrips_addParkRideClickIsForwarded() {
        var addClicks = 0
        composeRule.setContent { ListBody(state = emptyState, onAddParkRideClick = { addClicks++ }) }

        composeRule.onNodeWithText(ADD_FIRST_PARK_RIDE).performClick()

        assertEquals(1, addClicks)
    }

    // endregion

    // region Populated saved trips

    @Test
    fun withSavedTrips_savedTripsHeadingIsShown() {
        composeRule.setContent { ListBody(state = populatedState) }

        composeRule.onNodeWithText(SAVED_TRIPS_TITLE).assertIsDisplayed()
    }

    @Test
    fun withSavedTrips_parkRideSectionStillRenders() {
        composeRule.setContent { ListBody(state = populatedState) }

        composeRule.onNodeWithText(PARK_RIDE_TITLE).assertIsDisplayed()
    }

    // endregion

    // region Loading

    @Test
    fun loading_rendersNothing() {
        composeRule.setContent { ListBody(state = SavedTripsState(isSavedTripsLoading = true)) }

        composeRule.onNodeWithText(SAVED_TRIPS_TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(PARK_RIDE_TITLE).assertDoesNotExist()
    }

    // endregion

    @Composable
    private fun ListBody(
        state: SavedTripsState,
        onAddParkRideClick: () -> Unit = {},
    ) {
        PreviewTheme {
            val lazyListState = rememberLazyListState()
            val reorderState = rememberReorderableLazyListState(lazyListState) { _, _ -> }
            val expandedMap: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }

            LazyColumn(state = lazyListState) {
                savedTripsListBody(
                    savedTripsState = state,
                    trackedJourney = null,
                    onEvent = {},
                    onSavedTripCardClick = { _, _ -> },
                    onTrackingCardClick = {},
                    onStopTracking = {},
                    expandedMap = expandedMap,
                    editing = false,
                    reorderState = reorderState,
                    onEnterEditing = {},
                    onAddParkRideClick = onAddParkRideClick,
                )
            }
        }
    }

    private companion object {
        const val SAVED_TRIPS_TITLE = "Saved Trips"
        const val PARK_RIDE_TITLE = "Park & Ride"
        const val ADD_FIRST_PARK_RIDE = "Add Park & Ride"
        const val EMPTY_PLACEHOLDER = "Tap ★ on a trip to save it here."

        val emptyState = SavedTripsState(isSavedTripsLoading = false)

        val populatedState = SavedTripsState(
            isSavedTripsLoading = false,
            savedTrips = persistentListOf(
                Trip(
                    fromStopId = "200060",
                    fromStopName = "Central Station",
                    toStopId = "200070",
                    toStopName = "Town Hall Station",
                ),
            ),
        )
    }
}
