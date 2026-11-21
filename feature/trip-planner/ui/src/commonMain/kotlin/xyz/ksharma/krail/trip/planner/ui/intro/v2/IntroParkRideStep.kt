package xyz.ksharma.krail.trip.planner.ui.intro.v2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/**
 * The longest the Park & Ride step waits for the stops before it gives up on a station. Past it the
 * answer is the same as no station near, with no error, because the nearest station needs the
 * stops table for its coordinates.
 */
internal const val PARK_RIDE_STOPS_WAIT_MS = 10_000L

/**
 * The Park & Ride step of the Intro: the answer, the one location result and the nearest station. It
 * runs in [scope], the view model's scope, so all of it is cancelled when the view model is cleared,
 * and it starts no timer and no repeat.
 *
 * The rider's position arrives in [onLocationResult], is used to find the station inside one
 * coroutine and goes out of scope with it. It is never written to the state or to what is saved.
 */
internal class IntroParkRideStep(
    private val scope: CoroutineScope,
    private val readiness: AppDataReadiness,
    private val gateway: IntroParkRideGateway,
    private val read: () -> IntroState,
    private val write: (IntroState.() -> IntroState) -> Unit,
    private val onLeftQuestion: () -> Unit,
) {
    private var job: Job? = null

    /**
     * The answer is yes and the screen now asks for the permission, then sends the result. Ignored
     * while a result is awaited and while a card shows, so a second tap never starts a second look.
     */
    fun askLocation() {
        if (read().parkRideCard.isInProgressOrShown()) return
        write { copy(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.Locating()) }
    }

    /** The answer is no, so nothing the step started is wanted any more. */
    fun cancel() {
        job?.cancel()
        job = null
    }

    /**
     * The permission result and, for a granted one, the fix. A result nobody asked for is ignored.
     * No usable location moves to the trip step with no message and no error. A granted result with
     * no fix is recorded as unavailable, because there is nothing to find a station from.
     */
    fun onLocationResult(result: LocationResult, location: LatLng?) {
        if (read().parkRideCard !is ParkRideCardPhase.Locating) return
        if (result == LocationResult.GRANTED && location != null) {
            write { copy(locationResult = LocationResult.GRANTED) }
            resolve { gateway.nearestStation(location) }
        } else {
            val recorded = if (result == LocationResult.GRANTED) LocationResult.UNAVAILABLE else result
            write {
                copy(locationResult = recorded, step = IntroStep.FIRST_TRIP, parkRideCard = ParkRideCardPhase.None)
            }
            onLeftQuestion()
        }
    }

    private fun resolve(find: suspend () -> ParkRideStationPickerItem?) {
        job?.cancel()
        job = scope.launch {
            val found = if (readiness.awaitReady(PARK_RIDE_STOPS_WAIT_MS) is StopsReadiness.Ready) {
                attempt(find)
            } else {
                null
            }
            if (found == null) {
                write { copy(parkRideCard = ParkRideCardPhase.NoStationNear) }
            } else {
                show(found)
            }
        }
    }

    // The card shows at once with names only.
    private fun show(found: ParkRideStationPickerItem) {
        val card = ParkRideCardStation(
            stationId = found.stationId,
            stationName = found.stationName,
            carParks = found.carParksWithoutReadings(),
        )
        write { copy(parkRideCard = ParkRideCardPhase.Card(card)) }
    }
}
