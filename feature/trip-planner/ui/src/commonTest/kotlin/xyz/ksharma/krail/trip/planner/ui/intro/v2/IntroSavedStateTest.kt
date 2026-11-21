package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.lifecycle.SavedStateHandle
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IntroSavedStateTest {

    // Every saved field is off its default, so a field left out of the snapshot fails the round trip.
    private val everyFieldSet = IntroState(
        step = IntroStep.FIRST_TRIP,
        parkRideAnswer = ParkRideAnswer.YES,
        locationResult = LocationResult.GRANTED,
        startStopId = "200060",
        startName = "Central Station",
        endStopId = "200080",
        endName = "Wynyard Station",
        addedParkRide = true,
        usedSearch = true,
        saved = true,
    )

    @Test
    fun `every saved field comes back as it was saved`() {
        val handle = SavedStateHandle()

        handle.saveIntroState(everyFieldSet)

        assertEquals(everyFieldSet, handle.restoreIntroState())
    }

    @Test
    fun `a later save replaces an earlier one`() {
        val handle = SavedStateHandle()
        handle.saveIntroState(everyFieldSet)

        handle.saveIntroState(IntroState(step = IntroStep.PARK_RIDE))

        assertEquals(IntroState(), handle.restoreIntroState())
    }

    @Test
    fun `nothing saved is the first screen`() {
        assertEquals(IntroState(), SavedStateHandle().restoreIntroState())
    }

    @Test
    fun `saved text that is not a snapshot is the first screen`() {
        listOf("{not json", "", """{"step":"FIRST_TRIP"}""", """{"step":"NOT_A_STEP"}""").forEach { text ->
            val handle = SavedStateHandle()
            handle[INTRO_SAVED_STATE_KEY] = text

            assertEquals(IntroState(), handle.restoreIntroState(), "saved text $text")
        }
    }

    @Test
    fun `no station near comes back as no station near`() {
        val handle = SavedStateHandle()
        val state = IntroState(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.NoStationNear)

        handle.saveIntroState(state)

        assertEquals(state, handle.restoreIntroState())
    }

    @Test
    fun `a card is saved as its station id alone and comes back as a skeleton for that station`() {
        val handle = SavedStateHandle()
        val card = ParkRideCardStation(stationId = "2153478", stationName = "Bella Vista")

        handle.saveIntroState(
            IntroState(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.Card(card)),
        )

        assertEquals(
            IntroState(
                parkRideAnswer = ParkRideAnswer.YES,
                parkRideCard = ParkRideCardPhase.Locating(stationId = "2153478"),
            ),
            handle.restoreIntroState(),
        )
        assertFalse("Bella Vista" in handle.get<String>(INTRO_SAVED_STATE_KEY).orEmpty(), "only the id is kept")
    }

    @Test
    fun `a station being worked out again stays saved, so a second kill loses nothing`() {
        val handle = SavedStateHandle()
        val state = IntroState(parkRideCard = ParkRideCardPhase.Locating(stationId = "2153478"))

        handle.saveIntroState(state)

        assertEquals(state, handle.restoreIntroState())
    }

    @Test
    fun `a location still awaited is not saved, so the question comes back`() {
        val handle = SavedStateHandle()

        handle.saveIntroState(
            IntroState(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.Locating()),
        )

        assertEquals(IntroState(parkRideAnswer = ParkRideAnswer.YES), handle.restoreIntroState())
    }

    @Test
    fun `a snapshot saved before the card existed still reads`() {
        val handle = SavedStateHandle()
        handle[INTRO_SAVED_STATE_KEY] = """{"step":"FIRST_TRIP","parkRideAnswer":"NO","locationResult":"NOT_ASKED",""" +
            """"startStopId":null,"startName":null,"endStopId":null,"endName":null,""" +
            """"addedParkRide":false,"usedSearch":false,"saved":false}"""

        assertEquals(
            IntroState(step = IntroStep.FIRST_TRIP, parkRideAnswer = ParkRideAnswer.NO),
            handle.restoreIntroState(),
        )
    }
}
