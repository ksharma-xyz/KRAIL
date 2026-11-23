package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Search Stop's list state, which a tap on a pill raises, read from the real screen: the cloud gradient over the map,
 * the same box at the top, and under it the label chips row, Select on map, Recent with Clear all and the stop rows as
 * the Search Stop list always drew them. Back is two steps, the list to the map and the map to leaving.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = W411, manifest = Config.NONE)
class SearchStopListStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val quietKeyboard = object : SoftwareKeyboardController {
        override fun show() = Unit
        override fun hide() = Unit
    }
    private val gaps = searchStopGaps()
    private var windowHeight = 0.dp
    private var wentBack = 0

    private fun withMap(
        state: SearchStopState = SearchStopFixtures.recentWithHomeSet(),
        // A fix in hand means the location is allowed, which is what puts the Options button on the map state.
        display: MapDisplay = MapDisplay(userLocation = LatLng(-33.8688, 151.2093)),
    ) = state.copy(isMapsAvailable = true, mapUiState = MapUiState.Ready(mapDisplay = display))

    private fun setScreen(state: SearchStopState, fontScale: Float? = null) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides if (fontScale != null) Density(density.density, fontScale) else density,
                LocalInspectionMode provides true,
                LocalSoftwareKeyboardController provides quietKeyboard,
            ) {
                PreviewTheme {
                    windowHeight = LocalWindowInfo.current.containerDpSize.height
                    SearchStopScreen(searchStopState = state, onEvent = {}, goBack = { wentBack++ })
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    /** The words themselves, not the row that holds them, so a row's own padding is read as the padding it is. */
    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(abs((expected - actual).value) <= 1f, "$what: expected $expected, got $actual")
    }

    private fun tapToPill() = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()

    // region a tap on a pill raises the list state

    @Test
    fun `a tap on the To pill shows the list with the label chips, Select on map and the recents`() {
        setScreen(withMap())
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertDoesNotExist()

        tapToPill()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LABEL_ROW).assertIsDisplayed()
        composeRule.onNodeWithText("Manage").assertIsDisplayed()
        composeRule.onNodeWithText("Select on map").assertIsDisplayed()
        composeRule.onNodeWithText("Recent").assertIsDisplayed()
        composeRule.onNodeWithText("Clear all").assertIsDisplayed()
        composeRule.onNodeWithText("Central Station").assertIsDisplayed()
        composeRule.onNodeWithText("Town Hall").assertIsDisplayed()
    }

    @Test
    fun `a tap on the From pill shows the same list for the start`() {
        setScreen(withMap())

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertIsDisplayed()
        composeRule.onNodeWithText("Select on map").assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).assertIsDisplayed()
    }

    @Test
    fun `the list state covers the map with the cloud gradient and the map's own controls are not reachable`() {
        setScreen(withMap())
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LIST_BACKGROUND).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON).assertExists()

        tapToPill()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LIST_BACKGROUND).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertDoesNotExist()
        assertEquals(0, wentBack)
    }

    @Test
    fun `with no map the list is the screen and has no Select on map`() {
        setScreen(SearchStopFixtures.recentWithHomeSet())

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LIST_BACKGROUND).assertExists()
        composeRule.onNodeWithText("Select on map").assertDoesNotExist()
    }

    @Test
    fun `near you shows in the list state for a rider with a location and no recents`() {
        val near = NearbyStopFeature(
            stopId = "stop_wynyard",
            stopName = "Wynyard Station",
            position = LatLng(-33.8662, 151.2065),
            transportModes = persistentListOf(TransportMode.Train),
        )
        val state = withMap(
            state = SearchStopFixtures.freshInstall(),
            display = MapDisplay(
                userLocation = LatLng(-33.8660, 151.2060),
                nearbyStops = persistentListOf(near),
                selectedTransportModes = persistentSetOf(TransportMode.Train.productClass),
            ),
        )
        setScreen(state)
        composeRule.onNodeWithText("Near you").assertDoesNotExist()

        tapToPill()
        composeRule.waitForIdle()

        // The nearest stop also fills the From pill by itself, but the pill being edited starts empty with its fixed
        // prompt and never reads the stop's name, so the name is on the screen once, in the row.
        composeRule.onNodeWithText("Near you").assertIsDisplayed()
        composeRule.onAllNodesWithText("Wynyard Station").assertCountEquals(1)
        composeRule.onNodeWithText("Choose starting point").assertExists()
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
    }

    @Test
    fun `Select on map returns to the map state with the map's controls back`() {
        setScreen(withMap())
        tapToPill()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Select on map").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON).assertExists()
        assertEquals(0, wentBack, "Select on map is not leaving.")
    }

    @Test
    fun `back is two steps, the list state to the map state and then out`() {
        setScreen(withMap())
        tapToPill()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertIsDisplayed()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_BACK).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertIsDisplayed()
        assertEquals(0, wentBack, "The first back stays on the screen.")

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_BACK).performClick()
        composeRule.waitForIdle()
        assertEquals(1, wentBack, "The second back leaves.")
    }

    // endregion

    // region the rows as the list always drew them

    @Test
    fun `a plain recent has a chevron at its end and a stop with a label shows its pill and none`() {
        setScreen(withMap())
        tapToPill()
        composeRule.waitForIdle()

        // Town Hall is plain, Central Station is saved as Home and shows its small label pill instead.
        composeRule.onAllNodesWithContentDescription("Assign to a label").assertCountEquals(1)
    }

    @Test
    fun `the list reads label chips, then Select on map, then Recent, then the rows, all under the box`() {
        setScreen(withMap())
        tapToPill()
        composeRule.waitForIdle()

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val chips = boundsOf(TripPlannerTestTags.SEARCH_STOP_LABEL_ROW)
        val select = textBounds("Select on map")
        val recent = textBounds("Recent")
        val central = textBounds("Central Station")
        val townHall = textBounds("Town Hall")
        assertTrue(box.bottom <= chips.top, "The label chips $chips are under the box $box")
        assertTrue(chips.bottom <= select.top, "Select on map $select is under the chips $chips")
        assertTrue(select.bottom <= recent.top, "Recent $recent is under Select on map $select")
        assertTrue(recent.bottom <= central.top, "The first row $central is under Recent $recent")
        assertTrue(central.bottom <= townHall.top, "The rows keep their order")
    }

    @Test
    fun `the list is as wide as the screen with the stop rows' own 16 dp padding`() {
        setScreen(withMap())
        tapToPill()
        composeRule.waitForIdle()

        val list = boundsOf(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST)
        assertNear("the list starts at the screen's edge", 0.dp, list.left)
        assertNear("the list ends at the screen's edge", 411.dp, list.right)
        // 16 dp of page padding, the 20 dp map icon and 8 dp between the icon and the words.
        assertNear("Select on map's words", 16.dp + 20.dp + 8.dp, textBounds("Select on map").left)
        assertNear("a stop name is 16 dp in", 16.dp, textBounds("Town Hall").left)
        assertTrue(gaps.edge == 16.dp, "The box is the same 16 dp in from the edge.")
        assertNear("the box is 16 dp in", 16.dp, boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).left)
    }

    @Test
    fun `the box is the same box in the list state, at the same place and size`() {
        setScreen(withMap())
        val before = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)

        tapToPill()
        composeRule.waitForIdle()

        val after = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertEquals(before, after, "Tapping a pill does not move or resize the box.")
        assertNear("To is 48 tall", 48.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height)
        assertNear("From is 48 tall", 48.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).height)
        assertNear("the pills are one gap apart", gaps.betweenRows, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).top - boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).bottom)
    }

    // endregion

    // region the space under the box

    @Test
    fun `the first row is one 16 dp step under the box, clear of the glow, with the label chips first`() {
        setScreen(withMap())
        tapToPill()
        composeRule.waitForIdle()

        val gap = boundsOf(TripPlannerTestTags.SEARCH_STOP_LABEL_ROW).top - boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).bottom
        assertNear("the chips row is one 16 dp step under the box", 16.dp, gap)
        assertTrue(gap >= InfoTileDefaults.shadowRadius + InfoTileDefaults.shadowSpread, "$gap clears the glow's 14 dp")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun `Select on map is one 16 dp step under the box when it is the first row, at 200 percent text too`() {
        setScreen(withMap(state = SearchStopFixtures.freshInstall()), fontScale = 2f)
        tapToPill()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LABEL_ROW).assertDoesNotExist()
        val gap = boundsOf(TripPlannerTestTags.SEARCH_STOP_SELECT_ON_MAP).top - boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).bottom
        assertNear("Select on map is one 16 dp step under the box", 16.dp, gap)
        assertTrue(gap >= InfoTileDefaults.shadowRadius + InfoTileDefaults.shadowSpread, "$gap clears the glow's 14 dp")
    }

    // endregion

    // region 320 dp at 200 percent

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun `at 320 dp and 200 percent text the list stays under the box, inside the window, with the pill still one line`() {
        setScreen(withMap(), fontScale = 2f)
        tapToPill()
        composeRule.waitForIdle()

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val chips = boundsOf(TripPlannerTestTags.SEARCH_STOP_LABEL_ROW)
        val select = textBounds("Select on map")
        assertEquals(136.dp, box.height, "The box is the same 136 dp at 200 percent.")
        assertTrue(chips.top >= box.bottom, "The chips $chips are under the box $box")
        assertTrue(select.top >= chips.bottom, "Select on map $select is under the chips $chips")
        assertTrue(select.left >= 0.dp && select.right <= 320.dp, "Select on map $select stays inside the window")
        assertEquals(48.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height, "The pill stays one 48 dp line.")
        assertTrue(select.width > 0.dp && windowHeight > 0.dp)
    }

    // endregion
}

private const val W411 = "w411dp-h891dp"
