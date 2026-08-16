package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LabelWordGuardTest {

    /** Stands in for the stop search, which will match anything it is asked about. */
    private class AlwaysMatches(private val stop: StopItem) : StopTextResolver {
        override val name: String = "always"
        var lastQuery: String? = null
        override suspend fun resolve(query: String): StopItem? {
            lastQuery = query
            return stop
        }
    }

    private val workersClub = StopItem(stopId = "99", stopName = "Blacktown Workers Club")

    @Test
    fun `a label word never reaches the stop search`() = runTest {
        // The reported bug, exactly: no Work label set, rider says "work", and the search
        // matched the letters in "Workers".
        val search = AlwaysMatches(workersClub)

        val result = LabelWordGuard(search).resolve("work")

        assertNull(result, "\"work\" must not resolve to a stop that merely contains the letters")
        assertNull(search.lastQuery, "the search should never have been asked")
    }

    @Test
    fun `every label word is guarded`() = runTest {
        val search = AlwaysMatches(workersClub)
        val guard = LabelWordGuard(search)

        listOf("home", "house", "work", "office", "job", "workplace", "uni", "university", "gym")
            .forEach { word ->
                assertNull(guard.resolve(word), "\"$word\" should not be searched for")
            }
    }

    @Test
    fun `case and space do not sneak a label word past the guard`() = runTest {
        val guard = LabelWordGuard(AlwaysMatches(workersClub))

        assertNull(guard.resolve("  WORK "))
    }

    @Test
    fun `an ordinary place name passes straight through`() = runTest {
        val search = AlwaysMatches(workersClub)

        val result = LabelWordGuard(search).resolve("Blacktown Workers Club")

        assertEquals(workersClub, result)
        assertEquals("Blacktown Workers Club", search.lastQuery)
    }

    @Test
    fun `a stop whose name merely contains a label word is still findable`() = runTest {
        // The guard is about the WORD being a label word, not about the stop. Somebody typing
        // the club's actual name has every right to find it.
        val search = AlwaysMatches(workersClub)

        val result = LabelWordGuard(search).resolve("workers club")

        assertEquals(workersClub, result)
    }
}
