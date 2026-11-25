package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.toImmutableSet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The home Park & Ride card's Directions: one button for each car park of the open card whose position is known. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class ParkRideCardDirectionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val directed = mutableListOf<String>()
    private var cardTaps = 0

    private fun facility(id: String, positioned: Boolean = true) = ParkRideUiState.ParkRideFacilityDetail(
        facilityName = "Tallawong P$id",
        spotsAvailable = 120,
        percentageFull = 40,
        totalSpots = 200,
        stopId = "2155384",
        timeText = "8:00 AM",
        facilityId = id,
        latitude = if (positioned) -33.69 else null,
        longitude = if (positioned) 150.9 else null,
    )

    private fun card(vararg facilities: ParkRideUiState.ParkRideFacilityDetail) = ParkRideUiState(
        stopName = "Tallawong Station",
        stopId = "2155384",
        facilities = facilities.toList().toImmutableSet(),
        isLoading = false,
        error = null,
    )

    private fun show(
        state: ParkRideUiState,
        expanded: Boolean = true,
        offersDirections: Boolean = true,
        fontScale: Float = 1f,
    ) {
        val onDirections: (ParkRideUiState.ParkRideFacilityDetail) -> Unit = { directed += it.facilityId }
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                PreviewTheme {
                    ParkRideCard(
                        parkRideUiState = state,
                        isExpanded = expanded,
                        onClick = { cardTaps++ },
                        onDirectionsClick = onDirections.takeIf { offersDirections },
                    )
                }
            }
        }
    }

    private fun button(id: String) = composeRule.onNodeWithTag(TripPlannerTestTags.parkRideDirections(id), true)

    @Test
    fun `each car park of the open card has its own Directions`() {
        show(card(facility("1"), facility("2")))

        button("1").assertExists()
        button("2").assertExists()
    }

    @Test
    fun `a tap asks for that car park and leaves the card open`() {
        show(card(facility("1"), facility("2")))

        button("2").performClick()

        assertEquals(listOf("2"), directed)
        assertEquals(0, cardTaps)
    }

    @Test
    fun `a car park with no position has no Directions`() {
        show(card(facility("1", positioned = false), facility("2")))

        composeRule.onNodeWithText("Tallawong P1").assertExists()
        button("1").assertDoesNotExist()
        button("2").assertExists()
    }

    @Test
    fun `a closed card has no Directions`() {
        show(card(facility("1")), expanded = false)

        button("1").assertDoesNotExist()
    }

    @Test
    fun `a card that only shows has no Directions`() {
        show(card(facility("1")), offersDirections = false)

        button("1").assertDoesNotExist()
    }

    private fun text(value: String) =
        composeRule.onNodeWithText(value, substring = true, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertBesideTheText() {
        val bounds = button("1").getUnclippedBoundsInRoot()
        listOf("Tallawong P1", "spots available", "full", "Last updated").forEach { item ->
            assertTrue(bounds.left >= text(item).right, "the button starts after $item")
        }
        assertTrue(bounds.top >= text("Tallawong P1").top, "the button is not above the car park's name")
        assertTrue(bounds.bottom <= text("Last updated").bottom, "the button is not below the car park's time")
    }

    @Test
    fun `on a phone the button stands at the end edge beside the car park's text`() {
        show(card(facility("1")))

        assertBesideTheText()
        val width = composeRule.onRoot().getUnclippedBoundsInRoot().width
        val gapToEdge = width - button("1").getUnclippedBoundsInRoot().right
        assertTrue(gapToEdge <= 24.dp, "gap to the edge $gapToEdge")
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xhdpi")
    fun `on a narrow phone the button is still beside the text`() {
        show(card(facility("1")))

        assertBesideTheText()
    }

    @Test
    @Config(qualifiers = "w320dp-h900dp-xhdpi")
    fun `at 320 dp and twice the font size the button keeps its size inside the screen`() {
        show(card(facility("1")), fontScale = 2f)

        val bounds = button("1").getUnclippedBoundsInRoot()
        assertTrue(bounds.width >= 48.dp, "width ${bounds.width}")
        assertTrue(bounds.height >= 48.dp, "height ${bounds.height}")
        assertTrue(bounds.right <= 320.dp, "right edge ${bounds.right}")
        assertTrue(
            bounds.top >= text("Last updated").bottom,
            "there is no room beside, so the button is under the text",
        )
    }
}
