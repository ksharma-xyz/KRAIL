package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.savedtrips.CarParkBlock
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Park & Ride pin card on real nodes (`docs/MAP_HOME_UX.md` rules 22, 40 and 45, decisions G1 and
 * G3): every car park of the station stacked at one left edge at every width and text size, a total
 * only for several car parks, Directions only on a car park with a position of its own and always
 * 48 dp, the same height before and after the readings land, and the action Go here will share a
 * slot with.
 *
 * Each test runs in a window of its own size at density 1, so a dp is a pixel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [PIN_TEST_SDK], manifest = Config.NONE)
class ParkRidePinCardLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val directions = mutableListOf<CarParkBlock>()
    private var toggled = 0
    private var closed = 0

    @Test
    @Config(qualifiers = PHONE)
    fun `every car park is a block of its own under the last at one left edge`() = assertStacked(fontScale = 1f)

    @Test
    @Config(qualifiers = "w320dp-h900dp")
    fun `every car park stacks at 320 dp and 200 percent text too`() = assertStacked(fontScale = 2f)

    private fun assertStacked(fontScale: Float) {
        showCard(TALLAWONG, TALLAWONG_READINGS, fontScale = fontScale)

        val blocks = listOf("26", "27", "28").map { bounds(TripPlannerTestTags.parkRideCarPark(it)) }
        blocks.zipWithNext().forEach { (above, below) ->
            assertTrue(
                below.top >= above.bottom - TOLERANCE,
                "A car park starts at ${below.top}, over the one above ending at ${above.bottom}",
            )
        }
        blocks.forEach { assertNear("left edge", blocks.first().left, it.left) }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a station of several car parks shows its total and a station of one does not`() {
        showCard(TALLAWONG, TALLAWONG_READINGS)
        composeRule.onNodeWithTag(StopCard.PARK_RIDE_TOTAL).assertExists()
        composeRule.onNodeWithText("155").assertExists()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a station of one car park has no total of its own`() {
        showCard(
            BELLA_VISTA,
            listOf(reading("31", "Bella Vista Car Park", "2153478", free = 120, lat = -33.7, lon = 150.9)),
        )
        composeRule.onAllNodesWithTag(StopCard.PARK_RIDE_TOTAL).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `directions are 48 dp and only on a car park with a position of its own`() {
        showCard(TALLAWONG, TALLAWONG_READINGS)

        listOf("26", "27").forEach { facility ->
            val button = bounds(TripPlannerTestTags.parkRideDirections(facility))
            assertNear("directions width", 48.dp, button.width)
            assertNear("directions height", 48.dp, button.height)
        }
        composeRule.onAllNodesWithTag(TripPlannerTestTags.parkRideDirections("28")).assertCountEquals(0)

        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideDirections("27")).performClick()
        assertEquals(listOf("27"), directions.map { it.facilityId })
        composeRule.onNodeWithContentDescription("Directions to Tallawong P2").assertExists()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `no car park has directions until its reading says where it is`() {
        showCard(TALLAWONG, emptyList())

        composeRule.onAllNodesWithTag(TripPlannerTestTags.parkRideDirections("26")).assertCountEquals(0)
        composeRule.onAllNodesWithTag(TripPlannerTestTags.parkRideDirections("27")).assertCountEquals(0)
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideCarPark("26")).assertExists()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the card is the same height before and after the readings land`() = assertSameHeightAcrossReadings(1f)

    @Test
    @Config(qualifiers = "w320dp-h900dp")
    fun `the card is the same height before and after the readings land at 320 dp and 200 percent`() =
        assertSameHeightAcrossReadings(2f)

    private fun assertSameHeightAcrossReadings(fontScale: Float) {
        var readings by mutableStateOf<List<ParkRideFacilityDetail>>(emptyList())
        composeRule.setContent {
            WithFontScale(fontScale) { PreviewTheme { CardOf(TALLAWONG, readings) } }
        }
        composeRule.waitForIdle()
        val waiting = bounds(StopCard.CARD).height

        readings = TALLAWONG_READINGS.map { it.copy(latitude = -33.7, longitude = 150.9) }
        composeRule.waitForIdle()

        assertNear(
            "the card with its readings against the card waiting for them",
            waiting,
            bounds(StopCard.CARD).height,
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the action reads Add to home off home and Remove from home on it, and takes the tap`() {
        showCard(TALLAWONG, TALLAWONG_READINGS, isOnHome = false)
        composeRule.onNodeWithText("Add to home").performClick()
        assertEquals(1, toggled)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a station on home offers to remove it`() {
        showCard(TALLAWONG, TALLAWONG_READINGS, isOnHome = true)
        composeRule.onNodeWithText("Remove from home").assertExists()
        composeRule.onAllNodesWithTag(StopCard.ACTIONS).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the close button is 48 dp and closes the card`() {
        showCard(TALLAWONG, TALLAWONG_READINGS)

        assertNear("close height", 48.dp, bounds(StopCard.CLOSE).height)
        composeRule.onNodeWithTag(StopCard.CLOSE).performClick()
        assertEquals(1, closed)
    }

    private fun showCard(
        station: ParkRideStationPickerItem,
        readings: List<ParkRideFacilityDetail>,
        isOnHome: Boolean = true,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent { WithFontScale(fontScale) { PreviewTheme { CardOf(station, readings, isOnHome) } } }
        composeRule.waitForIdle()
    }

    @Composable
    private fun CardOf(
        station: ParkRideStationPickerItem,
        readings: List<ParkRideFacilityDetail>,
        isOnHome: Boolean = true,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                ParkRidePinCard(
                    tile = station.toPinCardTile(readings),
                    isOnHome = isOnHome,
                    onToggleHome = { toggled++ },
                    onDirectionsClick = { directions += it },
                    onClose = { closed++ },
                )
            }
        }
    }

    @Composable
    private fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
            content = content,
        )
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getVisibleBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE.value,
            "$what should be $expected but is $actual.",
        )
    }

    private companion object {
        const val PHONE = "w411dp-h891dp"
        val TOLERANCE = 1.5.dp

        val TALLAWONG = station(
            "2155384",
            "Tallawong",
            ParkRideMapping("2155384", "26", "Tallawong P1"),
            ParkRideMapping("2155384", "27", "Tallawong P2"),
            ParkRideMapping("2155384", "28", "Tallawong P3"),
        )

        val TALLAWONG_READINGS = listOf(
            reading("26", "Tallawong P1", "2155384", free = 50, lat = -33.69, lon = 150.90, readAt = 300L),
            reading("27", "Tallawong P2", "2155384", free = 7, lat = -33.70, lon = 150.91, readAt = 100L),
            reading("28", "Tallawong P3", "2155384", free = 98, lat = 0.0, lon = 0.0, readAt = 200L),
        )

        val BELLA_VISTA = station("2153478", "Bella Vista", ParkRideMapping("2153478", "31", "Bella Vista Car Park"))

        fun station(stop: String, name: String, vararg mappings: ParkRideMapping) = ParkRideStationPickerItem(
            stationId = stop,
            stationName = name,
            stopName = "$name Station",
            carParkNames = mappings.map { it.facilityName },
            mappings = mappings.toList(),
        )

        fun reading(
            id: String,
            name: String,
            stop: String,
            free: Int,
            lat: Double? = null,
            lon: Double? = null,
            time: String = "8:05 AM",
            readAt: Long = 100L,
        ) = ParkRideFacilityDetail(
            spotsAvailable = free,
            totalSpots = 250,
            facilityName = name,
            percentageFull = 80,
            stopId = stop,
            timeText = time,
            facilityId = id,
            latitude = lat,
            longitude = lon,
            readAtEpochSeconds = readAt,
        )
    }
}

private const val PIN_TEST_SDK = 34
