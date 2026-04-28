package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchStopRulesTest {

    private val home = StopLabel(emoji = "🏠", label = "Home")
    private val homeSet = StopLabel(
        emoji = "🏠",
        label = "Home",
        stopId = "stop_central",
        stopName = "Central Station",
    )
    private val workSet = StopLabel(
        emoji = "💼",
        label = "Work",
        stopId = "stop_town_hall",
        stopName = "Town Hall",
    )
    private val gym = StopLabel(emoji = "🏋", label = "Gym")
    private val centralStop = StopItem(stopId = "stop_central", stopName = "Central Station")
    private val townHallStop = StopItem(stopId = "stop_town_hall", stopName = "Town Hall")

    // region pillRowBannerText

    @Test
    fun `banner is null when idle (no editing, no assigning)`() {
        val text = pillRowBannerText(
            editing = false,
            assigningLabel = null,
            stopLabels = listOf(home),
        )
        assertNull(text)
    }

    @Test
    fun `banner shows reorder hint while editing, regardless of assigning state`() {
        val text = pillRowBannerText(
            editing = true,
            assigningLabel = home,
            stopLabels = listOf(home),
        )
        assertEquals("Drag a pill to reorder. Tap ✕ to delete.", text)
    }

    @Test
    fun `banner asks user to tap star while assigning an unset label`() {
        val text = pillRowBannerText(
            editing = false,
            assigningLabel = home,
            stopLabels = listOf(home),
        )
        assertEquals("Tap the ⭐ next to a stop to save it as Home", text)
    }

    @Test
    fun `banner clears once the assigning label has been satisfied`() {
        // assigningLabel still references the unset Home, but stopLabels now shows it set.
        val text = pillRowBannerText(
            editing = false,
            assigningLabel = home,
            stopLabels = listOf(homeSet),
        )
        assertNull(text)
    }

    @Test
    fun `banner stays visible if a different label became set, not the one assigning`() {
        val text = pillRowBannerText(
            editing = false,
            assigningLabel = home,
            stopLabels = listOf(home, workSet),
        )
        assertEquals("Tap the ⭐ next to a stop to save it as Home", text)
    }

    // endregion

    // region savedStopIds

    @Test
    fun `savedStopIds returns only stopIds from set labels`() {
        val ids = savedStopIds(listOf(home, homeSet, workSet, gym))
        assertEquals(setOf("stop_central", "stop_town_hall"), ids)
    }

    @Test
    fun `savedStopIds is empty when no labels are set`() {
        val ids = savedStopIds(listOf(home, gym))
        assertTrue(ids.isEmpty())
    }

    // endregion

    // region conflictForAssign

    @Test
    fun `no conflict when assigning a fresh stop to an unset label`() {
        val conflict = conflictForAssign(
            target = home,
            stop = centralStop,
            allLabels = listOf(home, gym),
        )
        assertNull(conflict)
    }

    @Test
    fun `no conflict when re-assigning the same stop to the same label (idempotent)`() {
        val conflict = conflictForAssign(
            target = homeSet,
            stop = centralStop,
            allLabels = listOf(homeSet),
        )
        assertNull(conflict)
    }

    @Test
    fun `stop-side conflict when stop is already saved on another label`() {
        // Central is already on Home; user is now trying to attach it to Gym too.
        val conflict = conflictForAssign(
            target = gym,
            stop = centralStop,
            allLabels = listOf(homeSet, gym),
        )
        val expected = AssignConflict.StopAlreadyOnAnotherLabel(
            target = gym,
            stop = centralStop,
            existingLabel = homeSet,
        )
        assertEquals(expected, conflict)
    }

    @Test
    fun `label-side conflict when label has a different stop already`() {
        // Home currently points at Central; user is trying to reassign to Town Hall.
        val conflict = conflictForAssign(
            target = homeSet,
            stop = townHallStop,
            allLabels = listOf(homeSet),
        )
        val expected = AssignConflict.LabelHasDifferentStop(
            target = homeSet,
            stop = townHallStop,
            existingStopName = "Central Station",
        )
        assertEquals(expected, conflict)
    }

    @Test
    fun `stop-side conflict takes precedence over label-side conflict`() {
        // Home has Central, Work has TownHall. Re-assigning TownHall to Home
        // is BOTH a stop-side conflict (TownHall is on Work) AND a label-side
        // (Home has Central). Stop-side should win — that's the more visible
        // 1:1 invariant from the user's POV.
        val conflict = conflictForAssign(
            target = homeSet,
            stop = townHallStop,
            allLabels = listOf(homeSet, workSet),
        )
        assertTrue(
            conflict is AssignConflict.StopAlreadyOnAnotherLabel,
            "expected stop-side conflict, got $conflict",
        )
    }

    // endregion
}
