package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.toImmutableSet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Park & Ride row of `docs/MAP_HOME_UX.md` (rules 22, 29, 40 and 45 and the home stack) on real
 * nodes: tiles 148 dp wide that scale with the text and stop 60 dp short of the column, all one
 * height in every state, a 16 dp inset at both ends of the scroll with the next tile peeking, a tile
 * that opens to the row's width with every car park stacked, and Directions only where a car park
 * has a position.
 *
 * Each test runs in a window of its own width (density 1, so a dp is a pixel) and reads where the
 * real nodes landed. The row is drawn on its own, so its column is the whole window.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [ROW_TEST_SDK], manifest = Config.NONE)
class ParkRideRowLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val directionsTaps = mutableListOf<CarParkBlock>()

    // region the tiles

    @Test
    @Config(qualifiers = PHONE)
    fun `a tile is 148 dp wide, 8 dp apart, and the first starts 16 dp in`() {
        setRow(stations = FOUR_STATIONS)

        FOUR_STATIONS.forEachIndexed { index, station ->
            val tile = tile(station.stopId)
            assertNear("width of tile $index", 148.dp, tile.width)
            assertNear("left edge of tile $index", 16.dp + (148.dp + 8.dp) * index, tile.left)
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the next tile peeks past the right edge and nothing masks it`() {
        setRow(stations = FOUR_STATIONS)

        val third = tile(FOUR_STATIONS[2].stopId)
        assertTrue(third.left < 411.dp, "The third tile starts at ${third.left}, inside the 411 dp window")
        assertTrue(third.right > 411.dp, "The third tile ends at ${third.right}, so it is cut by the screen edge")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the row ends with the 16 dp inset after the last tile at the end of the scroll`() {
        setRow(stations = FOUR_STATIONS)

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW)
            .performTouchInput { swipeLeft(startX = right - 1f, endX = left + 1f) }
        composeRule.waitForIdle()

        assertNear("last tile right edge at the end of the scroll", 411.dp - 16.dp, tile(FOUR_STATIONS[3].stopId).right)
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `at 320 dp and 200 percent text a tile is the column less 60 dp, so the next tile peeks`() {
        setRow(stations = FOUR_STATIONS, fontScale = 2f)

        FOUR_STATIONS.forEach { assertNear("tile width", 260.dp, tile(it.stopId).width) }
        val second = tile(FOUR_STATIONS[1].stopId)
        assertTrue(second.left < 320.dp && second.right > 320.dp, "The second tile $second peeks past 320 dp")
    }

    @Test
    @Config(qualifiers = "w560dp-h900dp")
    fun `in the widest column a tile at 200 percent is 296 dp`() {
        setRow(stations = FOUR_STATIONS, fontScale = 2f)

        FOUR_STATIONS.forEach { assertNear("tile width", 296.dp, tile(it.stopId).width) }
    }

    // endregion

    // region one height in every state

    @Test
    @Config(qualifiers = PHONE)
    fun `the row is the same height whether its numbers are loaded, loading or failed`() =
        assertSameHeightInEveryState(fontScale = 1f)

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `the row is the same height at 320 dp and 200 percent in every state`() =
        assertSameHeightInEveryState(fontScale = 2f)

    /**
     * Each state alone in the row, so no taller neighbour can stretch it: a tile with no reading
     * and one with an hours old reading must be exactly as tall as one with a fresh number.
     */
    private fun assertSameHeightInEveryState(fontScale: Float) {
        val shown = mutableStateOf(listOf(LOADED))
        composeRule.setContent { WithFontScale(fontScale) { PreviewTheme { RowOf(shown.value) } } }
        composeRule.waitForIdle()
        val loaded = tile(LOADED.stopId).height

        shown.value = listOf(NO_READING)
        composeRule.waitForIdle()
        assertNear("a tile with no reading is as tall as a loaded one", loaded, tile(NO_READING.stopId).height)

        shown.value = listOf(STALE)
        composeRule.waitForIdle()
        assertNear("a tile with an old reading is as tall as a fresh one", loaded, tile(STALE.stopId).height)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a station with no reading shows its name alone and no number`() {
        setRow(stations = listOf(NO_READING))

        composeRule.onNodeWithText("Penrith").assertExists()
        composeRule.onNodeWithText("spots").assertDoesNotExist()
        composeRule.onNodeWithText("Last updated at 12:00 AM").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a stale reading still shows its number and its time and is never hidden`() {
        setRow(stations = listOf(STALE))

        composeRule.onNodeWithText("12").assertExists()
        composeRule.onNodeWithText("Last updated at 6:00 AM").assertExists()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a closed tile reads the total across the car parks and the oldest time`() {
        setRow(stations = listOf(TALLAWONG))

        // 50 + 7 + 98, and the oldest of the three readings.
        composeRule.onNodeWithText("155").assertExists()
        composeRule.onNodeWithText("Last updated at 7:50 AM").assertExists()
        composeRule.onNodeWithText("Tallawong").assertExists()
    }

    // endregion

    // region open, stacked, Directions

    @Test
    @Config(qualifiers = PHONE)
    fun `tapping a tile opens it to the full row width, 16 dp in on both sides`() {
        setRow(stations = listOf(TALLAWONG, LOADED))

        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(TALLAWONG.stopId)).performClick()
        composeRule.waitForIdle()

        val open = bounds(TripPlannerTestTags.parkRideTileOpen(TALLAWONG.stopId))
        assertNear("open tile left edge", 16.dp, open.left)
        assertNear("open tile right edge", 411.dp - 16.dp, open.right)
        composeRule.onAllNodesWithTag(TripPlannerTestTags.parkRideTile(LOADED.stopId)).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `every car park of the station is a block of its own at one left edge`() =
        assertCarParksStack(window = 411.dp, fontScale = 1f)

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `at 320 dp and 200 percent the car parks are still stacked and never squeezed`() =
        assertCarParksStack(window = 320.dp, fontScale = 2f)

    private fun assertCarParksStack(window: Dp, fontScale: Float) {
        setRow(stations = listOf(TALLAWONG), fontScale = fontScale)
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(TALLAWONG.stopId)).performClick()
        composeRule.waitForIdle()

        val blocks = TALLAWONG.facilities.map { bounds(TripPlannerTestTags.parkRideCarPark(it.facilityId)) }
            .sortedBy { it.top }
        assertEquals(3, blocks.size, "All three car parks are listed")
        blocks.zipWithNext().forEach { (above, below) ->
            assertTrue(below.top >= above.bottom, "A car park block $below must sit below $above, never beside it")
        }
        blocks.forEach { block ->
            assertNear("car park block left edge", blocks.first().left, block.left)
            assertTrue(block.right <= window - 16.dp + 1.dp, "A block $block must end inside the tile")
        }
        val open = bounds(TripPlannerTestTags.parkRideTileOpen(TALLAWONG.stopId))
        assertTrue(blocks.all { it.top >= open.top && it.bottom <= open.bottom }, "Every block is inside the open tile")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `tapping the name closes the tile and the closed tiles come back`() {
        setRow(stations = listOf(TALLAWONG, LOADED))
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(TALLAWONG.stopId)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTileName(TALLAWONG.stopId)).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(TripPlannerTestTags.parkRideTileOpen(TALLAWONG.stopId)).assertCountEquals(0)
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(TALLAWONG.stopId)).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(LOADED.stopId)).assertExists()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the name that closes a tile is at least 48 dp tall`() {
        setRow(stations = listOf(TALLAWONG))
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(TALLAWONG.stopId)).performClick()
        composeRule.waitForIdle()

        assertTrue(bounds(TripPlannerTestTags.parkRideTileName(TALLAWONG.stopId)).height >= 48.dp)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `Directions is a 48 dp button only on a car park with a position of its own`() {
        setRow(stations = listOf(TALLAWONG))
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(TALLAWONG.stopId)).performClick()
        composeRule.waitForIdle()

        val withPosition = bounds(TripPlannerTestTags.parkRideDirections("26"))
        assertNear("Directions width", 48.dp, withPosition.width)
        assertNear("Directions height", 48.dp, withPosition.height)
        composeRule.onAllNodesWithTag(TripPlannerTestTags.parkRideDirections("27")).assertCountEquals(0)
        composeRule.onAllNodesWithTag(TripPlannerTestTags.parkRideDirections("28")).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `Directions go to the car park that was tapped and tell a screen reader which one`() {
        setRow(stations = listOf(TALLAWONG))
        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(TALLAWONG.stopId)).performClick()
        composeRule.waitForIdle()

        // Found by what a screen reader hears, which is the point of the test.
        composeRule.onNodeWithContentDescription("Directions to Tallawong P1").performClick()

        assertEquals(1, directionsTaps.size)
        assertEquals("26", directionsTaps.single().facilityId)
        assertEquals(LatLng(-33.69, 150.90), directionsTaps.single().directionsTo)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a closed tile tells a screen reader what a tap does`() {
        setRow(stations = listOf(LOADED))

        val node = composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(LOADED.stopId)).fetchSemanticsNode()
        assertEquals("Show car parks at Bella Vista", node.config[SemanticsActions.OnClick].label)
        assertTrue(SemanticsProperties.Role in node.config, "A tile is announced as a button")
    }

    // endregion

    // region type

    @Test
    @Config(qualifiers = PHONE)
    fun `a station name and a car park name are in the name style of the old Park and Ride card`() {
        setRow(stations = listOf(TALLAWONG))
        assertNameStyle("Tallawong")

        composeRule.onNodeWithTag(TripPlannerTestTags.parkRideTile(TALLAWONG.stopId)).performClick()
        composeRule.waitForIdle()

        assertNameStyle("Tallawong")
        assertNameStyle("Tallawong P1")
        assertNameStyle("Tallawong P2")
    }

    /** `displaySmall` at the normal weight, 20 sp, which is what the home's Park & Ride card has always used. */
    private fun assertNameStyle(name: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        val node = composeRule.onNodeWithText(name, useUnmergedTree = true).fetchSemanticsNode()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val style = layouts.single().layoutInput.style
        assertEquals(20.sp, style.fontSize, "$name size")
        assertEquals(FontWeight.Normal, style.fontWeight, "$name weight")
    }

    // endregion

    private fun setRow(
        stations: List<ParkRideUiState>,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent { WithFontScale(fontScale) { PreviewTheme { RowOf(stations) } } }
        composeRule.waitForIdle()
    }

    /** The row with the open tile held the way the screen holds it: one at a time, by station id. */
    @Composable
    private fun RowOf(stations: List<ParkRideUiState>) {
        var open by remember { mutableStateOf<String?>(null) }
        val tiles = remember(stations) { stations.toTiles() }
        ParkRideRow(
            tiles = tiles,
            openStationId = open,
            onTileClick = { tile -> open = openTilesAfterTap(setOfNotNull(open), tile.stationId).firstOrNull() },
            onDirectionsClick = { directionsTaps += it },
        )
    }

    @Composable
    private fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
            content = content,
        )
    }

    private fun tile(stationId: String): DpRect = bounds(TripPlannerTestTags.parkRideTile(stationId))

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE_DP,
            "$what should be $expected but is $actual.",
        )
    }

    private companion object {
        const val PHONE = "w411dp-h891dp"
        const val TOLERANCE_DP = 1.5f

        val LOADED = station("2153478", "Bella Vista", car("31", "Bella Vista Car Park", "2153478", free = 120))
        val LOADED_TWO = station("2000001", "Hornsby", car("41", "Hornsby Car Park", "2000001", free = 64))
        val LOADED_THREE = station("2000002", "Leppington", car("51", "Leppington Car Park", "2000002", free = 9))
        val NO_READING = station(
            "2000003",
            "Penrith",
            car("61", "Penrith Car Park", "2000003", free = -1, time = "", readAt = 0L),
        )
        val STALE = station(
            "2000004",
            "Kellyville",
            car("71", "Kellyville Car Park", "2000004", free = 12, time = "6:00 AM", readAt = 1L),
        )

        val TALLAWONG = station(
            "2155384",
            "Tallawong",
            car(
                "26",
                "Tallawong P1",
                "2155384",
                free = 50,
                time = "8:05 AM",
                readAt = 300L,
                lat = -33.69,
                lon = 150.90,
            ),
            car("27", "Tallawong P2", "2155384", free = 7, time = "7:50 AM", readAt = 100L),
            car("28", "Tallawong P3", "2155384", free = 98, time = "8:00 AM", readAt = 200L, lat = 0.0, lon = 0.0),
        )

        val FOUR_STATIONS = listOf(LOADED, LOADED_TWO, LOADED_THREE, STALE)

        fun car(
            id: String,
            name: String,
            stop: String,
            free: Int = 40,
            time: String = "8:00 AM",
            readAt: Long = 100L,
            lat: Double? = null,
            lon: Double? = null,
        ) = ParkRideFacilityDetail(
            spotsAvailable = free,
            totalSpots = if (free < 0) -1 else 200,
            facilityName = name,
            percentageFull = if (free < 0) -1 else 80,
            stopId = stop,
            timeText = time,
            facilityId = id,
            latitude = lat,
            longitude = lon,
            readAtEpochSeconds = readAt,
        )

        fun station(id: String, name: String, vararg cars: ParkRideFacilityDetail) = ParkRideUiState(
            stopId = id,
            stopName = name,
            facilities = cars.toList().toImmutableSet(),
        )
    }
}

private const val ROW_TEST_SDK = 34
