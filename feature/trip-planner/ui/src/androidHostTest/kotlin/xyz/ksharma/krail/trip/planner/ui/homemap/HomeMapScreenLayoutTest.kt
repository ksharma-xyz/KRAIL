package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The map home on the real screen, `docs/MAP_HOME_UX.md` rules 1, 6, 12, 20 and 24, the Home structure table and
 * decision G6: the map behind everything, the top row with the logo at the left and Discover beside Settings at
 * the right, the notices centred under it, the card stack below it, and the chips and the one search pill at the
 * bottom with the microphone inside the pill.
 *
 * The map is a flat stand-in, because a host test has no GPU. Each test runs in a window of its own width (density
 * 1, so a dp is a pixel) and reads where the real nodes landed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [HOME_MAP_TEST_SDK], manifest = Config.NONE)
class HomeMapScreenLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var toClicks = 0
    private val aiEvents = mutableListOf<AiSearchInputEvent>()

    // region the map and the top row

    @Test
    @Config(qualifiers = PHONE)
    fun `the map fills the window behind everything`() {
        setScreen()

        val map = bounds(MAP_TAG)
        assertNear("map width", PHONE_WIDTH, map.width)
        assertNear("map height", PHONE_HEIGHT, map.height)
        assertNear("map left", 0.dp, map.left)
        assertNear("map top", 0.dp, map.top)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the logo is at the left and Settings is at the right on one centre line`() {
        setScreen()

        val logo = textBounds("KRAIL")
        val settings = bounds(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS)
        assertNear("Settings is 48 tall", 48.dp, settings.height)
        assertTrue(logo.right < settings.left, "The logo $logo is left of Settings $settings.")
        assertNear(
            "logo and Settings share a centre line",
            settings.top + settings.height / 2,
            logo.top + logo.height / 2,
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `Discover sits beside Settings, left of it, when it is available`() {
        setScreen(state = state(discover = true))

        val discover = bounds(TripPlannerTestTags.SAVED_TRIPS_ACTION_DISCOVER)
        val settings = bounds(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS)
        assertTrue(discover.right <= settings.left, "Discover $discover is left of Settings $settings.")
        assertNear("Discover and Settings share a top", settings.top, discover.top)
        assertNear("Settings keeps its 16 dp inset", PHONE_WIDTH - 16.dp, settings.right)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with Discover unavailable only Settings is at the right`() {
        setScreen(state = state(discover = false))

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DISCOVER).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the card stack starts below the top row and never rises into it`() {
        setScreen()

        val topRow = bounds(TripPlannerTestTags.HOME_MAP_TOP_ROW)
        val card = bounds(TripPlannerTestTags.savedTripCard(TRIP.tripId))
        assertTrue(card.top >= topRow.bottom, "The card $card starts below the top row $topRow.")
    }

    // endregion

    // region the notices, rule 24

    @Test
    @Config(qualifiers = PHONE)
    fun `the location pill is centred under the buttons`() {
        setScreen(notices = persistentListOf(HomeNotice.LocationPermission))

        val pill = bounds(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val settings = bounds(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS)
        assertNear("the pill is centred on the screen", PHONE_WIDTH / 2, pill.left + pill.width / 2)
        assertTrue(pill.top >= settings.bottom - 1.dp, "The pill $pill is under the buttons $settings.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with both notices the two share one row, centred as a pair, while there is room`() {
        setScreen(notices = persistentListOf(HomeNotice.LocationPermission, HomeNotice.MapOffline))

        val pill = bounds(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val chip = bounds(TripPlannerTestTags.HOME_MAP_OFFLINE_CHIP)
        assertNear("one row, so one top", pill.top, chip.top)
        assertTrue(pill.right <= chip.left, "The pill $pill comes first, then the chip $chip.")
        assertNear("the pair is centred", PHONE_WIDTH / 2, (pill.left + chip.right) / 2)
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent text both notices are whole items, one under the other, never squeezed`() {
        setScreen(
            fontScale = 2f,
            notices = persistentListOf(HomeNotice.LocationPermission, HomeNotice.MapOffline),
        )

        val pill = bounds(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val chip = bounds(TripPlannerTestTags.HOME_MAP_OFFLINE_CHIP)
        assertTrue(pill.bottom <= chip.top, "The chip $chip wraps under the pill $pill instead of beside it.")
        listOf(pill, chip).forEach { item ->
            assertTrue(item.left >= 0.dp && item.right <= NARROW_WIDTH, "$item stays inside the 320 dp window.")
        }
        val pillText = textBounds("Location permission required")
        assertTrue(
            pillText.left >= pill.left - 1.dp && pillText.right <= pill.right + 1.dp,
            "The pill text $pillText is whole inside the pill $pill.",
        )
        assertNear("the chip is centred", NARROW_WIDTH / 2, chip.left + chip.width / 2)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the Map offline chip is a status and does nothing when it is tapped`() {
        var located = 0
        setScreen(
            notices = persistentListOf(HomeNotice.MapOffline),
            onLocationClick = { located++ },
        )

        composeRule.onNodeWithTag(TripPlannerTestTags.HOME_MAP_OFFLINE_CHIP, useUnmergedTree = true).performClick()

        assertEquals(0, located)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `tapping the location pill asks the screen`() {
        var located = 0
        setScreen(
            notices = persistentListOf(HomeNotice.LocationPermission),
            onLocationClick = { located++ },
        )

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, useUnmergedTree = true).performClick()

        assertEquals(1, located)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `no notice draws no row at all`() {
        setScreen(notices = persistentListOf())

        composeRule.onNodeWithTag(TripPlannerTestTags.HOME_MAP_NOTICES).assertDoesNotExist()
    }

    // endregion

    // region the pill and the chips at the bottom

    @Test
    @Config(qualifiers = PHONE)
    fun `the pill is 48 dp tall on the cards' edges, 16 dp above the bottom`() {
        setScreen()

        val pill = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val card = bounds(TripPlannerTestTags.savedTripCard(TRIP.tripId))
        assertNear("pill height", 48.dp, pill.height)
        assertNear("pill left edge is the card's", card.left, pill.left)
        assertNear("pill right edge is the card's", card.right, pill.right)
        assertNear("16 dp under the pill", PHONE_HEIGHT - 16.dp, pill.bottom)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the chips sit 8 dp above the pill with the map showing around them`() {
        setScreen()

        val pill = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val chip = bounds(TripPlannerTestTags.homeLabelChip("Home"))
        assertNear("gap between the chips and the pill", 8.dp, pill.top - chip.bottom)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the pill reads Where are you going and a tap opens Search Stop`() {
        setScreen()

        composeRule.onNodeWithText("Where are you going?", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true).performClick()

        assertEquals(1, toClicks)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `without Ask KRAIL the pill has no microphone`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = false))

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a device that cannot run it gets no microphone either`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = true, isDeviceCapable = false))

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `where Ask KRAIL is allowed the microphone is 48 dp at the pill's end and opens it`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = true))

        val pill = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val mic = bounds(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL)
        assertNear("microphone width", 48.dp, mic.width)
        assertNear("microphone height", 48.dp, mic.height)
        assertNear("microphone sits at the pill's end", pill.right, mic.right)
        assertTrue(textBounds("Where are you going?").right <= mic.left, "The text stops short of the microphone.")

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL, useUnmergedTree = true).performClick()

        assertEquals(listOf<AiSearchInputEvent>(AiSearchInputEvent.OpenInput), aiEvents)
        assertEquals(0, toClicks, "The microphone is its own control, never also a tap on the pill.")
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent text the pill is one line 48 dp tall and the microphone is whole`() {
        setScreen(fontScale = 2f, ai = AiSearchInputUiState(isFeatureEnabled = true))

        val pill = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val mic = bounds(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL)
        assertNear("pill height at 200 percent", 48.dp, pill.height)
        assertTrue(mic.left >= pill.left && mic.right <= pill.right + 1.dp, "The microphone $mic is inside $pill.")
        assertNear("microphone width", 48.dp, mic.width)
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent text the chips scroll in one line with the first whole`() {
        setScreen(fontScale = 2f)

        val row = bounds(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW)
        val first = bounds(TripPlannerTestTags.homeLabelChip("Home"))
        assertTrue(first.left >= row.left, "The first chip $first is whole inside the row $row.")
        assertTrue(first.right <= row.right, "The first chip $first is whole inside the row $row.")
    }

    // endregion

    // region what takes the bottom's place

    @Test
    @Config(qualifiers = PHONE)
    fun `a rider holding a stop still sees the one pill and never a second bar`() {
        setScreen(
            state = state(
                toStop = StopItem(stopId = "200080", stopName = "Wynyard Station"),
                fromStop = StopItem(stopId = "200060", stopName = "Central Station"),
            ),
        )

        composeRule.onNodeWithText("Where are you going?", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("Wynyard Station", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `an open card takes the place of the chips and the pill and the stack stops above it`() {
        setScreen(card = { placement -> TestCard(placement, height = 220.dp) })

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW).assertDoesNotExist()
        val card = bounds(CARD_TAG)
        val list = bounds(TripPlannerTestTags.SAVED_TRIPS_LIST)
        assertTrue(list.bottom <= card.top + 1.dp, "The stack $list stops above the card $card.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `while loading the bottom is empty and the top row still shows`() {
        setScreen(state = state(loading = true))

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.HOME_MAP_TOP_ROW).assertIsDisplayed()
    }

    // endregion

    // region a wide window

    @Test
    @Config(qualifiers = "w1024dp-h768dp")
    fun `a wide window keeps the 480 dp list pane with the one map beside it`() {
        setScreen()

        val map = bounds(MAP_TAG)
        val pill = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val settings = bounds(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS)
        assertNear("the map starts where the list pane ends", DUAL_PANE_LIST_WIDTH, map.left)
        assertNear("the map takes the rest of the width", 1024.dp - DUAL_PANE_LIST_WIDTH, map.width)
        assertTrue(pill.right <= DUAL_PANE_LIST_WIDTH, "The pill $pill stays in the list pane.")
        assertTrue(settings.right <= DUAL_PANE_LIST_WIDTH, "Settings $settings stays in the list pane.")
        composeRule.onNodeWithTag(MAP_TAG).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp")
    fun `in a wide window a card floats over the map and the chips and pill stay`() {
        setScreen(card = { placement -> TestCard(placement, height = 220.dp) })

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW).assertExists()
        val card = bounds(CARD_TAG)
        assertTrue(card.left >= DUAL_PANE_LIST_WIDTH, "The card $card floats over the map pane, not the list.")
    }

    // endregion

    // region setup

    private fun state(
        loading: Boolean = false,
        discover: Boolean = false,
        toStop: StopItem? = null,
        fromStop: StopItem? = null,
    ) = SavedTripsState(
        isSavedTripsLoading = loading,
        isDiscoverAvailable = discover,
        savedTrips = persistentListOf(TRIP),
        stopLabels = LABELS,
        toStop = toStop,
        fromStop = fromStop,
    )

    private fun setScreen(
        fontScale: Float = 1f,
        state: SavedTripsState = state(),
        notices: ImmutableList<HomeNotice> = persistentListOf(),
        ai: AiSearchInputUiState = AiSearchInputUiState(),
        onLocationClick: () -> Unit = {},
        card: (@Composable BoxScope.(HomeCardPlacement) -> Unit)? = null,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
            ) {
                PreviewTheme {
                    HomeMapScreen(
                        savedTripsState = state,
                        host = HomeMapHost(
                            map = { Box(Modifier.fillMaxSize().background(Color.LightGray).testTag(MAP_TAG)) },
                            notices = notices,
                            locationPill = LocationPill.Ask,
                            onLocationClick = onLocationClick,
                            card = card,
                            onCardClose = {},
                        ),
                        aiState = ai,
                        onAiEvent = { aiEvents += it },
                        actions = HomeMapActions(toButtonClick = { toClicks++ }),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun BoxScope.TestCard(placement: HomeCardPlacement, height: Dp) {
        LaunchedEffect(height) { placement.onHeight(height) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(height)
                .background(Color.White)
                .testTag(CARD_TAG),
        )
    }

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE_DP,
            "$what should be at $expected but is at $actual.",
        )
    }

    // endregion

    private companion object {
        const val PHONE = "w411dp-h891dp"
        const val NARROW = "w320dp-h1280dp"
        const val MAP_TAG = "test.map"
        const val CARD_TAG = "test.card"
        const val TOLERANCE_DP = 1.5f
        val PHONE_WIDTH = 411.dp
        val PHONE_HEIGHT = 891.dp
        val NARROW_WIDTH = 320.dp

        val TRIP = Trip(
            fromStopId = "200060",
            fromStopName = "Central Station",
            toStopId = "200070",
            toStopName = "Town Hall Station",
        )

        val LABELS = persistentListOf(
            StopLabel(emoji = "H", label = "Home", stopId = "200080", stopName = "Wynyard Station"),
            StopLabel(emoji = "W", label = "Work"),
        )
    }
}

private const val HOME_MAP_TEST_SDK = 34
