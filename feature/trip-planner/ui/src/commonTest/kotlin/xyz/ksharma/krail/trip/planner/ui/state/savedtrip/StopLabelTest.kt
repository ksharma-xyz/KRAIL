package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [StopLabel] — a small data class with three load-bearing derived
 * properties that the rest of the feature reads aggressively:
 *
 * - `isSet` decides star fill vs outline on every stop result row and whether the
 *   pill is tappable as a save shortcut.
 * - `isProtected` keeps Home undeletable everywhere (UI hides ✕, VM handler
 *   early-returns).
 * - `defaults` is the seed list used when the DB is empty on first install — its
 *   shape determines what the user sees out-of-the-box.
 *
 * Lives under feature/trip-planner/ui/src/commonTest because the state module
 * doesn't have its own test infra wired up yet; the assertions don't depend on
 * anything ui-specific so they're agnostic to that placement.
 */
class StopLabelTest {

    // region isSet

    @Test
    fun `isSet is false when neither stopId nor stopName provided`() {
        val label = StopLabel(emoji = "🏠", label = "Home")
        assertFalse(label.isSet, "default-constructed label should be unset")
    }

    @Test
    fun `isSet is true when both stopId and stopName provided`() {
        val label = StopLabel(
            emoji = "🏠",
            label = "Home",
            stopId = "stop_central",
            stopName = "Central Station",
        )
        assertTrue(label.isSet)
    }

    @Test
    fun `isSet is false when only stopId provided`() {
        // Defensive: if migration ever leaves a row half-populated, isSet must
        // refuse to treat it as a clickable shortcut.
        val label = StopLabel(emoji = "🏠", label = "Home", stopId = "stop_central")
        assertFalse(label.isSet)
    }

    @Test
    fun `isSet is false when only stopName provided`() {
        val label = StopLabel(emoji = "🏠", label = "Home", stopName = "Central Station")
        assertFalse(label.isSet)
    }

    // endregion

    // region isProtected

    @Test
    fun `isProtected is true for Home`() {
        val home = StopLabel(emoji = "🏠", label = StopLabel.PROTECTED_LABEL)
        assertTrue(home.isProtected)
    }

    @Test
    fun `isProtected matches case-insensitively`() {
        // Defensive: even if the DB ever contains a lowercase variant, the protection
        // must hold so the user never lands on a deletable Home.
        val home = StopLabel(emoji = "🏠", label = "home")
        assertTrue(home.isProtected)
    }

    @Test
    fun `isProtected is false for Work`() {
        val work = StopLabel(emoji = "💼", label = "Work")
        assertFalse(work.isProtected)
    }

    @Test
    fun `isProtected is false for arbitrary user label`() {
        val gym = StopLabel(emoji = "🏋", label = "Gym")
        assertFalse(gym.isProtected)
    }

    // endregion

    // region toStopItem

    @Test
    fun `toStopItem returns null when label is unset`() {
        val unset = StopLabel(emoji = "🏠", label = "Home")
        assertNull(unset.toStopItem(), "unset labels can't navigate to a stop")
    }

    @Test
    fun `toStopItem returns StopItem with same id and name when set`() {
        val home = StopLabel(
            emoji = "🏠",
            label = "Home",
            stopId = "stop_central",
            stopName = "Central Station",
        )
        val expected = StopItem(stopId = "stop_central", stopName = "Central Station")
        assertEquals(expected, home.toStopItem())
    }

    // endregion

    // region defaults

    @Test
    fun `defaults contains Home and Work both unset`() {
        // First-install contract: UI must render two unset pills without ✕ on Home.
        // Anything else here would change the freshInstall snapshot.
        assertEquals(2, StopLabel.defaults.size)

        val home = StopLabel.defaults[0]
        assertEquals(StopLabel.PROTECTED_LABEL, home.label)
        assertFalse(home.isSet)
        assertTrue(home.isProtected)

        val work = StopLabel.defaults[1]
        assertEquals("Work", work.label)
        assertFalse(work.isSet)
        assertFalse(work.isProtected)
    }

    @Test
    fun `PROTECTED_LABEL constant is exactly Home`() {
        // Pinned because RealSandook + ViewModel + UI all check
        // .equals(PROTECTED_LABEL, ignoreCase = true) — renaming this without
        // updating callers would silently break the protection.
        assertEquals("Home", StopLabel.PROTECTED_LABEL)
    }

    // endregion

    // region copy semantics

    @Test
    fun `copy with stopId and stopName makes label set`() {
        // The VM uses copy() to optimistically attach a stop in AssignLabelStop —
        // make sure that actually flips isSet to true.
        val unset = StopLabel(emoji = "🏠", label = "Home")
        val set = unset.copy(stopId = "stop_central", stopName = "Central Station")
        assertTrue(set.isSet)
        assertNotNull(set.toStopItem())
    }

    @Test
    fun `copy with null stopId clears the assignment`() {
        // ClearLabelStop is implemented as copy(stopId = null, stopName = null).
        val set = StopLabel(
            emoji = "🏠",
            label = "Home",
            stopId = "stop_central",
            stopName = "Central Station",
        )
        val cleared = set.copy(stopId = null, stopName = null)
        assertFalse(cleared.isSet)
        assertNull(cleared.toStopItem())
    }

    // endregion
}
