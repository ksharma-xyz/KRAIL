package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** The shape the screens and the view model agree on, so renaming or dropping a field is a test failure. */
class IntroUiEventTest {

    private val stop = SuggestedStop(stopId = "200060", name = "Central Station")

    @Test
    fun `each event carries what it was built from`() {
        val fix = LatLng(-33.88, 151.21)

        assertEquals(true, IntroUiEvent.AnswerParkRide(yes = true).yes)
        assertEquals(LocationResult.GRANTED, IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, fix).result)
        assertEquals(fix, IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, fix).location)
        assertNull(IntroUiEvent.LocationResultReceived(LocationResult.DENIED, null).location)
        assertEquals(stop, IntroUiEvent.PickStart(stop).stop)
        assertEquals(stop, IntroUiEvent.PickEnd(stop).stop)
        IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = "9", stopName = "Nine").let {
            assertEquals(listOf(false, "9", "Nine"), listOf(it.isStart, it.stopId, it.stopName))
        }
    }

    @Test
    fun `the events without values are distinct and the effect is one value`() {
        val plain = setOf(
            IntroUiEvent.Back,
            IntroUiEvent.AddParkRideAndContinue,
            IntroUiEvent.DeclineParkRide,
            IntroUiEvent.FinishPressed,
            IntroUiEvent.FinishWithoutTrip,
        )

        assertEquals(5, plain.size)
        assertSame(IntroEffect.NavigateToSavedTrips, IntroEffect.NavigateToSavedTrips)
    }
}
