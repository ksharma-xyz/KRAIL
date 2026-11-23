package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals

class IntroFlowViewModelStepShownTest {

    private fun IntroRig.screenViews(): List<Any?> =
        analytics.getTrackedEvents("view_screen").map { it.properties?.get("name") }

    @Test
    fun `showing step 1 reports its screen once, however often it is shown`() = krailRunTest {
        val rig = IntroRig(this)

        repeat(3) { rig.viewModel.onStepShown(IntroStep.PARK_RIDE) }

        assertEquals(listOf<Any?>("IntroParkRide"), rig.screenViews())
    }

    @Test
    fun `each step reports its own screen, and showing one again reports nothing`() = krailRunTest {
        val rig = IntroRig(this)

        rig.viewModel.onStepShown(IntroStep.PARK_RIDE)
        rig.viewModel.onStepShown(IntroStep.DESTINATION)
        rig.viewModel.onStepShown(IntroStep.DESTINATION)
        rig.viewModel.onStepShown(IntroStep.PARK_RIDE)

        assertEquals(listOf<Any?>("IntroParkRide", "IntroDestination"), rig.screenViews())
    }

    @Test
    fun `walking the flow reports five screens, one for each step, once each and in order`() = krailRunTest {
        val rig = IntroRig(this)

        IntroStep.entries.forEach { step ->
            rig.viewModel.onStepShown(step)
            rig.viewModel.onStepShown(step)
        }

        assertEquals(
            listOf<Any?>("IntroParkRide", "IntroDestination", "IntroOrigin", "IntroConfirm", "IntroTheme"),
            rig.screenViews(),
        )
    }

    @Test
    fun `a turn of the screen shows the same step again and reports nothing`() = krailRunTest {
        val rig = IntroRig(this)
        IntroStep.entries.forEach { rig.viewModel.onStepShown(it) }
        val before = rig.screenViews()

        // A rotation builds the screen again over the same view model, which shows the step it is on once more.
        repeat(2) { rig.viewModel.onStepShown(IntroStep.THEME) }

        assertEquals(before, rig.screenViews())
    }

    @Test
    fun `the theme screen on its own reports the theme screen, as it does after the process was killed`() =
        krailRunTest {
            val rig = IntroRig(this)

            repeat(2) { rig.viewModel.onStepShown(IntroStep.THEME) }

            assertEquals(listOf<Any?>("IntroTheme"), rig.screenViews())
        }

    @Test
    fun `no step reports either of the older Intro names`() = krailRunTest {
        val rig = IntroRig(this)

        IntroStep.entries.forEach { rig.viewModel.onStepShown(it) }

        assertEquals(emptyList<Any?>(), rig.screenViews().filter { it == "Intro" || it == "IntroFirstTrip" })
    }

    @Test
    fun `only the view_screen event is sent for a screen, no other event name`() = krailRunTest {
        val rig = IntroRig(this)

        IntroStep.entries.forEach { rig.viewModel.onStepShown(it) }

        assertEquals(setOf("view_screen"), rig.analytics.events.map { it.name }.toSet())
        assertEquals(IntroStep.entries.size, rig.analytics.events.size)
    }
}
