package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** The shape the screens and the view model agree on, so renaming or dropping a field is a test failure. */
class IntroUiEventTest {

    @Test
    fun `each event carries what it was built from`() {
        val fix = LatLng(-33.88, 151.21)

        assertEquals(true, IntroUiEvent.AnswerParkRide(yes = true).yes)
        assertEquals(LocationResult.GRANTED, IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, fix).result)
        assertEquals(fix, IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, fix).location)
        assertNull(IntroUiEvent.LocationResultReceived(LocationResult.DENIED, null).location)
        assertEquals(CENTRAL_STOP, IntroUiEvent.SelectStop(CENTRAL_STOP).stop)
        assertEquals("chat", IntroUiEvent.QueryChanged("chat").text)
    }

    @Test
    fun `the events without values are distinct and the effect is one value`() {
        val plain = setOf(
            IntroUiEvent.AddParkRideAndContinue,
            IntroUiEvent.DeclineParkRide,
            IntroUiEvent.AnswerSaveTrip(yes = true),
            IntroUiEvent.AnswerSaveTrip(yes = false),
            IntroUiEvent.ConfirmStop,
        )

        assertEquals(5, plain.size)
        assertSame(IntroEffect.NavigateToSavedTrips, IntroEffect.NavigateToSavedTrips)
    }
}
