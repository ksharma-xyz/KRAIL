package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val WORK_STOP = StopItem(stopId = "10102", stopName = "Town Hall")

/** Assigns [stop] to [label]; a null stop is a label the rider made but never pointed anywhere. */
private fun FakeSandook.putLabel(label: String, stop: StopItem?) {
    upsertStopLabel(
        label = label,
        emoji = "\uD83D\uDCCD",
        stopId = stop?.stopId,
        stopName = stop?.stopName,
        sortOrder = 0L,
    )
}

class StopLabelTextResolverTest {

    private val sandook = FakeSandook()
    private val resolver = StopLabelTextResolver(sandook)

    @Test
    fun `resolves a label to the stop it points at`() = runTest {
        sandook.putLabel("work", WORK_STOP)

        assertEquals(WORK_STOP, resolver.resolve("work"))
    }

    @Test
    fun `label matching ignores case and surrounding space`() = runTest {
        sandook.putLabel("Work", WORK_STOP)

        assertEquals(WORK_STOP, resolver.resolve("  WORK "))
    }

    @Test
    fun `custom labels resolve the same way the built-in ones do`() = runTest {
        val gym = StopItem(stopId = "20001", stopName = "Redfern Station")
        sandook.putLabel("the gym", gym)

        assertEquals(gym, resolver.resolve("the gym"))
    }

    @Test
    fun `a label with no stop assigned yet resolves to null`() = runTest {
        sandook.putLabel("uni", null)

        assertNull(resolver.resolve("uni"))
    }

    @Test
    fun `a rider's own word for the place resolves to their label`() = runTest {
        // Reported: label is "Work", rider said "office by Monday morning", nothing resolved.
        sandook.putLabel("Work", WORK_STOP)

        assertEquals(WORK_STOP, resolver.resolve("office"))
        assertEquals(WORK_STOP, resolver.resolve("the job".removePrefix("the ")))
    }

    @Test
    fun `a literal label wins over a synonym of another one`() = runTest {
        // A rider with both has named two different stops, and "office" has to reach the one
        // they actually called Office.
        val office = StopItem(stopId = "30001", stopName = "Wynyard")
        sandook.putLabel("Work", WORK_STOP)
        sandook.putLabel("Office", office)

        assertEquals(office, resolver.resolve("office"))
        assertEquals(WORK_STOP, resolver.resolve("work"))
    }

    @Test
    fun `a synonym of a label the rider never pinned resolves to null`() = runTest {
        sandook.putLabel("Work", null)

        assertNull(resolver.resolve("office"))
    }

    @Test
    fun `partial matches are not labels`() = runTest {
        sandook.putLabel("home", WORK_STOP)

        // "homebush" is a stop name, not this rider's "home" label.
        assertNull(resolver.resolve("homebush"))
    }

    @Test
    fun `unknown text falls through`() = runTest {
        sandook.putLabel("work", WORK_STOP)

        assertNull(resolver.resolve("central"))
    }
}

class StopSearchTextResolverTest {

    private val stopResultsManager = FakeStopResultsManager().apply { spaceInsensitiveSearch = true }
    private val resolver = StopSearchTextResolver(stopResultsManager)

    @Test
    fun `a query inside a longer word is not a match`() = runTest {
        // The bug this exists for: the stop search offers "70 Powderworks Rd" for "work",
        // which is a fine row to show a rider choosing from a list and a terrible thing to
        // fill a field with on their behalf.
        assertNull(resolver.resolve("work"))
    }

    @Test
    fun `a whole word still matches`() = runTest {
        assertEquals(
            StopItem(stopId = "10101", stopName = "Central Station"),
            resolver.resolve("Central Station"),
        )
    }

    @Test
    fun `a word start still matches`() = runTest {
        // "central" prefixes "Central"; the rider does not have to say "Station".
        assertEquals(
            StopItem(stopId = "10101", stopName = "Central Station"),
            resolver.resolve("central"),
        )
    }

    @Test
    fun `every query word has to land somewhere`() = runTest {
        // "town" matches, "beach" does not, so the whole query does not.
        assertNull(resolver.resolve("town beach"))
    }

    @Test
    fun `a name written closed still finds the stop written open`() = runTest {
        // The bug this exists for: the search screen finds Town Hall for "townhall" instantly,
        // and this path told the rider no such stop existed. Word-by-word matching could not
        // see a name whose words the rider had run together.
        assertEquals(
            StopItem(stopId = "10102", stopName = "Town Hall"),
            resolver.resolve("townhall"),
        )
    }

    @Test
    fun `running words together does not let a query reach inside a word`() = runTest {
        // Joining runs of adjacent words must not reopen what the word-boundary rule closed.
        assertNull(resolver.resolve("work"))
    }

    @Test
    fun `nothing matching resolves to null`() = runTest {
        assertNull(resolver.resolve("Nonexistent Place"))
    }
}

class ChainedStopTextResolverTest {

    private val sandook = FakeSandook()
    private val stopResultsManager = FakeStopResultsManager()

    private val chain = ChainedStopTextResolver(
        listOf(
            StopLabelTextResolver(sandook),
            StopSearchTextResolver(stopResultsManager),
        ),
    )

    @Test
    fun `a label wins over a stop of the same name`() = runTest {
        // FakeStopResultsManager knows a stop called "Central Station"; the rider has also
        // labelled a different stop "central". Their own word wins.
        val theirCentral = StopItem(stopId = "99999", stopName = "Redfern Station")
        sandook.putLabel("central", theirCentral)

        assertEquals(theirCentral, chain.resolve("central"))
    }

    @Test
    fun `falls through to stop search when no label matches`() = runTest {
        assertEquals(
            StopItem(stopId = "10101", stopName = "Central Station"),
            chain.resolve("Central Station"),
        )
    }

    @Test
    fun `resolves to null when no capability can answer`() = runTest {
        assertNull(chain.resolve("Nonexistent Place"))
    }
}
