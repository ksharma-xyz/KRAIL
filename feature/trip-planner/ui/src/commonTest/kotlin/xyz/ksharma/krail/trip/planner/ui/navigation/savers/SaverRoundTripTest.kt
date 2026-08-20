package xyz.ksharma.krail.trip.planner.ui.navigation.savers

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A Saver only ever runs on a device at the moment the Activity is destroyed, which is also
 * the moment nobody is watching. These round trips are what stands in for that.
 */
class StopDisplaySaverTest {

    private val saver = stopDisplaySaver()

    @Test
    fun `a labelled stop comes back whole`() {
        val stop = StopDisplay(stopId = "200060", name = "Central Station", label = "Home")

        assertEquals(stop, saver.roundTrip(stop))
    }

    @Test
    fun `an unlabelled stop comes back with no label rather than an empty one`() {
        val stop = StopDisplay(stopId = "200060", name = "Central Station")

        val restored = saver.roundTrip(stop)

        assertEquals(stop, restored)
        assertNull(restored?.label, "An empty label would render as a stop labelled nothing")
    }

    @Test
    fun `no open sheet stays no open sheet`() {
        assertNull(saver.roundTrip(null))
    }

    @Test
    fun `the saved form is something a bundle can hold`() {
        val saved = saver.saveForTest(
            StopDisplay(stopId = "200060", name = "Central Station", label = "Home"),
        )

        assertTrue(
            saved != null && saved.all { it is String },
            "Anything but a primitive or a String would need a Saver of its own on the way out",
        )
    }
}

class ParkRideExpansionSaverTest {

    private val saver = parkRideExpansionSaver()

    @Test
    fun `the open cards come back open and the closed ones stay closed`() {
        val restored = saver.roundTrip(
            expansions("2153478" to true, "9999999" to false, "2155382" to true),
        )

        assertEquals(setOf("2153478", "2155382"), restored?.filterValues { it }?.keys)
        assertEquals(
            false,
            restored?.get("9999999") ?: false,
            "A card that was never open does not need storing to read as closed",
        )
    }

    @Test
    fun `nothing open saves as nothing`() {
        assertEquals(emptyList(), saver.saveForTest(expansions("2153478" to false)))
    }

    private fun expansions(vararg entries: Pair<String, Boolean>): SnapshotStateMap<String, Boolean> =
        mutableStateMapOf<String, Boolean>().apply { putAll(entries) }
}

/**
 * The real registry decides what a Bundle will take. A round-trip test is asking a different
 * question, so it accepts whatever it is handed.
 */
private object AcceptEverything : SaverScope {
    override fun canBeSaved(value: Any): Boolean = true
}

private fun <Original, Saveable : Any> Saver<Original, Saveable>.saveForTest(
    value: Original,
): Saveable? = with(this) { AcceptEverything.save(value) }

private fun <Original, Saveable : Any> Saver<Original, Saveable>.roundTrip(
    value: Original,
): Original? = saveForTest(value)?.let(::restore)
