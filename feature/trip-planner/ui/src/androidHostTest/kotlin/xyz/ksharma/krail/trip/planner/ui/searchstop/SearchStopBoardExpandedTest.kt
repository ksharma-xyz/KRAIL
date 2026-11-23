package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCard
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardAction
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardBackHandlers
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardPlacement
import xyz.ksharma.krail.trip.planner.ui.stopcard.stopCardTopReserveFor

/**
 * While the Departure Board of an open stop card is expanded the box at the top gets out of the way: it slides up and fades,
 * kept and not rebuilt, takes no touch, and the card's room grows as it leaves on the same curve, so the action stays on the
 * card at 320 dp and 200 percent text. It returns the same way when the board is collapsed or the card dismissed. Back shuts
 * the board first, then dismisses the card, then is the screen's. All read on a paused test clock.
 */
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class SearchStopBoardExpandedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var cardOpen by mutableStateOf(true)
    private var boardExpanded by mutableStateOf(false)
    private var mapTaps = 0
    private var boxTaps = 0
    private var created = 0
    private var disposed = 0
    private lateinit var boxState: TextFieldState

    private val form = 136.dp + 16.dp

    /** True while a test reads the box's place: the box stays in the semantics tree though it is hidden, so its bounds are readable. */
    private var keepBoxReadable = true

    private fun show(fontScale: Float = 1f, reduceMotion: Boolean = false, board: DeparturesState = TALL) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PreviewTheme {
                    val hidden = topBoxHiddenFor(SearchStopMode.Map, boardExpanded = boardExpanded && cardOpen)
                    val slide by rememberTopBoxSlide(hidden = hidden, reduceMotion = reduceMotion)
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { mapTaps++ } })
                        TopBox(slideProgress = { slide }, hidden = hidden && !keepBoxReadable)
                        if (cardOpen) {
                            StopCardPlacement(topReserve = stopCardTopReserveFor(form, slide), onHeight = {}) {
                                StopCard(
                                    stop = CENTRAL,
                                    departures = board,
                                    onEvent = {},
                                    onClose = { cardOpen = false },
                                    boardExpanded = boardExpanded,
                                    onBoardExpandedChange = { boardExpanded = it },
                                    actions = {
                                        StopCardAction(text = "Set as destination", onClick = {}, modifier = Modifier.testTag(StopCard.PICK))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    /** A stand in for the box: one field with a typed word and a selection, which counts being built and torn down. */
    @Composable
    private fun BoxScope.TopBox(slideProgress: () -> Float, hidden: Boolean) {
        val state = rememberTextFieldState(initialText = "Cen", initialSelection = TextRange(1, 3))
        boxState = state
        LaunchedEffect(Unit) { created++ }
        DisposableEffect(Unit) { onDispose { disposed++ } }
        val extra = with(LocalDensity.current) { 24.dp.toPx() }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .height(136.dp)
                .slideAwayUp(progress = slideProgress, extraDistancePx = extra, hidden = hidden)
                .pointerInput(Unit) { detectTapGestures { boxTaps++ } },
        ) {
            Box(Modifier.fillMaxSize().testTag(BOX)) {
                BasicTextField(state = state, modifier = Modifier.testTag(FIELD))
            }
        }
    }

    private fun setBoard(expanded: Boolean) = composeRule.runOnUiThread {
        boardExpanded = expanded
        Snapshot.sendApplyNotifications()
    }

    private fun boxTop() = composeRule.onNodeWithTag(BOX).fetchSemanticsNode().boundsInRoot.top

    private fun cardTop() = composeRule.onNodeWithTag(StopCard.CARD).fetchSemanticsNode().boundsInRoot.top

    private fun frames(count: Int, read: () -> Float): List<Float> = List(count) {
        composeRule.mainClock.advanceTimeByFrame()
        read()
    }

    // region the box leaves and returns

    @Test
    fun `expanding the board slides the box up and out, in step, and collapsing slides it back to where it was`() {
        show()
        val home = boxTop()

        setBoard(true)
        val leaving = frames(24) { boxTop() }
        assertTrue(leaving.any { it < home - 1f && it > home - 140f }, "A frame has the box part way: $leaving")
        assertTrue(leaving.zipWithNext().all { (a, b) -> b <= a + 0.01f }, "It only moves up: $leaving")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertTrue(boxTop() + 136f * composeRule.density.density <= 0f, "and is off the top of the screen: ${boxTop()}")

        setBoard(false)
        val returning = frames(24) { boxTop() }
        assertTrue(returning.zipWithNext().all { (a, b) -> b >= a - 0.01f }, "It comes back down the same way: $returning")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(home, boxTop(), "to the very place it was")
    }

    @Test
    fun `the box is kept, not rebuilt, so its text and selection are as they were`() {
        show()

        setBoard(true)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        setBoard(false)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        assertEquals(1, created, "Built once")
        assertEquals(0, disposed, "and never torn down")
        assertEquals("Cen", boxState.text.toString())
        assertEquals(TextRange(1, 3), boxState.selection)
    }

    @Test
    fun `while the box is hidden it takes no touch, the map under it does`() {
        keepBoxReadable = false
        show()
        val centre = composeRule.onNodeWithTag(BOX).fetchSemanticsNode().boundsInRoot.center
        setBoard(true)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        // Where the box was, now empty map: the tap is the map's and the box never sees one.
        composeRule.onRoot().performTouchInput { click(Offset(centre.x, centre.y)) }
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 2)

        assertEquals(0, boxTaps, "The box takes no tap while it is gone")
    }

    @Test
    fun `with reduced motion the box is gone and back within a frame`() {
        show(reduceMotion = true)
        val home = boxTop()

        setBoard(true)
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertTrue(boxTop() < home - 100f, "Gone at once, not part way: ${boxTop()}")

        setBoard(false)
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertEquals(home, boxTop())
    }

    // endregion

    // region the card takes the room

    @Test
    fun `the card's room grows as the box leaves, on the same curve, and shrinks back as it returns`() {
        show()
        val collapsed = cardTop()

        setBoard(true)
        val growing = frames(24) { cardTop() }
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        val expanded = cardTop()

        assertTrue(expanded < collapsed - 20f, "The card reaches higher with the box gone: $collapsed to $expanded")
        assertTrue(growing.zipWithNext().all { (a, b) -> b <= a + 0.01f }, "Its top only rises: $growing")
        assertTrue(growing.any { it < collapsed - 1f && it > expanded + 1f }, "through heights between the two: $growing")

        setBoard(false)
        val shrinking = frames(24) { cardTop() }
        assertTrue(shrinking.zipWithNext().all { (a, b) -> b >= a - 0.01f }, "and comes back down: $shrinking")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertTrue(abs(cardTop() - collapsed) < 1f)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun `at 320 dp and 200 percent with the board expanded the action stays on the card and the card stays on screen`() {
        show(fontScale = 2f)

        setBoard(true)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        val density = composeRule.density.density
        val action = composeRule.onNodeWithTag(StopCard.PICK).fetchSemanticsNode().boundsInRoot
        assertTrue(action.bottom <= 640f * density + 1f, "The action is on screen: ${action.bottom}")
        assertTrue(action.top >= cardTop(), "and on the card")
        assertTrue(cardTop() >= 8f * density, "The card stays below the top of the screen: ${cardTop()}")
        assertEquals(1, composeRule.onAllNodesWithTag(StopCard.BOARD_HEADING).fetchSemanticsNodes().size)
    }

    // endregion

    // region back

    @Test
    fun `back shuts the board, then dismisses the card, then is the screen's`() {
        var screenBacks = 0
        composeRule.setContent {
            PreviewTheme {
                BackHandler(enabled = true) { screenBacks++ }
                StopCardBackHandlers(
                    cardOpen = cardOpen,
                    boardExpanded = boardExpanded,
                    covered = false,
                    onCollapseBoard = { boardExpanded = false },
                    onDismissCard = { cardOpen = false },
                )
            }
        }
        composeRule.runOnUiThread { boardExpanded = true; cardOpen = true }
        composeRule.waitForIdle()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(false, boardExpanded, "The first back shuts the board")
        assertEquals(true, cardOpen, "and the card is still there")
        assertEquals(0, screenBacks)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(false, cardOpen, "The next dismisses the card")
        assertEquals(0, screenBacks)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(1, screenBacks, "and the next is the screen's own step back")
    }

    // endregion

    private companion object {
        const val BOX = "topbox"
        const val FIELD = "topbox.field"
        const val FRAME_MS = 16L
        const val SETTLED_MS = 1_000L

        val CENTRAL = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = LatLng(-33.8830, 151.2070),
            transportModes = persistentListOf(TransportMode.Train, TransportMode.Bus),
        )

        val TALL = DeparturesState(
            isLoading = false,
            departures = persistentListOf<StopDeparture>().addAll(
                (1..16).map {
                    StopDeparture(
                        lineNumber = "T$it",
                        lineColorCode = "#F99D1C",
                        transportModeName = "Train",
                        destinationName = "Anywhere",
                        departureTimeText = "11:${10 + it} AM",
                        departureUtcDateTime = "2026-11-22T01:30:00Z",
                    )
                },
            ),
        )
    }
}
