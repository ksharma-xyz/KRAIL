package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
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
