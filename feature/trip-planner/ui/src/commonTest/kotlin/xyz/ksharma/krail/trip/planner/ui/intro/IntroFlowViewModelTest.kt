package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val SUGGESTION_FLAG_WRITE = "pref:KEY_PARK_RIDE_SUGGESTION_SHOWN=true"

class IntroFlowViewModelTest {

    @Test
    fun `the first screen is the park and ride question with nothing chosen`() = krailRunTest {
        val rig = IntroRig(this)

        assertEquals(IntroState(), rig.state)
    }

    @Test
    fun `suggested stops come from the remote config flag when it holds a valid list`() = krailRunTest {
        val rig = IntroRig(this)
        rig.flag.setFlagValue(
            FlagKeys.INTRO_SUGGESTED_STOPS.key,
            FlagValue.JsonValue(
                """{"start":[{"stopId":"1","name":"One"},{"stopId":"2","name":"Two"}],""" +
                    """"end":[{"stopId":"3","name":"Three"}]}""",
            ),
        )

        assertEquals(
            SuggestedStops(
                start = listOf(SuggestedStop("1", "One"), SuggestedStop("2", "Two")),
                end = listOf(SuggestedStop("3", "Three")),
            ),
            rig.viewModel.suggestedStops,
        )
    }

    @Test
    fun `suggested stops fall back to the bundled lists when the flag is unset, empty or unusable`() =
        krailRunTest {
            val unusable = listOf<FlagValue?>(
                null,
                FlagValue.StringValue(""),
                FlagValue.JsonValue("{not json"),
                FlagValue.JsonValue("""{"start":[],"end":[]}"""),
            )

            unusable.forEach { value ->
                val rig = IntroRig(this)
                value?.let { rig.flag.setFlagValue(FlagKeys.INTRO_SUGGESTED_STOPS.key, it) }

                assertEquals(DEFAULT_SUGGESTED_STOPS, rig.viewModel.suggestedStops, "flag value $value")
            }
        }

