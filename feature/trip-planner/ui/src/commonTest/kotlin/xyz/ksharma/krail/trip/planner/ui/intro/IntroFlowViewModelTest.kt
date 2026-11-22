package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val SUGGESTION_FLAG_WRITE = "pref:KEY_PARK_RIDE_SUGGESTION_SHOWN=true"

class IntroFlowViewModelTest {

    @Test
    fun `the park and ride question opens with nothing chosen`() = krailRunTest {
        val rig = IntroRig(this)

        assertEquals(ON_PARK_RIDE, rig.state)
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
    fun `there is no way past the question without answering it`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(
            IntroUiEvent.FinishPressed,
            IntroUiEvent.FinishWithoutTrip,
            IntroUiEvent.AddParkRideAndContinue,
            IntroUiEvent.DeclineParkRide,
        )

        assertEquals(ON_PARK_RIDE, rig.state)
        assertEquals(emptyList(), rig.log.entries)
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

        assertEquals(ON_PARK_RIDE, rig.state)
        assertFalse(rig.state.addedParkRide)
    }

    @Test
    fun `the same stop on both lists is a valid trip when the other end is different`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(IntroState(step = IntroStep.DESTINATION)))

        rig.send(IntroUiEvent.ChooseStop(CENTRAL.toIntroStop()), IntroUiEvent.ChooseStop(WYNYARD.toIntroStop()))

        assertEquals(true, rig.state.hasTrip)
        assertEquals(IntroStep.CONFIRM, rig.state.step)
    }

    @Test
    fun `the step, the answer and the picks survive a new view model on the same saved state`() =
        krailRunTest {
            val before = IntroRig(this)
            before.send(
                IntroUiEvent.AnswerParkRide(yes = false),
                IntroUiEvent.ChooseStop(IntroStop(stopId = "9", name = "Nine")),
                IntroUiEvent.ChooseStop(TALLAWONG.toIntroStop()),
            )

            val after = IntroRig(this, handle = before.handle)

            assertEquals(
                IntroState(
                    step = IntroStep.CONFIRM,
                    parkRideAnswer = ParkRideAnswer.NO,
                    startStopId = TALLAWONG.stopId,
                    startName = TALLAWONG.name,
                    endStopId = "9",
                    endName = "Nine",
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
