package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The event that shuts the mode block when the Options button goes away, with no layout run. */
class SearchStopModeBlockEventTest {

    @Test
    fun `a hidden button shuts the block and a shown or absent one does not`() {
        assertEquals(ModeBlockEvent.ControlsHidden, modeBlockEventFor(ModeControls.Hidden))
        assertNull(modeBlockEventFor(ModeControls.Shown))
        assertNull(modeBlockEventFor(ModeControls.Absent))
    }

    @Test
    fun `the event for a hidden button leaves an open block shut and a shut block shut`() {
        val event = requireNotNull(modeBlockEventFor(ModeControls.Hidden))
        assertEquals(false, reduceModeBlock(open = true, event))
        assertEquals(false, reduceModeBlock(open = false, event))
    }

    @Test
    fun `the button going away for a stop card or a missing permission is what hides it`() {
        val noCard = modeControlsFor(SearchStopMode.Map, mapReady = true, locationGranted = true, stopCardOpen = false)
        val cardOpen = modeControlsFor(SearchStopMode.Map, mapReady = true, locationGranted = true, stopCardOpen = true)
        val noPermission =
            modeControlsFor(SearchStopMode.Map, mapReady = true, locationGranted = false, stopCardOpen = false)
        assertNull(modeBlockEventFor(noCard))
        assertEquals(ModeBlockEvent.ControlsHidden, modeBlockEventFor(cardOpen))
        assertEquals(ModeBlockEvent.ControlsHidden, modeBlockEventFor(noPermission))
    }
}
