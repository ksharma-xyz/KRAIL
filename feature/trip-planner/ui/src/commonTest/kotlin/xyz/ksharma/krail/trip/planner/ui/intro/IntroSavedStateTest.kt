package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class IntroSavedStateTest {

    // Every saved field is off its default, so a field left out of the snapshot fails the round trip.
    private val everyFieldSet = IntroState(
        step = IntroStep.DESTINATION,
        parkRideAnswer = ParkRideAnswer.YES,
        locationResult = LocationResult.GRANTED,
        startStopId = "200060",
        startName = "Central Station",
        endStopId = "200080",
        endName = "Wynyard Station",
        startFromParkRide = true,
        addedParkRide = true,
        usedSearch = true,
        saved = true,
        selectedThemeId = 5,
    )

    @Test
    fun `every saved field comes back as it was saved`() {
        val handle = SavedStateHandle()

        handle.saveIntroState(everyFieldSet)

        assertEquals(everyFieldSet, handle.restoreIntroState())
    }

    @Test
    fun `the selected theme is saved with the screen, and a snapshot from before it existed has none`() {
        val handle = SavedStateHandle()
        handle.saveIntroState(IntroState(step = IntroStep.THEME, selectedThemeId = 9))
        assertEquals(9, handle.restoreIntroState().selectedThemeId)
        assertEquals(IntroStep.THEME, handle.restoreIntroState().step)

        val older = SavedStateHandle()
        older[INTRO_SAVED_STATE_KEY] = """{"step":"DESTINATION","parkRideAnswer":"NO","locationResult":"NOT_ASKED",""" +
            """"startStopId":null,"startName":null,"endStopId":null,"endName":null,""" +
            """"addedParkRide":false,"usedSearch":false,"saved":false}"""
        assertEquals(IntroStep.DESTINATION, older.restoreIntroState().step)
        assertNull(older.restoreIntroState().selectedThemeId)
    }

    @Test
    fun `the stations near the rider, the badges, the typed text, its results and the selected stop are never saved`() {
        val handle = SavedStateHandle()
        val kept = IntroState(
            step = IntroStep.ORIGIN,
            endStopId = "200060",
            endName = "Central Station",
            startStopId = "201510",
            startName = "Redfern Station",
        )
        val withTransient = kept.copy(
            nearbyStops = persistentListOf(IntroStop("201510", "Redfern Station")),
            stopModes = persistentMapOf("201510" to persistentListOf(TransportMode.Train)),
            query = "redf",
            search = IntroSearchPhase.Results(persistentListOf(IntroStop("201510", "Redfern Station"))),
            selectedStop = IntroStop("200080", "Wynyard Station"),
        )

        handle.saveIntroState(withTransient)

        assertEquals(kept, handle.restoreIntroState(), "the chosen stops and the screen come back, the rest does not")
        val saved = handle.get<String>(INTRO_SAVED_STATE_KEY).orEmpty()
        assertEquals(false, "redf" in saved, "the typed text is not in the saved text: $saved")
        assertEquals(false, "200080" in saved || "Wynyard" in saved, "the selected stop is not saved: $saved")
    }

    @Test
    fun `the chosen stops are saved as an id and a name each`() {
        val handle = SavedStateHandle()
        val chosen = IntroState(
            step = IntroStep.CONFIRM,
            startStopId = "2155384",
            startName = "Tallawong Station",
            endStopId = "200080",
            endName = "Wynyard Station",
        )

        handle.saveIntroState(chosen)

        assertEquals(chosen, handle.restoreIntroState())
    }

    @Test
    fun `a later save replaces an earlier one`() {
        val handle = SavedStateHandle()
        handle.saveIntroState(everyFieldSet)

        handle.saveIntroState(IntroState(step = IntroStep.PARK_RIDE))

        assertEquals(IntroState(step = IntroStep.PARK_RIDE), handle.restoreIntroState())
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
        handle[INTRO_SAVED_STATE_KEY] = """{"step":"DESTINATION","parkRideAnswer":"NO","locationResult":"NOT_ASKED",""" +
            """"startStopId":null,"startName":null,"endStopId":null,"endName":null,""" +
            """"addedParkRide":false,"usedSearch":false,"saved":false}"""

        assertEquals(
            IntroState(step = IntroStep.DESTINATION, parkRideAnswer = ParkRideAnswer.NO),
            handle.restoreIntroState(),
        )
    }
}
