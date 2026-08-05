package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchSessionStoreTest {

    private val store = RealSearchSessionStore()

    @Test
    fun `GIVEN a searched stop WHEN that trip is loaded THEN the session id is attached`() {
        store.recordSelection(searchSessionId = SESSION, stopId = CENTRAL)

        assertEquals(SESSION, store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL))
    }

    @Test
    fun `GIVEN the searched stop is the destination THEN the session id is still attached`() {
        store.recordSelection(searchSessionId = SESSION, stopId = TOWN_HALL)

        assertEquals(SESSION, store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL))
    }

    @Test
    fun `GIVEN a consumed session WHEN the same trip is loaded again THEN nothing is attached`() {
        store.recordSelection(searchSessionId = SESSION, stopId = CENTRAL)
        store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL)

        assertNull(store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL))
    }

    @Test
    fun `GIVEN a search WHEN an unrelated trip is loaded THEN nothing is attached`() {
        store.recordSelection(searchSessionId = SESSION, stopId = CENTRAL)

        assertNull(store.consumeFor(fromStopId = "200080", toStopId = TOWN_HALL))
    }

    @Test
    fun `GIVEN an unrelated trip consumed the pending selection THEN it does not resurface`() {
        store.recordSelection(searchSessionId = SESSION, stopId = CENTRAL)
        store.consumeFor(fromStopId = "200080", toStopId = TOWN_HALL)

        assertNull(store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL))
    }

    @Test
    fun `GIVEN a recent or map selection THEN no session is attached`() {
        store.recordSelection(searchSessionId = null, stopId = CENTRAL)

        assertNull(store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL))
    }

    @Test
    fun `GIVEN a search followed by a recent tap THEN the earlier session is cleared`() {
        store.recordSelection(searchSessionId = SESSION, stopId = CENTRAL)
        store.recordSelection(searchSessionId = null, stopId = TOWN_HALL)

        assertNull(store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL))
    }

    @Test
    fun `GIVEN two searched stops THEN the later selection wins`() {
        store.recordSelection(searchSessionId = SESSION, stopId = CENTRAL)
        store.recordSelection(searchSessionId = LATER_SESSION, stopId = TOWN_HALL)

        assertEquals(LATER_SESSION, store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL))
    }

    @Test
    fun `GIVEN nothing recorded THEN nothing is attached`() {
        assertNull(store.consumeFor(fromStopId = CENTRAL, toStopId = TOWN_HALL))
    }

    private companion object {
        const val SESSION = "a1b2c3d4e5f60718"
        const val LATER_SESSION = "f0e1d2c3b4a59687"
        const val CENTRAL = "200060"
        const val TOWN_HALL = "200070"
    }
}
