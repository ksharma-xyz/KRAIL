package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFinishAction.GettingReady
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFinishAction.LetsKrail
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFinishAction.Next
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFinishAction.SaveAndStart
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFinishAction.SkipForNow
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFinishAction.TryAgain
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * One row per state, written out in full: what the last button says must never be something the
 * code works out a second time here. Two guards keep a new state from being skipped, a new action
 * with no row and a state with no answer.
 */
class IntroFinishActionTest {

    private enum class Picks { NONE, START_ONLY, BOTH }

    private data class Row(
        val step: IntroStep,
        val saved: Boolean,
        val picks: Picks,
        val readiness: StopsReadiness,
        val expected: IntroFinishAction,
    ) {
        fun state() = IntroState(
            step = step,
            saved = saved,
            startStopId = "1".takeIf { picks != Picks.NONE },
            endStopId = "2".takeIf { picks == Picks.BOTH },
        )
    }

    private val unknown = StopsReadiness.Unknown
    private val loading = StopsReadiness.Loading
    private val ready = StopsReadiness.Ready(stopCount = 100)
    private val failed = StopsReadiness.Failed(attempt = 1, reason = "disk full")

    private val table = listOf(
        // Step 1 never waits on the stops, whatever their state.
        Row(IntroStep.PARK_RIDE, false, Picks.NONE, unknown, Next),
        Row(IntroStep.PARK_RIDE, false, Picks.NONE, loading, Next),
        Row(IntroStep.PARK_RIDE, false, Picks.NONE, ready, Next),
        Row(IntroStep.PARK_RIDE, false, Picks.NONE, failed, Next),
        // Step 2, stops not ready, whatever has been picked.
        Row(IntroStep.FIRST_TRIP, false, Picks.NONE, unknown, GettingReady),
        Row(IntroStep.FIRST_TRIP, false, Picks.START_ONLY, unknown, GettingReady),
        Row(IntroStep.FIRST_TRIP, false, Picks.BOTH, unknown, GettingReady),
        Row(IntroStep.FIRST_TRIP, false, Picks.NONE, loading, GettingReady),
        Row(IntroStep.FIRST_TRIP, false, Picks.START_ONLY, loading, GettingReady),
        Row(IntroStep.FIRST_TRIP, false, Picks.BOTH, loading, GettingReady),
        // Step 2, stops failed.
        Row(IntroStep.FIRST_TRIP, false, Picks.NONE, failed, TryAgain),
        Row(IntroStep.FIRST_TRIP, false, Picks.START_ONLY, failed, TryAgain),
        Row(IntroStep.FIRST_TRIP, false, Picks.BOTH, failed, TryAgain),
        // Step 2, stops ready. Only a complete pair is a trip.
        Row(IntroStep.FIRST_TRIP, false, Picks.NONE, ready, SkipForNow),
        Row(IntroStep.FIRST_TRIP, false, Picks.START_ONLY, ready, SkipForNow),
        Row(IntroStep.FIRST_TRIP, false, Picks.BOTH, ready, SaveAndStart),
        // Once saved there is nothing left to wait for, so readiness no longer matters.
        Row(IntroStep.FIRST_TRIP, true, Picks.BOTH, unknown, LetsKrail),
        Row(IntroStep.FIRST_TRIP, true, Picks.BOTH, loading, LetsKrail),
        Row(IntroStep.FIRST_TRIP, true, Picks.BOTH, ready, LetsKrail),
        Row(IntroStep.FIRST_TRIP, true, Picks.BOTH, failed, LetsKrail),
    )

    @Test
    fun `every state has the button the table says`() {
        table.forEach { row ->
            assertEquals(
                row.expected,
                introFinishAction(row.state(), row.readiness),
                "step=${row.step} saved=${row.saved} picks=${row.picks} readiness=${row.readiness}",
            )
        }
    }

    @Test
    fun `every action appears in the table`() {
        assertEquals(IntroFinishAction.entries.toSet(), table.map { it.expected }.toSet())
    }

    @Test
    fun `every step and readiness is exercised by the table`() {
        assertEquals(IntroStep.entries.toSet(), table.map { it.step }.toSet())
        assertEquals(
            setOf(unknown, loading, ready, failed).map { it::class }.toSet(),
            table.map { it.readiness::class }.toSet(),
        )
    }
}
