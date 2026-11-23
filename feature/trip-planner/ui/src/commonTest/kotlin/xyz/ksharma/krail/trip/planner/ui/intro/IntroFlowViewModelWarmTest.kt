package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The stops load is asked to start when the Intro opens, so it is normally Ready by the last screen and nothing there has
 * to wait for it. It is the launch's own call, so it starts nothing while the load is under way or done.
 */
class IntroFlowViewModelWarmTest {

    @Test
    fun `opening the Intro starts the stops load once when nothing has started it`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Unknown)

        rig.settle()

        assertEquals(1, rig.readiness.retryCallCount)
    }

    @Test
    fun `opening the Intro after the stops load failed asks for one more attempt, once`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Failed(attempt = 4, reason = "disk"))

        rig.settle()
        rig.send(IntroUiEvent.AnswerParkRide(yes = false))

        assertEquals(1, rig.readiness.retryCallCount)
    }

    @Test
    fun `opening the Intro while the stops load is under way or done calls nothing, so there is no second run`() =
        krailRunTest {
            listOf(StopsReadiness.Loading, StopsReadiness.Ready(stopCount = 100)).forEach { state ->
                val rig = IntroRig(this, initialReadiness = state)

                rig.settle()

                assertEquals(0, rig.readiness.retryCallCount, "readiness $state")
            }
        }
}
