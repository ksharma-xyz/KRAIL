package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class IntroStopsOpsTest {

    private val train = listOf(TransportMode.Train)
    private val trainAndMetro = listOf(TransportMode.Train, TransportMode.Metro)
    private val found = listOf(
        ListedStop(REDFERN_STOP, train),
        ListedStop(CENTRAL_STOP, trainAndMetro),
    )

    // region nearby

    @Test
    fun `stations found are kept as ids and names, in the order found, with their modes for the badges`() {
        val state = IntroState(step = IntroStep.DESTINATION).withNearby(found)

        assertEquals(listOf(REDFERN_STOP, CENTRAL_STOP), state.nearbyStops)
        assertEquals<List<TransportMode>?>(train, state.stopModes["201510"])
        assertEquals<List<TransportMode>?>(trainAndMetro, state.stopModes["200060"])
    }

    @Test
    fun `stations found while the rider is before the origin screen are kept, on every screen before it`() {
        listOf(IntroStep.THEME, IntroStep.PARK_RIDE, IntroStep.DESTINATION).forEach { step ->
            assertEquals(2, IntroState(step = step).withNearby(found).nearbyStops.size, "$step")
        }
    }

    @Test
    fun `stations found once the rider is on the origin screen or past it change nothing, so the rows never move`() {
        listOf(IntroStep.ORIGIN, IntroStep.CONFIRM).forEach { step ->
            val state = IntroState(step = step)

            assertSame(state, state.withNearby(found), "$step")
        }
    }

    @Test
    fun `no stations found leaves the list empty and the curated list in use`() {
        assertEquals(emptyList(), IntroState(step = IntroStep.DESTINATION).withNearby(emptyList()).nearbyStops)
    }

    // endregion

    // region modes

    @Test
    fun `modes are added to the ones known and nothing is removed`() {
        val known = IntroState().withModes(mapOf("1" to train))

        val more = known.withModes(mapOf("2" to trainAndMetro))

        assertEquals(mapOf("1" to train, "2" to trainAndMetro), more.stopModes)
    }

    @Test
    fun `a later answer for a stop replaces its modes, and no modes at all is the same state`() {
        val known = IntroState().withModes(mapOf("1" to train))

        assertEquals<List<TransportMode>?>(trainAndMetro, known.withModes(mapOf("1" to trainAndMetro)).stopModes["1"])
        assertSame(known, known.withModes(emptyMap()))
    }

    // endregion

    // region the typed search

    @Test
    fun `typing sets the text and keeps what was found until the new answer comes`() {
        val showing = IntroState(query = "cen", search = IntroSearchPhase.Results(persistentListOf(CENTRAL_STOP)))

        val typed = showing.withQuery("cent")

        assertEquals("cent", typed.query)
        assertEquals(showing.search, typed.search)
    }

    @Test
    fun `clearing the text shows the suggestions again at once, whatever was found`() {
        listOf(
            IntroSearchPhase.Results(persistentListOf(CENTRAL_STOP)),
            IntroSearchPhase.NoMatch,
            IntroSearchPhase.Idle,
        ).forEach { phase ->
            val cleared = IntroState(query = "cen", search = phase).withQuery("")

            assertEquals(IntroSearchPhase.Idle, cleared.search, "$phase")
            assertEquals("", cleared.query)
        }
        assertEquals(IntroSearchPhase.Idle, IntroState(search = IntroSearchPhase.NoMatch).withQuery("   ").search)
    }

    @Test
    fun `an answer for the text that is in the field is shown, as results or as no match`() {
        val typed = IntroState(query = "cen")

        val results = typed.withSearchResult("cen", listOf(ListedStop(CENTRAL_STOP, trainAndMetro)))
        val none = typed.withSearchResult("cen", emptyList())

        assertEquals(IntroSearchPhase.Results(persistentListOf(CENTRAL_STOP)), results.search)
        assertEquals<List<TransportMode>?>(trainAndMetro, results.stopModes["200060"])
        assertEquals(IntroSearchPhase.NoMatch, none.search)
    }

    @Test
    fun `an answer for text the rider has typed past is dropped`() {
        val typed = IntroState(query = "cent", search = IntroSearchPhase.Idle)

        assertSame(typed, typed.withSearchResult("cen", found))
    }

    @Test
    fun `an answer with the field empty is dropped, because blank text is no search`() {
        val empty = IntroState(query = "")

        assertSame(empty, empty.withSearchResult("", found))
    }

    @Test
    fun `a search that failed leaves the suggestions showing`() {
        val typed = IntroState(query = "cen", search = IntroSearchPhase.NoMatch)

        assertEquals(IntroSearchPhase.Idle, typed.withSearchResult("cen", null).search)
    }

    // endregion
}
