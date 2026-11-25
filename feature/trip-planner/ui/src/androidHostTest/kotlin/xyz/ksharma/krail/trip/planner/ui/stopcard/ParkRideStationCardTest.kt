package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The card a station's `P` opens: its car parks and their numbers, Directions and Close, and no departures. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class ParkRideStationCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val directed = mutableListOf<String>()
    private var closed = 0

    private val read = ParkRideFacilityDetail(
        spotsAvailable = 60,
        totalSpots = 123,
        facilityName = "Tallawong P1",
        percentageFull = 51,
        stopId = "2155384",
        timeText = "8:12 PM",
        facilityId = "1",
        latitude = -33.69,
        longitude = 150.9,
    )
    private val unread = ParkRideFacilityDetail(
        spotsAvailable = -1,
        totalSpots = -1,
        facilityName = "Tallawong P2",
        percentageFull = -1,
        stopId = "2155384",
        timeText = "",
        facilityId = "2",
    )

    private var loading by mutableStateOf(false)

    private fun show(isLoading: Boolean) {
        loading = isLoading
        composeRule.setContent {
            PreviewTheme {
                ParkRideStationCard(
                    stationName = "Tallawong Station",
                    carParks = persistentListOf(read, unread),
                    isLoading = loading,
                    onDirectionsClick = { directed += it.facilityId },
                    onClose = { closed++ },
                )
            }
        }
    }

    private fun count(text: String) =
        composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `it lists every car park with its numbers and has no departures`() {
        show(isLoading = false)

        assertEquals(1, count("Tallawong P1"))
        assertEquals(1, count("Tallawong P2"))
        assertEquals(1, count("spots available"))
        assertEquals(0, count("Departure Board"))
    }

    @Test
    fun `Directions go to the car park that has a position, and only it has them`() {
        show(isLoading = false)

        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideDirections("1"), true).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideDirections("2"), true).assertDoesNotExist()

        assertEquals(listOf("1"), directed)
    }

    private fun cardHeight() =
        composeRule.onNodeWithTag(TripPlannerTestTags.StopCard.CARD).fetchSemanticsNode().size.height

    // What a screen reader gets: the room held for a reading is drawn clear and cleared from it.
    private fun spoken(text: String) =
        composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().size

    @Test
    fun `while loading, a car park with no reading holds room for its numbers and says none`() {
        show(isLoading = true)

        assertEquals(1, spoken("spots available"))
        val heldHeight = cardHeight()

        loading = false
        composeRule.waitForIdle()

        assertEquals(1, count("Tallawong P2"))
        assertTrue(cardHeight() < heldHeight, "the room is given back when no reading came")
    }

    @Test
    fun `close asks to close`() {
        show(isLoading = false)

        composeRule.onNodeWithContentDescription("Close Tallawong Station parking", useUnmergedTree = true)
            .performClick()

        assertEquals(1, closed)
    }

    @Test
    fun `a tall window shows the parking logo in the header`() {
        show(isLoading = false)

        composeRule.onNodeWithContentDescription("Park & Ride", useUnmergedTree = true).assertExists()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-420dpi")
    fun `a short window leaves the logo out, to give its height to the car parks`() {
        show(isLoading = false)

        composeRule.onNodeWithContentDescription("Park & Ride", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(1, count("Tallawong P1"))
    }
}
