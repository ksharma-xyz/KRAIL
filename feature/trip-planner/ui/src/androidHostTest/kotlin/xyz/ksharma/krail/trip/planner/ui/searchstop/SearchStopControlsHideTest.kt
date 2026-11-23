package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.MapMotion
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapControlsLayer
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The map's controls get out of an open stop card's way by hiding, read frame by frame on a paused test clock. The location
 * control shrinks to nothing about its own centre while it fades and grows back to the same place, Options and its card
 * go and return the way the card does, neither takes a tap while it is hidden (the tap reaches the map under it), a card
 * that was open comes back shut, and with reduced motion it is plain. The map is a stand in that counts the taps it is given.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class SearchStopControlsHideTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()
    private var cardOpen by mutableStateOf(false)
    private var blockOpen by mutableStateOf(false)
    private var mapTaps = 0
    private var locationTaps = 0
    private var optionsTaps = 0
    private var windowHeightPx = 0

    private fun setControls(reduceMotion: Boolean = false, hides: Boolean = true) {
        composeRule.setContent {
            PreviewTheme {
                windowHeightPx = LocalWindowInfo.current.containerSize.height
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { mapTaps++ } })
                    MapControlsLayer(
                        showsLocationButton = true,
                        isLocationActive = true,
                        onLocationButtonClick = { locationTaps++ },
                        cardOpen = cardOpen,
                        cardHeight = CARD_HEIGHT,
                        hidesControlsForCard = hides,
                        locationBottomPadding = { clearance ->
                            locationControlBottomPadding(gaps, MAP_OPTIONS_PILL_REST_HEIGHT, clearance)
                        },
                        bottomControls = { clearance, controlsHidden ->
                            androidx.compose.runtime.LaunchedEffect(controlsHidden) {
                                if (controlsHidden) blockOpen = reduceModeBlock(blockOpen, ModeBlockEvent.ControlsHidden)
                            }
                            SearchStopModeControls(
                                open = blockOpen,
                                selectedModes = persistentSetOf(),
                                onButtonClick = {
                                    optionsTaps++
                                    blockOpen = reduceModeBlock(blockOpen, ModeBlockEvent.ButtonTapped)
                                },
                                onToggleMode = {},
                                pillHeight = MAP_OPTIONS_PILL_REST_HEIGHT,
                                onPillHeight = {},
                                clearance = clearance,
                                visible = !controlsHidden,
                                reduceMotion = reduceMotion,
                            )
                        },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    private fun setCard(open: Boolean) = composeRule.runOnUiThread {
        cardOpen = open
        Snapshot.sendApplyNotifications()
    }

    private fun count(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private val location = TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON
    private val options = TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON
    private val chips = TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS

    private fun tapAt(position: Offset): String {
        mapTaps = 0
        locationTaps = 0
        optionsTaps = 0
        composeRule.onRoot().performTouchInput { click(position) }
        composeRule.mainClock.advanceTimeBy(FRAME_MS)
        return "map=$mapTaps location=$locationTaps options=$optionsTaps"
    }

    @Test
    fun `with no card both controls are there at full size`() {
        setControls()

        assertEquals(1, count(location), "The location control")
        assertEquals(1, count(options), "The Options button")
        assertTrue(abs(bounds(location).width - 48f * composeRule.density.density) < 1f, "at its full 48 dp")
    }

    @Test
    fun `opening a card shrinks the location control about its own centre, and it is gone once it has shrunk`() {
        setControls()
        val full = bounds(location)

        setCard(true)
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 5)

        assertEquals(1, count(location), "It does not snap away: it is still there part way through")
        val mid = bounds(location)
        assertTrue(mid.width < full.width - 1f && mid.width > 0.5f, "and already smaller, ${mid.width} of ${full.width}")
        assertTrue(abs(mid.center.x - full.center.x) < 1f && abs(mid.center.y - full.center.y) < 1f, "about its own centre")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, count(location), "and out of the tree once it has gone")
    }

    @Test
    fun `dismissing the card grows the location control back to the very same place and size`() {
        setControls()
        val before = bounds(location)
        setCard(true)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        setCard(false)
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)
        val mid = bounds(location)
        assertTrue(mid.width < before.width - 1f && mid.width > 0.5f, "It comes back from nothing, ${mid.width} of ${before.width}")

        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        val after = bounds(location)
        assertEquals(before, after, "and ends where it began, with no gap and no jump")
    }

    @Test
    fun `no clearance lifts the location control above the card, it hides instead`() {
        setControls()
        val before = bounds(location)

        setCard(true)
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 3)

        assertTrue(abs(bounds(location).center.y - before.center.y) < 1f, "It is not lifted while it goes")
    }

    @Test
    fun `Options and its card leave with the stop card and come back with the card shut`() {
        setControls()
        composeRule.runOnUiThread { blockOpen = true; Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(1, count(chips), "The Options card is open")
        val full = bounds(options)

        setCard(true)
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)
        val mid = bounds(options)
        assertEquals(1, count(options), "Options is still there part way through")
        assertTrue(mid.width < full.width - 0.5f && mid.width > full.width * MapMotion.POP_START_SCALE - 1f, "shrinking, ${mid.width} of ${full.width}")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, count(options), "then gone")
        assertEquals(0, count(chips), "and so is the card")

        setCard(false)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(1, count(options), "Options returns")
        assertEquals(0, count(chips), "with its card shut, as the rider left the map")
        assertEquals(full, bounds(options), "to the same place")
    }

    @Test
    fun `while hidden neither control takes a tap, the map under them does`() {
        setControls()
        val locationCentre = bounds(location).center
        val optionsCentre = bounds(options).center
        setCard(true)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        assertEquals("map=1 location=0 options=0", tapAt(locationCentre), "where the location control was")
        assertEquals("map=1 location=0 options=0", tapAt(optionsCentre), "where Options was")
    }

    @Test
    fun `a tap on a control that is on its way out is swallowed, not acted on`() {
        setControls()
        val locationCentre = bounds(location).center
        setCard(true)
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 2)

        mapTaps = 0
        locationTaps = 0
        composeRule.onRoot().performTouchInput { click(locationCentre) }

        assertEquals(0, locationTaps, "It does not fire while it fades")
    }

    @Test
    fun `with reduced motion the controls are gone within a frame and back within a frame`() {
        setControls(reduceMotion = true)
        setCard(true)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(0, count(options), "Options is gone")

        setCard(false)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, count(options), "and back at its final size, with no part way")
    }

    @Test
    fun `a host that does not hide keeps its controls above the card as before`() {
        setControls(hides = false)
        val before = bounds(location)

        setCard(true)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        assertEquals(1, count(location), "The location control stays")
        assertTrue(bounds(location).center.y < before.center.y - 100f, "and rides above the card")
    }

    @Test
    fun `a card that was open is shut by the button going and Options never takes the event twice`() {
        assertEquals(false, reduceModeBlock(true, ModeBlockEvent.ControlsHidden))
        assertEquals(false, reduceModeBlock(false, ModeBlockEvent.ControlsHidden))
    }

    private companion object {
        val CARD_HEIGHT = 300.dp
        const val FRAME_MS = 16L
        const val SETTLED_MS = 1_000L
    }
}