    @Test
    fun `back returns from the trip step to the question and never goes before it`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.AnswerParkRide(yes = false))
        assertEquals(IntroStep.DESTINATION, rig.state.step)
        rig.send(IntroUiEvent.Back)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step)
        rig.send(IntroUiEvent.Back)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step)
    }

    @Test
    fun `back never returns to a Park and Ride screen that had nothing to decide`() = krailRunTest {
        val noLocation = IntroRig(this)
        noLocation.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.DENIED, null),
        )
        assertEquals(IntroStep.DESTINATION, noLocation.state.step)

        noLocation.send(IntroUiEvent.Back)

        assertEquals(IntroStep.DESTINATION, noLocation.state.step, "denied leaves nothing to answer")

        val declined = IntroRig(this)
        declined.send(IntroUiEvent.AnswerParkRide(yes = false), IntroUiEvent.Back)

        assertEquals(IntroStep.PARK_RIDE, declined.state.step, "a no can still be changed")
    }

    @Test
    fun `there is no way past the question without answering it`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(
            IntroUiEvent.Back,
            IntroUiEvent.FinishPressed,
            IntroUiEvent.FinishWithoutTrip,
            IntroUiEvent.AddParkRideAndContinue,
            IntroUiEvent.DeclineParkRide,
        )

        assertEquals(IntroState(), rig.state)
        assertEquals(emptyList(), rig.log.entries)
    }

    @Test
    fun `answering no and then going back and answering no again marks the suggestion only once`() =
        krailRunTest {
            val rig = IntroRig(this)

            rig.send(IntroUiEvent.AnswerParkRide(yes = false))
            assertEquals(IntroStep.DESTINATION, rig.state.step)
            assertEquals(ParkRideAnswer.NO, rig.state.parkRideAnswer)
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE))

            rig.send(IntroUiEvent.Back, IntroUiEvent.AnswerParkRide(yes = false))

            assertEquals(IntroStep.DESTINATION, rig.state.step)
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE))
        }

    @Test
    fun `answering yes keeps the question step and writes nothing until a result arrives, and not now gives up`() =
        krailRunTest {
            val rig = IntroRig(this)

            rig.send(IntroUiEvent.AnswerParkRide(yes = true))

            assertEquals(ParkRideAnswer.YES, rig.state.parkRideAnswer)
            assertEquals(IntroStep.PARK_RIDE, rig.state.step)
            assertEquals(0, rig.log.count(SUGGESTION_FLAG_WRITE))

            rig.send(IntroUiEvent.DeclineParkRide)

            assertEquals(ParkRideAnswer.YES, rig.state.parkRideAnswer)
            assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard, "the look is given up")
            assertEquals(IntroStep.DESTINATION, rig.state.step)
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE))
        }

    @Test
    fun `add to home with no card on screen changes nothing`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.AddParkRideAndContinue)

        assertEquals(IntroState(), rig.state)
        assertFalse(rig.state.addedParkRide)
    }

    @Test
    fun `picking a start and an end sets their ids and names without marking search`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.PickStart(TALLAWONG), IntroUiEvent.PickEnd(CENTRAL))

        assertEquals(TALLAWONG.stopId, rig.state.startStopId)
        assertEquals(TALLAWONG.name, rig.state.startName)
        assertEquals(CENTRAL.stopId, rig.state.endStopId)
        assertEquals(CENTRAL.name, rig.state.endName)
        assertFalse(rig.state.usedSearch)
    }

    @Test
    fun `a stop picked from search sets the chosen end and marks search used`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = "9", stopName = "Nine"))

        assertEquals("9", rig.state.endStopId)
        assertEquals("Nine", rig.state.endName)
        assertEquals(null, rig.state.startStopId)
        assertEquals(true, rig.state.usedSearch)

        rig.send(IntroUiEvent.StopPickedFromSearch(isStart = true, stopId = "8", stopName = "Eight"))

        assertEquals("8", rig.state.startStopId)
        assertEquals("Eight", rig.state.startName)
    }

    @Test
    fun `the same stop on both lists is a valid trip when the other end is different`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.PickStart(CENTRAL), IntroUiEvent.PickEnd(WYNYARD))

        assertEquals(true, rig.state.hasTrip)
    }

    @Test
    fun `picking the stop already chosen for the other end keeps the new pick and clears the other end`() =
        krailRunTest {
            val rig = IntroRig(this)
            rig.send(IntroUiEvent.PickStart(CENTRAL), IntroUiEvent.PickEnd(WYNYARD))

            rig.send(IntroUiEvent.PickStart(WYNYARD))

            assertEquals(WYNYARD.stopId, rig.state.startStopId)
            assertEquals(null, rig.state.endStopId)
            assertEquals(null, rig.state.endName)
            assertFalse(rig.state.hasTrip)

            rig.send(IntroUiEvent.PickEnd(CENTRAL), IntroUiEvent.PickStart(TALLAWONG))
            rig.send(IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = TALLAWONG.stopId, stopName = "x"))

            assertEquals(null, rig.state.startStopId)
            assertEquals(TALLAWONG.stopId, rig.state.endStopId)
        }

    @Test
    fun `the step, the answer and the picks survive a new view model on the same saved state`() =
        krailRunTest {
            val before = IntroRig(this)
            before.send(
                IntroUiEvent.AnswerParkRide(yes = false),
                IntroUiEvent.PickStart(TALLAWONG),
                IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = "9", stopName = "Nine"),
            )

            val after = IntroRig(this, handle = before.handle)

            assertEquals(
                IntroState(
                    step = IntroStep.DESTINATION,
                    parkRideAnswer = ParkRideAnswer.NO,
                    startStopId = TALLAWONG.stopId,
                    startName = TALLAWONG.name,
                    endStopId = "9",
                    endName = "Nine",
                    usedSearch = true,
                ),
                after.state,
            )
        }

    @Test
    fun `unreadable saved state falls back to the first screen`() = krailRunTest {
        val handle = savedAt(IntroState(step = IntroStep.DESTINATION))
        handle[INTRO_SAVED_STATE_KEY] = "{not a snapshot"

        assertEquals(IntroState(), IntroRig(this, handle = handle).state)
    }
}
