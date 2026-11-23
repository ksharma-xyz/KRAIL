package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Nothing moves on without the rider's own answer. Each event is valid on exactly one step, or on the two stop screens, and
 * is dropped everywhere else, and an event that says the screen it was pressed on is dropped when the Intro is no longer on
 * it. The table below is the specification, written out here and not read from the code, so a change to the code's own
 * table fails against it.
 */
class IntroTapSafetyTest {

    private val train = KrailThemeStyle.Train.id
    private val bus = KrailThemeStyle.Bus.id
    private val denied = IntroUiEvent.LocationResultReceived(LocationResult.DENIED, null)
    private val grantedNear = IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA)

    // Every shape of event, with the one step, or the two, each is for.
    private val table: List<Pair<IntroUiEvent, Set<IntroStep>>> = listOf(
        IntroUiEvent.AnswerParkRide(yes = true) to setOf(IntroStep.PARK_RIDE),
        IntroUiEvent.AnswerParkRide(yes = false) to setOf(IntroStep.PARK_RIDE),
        grantedNear to setOf(IntroStep.PARK_RIDE),
        denied to setOf(IntroStep.PARK_RIDE),
        IntroUiEvent.AddParkRideAndContinue to setOf(IntroStep.PARK_RIDE),
        IntroUiEvent.DeclineParkRide to setOf(IntroStep.PARK_RIDE),
        IntroUiEvent.AcknowledgeNoStation to setOf(IntroStep.PARK_RIDE),
        IntroUiEvent.SelectStop(CENTRAL_STOP) to setOf(IntroStep.DESTINATION, IntroStep.ORIGIN),
        IntroUiEvent.ConfirmStop to setOf(IntroStep.DESTINATION, IntroStep.ORIGIN),
        IntroUiEvent.QueryChanged("cen") to setOf(IntroStep.DESTINATION, IntroStep.ORIGIN),
        IntroUiEvent.AnswerSaveTrip(yes = true) to setOf(IntroStep.CONFIRM),
        IntroUiEvent.AnswerSaveTrip(yes = false) to setOf(IntroStep.CONFIRM),
        IntroUiEvent.PickTheme(bus) to setOf(IntroStep.THEME),
        IntroUiEvent.ConfirmTheme to setOf(IntroStep.THEME),
    )
    private val allEvents = table.map { it.first }

    private val withBothStops = IntroState(
        parkRideAnswer = ParkRideAnswer.NO,
        startStopId = TALLAWONG.stopId,
        startName = TALLAWONG.name,
        endStopId = CENTRAL.stopId,
        endName = CENTRAL.name,
    )

    // The shapes a screen can be in. A stop is selected on a list screen in the second shape, and a theme on the last.
    private val shapes = mapOf(
        IntroStep.PARK_RIDE to listOf("asking", "locating", "card", "no station near"),
        IntroStep.DESTINATION to listOf("nothing selected", "a stop selected"),
        IntroStep.ORIGIN to listOf("nothing selected", "a stop selected"),
        IntroStep.CONFIRM to listOf("the trip"),
        IntroStep.THEME to listOf("no theme", "a theme"),
    )

    private fun KrailTestScope.riderOn(step: IntroStep, shape: String): IntroRig {
        val rig = when (step) {
            IntroStep.PARK_RIDE -> IntroRig(this)
            IntroStep.DESTINATION -> IntroRig(this, handle = savedAt(IntroState(step = step, parkRideAnswer = ParkRideAnswer.NO)))
            IntroStep.ORIGIN -> IntroRig(
                this,
                handle = savedAt(
                    IntroState(step = step, parkRideAnswer = ParkRideAnswer.NO, endStopId = CENTRAL.stopId, endName = CENTRAL.name),
                ),
            )
            IntroStep.CONFIRM -> IntroRig(this, handle = savedAt(withBothStops.copy(step = step)))
            IntroStep.THEME -> IntroRig(
                this,
                handle = savedAt(withBothStops.copy(step = step, tripDecision = TripDecision.SAVED)),
            )
        }
        when (shape) {
            "locating" -> rig.send(IntroUiEvent.AnswerParkRide(yes = true))
            "card" -> rig.showBellaVista()
            "no station near" -> rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
            )
            "a stop selected" -> rig.send(IntroUiEvent.SelectStop(WYNYARD_STOP))
            "a theme" -> rig.send(IntroUiEvent.PickTheme(train))
        }
        rig.settle()
        return rig
    }

    @Test
    fun `the table covers every step and every event type, so a new one is a row here`() {
        assertEquals(IntroStep.entries.toSet(), shapes.keys)
        assertEquals(
            setOf(
                IntroUiEvent.AnswerParkRide::class, IntroUiEvent.LocationResultReceived::class,
                IntroUiEvent.AddParkRideAndContinue::class, IntroUiEvent.DeclineParkRide::class,
                IntroUiEvent.AcknowledgeNoStation::class, IntroUiEvent.SelectStop::class, IntroUiEvent.ConfirmStop::class, IntroUiEvent.QueryChanged::class,
                IntroUiEvent.AnswerSaveTrip::class, IntroUiEvent.PickTheme::class, IntroUiEvent.ConfirmTheme::class,
            ),
            allEvents.map { it::class }.toSet(),
        )
        table.forEach { (event, steps) -> assertEquals(steps, event.validSteps(), "$event") }
    }

    @Test
    fun `an event from a step it is not for changes nothing at all, in every shape of every screen`() = krailRunTest {
        IntroStep.entries.forEach { step ->
            shapes.getValue(step).forEach { shape ->
                table.filter { (_, steps) -> step !in steps }.forEach { (event, _) ->
                    val rig = riderOn(step, shape)
                    val before = rig.state
                    val logBefore = rig.log.entries.toList()

                    rig.send(event)

                    assertEquals(before, rig.state, "$event changed the $step screen, $shape")
                    assertEquals(logBefore, rig.log.entries, "$event wrote or reported something on $step, $shape")
                    assertEquals(0, rig.sandook.selectAllTrips().size, "$event saved a trip on $step, $shape")
                    rig.clearViewModel()
                }
            }
        }
    }

    @Test
    fun `the answers that advance each screen do, and the screen they take the rider to is the next one`() = krailRunTest {
        data class Advance(val step: IntroStep, val shape: String, val event: IntroUiEvent, val to: IntroStep)
        listOf(
            Advance(IntroStep.PARK_RIDE, "asking", IntroUiEvent.AnswerParkRide(yes = false), IntroStep.DESTINATION),
            Advance(IntroStep.PARK_RIDE, "locating", IntroUiEvent.DeclineParkRide, IntroStep.DESTINATION),
            Advance(IntroStep.PARK_RIDE, "locating", denied, IntroStep.DESTINATION),
            Advance(IntroStep.PARK_RIDE, "card", IntroUiEvent.DeclineParkRide, IntroStep.DESTINATION),
            Advance(IntroStep.PARK_RIDE, "card", IntroUiEvent.AddParkRideAndContinue, IntroStep.DESTINATION),
            Advance(IntroStep.PARK_RIDE, "no station near", IntroUiEvent.AcknowledgeNoStation, IntroStep.DESTINATION),
            Advance(IntroStep.DESTINATION, "a stop selected", IntroUiEvent.ConfirmStop, IntroStep.ORIGIN),
            Advance(IntroStep.ORIGIN, "a stop selected", IntroUiEvent.ConfirmStop, IntroStep.CONFIRM),
            Advance(IntroStep.CONFIRM, "the trip", IntroUiEvent.AnswerSaveTrip(yes = true), IntroStep.THEME),
            Advance(IntroStep.CONFIRM, "the trip", IntroUiEvent.AnswerSaveTrip(yes = false), IntroStep.THEME),
        ).forEach { advance ->
            val rig = riderOn(advance.step, advance.shape)

            rig.send(advance.event)

            assertEquals(advance.to, rig.state.step, "$advance")
            rig.clearViewModel()
        }
    }

    @Test
    fun `no run of events with no answer for the screen reaches the next one, from every shape of every screen`() =
        krailRunTest {
            // The answer for each screen, which is the only way off it. The Park & Ride screen passes itself over, with no message,
            // only when the location cannot be used, which is the defined skip in this list as denied.
            val answers: Map<IntroStep, Set<IntroUiEvent>> = mapOf(
                IntroStep.PARK_RIDE to setOf(
                    IntroUiEvent.AnswerParkRide(yes = false), IntroUiEvent.AddParkRideAndContinue,
                    IntroUiEvent.DeclineParkRide, IntroUiEvent.AcknowledgeNoStation, denied,
                ),
                IntroStep.DESTINATION to setOf(IntroUiEvent.ConfirmStop),
                IntroStep.ORIGIN to setOf(IntroUiEvent.ConfirmStop),
                IntroStep.CONFIRM to setOf(IntroUiEvent.AnswerSaveTrip(yes = true), IntroUiEvent.AnswerSaveTrip(yes = false)),
                IntroStep.THEME to setOf(IntroUiEvent.ConfirmTheme),
            )
            IntroStep.entries.forEach { step ->
                val sendable = allEvents.filter { it !in answers.getValue(step) }
                shapes.getValue(step).forEach { shape ->
                    sendable.forEach { first ->
                        sendable.forEach { second ->
                            val rig = riderOn(step, shape)

                            rig.send(first, second, first)

                            assertEquals(step, rig.state.step, "$first, $second, $first moved $step on, $shape")
                            rig.clearViewModel()
                        }
                    }
                }
            }
        }

    @Test
    fun `a confirm with no stop selected for the screen on screen does nothing, and a selection never carries to the next screen`() =
        krailRunTest {
            val none = riderOn(IntroStep.DESTINATION, "nothing selected")
            none.send(IntroUiEvent.ConfirmStop)
            assertEquals(IntroStep.DESTINATION, none.state.step)

            val rig = riderOn(IntroStep.DESTINATION, "a stop selected")
            rig.send(IntroUiEvent.ConfirmStop)
            assertEquals(IntroStep.ORIGIN, rig.state.step)
            assertNull(rig.state.selectedStop, "the stop selected for the destination is still selected on the origin screen")

            // A second tap, which is what arrives from a double tap, finds nothing selected for this screen.
            rig.send(IntroUiEvent.ConfirmStop)
            assertEquals(IntroStep.ORIGIN, rig.state.step)
            assertNull(rig.state.startStopId)
        }

    @Test
    fun `a stale event from the previous step is dropped, and the same event pressed on the screen on screen is taken`() =
        krailRunTest {
            val rig = riderOn(IntroStep.ORIGIN, "nothing selected")

            // Pressed on the destination screen, which has gone: a row tapped as the screen slid out.
            rig.viewModel.onEvent(IntroUiEvent.SelectStop(WYNYARD_STOP), pressedOn = IntroStep.DESTINATION)
            rig.settle()
            assertNull(rig.state.selectedStop, "a tap from the destination screen selected a stop on the origin screen")

            rig.viewModel.onEvent(IntroUiEvent.SelectStop(WYNYARD_STOP), pressedOn = IntroStep.ORIGIN)
            rig.settle()
            assertEquals(WYNYARD_STOP, rig.state.selectedStop)

            // The confirm that was meant for the destination screen cannot confirm the origin's stop.
            rig.viewModel.onEvent(IntroUiEvent.ConfirmStop, pressedOn = IntroStep.DESTINATION)
            rig.settle()
            assertEquals(IntroStep.ORIGIN, rig.state.step)
            assertEquals(WYNYARD_STOP, rig.state.selectedStop)

            rig.viewModel.onEvent(IntroUiEvent.ConfirmStop, pressedOn = IntroStep.ORIGIN)
            rig.settle()
            assertEquals(IntroStep.CONFIRM, rig.state.step)
        }

    @Test
    fun `every kind of event pressed on a step the Intro has left is dropped, whatever the Intro is on now`() = krailRunTest {
        IntroStep.entries.forEach { now ->
            IntroStep.entries.filter { it != now }.forEach { was ->
                shapes.getValue(now).forEach { shape ->
                    allEvents.forEach { event ->
                        val rig = riderOn(now, shape)
                        val before = rig.state

                        rig.viewModel.onEvent(event, pressedOn = was)
                        rig.settle()

                        assertEquals(before, rig.state, "$event pressed on $was changed the $now screen, $shape")
                        rig.clearViewModel()
                    }
                }
            }
        }
    }

    @Test
    fun `a granted fix far from every station shows the message and does not move on, the OK moves on once`() =
        krailRunTest {
            val rig = riderOn(IntroStep.PARK_RIDE, "locating")

            rig.send(IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL))

            assertEquals(IntroStep.PARK_RIDE, rig.state.step, "no screen moves on without the rider's answer")
            assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard)

            // A quick double tap on OK lands once: the second press is on a screen the Intro has left.
            rig.viewModel.onEvent(IntroUiEvent.AcknowledgeNoStation, pressedOn = IntroStep.PARK_RIDE)
            rig.viewModel.onEvent(IntroUiEvent.AcknowledgeNoStation, pressedOn = IntroStep.PARK_RIDE)
            rig.viewModel.onEvent(IntroUiEvent.AcknowledgeNoStation)
            rig.settle()

            assertEquals(IntroStep.DESTINATION, rig.state.step)
            assertTrue(rig.state.selectedStop == null)
        }
}
