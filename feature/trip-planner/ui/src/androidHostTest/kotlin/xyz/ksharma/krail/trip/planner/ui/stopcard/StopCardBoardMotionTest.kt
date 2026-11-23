package xyz.ksharma.krail.trip.planner.ui.stopcard

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
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
import xyz.ksharma.krail.trip.planner.ui.components.MapMotion
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature

/**
 * The Departure Board opens and closes smoothly, read frame by frame on a paused test clock: the card's height grows and
 * shrinks continuously with no snap, the chevron turns 180 degrees in step, the rows fade in a touch after the height starts
 * and are gone before it ends, the content under the board moves with it with no overshoot, and the rows are never removed
 * under a shrinking height. Reduced motion is plain, and a failed board fades into the rows rather than popping.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class StopCardBoardMotionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var board by mutableStateOf(ROWS)

    private fun setCard(reduceMotion: Boolean = false) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                        StopCard(
                            stop = CENTRAL,
                            departures = board,
                            onEvent = {},
                            onClose = {},
                            actions = { StopCardAction(text = "Set as destination", onClick = {}, modifier = Modifier.testTag(StopCard.PICK)) },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    private fun headingState() = composeRule.onNodeWithTag(StopCard.BOARD_HEADING).fetchSemanticsNode().config
        .getOrNull(SemanticsProperties.StateDescription)

    /**
     * A tap on the heading. The release of a tap on a paused clock lands a frame or two later, so this steps the clock until
     * the heading says it has changed, and the board's own motion begins on the frame after it, as a rider's would.
     */
    private fun toggle() {
        val before = headingState()
        composeRule.onNodeWithTag(StopCard.BOARD_HEADING).performClick()
        var guard = 0
        while (headingState() == before && guard++ < 20) composeRule.mainClock.advanceTimeByFrame()
    }

    private fun cardHeight() = composeRule.onNodeWithTag(StopCard.CARD).fetchSemanticsNode().boundsInRoot.height

    private fun top(tag: String) = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

    private fun chevron(): Float =
        composeRule.onNodeWithTag(StopCard.CHEVRON, useUnmergedTree = true).fetchSemanticsNode().config[ChevronDegrees]

    private fun bodyCount() = composeRule.onAllNodesWithTag(StopCard.BOARD_BODY).fetchSemanticsNodes().size

    private fun frames(count: Int, read: () -> Float): List<Float> = List(count) {
        composeRule.mainClock.advanceTimeByFrame()
        read()
    }

    /** The heights of the card when the board is shut and when it has finished opening. */
    private fun collapsedAndExpanded(): Pair<Float, Float> {
        val collapsed = cardHeight()
        toggle()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        val expanded = cardHeight()
        toggle()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(collapsed, cardHeight(), "Shut again before the test begins")
        return collapsed to expanded
    }

    // region opening

    @Test
    fun `opening grows the card continuously, through heights between shut and open, with no snap and no overshoot`() {
        setCard()
        val (collapsed, expanded) = collapsedAndExpanded()
        assertTrue(expanded > collapsed + 20f, "The open board is taller: $collapsed against $expanded")

        toggle()
        val series = frames(24) { cardHeight() }

        assertTrue(series.any { it > collapsed + 1f && it < expanded - 1f }, "A frame has a height between the two: $series")
        assertTrue(series.zipWithNext().all { (a, b) -> b >= a - 0.01f }, "It only grows: $series")
        assertTrue(series.all { it <= expanded + 0.5f }, "and never overshoots: $series")
        val steps = series.zipWithNext { a, b -> b - a }.filter { it > 0.01f }
        assertTrue(steps.size >= 6, "It takes many frames, not one jump: ${steps.size}")
        assertTrue(steps.max() < (expanded - collapsed) * 0.6f, "and no single frame is most of it: ${steps.max()}")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertTrue(abs(cardHeight() - expanded) < 0.5f, "and ends at its full height")
    }

    @Test
    fun `the chevron turns from 0 to 180 degrees in step with the height`() {
        setCard()
        val (collapsed, expanded) = collapsedAndExpanded()
        assertEquals(0f, chevron(), "It starts at rest")

        toggle()
        val angle = frames(24) { chevron() }
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        assertTrue(angle.any { it > 1f && it < 179f }, "A frame has the chevron part way: $angle")
        assertTrue(angle.zipWithNext().all { (a, b) -> b >= a - 0.01f }, "and it only turns one way: $angle")
        assertEquals(180f, chevron(), "It ends turned over")

        toggle()
        val back = frames(24) { chevron() }
        assertTrue(back.any { it > 1f && it < 179f }, "Closing turns it back through the middle: $back")
        assertTrue(back.zipWithNext().all { (a, b) -> b <= a + 0.01f })
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0f, chevron())
        assertTrue(collapsed < expanded)
    }

    @Test
    fun `the board's heading rides up with the height and the action under the board never overshoots`() {
        setCard()
        collapsedAndExpanded()
        val actionAtRest = top(StopCard.PICK)

        toggle()
        val heading = frames(24) { top(StopCard.BOARD_HEADING) }
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        val action = frames(1) { top(StopCard.PICK) }

        assertTrue(heading.zipWithNext().all { (a, b) -> b <= a + 0.01f }, "The heading only moves one way: $heading")
        assertTrue(heading.first() > heading.last() + 1f, "and it does move with the height")
        assertTrue(abs(action.single() - actionAtRest) < 0.5f, "The action stays pinned at the bottom of the card")
    }

    // endregion

    // region closing

    @Test
    fun `closing shrinks the card continuously and the rows stay in the tree until it has finished`() {
        setCard()
        val (collapsed, expanded) = collapsedAndExpanded()
        toggle()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(1, bodyCount())

        toggle()
        val series = frames(24) { cardHeight() }

        assertTrue(series.any { it > collapsed + 1f && it < expanded - 1f }, "A frame has a height between the two: $series")
        assertTrue(series.zipWithNext().all { (a, b) -> b <= a + 0.01f }, "It only shrinks: $series")
        composeRule.mainClock.advanceTimeBy(FRAME_MS)
        // The rows are in the tree for as long as the close takes and gone after it, never pulled out under the height.
        assertEquals(0, bodyCount(), "and they are gone once it has finished")
    }

    @Test
    fun `the rows are still there part way through a close`() {
        setCard()
        collapsedAndExpanded()
        toggle()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        toggle()
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 6)

        assertEquals(1, bodyCount(), "Still there with the height half way")
    }

    // endregion

    // region the fade of the rows

    @Test
    fun `the rows fade in a touch after the height starts and are gone before the height ends`() {
        var open by mutableStateOf(false)
        composeRule.setContent {
            PreviewTheme {
                val motion = rememberBoardMotion(open = open, reduceMotion = false)
                // Read once per frame by the test, on the clock it paces.
                probe = { motion.reveal() to motion.rowsAlpha() }
                Box(Modifier.testTag("probe"))
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnUiThread { open = true; Snapshot.sendApplyNotifications() }
        val opening = List(24) { composeRule.mainClock.advanceTimeByFrame(); probe() }
        val reveal = opening.map { it.first }
        val alpha = opening.map { it.second }
        val firstReveal = reveal.indexOfFirst { it > 0.01f }
        val firstAlpha = alpha.indexOfFirst { it > 0.01f }
        assertTrue(firstReveal in 0 until firstAlpha, "The height starts before the rows do: $firstReveal then $firstAlpha")
        assertTrue(reveal.zipWithNext().all { (a, b) -> b >= a - 0.001f } && alpha.zipWithNext().all { (a, b) -> b >= a - 0.001f })

        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        composeRule.runOnUiThread { open = false; Snapshot.sendApplyNotifications() }
        val closing = List(24) { composeRule.mainClock.advanceTimeByFrame(); probe() }
        val goneAt = closing.indexOfFirst { it.second < 0.001f }
        val shutAt = closing.indexOfFirst { it.first < 0.001f }.let { if (it < 0) closing.size else it }
        assertTrue(goneAt in 0 until shutAt, "The rows are gone ($goneAt) before the height ends ($shutAt)")
        assertTrue(closing[goneAt].first > 0.05f, "while there is still height left: ${closing[goneAt].first}")
    }

    private var probe: () -> Pair<Float, Float> = { 0f to 0f }

    // endregion

    // region the phases of the board

    @Test
    fun `a failed board fades into its rows, both on screen part way and only the rows at the end`() {
        board = FAILED
        setCard()
        toggle()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(1, composeRule.onAllNodesWithText("Retry").fetchSemanticsNodes().size, "The failed board shows Retry")

        composeRule.runOnUiThread { board = ROWS; Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)
        assertEquals(1, composeRule.onAllNodesWithText("Retry").fetchSemanticsNodes().size, "It has not popped away: Retry is still there")
        assertTrue(composeRule.onAllNodesWithText("Anywhere").fetchSemanticsNodes().isNotEmpty(), "and the rows are already coming in")

        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, composeRule.onAllNodesWithText("Retry").fetchSemanticsNodes().size, "Only the rows are left")
    }

    // endregion

    // region reduced motion

    @Test
    fun `with reduced motion the board is open or shut within a frame, with no height between`() {
        setCard(reduceMotion = true)
        val (collapsed, expanded) = collapsedAndExpanded()

        toggle()
        val series = frames(3) { cardHeight() }
        assertTrue(series.all { it == collapsed || abs(it - expanded) < 0.5f }, "Never between: $series")
        assertEquals(180f, chevron(), "and the chevron is over at once")

        toggle()
        frames(3) { cardHeight() }
        assertEquals(collapsed, cardHeight())
        assertEquals(0f, chevron())
        assertEquals(0, bodyCount())
        assertTrue(MapMotion.EXPAND_MS > MapMotion.EXIT_MS)
    }

    // endregion

    private companion object {
        const val FRAME_MS = 16L
        const val SETTLED_MS = 1_000L

        val CENTRAL = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = LatLng(-33.8830, 151.2070),
            transportModes = persistentListOf(TransportMode.Train),
        )

        val ROWS = DeparturesState(
            isLoading = false,
            departures = persistentListOf(row("T1"), row("T4", time = "11:42 AM"), row("T8", time = "11:50 AM")),
        )

        val FAILED = DeparturesState(isLoading = false, isError = true)

        fun row(line: String, time: String = "11:30 AM") = StopDeparture(
            lineNumber = line,
            lineColorCode = "#F99D1C",
            transportModeName = "Train",
            destinationName = "Anywhere",
            departureTimeText = time,
            departureUtcDateTime = "2026-11-22T01:30:00Z",
        )
    }
}
