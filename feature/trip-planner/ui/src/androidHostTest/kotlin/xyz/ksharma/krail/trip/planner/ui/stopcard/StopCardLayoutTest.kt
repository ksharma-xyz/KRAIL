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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The floating stop card of `docs/MAP_HOME_UX.md` (Stop card, rules 22 and 45) on real nodes: it
 * floats at the bottom of the pane in the one column and never over the form above it, closes from a
 * 48 dp button, holds its board closed until the heading is tapped, lists every line on the board
 * with the board's own filter, keeps its height in every phase and keeps its state across recreation.
 *
 * Each test runs in a window of its own size (density 1, so a dp is a pixel). A board that is
 * loading is not drawn here, because its dots animate forever and never let a host test go idle: the
 * loading phase's height is held by the same floor the failed and empty phases are measured against.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [CARD_TEST_SDK], manifest = Config.NONE)
class StopCardLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    private val events = mutableListOf<DeparturesUiEvent>()
    private var closed = 0
    private var picked = 0

    // region where the card floats

    @Test
    @Config(qualifiers = PHONE)
    fun `the card floats at the bottom of a phone, 16 dp in from the sides and the bottom`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD)

        val card = bounds(StopCard.CARD)
        assertNear("width", 379.dp, card.width)
        assertNear("left edge", 16.dp, card.left)
        assertNear("gap to the bottom", 16.dp, 891.dp - card.bottom)
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp")
    fun `in a wide pane the card is one column, 528 dp, centred`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD)

        val card = bounds(StopCard.CARD)
        assertNear("width", 528.dp, card.width)
        assertNear("left edge", 86.dp, card.left)
        assertNear("right gap", 86.dp, 700.dp - card.right)
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `at 320 dp and 200 percent text the card is 288 dp wide`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD, fontScale = 2f)

        assertNear("width", 288.dp, bounds(StopCard.CARD).width)
    }

    @Test
    @Config(qualifiers = "w411dp-h480dp")
    fun `a long board stays below the form above it and scrolls inside the card`() {
        showCard(MULTI_LINE_STOP, tallBoard(), topReserve = 120.dp, boardOpen = true)

        val card = bounds(StopCard.CARD)
        assertTrue(card.top >= 120.dp - TOLERANCE, "The card starts at ${card.top}, over the 120 dp form")
        assertTrue(
            card.height <= 480.dp - 120.dp - 24.dp + TOLERANCE,
            "The card is ${card.height} tall, taller than the room",
        )
        assertNear("the close button stays on the card", 48.dp, bounds(StopCard.CLOSE).height)
    }

    // endregion

    // region close and the heading

    @Test
    @Config(qualifiers = PHONE)
    fun `the close button is 48 dp, labelled with the stop and closes the card`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD)

        val close = bounds(StopCard.CLOSE)
        assertNear("close width", 48.dp, close.width)
        assertNear("close height", 48.dp, close.height)
        // Found by what a screen reader hears, which is the point of the test.
        composeRule.onNodeWithContentDescription("Close Central Station").assertExists()
        composeRule.onNodeWithTag(StopCard.CLOSE).performClick()
        assertEquals(1, closed)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a pin tap opens the board closed and asks for nothing until the heading is tapped`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD)

        composeRule.onAllNodesWithTag(StopCard.BOARD_BODY).assertCountEquals(0)
        assertTrue(events.none { it is DeparturesUiEvent.LoadDepartures }, "Opening the card must not fetch: $events")

        composeRule.onNodeWithTag(StopCard.BOARD_HEADING).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(StopCard.BOARD_BODY).assertExists()
        val load = events.filterIsInstance<DeparturesUiEvent.LoadDepartures>().single()
        assertEquals("200060", load.stopId)
        assertTrue(events.any { it is DeparturesUiEvent.DepartureBoardToggle && it.expand })
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `tapping the title opens and closes the board, not only the arrow`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD)

        composeRule.onNodeWithText("Departure Board").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(StopCard.BOARD_BODY).assertExists()

        composeRule.onNodeWithText("Departure Board").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(StopCard.BOARD_BODY).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the heading is one button at least 48 dp tall that says whether the board is open`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD)

        assertTrue(bounds(StopCard.BOARD_HEADING).height >= 48.dp - TOLERANCE, "The heading is too short to tap")
        assertEquals("Collapsed", headingConfig()[SemanticsProperties.StateDescription])
        assertEquals(Role.Button, headingConfig()[SemanticsProperties.Role])

        composeRule.onNodeWithTag(StopCard.BOARD_HEADING).performClick()
        composeRule.waitForIdle()
        assertEquals("Expanded", headingConfig()[SemanticsProperties.StateDescription])
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `at 320 dp and 200 percent the heading stays whole, 48 dp and inside the card`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD, boardOpen = true, fontScale = 2f)

        val heading = bounds(StopCard.BOARD_HEADING)
        val card = bounds(StopCard.CARD)
        assertTrue(heading.height >= 48.dp - TOLERANCE, "The heading is ${heading.height} tall")
        assertTrue(heading.right <= card.right + TOLERANCE, "The heading ends at ${heading.right}, past the card")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `closing the board stops the poll`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD, boardOpen = true)
        events.clear()

        composeRule.onNodeWithTag(StopCard.BOARD_HEADING).performClick()
        composeRule.waitForIdle()

        assertTrue(events.any { it == DeparturesUiEvent.StopPolling }, "Closing the board must stop polling: $events")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `an open board lists every line under the boards own filter and has no Lines chip`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD, boardOpen = true)

        composeRule.onNodeWithContentDescription("Hide line filter").assertExists()
        composeRule.onAllNodesWithText("Lines").assertCountEquals(0)
        composeRule.onAllNodesWithText("Anywhere").assertCountEquals(MANY_LINE_BOARD.departures.size)
    }

    // endregion

    // region one surface

    @Test
    @Config(qualifiers = PHONE)
    fun `the name, the title and the rows share one left edge, 16 dp in, with no card inside the card`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD, boardOpen = true)

        val card = bounds(StopCard.CARD)
        val name = composeRule.onNodeWithText("Central Station", useUnmergedTree = true).getVisibleBoundsInRoot()
        val title = composeRule.onNodeWithText("Departure Board", useUnmergedTree = true).getVisibleBoundsInRoot()
        val row = composeRule.onAllNodesWithText("Anywhere")[0].getVisibleBoundsInRoot()
        assertNear("stop name inset", 16.dp, name.left - card.left)
        assertNear("board title inset", 16.dp, title.left - card.left)
        // One 16 dp inset, not the 32 dp an inner card used to add.
        assertNear("row text inset", 16.dp, row.left - card.left)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the board's rows run the whole width of the card`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD, boardOpen = true)

        val card = bounds(StopCard.CARD)
        val body = bounds(StopCard.BOARD_BODY)
        assertNear("board left", card.left, body.left)
        assertNear("board right", card.right, body.right)
    }

    // endregion

    // region one height

    @Test
    @Config(qualifiers = PHONE)
    fun `a failed, an empty and a one row board hold the same height`() = assertSameHeightInEveryPhase(fontScale = 1f)

    @Test
    @Config(qualifiers = "w320dp-h900dp")
    fun `a failed, an empty and a one row board hold the same height at 320 dp and 200 percent`() =
        assertSameHeightInEveryPhase(fontScale = 2f)

    private fun assertSameHeightInEveryPhase(fontScale: Float) {
        val shown = mutableStateOf<DeparturesState>(ONE_ROW_BOARD)
        composeRule.setContent {
            WithFontScale(fontScale) { PreviewTheme { CardOf(SINGLE_LINE_STOP, shown.value, boardOpen = true) } }
        }
        composeRule.waitForIdle()
        val oneRow = bounds(StopCard.CARD).height

        shown.value = DeparturesState(isLoading = false, isError = true)
        composeRule.waitForIdle()
        assertNear("a failed board against a one row board", oneRow, bounds(StopCard.CARD).height)

        shown.value = DeparturesState(isLoading = false)
        composeRule.waitForIdle()
        assertNear("an empty board against a one row board", oneRow, bounds(StopCard.CARD).height)
    }

    // endregion

    // region state and keys

    @Test
    @Config(qualifiers = PHONE)
    fun `an open board is still open after the screen is recreated`() {
        restorationTester.setContent { PreviewTheme { CardOf(MULTI_LINE_STOP, MANY_LINE_BOARD, boardOpen = false) } }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(StopCard.BOARD_HEADING).performClick()
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(StopCard.BOARD_BODY).assertExists()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `another stop's card starts with its board closed`() {
        var stop by mutableStateOf(MULTI_LINE_STOP)
        composeRule.setContent { PreviewTheme { CardOf(stop, MANY_LINE_BOARD, boardOpen = false) } }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(StopCard.BOARD_HEADING).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(StopCard.BOARD_BODY).assertExists()

        stop = SINGLE_LINE_STOP
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(StopCard.BOARD_BODY).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the modes are one line for a screen reader and the action slot is there`() {
        showCard(MULTI_LINE_STOP, MANY_LINE_BOARD)

        val modes = composeRule.onNodeWithTag(StopCard.MODES).fetchSemanticsNode().config
        assertEquals("Train, Bus", modes[SemanticsProperties.ContentDescription].single())
        composeRule.onNodeWithTag(StopCard.ACTIONS).assertExists()
        composeRule.onNodeWithText("Select stop").performClick()
        assertEquals(1, picked)
    }

    // endregion

    private fun showCard(
        stop: NearbyStopFeature,
        board: DeparturesState,
        boardOpen: Boolean = false,
        fontScale: Float = 1f,
        topReserve: Dp = 0.dp,
    ) {
        composeRule.setContent {
            WithFontScale(fontScale) { PreviewTheme { CardOf(stop, board, boardOpen, topReserve) } }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun CardOf(
        stop: NearbyStopFeature,
        board: DeparturesState,
        boardOpen: Boolean,
        topReserve: Dp = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StopCardPlacement(topReserve = topReserve, onHeight = {}) {
                StopCard(
                    stop = stop,
                    departures = board,
                    onEvent = { events += it },
                    onClose = { closed++ },
                    boardOpenAtStart = boardOpen,
                    actions = { StopCardAction(text = "Select stop", onClick = { picked++ }) },
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

    private fun headingConfig() = composeRule.onNodeWithTag(StopCard.BOARD_HEADING).fetchSemanticsNode().config

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

        val SINGLE_LINE_STOP = NearbyStopFeature(
            stopId = "10101250",
            stopName = "Edgecliff Station, Stand B",
            position = LatLng(-33.8818, 151.2372),
            transportModes = persistentListOf(TransportMode.Bus),
        )

        val MULTI_LINE_STOP = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = LatLng(-33.8830, 151.2070),
            transportModes = persistentListOf(TransportMode.Train, TransportMode.Bus),
        )

        val ONE_ROW_BOARD = DeparturesState(isLoading = false, departures = persistentListOf(row("333", "Bus")))

        val ONE_LINE_BOARD = DeparturesState(
            isLoading = false,
            departures = persistentListOf(row("333", "Bus"), row("333", "Bus", time = "11:42 AM")),
        )

        val MANY_LINE_BOARD = DeparturesState(
            isLoading = false,
            departures = persistentListOf(row("T1"), row("T4", color = "#005AA3"), row("333", "Bus", "#00B5EF")),
        )

        fun row(line: String, mode: String = "Train", color: String = "#F99D1C", time: String = "11:30 AM") =
            StopDeparture(
                lineNumber = line,
                lineColorCode = color,
                transportModeName = mode,
                destinationName = "Anywhere",
                departureTimeText = time,
                departureUtcDateTime = "2026-11-22T01:30:00Z",
            )

        fun tallBoard() = DeparturesState(
            isLoading = false,
            departures = (1..14).map { row("T$it", time = "11:${30 + it} AM") }.toImmutableList(),
        )
    }
}

private const val CARD_TEST_SDK = 34
