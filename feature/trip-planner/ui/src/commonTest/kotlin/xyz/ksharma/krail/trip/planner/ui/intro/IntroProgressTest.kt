package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroProgressTest {

    private val bellaVista = ParkRideCardStation(
        stationId = "2153478",
        stationName = "Bella Vista",
        carParks = persistentListOf(
            ParkRideCarPark(
                facilityId = "31",
                name = "Bella Vista Car Park Level 7",
                spotsAvailable = 120,
                percentageFull = 40,
                timeText = "8:05 AM",
            ),
        ),
        availability = ParkRideAvailabilityState.Content,
    )

    // Everything the Intro keeps, off its default, so a field left out of the progress fails the round trip.
    private val everyKeptFieldSet = IntroState(
        step = IntroStep.THEME,
        parkRideAnswer = ParkRideAnswer.YES,
        locationResult = LocationResult.GRANTED,
        parkRideCard = ParkRideCardPhase.Card(bellaVista.copy(carParks = persistentListOf())),
        startStopId = "2153478",
        startName = "Bella Vista Station",
        endStopId = "200080",
        endName = "Wynyard Station",
        startFromParkRide = true,
        addedParkRide = true,
        usedSearch = true,
        saved = true,
        selectedThemeId = 5,
        tripDecision = TripDecision.SAVED,
    )

    // Every thing the Intro holds that must never reach the device, set to a value that can be searched for.
    private val everyDroppedFieldSet = everyKeptFieldSet.copy(
        parkRideCard = ParkRideCardPhase.Card(bellaVista),
        nearbyStops = persistentListOf(IntroStop("201510", "Nearby Redfern Station")),
        stopModes = persistentMapOf("201510" to persistentListOf(TransportMode.Train)),
        query = "typedquerytext",
        search = IntroSearchPhase.Results(persistentListOf(IntroStop("209999", "Searched Result Station"))),
        selectedStop = IntroStop("208888", "Selected Row Station"),
    )

    private fun roundTrip(state: IntroState): IntroState {
        val progress = introProgressFromText(state.toProgress().toText())
        return assertNotNull(progress, "the progress was not read back").toState()
    }

    @Test
    fun `every kept field comes back as it was kept`() {
        val back = roundTrip(everyKeptFieldSet)

        assertEquals(
            everyKeptFieldSet.copy(
                // A station added before is a card with its name and no car parks, not worked out again.
                parkRideCard = ParkRideCardPhase.Card(
                    bellaVista.copy(
                        carParks = persistentListOf(),
                        availability = ParkRideAvailabilityState.Unavailable,
                    ),
                ),
            ),
            back,
        )
    }

    @Test
    fun `every screen comes back as the screen it was`() {
        IntroStep.entries.forEach { step ->
            assertEquals(step, roundTrip(everyKeptFieldSet.copy(step = step)).step, "step $step")
        }
    }

    @Test
    fun `the answer on the confirm screen comes back, and none comes back as none`() {
        listOf(TripDecision.SAVED, TripDecision.DECLINED, null).forEach { decision ->
            val state = everyKeptFieldSet.copy(tripDecision = decision)

            assertEquals(decision, roundTrip(state).tripDecision, "decision $decision")
        }
    }

    @Test
    fun `the Park and Ride answer comes back as yes, no or none`() {
        val past = everyKeptFieldSet.copy(
            step = IntroStep.DESTINATION,
            addedParkRide = false,
            parkRideCard = ParkRideCardPhase.None,
        )

        ParkRideAnswer.entries.forEach { answer ->
            assertEquals(answer, roundTrip(past.copy(parkRideAnswer = answer)).parkRideAnswer, "answer $answer")
        }
    }

    @Test
    fun `the Park and Ride question still on screen is kept as unanswered, whatever was tapped or found on it`() {
        listOf(
            ParkRideCardPhase.None,
            ParkRideCardPhase.Locating(),
            ParkRideCardPhase.NoStationNear,
            ParkRideCardPhase.Card(bellaVista),
        ).forEach { card ->
            val onTheQuestion = IntroState(
                step = IntroStep.PARK_RIDE,
                parkRideAnswer = ParkRideAnswer.YES,
                locationResult = LocationResult.GRANTED,
                parkRideCard = card,
            )

            val back = roundTrip(onTheQuestion)

            assertEquals(IntroState(step = IntroStep.PARK_RIDE), back, "card $card")
        }
    }

    @Test
    fun `an added station is kept as its id and its name, and none is kept for one that was not added`() {
        val added = roundTrip(everyKeptFieldSet.copy(step = IntroStep.DESTINATION))
        val shown = added.parkRideCard as ParkRideCardPhase.Card
        assertEquals("2153478" to "Bella Vista", shown.station.stationId to shown.station.stationName)
        assertTrue(added.addedParkRide)

        val notAdded = roundTrip(
            everyKeptFieldSet.copy(
                step = IntroStep.DESTINATION,
                addedParkRide = false,
                parkRideCard = ParkRideCardPhase.Card(bellaVista),
            ),
        )
        assertEquals(ParkRideCardPhase.None, notAdded.parkRideCard)
        assertFalse(notAdded.addedParkRide)
    }

    @Test
    fun `a stop is kept as an id and a name, and half a stop is not kept`() {
        val back =
            roundTrip(
                IntroState(
                    step = IntroStep.CONFIRM,
                    startStopId = "1",
                    startName = null,
                    endStopId = null,
                    endName = "End",
                ),
            )

        assertNull(back.startStopId)
        assertNull(back.startName)
        assertNull(back.endStopId)
        assertNull(back.endName)
    }

    @Test
    fun `no position, typed text, nearby station, car park or selected row is in the saved text`() {
        val text = everyDroppedFieldSet.toProgress().toText()

        listOf(
            "typedquerytext", "Nearby Redfern", "201510", "Searched Result", "209999", "Selected Row", "208888",
            "Level 7", "spots", "8:05", "120", "latitude", "longitude", "location",
        ).forEach { word ->
            assertFalse(
                word.lowercase() in text.lowercase().replace("locationresult", ""),
                "$word is in the saved text: $text",
            )
        }
        assertEquals(everyKeptFieldSet.toProgress(), everyDroppedFieldSet.toProgress())
    }

    @Test
    fun `the saved text carries the version, so a reader can tell its shape`() {
        assertTrue("\"version\":$INTRO_PROGRESS_VERSION" in IntroState().toProgress().toText())
    }

    @Test
    fun `a progress of the first screen with nothing answered is the first screen`() {
        assertEquals(IntroState(), roundTrip(IntroState()))
    }

    @Test
    fun `a text of another version, newer or older, is ignored`() {
        val text = everyKeptFieldSet.toProgress().toText()

        listOf(0, INTRO_PROGRESS_VERSION + 1, 2, 99, -1).forEach { version ->
            val other = text.replace("\"version\":$INTRO_PROGRESS_VERSION", "\"version\":$version")

            assertNull(introProgressFromText(other), "version $version")
        }
    }

    @Test
    fun `a text from a newer shape that adds a key is ignored, not half used`() {
        val newer = everyKeptFieldSet.toProgress().toText().replace(
            "\"version\":$INTRO_PROGRESS_VERSION",
            "\"version\":${INTRO_PROGRESS_VERSION + 1},\"somethingNew\":true",
        )

        assertNull(introProgressFromText(newer))
    }

    @Test
    fun `a text that cannot be read is nothing`() {
        listOf(
            null,
            "",
            "{not json",
            "[]",
            "{}",
            """{"version":1}""",
            """{"version":1,"step":"NOT_A_STEP"}""",
            """{"version":1,"step":"FIRST_TRIP"}""",
            """{"version":1,"step":"THEME","themeId":"red"}""",
            """{"version":1,"step":"THEME","start":{"id":"1"}}""",
            """{"version":1,"step":"THEME","decision":"MAYBE"}""",
        ).forEach { text ->
            assertNull(introProgressFromText(text), "text $text")
        }
    }

    @Test
    fun `the saved text is small`() {
        // A guess at the worst case on the app's own long names, held here so it cannot grow by accident.
        assertTrue(everyKeptFieldSet.toProgress().toText().length < 500)
    }
}
