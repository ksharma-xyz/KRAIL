package xyz.ksharma.krail.core.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The event bus that carries a picked stop back across a pane boundary.
 *
 * It is one shared singleton with one channel per result key, and nothing about that is
 * type-checked: the key is `T::class.toString()`, the channel is `Channel<Any?>`, and the
 * receiving side casts. Everything that can go wrong with it goes wrong at runtime, on a
 * tablet, in the one flow nobody re-tests by hand. These pin the three behaviours screens
 * actually rely on.
 *
 * Note on the stand-in result types below. The real payloads (`StopSelectedResult`,
 * `TimetableStopChangedResult`) live in `:feature:trip-planner:ui`, which depends on this
 * module — importing them here would invert the dependency. The mechanism under test is the
 * key derivation, not the payload shape, so two local types stand in for them and are named
 * after the pair whose separation is the point.
 */
class ResultEventBusTest {

    @Test
    fun twoResultTypesNeverCrossDeliver() = runTest {
        // This is the entire reason TimetableStopChangedResult exists as a second type rather
        // than a flag on the first. SavedTrips listens for a stop pick on one channel; the
        // timetable's "change origin" search sends on another. Collapsing them means a rider
        // swapping one leg of a trip also rewrites the home screen's From field behind them.
        val bus = ResultEventBus.getInstance()
        bus.removeResult<StopSelected>()
        bus.removeResult<TimetableStopChanged>()

        bus.sendResult(result = StopSelected(stopId = HOME_STOP_ID))

        assertNull(
            bus.getResultFlow<TimetableStopChanged>(),
            "sending a StopSelected must not create or fill the TimetableStopChanged channel",
        )

        bus.sendResult(result = TimetableStopChanged(stopId = WORK_STOP_ID))

        // The cast is the production shape, not a shortcut. Channels are `Channel<Any?>` and
        // the flow the bus hands back is untyped: the reified T only ever picks the key, and
        // every real receiver casts the payload the same way. A wrong key therefore surfaces
        // as a ClassCastException in a rider's session, which is the other half of why these
        // two types must never share a channel.
        assertEquals(
            HOME_STOP_ID,
            (assertNotNull(bus.getResultFlow<StopSelected>()).first() as StopSelected).stopId,
        )
        assertEquals(
            WORK_STOP_ID,
            (
                assertNotNull(bus.getResultFlow<TimetableStopChanged>())
                    .first() as TimetableStopChanged
                ).stopId,
        )
    }

    @Test
    fun aResultSentWithNoReceiverWaitsForTheNextOne() = runTest {
        // PINNED AS IT IS, NOT AS IT SHOULD BE.
        //
        // The channel is BUFFERED and outlives every collector, so a result sent while nothing
        // is listening is not dropped: it sits in the buffer and is handed to whoever collects
        // that key next. That is what makes the pane handoff work at all — the sender navigates
        // back before the receiver's LaunchedEffect has restarted.
        //
        // It is also the stale-replay class flagged in the audit (D13-adjacent): the "next
        // collector" is not necessarily the screen the result was meant for, and after process
        // death and restore it can be a screen the rider reached by a completely different
        // route, which then acts on a stop they picked in a previous session. Nothing here
        // fixes that. This test exists so that a fix is a deliberate, visible change to a
        // documented behaviour rather than a silent one.
        val bus = ResultEventBus.getInstance()
        bus.removeResult<StopSelected>()

        bus.sendResult(result = StopSelected(stopId = HOME_STOP_ID))

        val delivered = assertNotNull(bus.getResultFlow<StopSelected>()).first() as StopSelected

        assertEquals(HOME_STOP_ID, delivered.stopId)
    }

    @Test
    fun concurrentSendsBothArrive() = runTest {
        // sendResult creates the channel lazily with a check-then-act on a plain map, so two
        // senders racing on the same key could each see it absent and the second could replace
        // the first's channel, taking the first result with it. Real threads, not the test
        // scheduler: a single-threaded dispatcher cannot interleave the two halves of that
        // check and would report green whatever the map does.
        repeat(RACE_ATTEMPTS) {
            val bus = ResultEventBus.getInstance()
            bus.removeResult<StopSelected>()

            withContext(Dispatchers.Default) {
                listOf(
                    async { bus.sendResult(result = StopSelected(stopId = HOME_STOP_ID)) },
                    async { bus.sendResult(result = StopSelected(stopId = WORK_STOP_ID)) },
                ).awaitAll()
            }

            val received = assertNotNull(bus.getResultFlow<StopSelected>())
                .take(2)
                .toList()
                .map { (it as StopSelected).stopId }
                .toSet()

            assertEquals(
                setOf(HOME_STOP_ID, WORK_STOP_ID),
                received,
                "both concurrent sends must survive; attempt $it lost one",
            )
        }
    }

    private data class StopSelected(val stopId: String)

    private data class TimetableStopChanged(val stopId: String)

    private companion object {
        const val HOME_STOP_ID = "2147"
        const val WORK_STOP_ID = "200070"

        // Enough attempts that a lost-update window of a few instructions shows up, few enough
        // that the test stays fast.
        const val RACE_ATTEMPTS = 200
    }
}
