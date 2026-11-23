package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * No state of the Intro is a dead end. Every place the Intro can be in is found by searching all of them, breadth first,
 * through the real view model and the fakes at its edges: every event, the passing of time, the Park & Ride availability
 * coming back with a reading or without, and the process being killed and the Intro reopened from what the system or the
 * device kept. The stops data is frozen at each of Unknown, Loading, Failed and Ready, and in the first three it never
 * answers, which is the situation that stranded a rider on the last screen. In every place:
 *
 * - the screen is never empty: there is a bar, with a live button on it (there is no disabled one to find),
 * - every button that is shown does something when pressed, so a press is never a no-op,
 * - some path of presses, time and answers from the data leads to a finished Intro, so the rider can always finish, and
 * - the step moves on only with the rider's own answer on it, apart from the Park & Ride screen passing itself over when
 *   there is no station near and no usable location.
 */
class IntroLivenessTest {

    @Test
    fun `with the stops Unknown for good, every place can finish, every button does something, and a step moves only on an answer`() =
        krailRunTest { checkEveryPlace(StopsReadiness.Unknown) }

    @Test
    fun `with the stops Loading for good, every place can finish, every button does something, and a step moves only on an answer`() =
        krailRunTest { checkEveryPlace(StopsReadiness.Loading) }

    @Test
    fun `with the stops Failed, every place can finish, every button does something, and a step moves only on an answer`() =
        krailRunTest { checkEveryPlace(StopsReadiness.Failed(attempt = 4, reason = "disk")) }

    @Test
    fun `with the stops Ready, every place can finish, every button does something, and a step moves only on an answer`() =
        krailRunTest { checkEveryPlace(StopsReadiness.Ready(stopCount = 100)) }

    private fun KrailTestScope.checkEveryPlace(readiness: StopsReadiness) {
        val graph = exploreWorld(readiness)
        println("liveness: readiness $readiness, ${graph.nodes.size} places, ${graph.edges.size} moves, ${graph.restores.size} reopenings")
        val problems = mutableListOf<String>()
        fun WorldNode.describe(): String {
            val recipe = graph.nodes.getValue(this)
            return "${state.step} answer=${state.parkRideAnswer} card=${state.parkRideCard::class.simpleName} " +
                "decision=${state.tripDecision} stop=${state.selectedStop?.name} theme=${state.selectedThemeId} " +
                "saved=${state.saved} finale=$finaleStarted done=$finished via ${recipe.root::class.simpleName} " +
                recipe.path.joinToString(" ") { it.short() }
        }

        // The search found the places that matter, so none of the rest passes by looking at nothing.
        assertTrue(graph.nodes.size > 40, "only ${graph.nodes.size} places were found")
        IntroStep.entries.forEach { step ->
            assertTrue(graph.nodes.keys.any { it.state.step == step }, "no place on $step was found")
        }
        assertTrue(graph.nodes.keys.any { it.finished }, "no path finished the Intro")

        graph.nodes.keys.forEach { place ->
            val bar = introBar(place.state)
            // The screen is never empty, and a restored place is held to it as much as any other.
            if (bar == null) {
                problems += "no bar, so no way to answer: ${place.describe()}"
                return@forEach
            }
            val pressable = listOfNotNull(
                bar.left.takeIf { bar.shown },
                bar.right.takeIf { bar.shown && bar.rightShown },
            )
            // Every shown button has words and an event of its own, and does something when it is pressed. Once the finish
            // is pressed the finale covers the screen and a second press does nothing more, which is meant.
            pressable.filter { !place.finaleStarted }.forEach { button ->
                if (button.label.isBlank() || button.spokenLabel.isBlank()) problems += "a button with no words: ${place.describe()}"
                val pressed = graph.edgesFrom(place).singleOrNull { it.action == WorldAction.Send(button.event) }
                when {
                    pressed == null -> problems += "${button.label} was never pressed: ${place.describe()}"
                    pressed.to == place -> problems += "${button.label} does nothing here: ${place.describe()}"
                }
            }
        }

        // Some path leads to the finish from every place the Intro can be in, the killed and reopened ones included.
        val canFinish = graph.placesThatCanFinish()
        graph.nodes.keys.filter { it !in canFinish }.forEach { problems += "cannot finish from: ${it.describe()}" }

        // A step moves on only with an answer of the rider's own on it, apart from the defined skips of Park & Ride.
        graph.edges.filter { it.from.state.step != it.to.state.step }.forEach { edge ->
            if (!edge.action.mayChange(edge.from.state.step)) {
                problems += "${edge.action.short()} moved ${edge.from.state.step} to ${edge.to.state.step} with no answer: " +
                    edge.from.describe()
            }
        }

        assertEquals(emptyList(), problems.take(MAX_REPORTED), "readiness $readiness, ${problems.size} problems")
    }

    // The answers that may end a step, and the automatic skips of the Park & Ride screen. Nothing else may change the step.
    private fun WorldAction.mayChange(step: IntroStep): Boolean = when (step) {
        IntroStep.PARK_RIDE -> this is WorldAction.Send && event.let {
            it is IntroUiEvent.AnswerParkRide ||
                it == IntroUiEvent.DeclineParkRide ||
                it == IntroUiEvent.AddParkRideAndContinue ||
                // No usable location and no station near are passed over by themselves.
                it is IntroUiEvent.LocationResultReceived
        } || this is WorldAction.Advance
        IntroStep.DESTINATION, IntroStep.ORIGIN -> this == WorldAction.Send(IntroUiEvent.ConfirmStop)
        IntroStep.CONFIRM -> this is WorldAction.Send && event is IntroUiEvent.AnswerSaveTrip
        IntroStep.THEME -> false
    }

    private fun WorldAction.short(): String = when (this) {
        is WorldAction.Send -> event.toString().substringAfterLast('.')
        is WorldAction.Advance -> "wait ${ms}ms"
        is WorldAction.LandAvailability -> "availability(reading=$withReading)"
    }

    private companion object {
        const val MAX_REPORTED = 12
    }
}
