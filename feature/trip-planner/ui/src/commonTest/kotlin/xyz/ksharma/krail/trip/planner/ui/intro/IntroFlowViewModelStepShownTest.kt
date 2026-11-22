package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals

class IntroFlowViewModelStepShownTest {

    private fun IntroRig.screenViews(): List<Any?> =
        analytics.getTrackedEvents("view_screen").map { it.properties?.get("name") }

    @Test
    fun `showing step 1 reports the Intro screen once, however often it is shown`() = krailRunTest {
        val rig = IntroRig(this)

        repeat(3) { rig.viewModel.onStepShown(IntroStep.PARK_RIDE) }

        assertEquals(listOf<Any?>("Intro"), rig.screenViews())
    }

    @Test
    fun `step 2 reports its own screen, and going back is a new visit to step 1`() = krailRunTest {
        val rig = IntroRig(this)

        rig.viewModel.onStepShown(IntroStep.PARK_RIDE)
        rig.viewModel.onStepShown(IntroStep.DESTINATION)
        rig.viewModel.onStepShown(IntroStep.DESTINATION)
        rig.viewModel.onStepShown(IntroStep.PARK_RIDE)

        assertEquals(listOf<Any?>("Intro", "IntroFirstTrip", "Intro"), rig.screenViews())
    }

    @Test
    fun `going from the theme screen to Park and Ride reports the Intro once, and back reports nothing more`() =
        krailRunTest {
            val rig = IntroRig(this)

            rig.viewModel.onStepShown(IntroStep.THEME)
            rig.viewModel.onStepShown(IntroStep.PARK_RIDE)
            rig.viewModel.onStepShown(IntroStep.THEME)
            rig.viewModel.onStepShown(IntroStep.PARK_RIDE)
            rig.viewModel.onStepShown(IntroStep.DESTINATION)

            assertEquals(listOf<Any?>("Intro", "IntroFirstTrip"), rig.screenViews())
        }

    @Test
    fun `the readiness the last button waits on is the one the data layer reports`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
        assertEquals(StopsReadiness.Loading, rig.viewModel.stopsReadiness.value)

        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))

        assertEquals(StopsReadiness.Ready(stopCount = 100), rig.viewModel.stopsReadiness.value)
    }
}
