package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableScreen

/**
 * The Timetable on every device profile (`docs/testing/DEVICE_PROFILES.md`): the title bar with Back, Reverse and Save, the
 * origin and destination header, a journey card and the footer. Everything the rider presses stands inside the safe area, and the
 * list scrolls clear of the bottom bar.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class TimeTableDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private val state = TimeTableState(
        isLoading = false,
        trip = Trip(
            fromStopId = "200060",
            fromStopName = "Central Station",
            toStopId = "200070",
            toStopName = "Town Hall Station",
        ),
        journeyList = persistentListOf(
            TimeTableState.JourneyCardInfo(
                timeText = "in 5 mins",
                originTime = "11:30pm",
                originUtcDateTime = "2030-01-01T12:30:00Z",
                destinationTime = "11:40pm",
                destinationUtcDateTime = "2030-01-01T12:40:00Z",
                travelTime = "10 min",
                transportModeLines = persistentListOf(),
                legs = persistentListOf(),
                totalUniqueServiceAlerts = 0,
            ),
        ),
    )

    @Test
    fun `the Timetable keeps its title bar, header, cards and footer inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "TimeTableScreen") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                TimeTableScreen(
                    timeTableState = state,
                    expandedJourneyId = null,
                    dateTimeSelectionItem = null,
                    onEvent = {},
                    onAlertClick = {},
                    onBackClick = {},
                    onJourneyLegClick = { _, _, _ -> },
                )
            }

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithContentDescription("Back"))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.TIME_TABLE_REVERSE))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.TIME_TABLE_SAVE_TRIP))
            assertScrollEndClearsBottomBar(onNodeWithTag(TripPlannerTestTags.TIME_TABLE_SCREEN))
        }
}
