package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.collections.immutable.toImmutableSet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Options card opens and closes with motion, read frame by frame on a paused test clock. It grows out of the Options
 * button (fades and scales up from its bottom start corner), its layout is final from the first frame, it reverses from
 * where it is when Options is tapped again, it leaves the tree only once it has gone, and with reduced motion it simply
 * appears and disappears.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class SearchStopModeBlockMotionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var open by mutableStateOf(false)
    private val allModes = NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS)
        .map { NswTransportConfig.productClassFor(it) }
        .toImmutableSet()

    /** The screen's own rule for the button: a tap flips it. The magenta ground makes a half faded card show. */
    private fun setControls(reduceMotion: Boolean) {
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.fillMaxSize().background(GROUND)) {
                    SearchStopModeControls(
                        open = open,
                        selectedModes = allModes,
                        onButtonClick = { open = reduceModeBlock(open, ModeBlockEvent.ButtonTapped) },
                        onToggleMode = {},
                        pillHeight = MAP_OPTIONS_PILL_REST_HEIGHT,
                        onPillHeight = {},
                        reduceMotion = reduceMotion,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    /**
     * What a tap on Options does to the block, without the tap itself: a real tap on a paused clock spends 100 ms of test
     * time between its press and its release, which would hide the first frames of what these tests read.
     */
    private fun tapOptions() = composeRule.runOnUiThread {
        open = reduceModeBlock(open, ModeBlockEvent.ButtonTapped)
        Snapshot.sendApplyNotifications()
    }

    private fun cardCount() =
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).fetchSemanticsNodes().size

    /** The card as it is drawn right now, scale included, so a card half way up reads smaller than its layout. */
    private fun drawnHeight(): Float =
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).fetchSemanticsNode().boundsInRoot.height

    private fun drawnWidth(): Float =
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).fetchSemanticsNode().boundsInRoot.width

    /** The card's own size once it has finished opening, found by opening it fully and shutting it again. */
    private fun finalHeight(): Float {
        tapOptions()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        val height = drawnHeight()
        tapOptions()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, cardCount(), "Shut again before the test begins.")
        return height
    }

    // region opening

    @Test
    fun `before the tap the card is not there, and one frame after it the card exists part way`() {
        setControls(reduceMotion = false)
        val final = finalHeight()

        assertEquals(0, cardCount(), "Absent before the tap.")
        tapOptions()
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 3)

        assertEquals(1, cardCount(), "The card is there on the first frames.")
        val height = drawnHeight()
        assertTrue(height < final - 1f, "It is still growing at $height of $final")
        assertTrue(height > final * MODE_BLOCK_START_SCALE - 1f, "and never below where it starts at $height of $final")
    }

    @Test
    fun `the card fades in from nothing and grows from its bottom start corner, with the layout final from the start`() {
        val enter = modeBlockEnter(reduceMotion = false).toString()
        val exit = modeBlockExit(reduceMotion = false).toString()
        assertTrue("Fade(alpha=0.0" in enter, "It fades in from fully transparent: $enter")
        assertTrue("Scale(scale=$MODE_BLOCK_START_SCALE" in enter, "and scales up from $MODE_BLOCK_START_SCALE: $enter")
        assertTrue("Fade(alpha=0.0" in exit, "It fades out to fully transparent: $exit")
        assertTrue("Scale(scale=$MODE_BLOCK_START_SCALE" in exit, "and scales back down to the same: $exit")
        assertTrue(
            "Shrink - null" in enter && "Shrink - null" in exit,
            "It never changes size, so the wrapped chips never reflow: $enter and $exit",
        )

        setControls(reduceMotion = false)
        finalHeight()
        tapOptions()
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 3)
        val width = drawnWidth()
        val linesAtFirst = chipLines()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertTrue(
            width < drawnWidth() - 0.5f && width > drawnWidth() * MODE_BLOCK_START_SCALE - 1f,
            "It grows from $width",
        )
        assertEquals(chipLines(), linesAtFirst, "The chips are on the lines they end on from the first frames.")
    }

    private fun chipLines(): Int = NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS)
        .map {
            composeRule.onNodeWithTag(TripPlannerTestTags.searchStopModeChip(NswTransportConfig.productClassFor(it)))
                .fetchSemanticsNode()
                .layoutInfo
                .coordinates
                .positionInRoot()
                .y
        }
        .distinct()
        .size

    @Test
    fun `after the animation the card is at its final size and does not move again`() {
        setControls(reduceMotion = false)
        val final = finalHeight()

        tapOptions()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(1, cardCount())
        assertTrue(abs(drawnHeight() - final) < 1f, "Final size ${drawnHeight()} against $final")
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 10)
        assertTrue(abs(drawnHeight() - final) < 1f, "and it stays there")
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertExistsWithState(expanded = true)
    }

    // endregion

    // region closing

    @Test
    fun `closing animates out and only then leaves the tree`() {
        setControls(reduceMotion = false)
        val final = finalHeight()
        tapOptions()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        tapOptions()
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 5)

        assertEquals(1, cardCount(), "It does not snap away: it is still there part way through")
        val height = drawnHeight()
        assertTrue(height < final - 1f, "and it is already shrinking at $height of $final")
        assertTrue(height > final * MODE_BLOCK_START_SCALE - 1f)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertExistsWithState(expanded = false)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, cardCount(), "and it is gone from the tree once it has finished")
    }

    @Test
    fun `tapping Options again part way through opening turns back from where it is, with one card`() {
        setControls(reduceMotion = false)
        val final = finalHeight()

        tapOptions()
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)
        tapOptions()
        val series = heightsOverFrames(8)

        assertEquals(1, cardCount(), "Never a second card.")
        assertTrue(series.all { it >= final * MODE_BLOCK_START_SCALE - 0.5f }, "It never restarts from zero: $series")
        assertTrue(series.all { it < final - 1f }, "and never jumps on to full size: $series")
        assertTrue(turnsOnce(series, upFirst = true), "It carries on a frame, then turns back: $series")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, cardCount(), "and it is gone once it has shut")
    }

    @Test
    fun `tapping Options again part way through closing turns back to open, with one card`() {
        setControls(reduceMotion = false)
        val final = finalHeight()
        tapOptions()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        tapOptions()
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)
        tapOptions()
        val series = heightsOverFrames(10)

        assertEquals(1, cardCount(), "Never a second card.")
        assertTrue(series.all { it >= final * MODE_BLOCK_START_SCALE - 0.5f }, "It never restarts from zero: $series")
        assertTrue(turnsOnce(series, upFirst = false), "It carries on a frame, then turns back up: $series")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(1, cardCount())
        assertTrue(abs(drawnHeight() - final) < 1f, "and it ends open at its final size")
    }

    /** The card's drawn height at each of the next [frames] frames, 0 once it has left the tree. */
    private fun heightsOverFrames(frames: Int): List<Float> = List(frames) {
        composeRule.mainClock.advanceTimeByFrame()
        if (cardCount() == 1) drawnHeight() else 0f
    }

    /** The series moves one way and then the other, once, with no jump back to where it started. */
    private fun turnsOnce(series: List<Float>, upFirst: Boolean): Boolean {
        val steps = series.zipWithNext { a, b -> b - a }.filter { abs(it) > 0.01f }
        val signs = steps.map { it > 0f }
        val changes = signs.zipWithNext().count { (a, b) -> a != b }
        return changes == 1 && signs.first() == upFirst
    }

    // endregion

    // region reduced motion

    @Test
    fun `with reduced motion the card appears at its final size and goes with no frame between`() {
        setControls(reduceMotion = true)
        val final = finalHeight()

        tapOptions()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, cardCount(), "It is there on the first frame")
        assertTrue(abs(drawnHeight() - final) < 1f, "at its final size, not part way: ${drawnHeight()} of $final")

        tapOptions()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(0, cardCount(), "and it is gone within a frame of the tap")
    }

    // endregion

    private fun SemanticsNodeInteraction.assertExistsWithState(expanded: Boolean) {
        assertExists()
        val state = fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals(if (expanded) MODE_BUTTON_EXPANDED else MODE_BUTTON_COLLAPSED, state)
    }

    private companion object {
        val GROUND = Color.Magenta
        const val FRAME_MS = 16L
        const val SETTLED_MS = 1_000L
    }
}
