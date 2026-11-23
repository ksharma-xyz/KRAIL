package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
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
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The stop card slides up from the bottom of the pane with a fade as it arrives and slides back down as it is dismissed,
 * smoothly in both directions, read frame by frame on a paused test clock. It is in the tree only while it is shown or on its
 * way out, and with reduced motion it simply appears and goes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class StopCardSlideTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var visible by mutableStateOf(false)

    private fun show(reduceMotion: Boolean = false) {
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedStopCardPlacement(
                        visible = visible,
                        topReserve = 0.dp,
                        onHeight = {},
                        reduceMotion = reduceMotion,
                    ) {
                        StopCard(
                            stop = CENTRAL,
                            departures = DeparturesState(isLoading = false),
                            onEvent = {},
                            onClose = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    private fun set(value: Boolean) = composeRule.runOnUiThread {
        visible = value
        Snapshot.sendApplyNotifications()
    }

    private fun count() = composeRule.onAllNodesWithTag(StopCard.CARD).fetchSemanticsNodes().size

    private fun top() = composeRule.onNodeWithTag(StopCard.CARD).fetchSemanticsNode().boundsInRoot.top

    private fun frames(n: Int): List<Float> = List(n) {
        composeRule.mainClock.advanceTimeByFrame()
        if (count() == 1) top() else Float.NaN
    }

    @Test
    fun `before it is shown the card is not in the tree`() {
        show()

        assertEquals(0, count())
    }

    @Test
    fun `the card slides up from below through places between, and settles where it floats`() {
        show()
        set(true)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        val home = top()
        set(false)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        set(true)
        val series = frames(20).filter { !it.isNaN() }

        assertTrue(series.first() > home + 20f, "It begins below its place: ${series.first()} against $home")
        assertTrue(
            series.any { it > home + 1f && it < series.first() - 1f },
            "and passes through places between: $series",
        )
        assertTrue(series.zipWithNext().all { (a, b) -> b <= a + 0.01f }, "and only moves up: $series")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertTrue(abs(top() - home) < 1f, "It ends where it floats, ${top()} against $home")
    }

    @Test
    fun `dismissing slides it back down and out, and it leaves the tree only once it has gone`() {
        show()
        set(true)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        val home = top()

        set(false)
        val series = frames(5)

        assertEquals(1, count(), "It does not snap away: it is still there part way through")
        assertTrue(series.last() > home + 1f, "and already moving down: ${series.last()} from $home")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, count(), "and it is out of the tree once it has gone")
    }

    @Test
    fun `with reduced motion the card is there and gone within a frame, with no place between`() {
        show(reduceMotion = true)
        set(true)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, count())
        val home = top()

        set(false)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(0, count())
        assertTrue(home < 891f)
    }

    private companion object {
        const val SETTLED_MS = 1_000L

        val CENTRAL = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = LatLng(-33.8830, 151.2070),
            transportModes = persistentListOf(TransportMode.Train),
        )
    }
}
