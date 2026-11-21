package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.lifecycle.SavedStateHandle
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
