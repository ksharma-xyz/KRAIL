package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * The dead end a rider met on a real phone, reproduced exactly and held shut: Park & Ride answered No, a stop picked for
 * each end, Yes save it, a colour picked, and the stops data never answering, Unknown or Loading. The last button used to be
 * off in that state and the only thing that could start the stops load ran when it was pressed.
 *
 * Now the button is live, and a press gives an answer at once, within 100 ms of virtual time, by starting the finale. The
 * trip and both flags are written at once too, and the app is not shown until the wait for the stops, which is bounded at 15
 * seconds, is over. The virtual clock is the test's, so the times are exact.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntroFinishNeverStuckTest {

    private class Timed(val effect: IntroEffect, val atMs: Long)

    private fun KrailTestScope.riderAtTheLastScreen(readiness: StopsReadiness): IntroRig {
        val rig = IntroRig(this, readiness)
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = false),
            IntroUiEvent.SelectStop(CENTRAL.toIntroStop()),
            IntroUiEvent.ConfirmStop,
            IntroUiEvent.SelectStop(TALLAWONG.toIntroStop()),
            IntroUiEvent.ConfirmStop,
            IntroUiEvent.AnswerSaveTrip(yes = true),
            IntroUiEvent.PickTheme(KrailThemeStyle.Bus.id),
        )
        assertEquals(IntroStep.THEME, rig.state.step)
        return rig
    }

    @Test
    fun `the last button is shown and live with the stops Unknown or Loading, and its press is answered at once`() =
        krailRunTest {
            listOf(StopsReadiness.Unknown, StopsReadiness.Loading).forEach { frozen ->
                val rig = riderAtTheLastScreen(frozen)
                val bar = requireNotNull(introBar(rig.state)) { "no bar on the last screen with the stops $frozen" }
                assertTrue(bar.shown, "the button is not shown with the stops $frozen")
                assertEquals(IntroUiEvent.ConfirmTheme, bar.right.event)
                val seen = mutableListOf<Timed>()
                val collecting = launch(UnconfinedTestDispatcher(scheduler)) {
                    rig.viewModel.effects.collect { seen += Timed(it, scheduler.currentTime) }
                }
                val pressedAt = scheduler.currentTime

                rig.send(bar.right.event)

                // The finale starts in the same instant as the press, so the rider is never left on a button that does nothing.
                assertEquals(listOf(IntroEffect.StartFinale), seen.map { it.effect }, "stops $frozen")
                val answeredAfter = seen.single().atMs - pressedAt
                assertTrue(answeredAfter <= RESPONSE_MS, "stops $frozen: the answer took $answeredAfter ms")

                // Within 100 ms the trip and the seen flag are written from the rider's own picks, and the app is not shown.
                pumpOnce(RESPONSE_MS)
                assertEquals(1, rig.sandook.selectAllTrips().size, "stops $frozen")
                assertEquals(1, rig.log.count("pref:KEY_HAS_SEEN_INTRO=true"), "stops $frozen")
                assertEquals(listOf(IntroEffect.StartFinale), seen.map { it.effect }, "stops $frozen")

                // The wait for the stops is bounded: the app is shown at 15 s, not a moment before, and never later.
                pumpOnce(FINISH_WAIT_TIMEOUT_MS - RESPONSE_MS - 1)
                assertEquals(listOf(IntroEffect.StartFinale), seen.map { it.effect }, "stops $frozen, 1 ms early")
                // The outcome's wait is read from the rig's own clock, which only moves when told.
                rig.testTime += FINISH_WAIT_TIMEOUT_MS.milliseconds
                pumpOnce(1)
                assertEquals(
                    listOf(IntroEffect.StartFinale, IntroEffect.NavigateToSavedTrips),
                    seen.map { it.effect },
                    "stops $frozen",
                )
                assertEquals(FINISH_WAIT_TIMEOUT_MS, seen.last().atMs - pressedAt, "stops $frozen")
                // Once at the finish to carry the load on, and from Unknown once more when the Intro opened.
                val expectedRetries = if (frozen == StopsReadiness.Unknown) 2 else 1
                assertEquals(expectedRetries, rig.readiness.retryCallCount, "the load carries on, stops $frozen")
                assertEquals("over_5s", rig.outcomeEvent().properties?.get("stopsWait"))
                collecting.cancel()
            }
        }

    @Test
    fun `if the stops arrive while the finale holds, the app is shown at once and not at the bound`() = krailRunTest {
        val rig = riderAtTheLastScreen(StopsReadiness.Loading)
        val seen = mutableListOf<Timed>()
        val collecting = launch(UnconfinedTestDispatcher(scheduler)) {
            rig.viewModel.effects.collect { seen += Timed(it, scheduler.currentTime) }
        }
        val pressedAt = scheduler.currentTime
        rig.send(IntroUiEvent.ConfirmTheme)

        pumpOnce(3_000L)
        assertEquals(listOf(IntroEffect.StartFinale), seen.map { it.effect })
        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        rig.send()

        assertEquals(listOf(IntroEffect.StartFinale, IntroEffect.NavigateToSavedTrips), seen.map { it.effect })
        assertEquals(3_000L, seen.last().atMs - pressedAt)
        collecting.cancel()
    }

    @Test
    fun `with the stops ready the finale starts and the app is shown in the same instant, with nothing to wait for`() =
        krailRunTest {
            val rig = riderAtTheLastScreen(StopsReadiness.Ready(stopCount = 100))
            val seen = mutableListOf<Timed>()
            val collecting = launch(UnconfinedTestDispatcher(scheduler)) {
                rig.viewModel.effects.collect { seen += Timed(it, scheduler.currentTime) }
            }
            val pressedAt = scheduler.currentTime

            rig.send(IntroUiEvent.ConfirmTheme)

            assertEquals(listOf(IntroEffect.StartFinale, IntroEffect.NavigateToSavedTrips), seen.map { it.effect })
            assertTrue(seen.all { it.atMs == pressedAt })
            assertEquals("none", rig.outcomeEvent().properties?.get("stopsWait"))
            collecting.cancel()
        }

    private companion object {
        const val RESPONSE_MS = 100L
    }
}
