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
 * The Park & Ride step of the Intro: the answer, the one location result, the nearest station and
 * its one availability reading. It runs in [scope], the view model's scope, so all of it is
 * cancelled when the view model is cleared, and it starts no timer and no repeat.
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
    private var toggleJob: Job? = null

    // The catalogue item behind the card, which Add needs and the state deliberately does not carry.
    private var station: ParkRideStationPickerItem? = null

    /**
     * The answer is yes and the screen now asks for the permission, then sends the result. Ignored
     * while a result is awaited and while a card shows, so a second tap never starts a second look.
     */
    fun askLocation() {
        if (read().parkRideCard.isInProgressOrShown()) return
        write { copy(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.Locating()) }
    }

    /** The answer is no, so nothing the step started is wanted any more, and the card goes with it. */
    fun cancel() {
        job?.cancel()
        job = null
        station = null
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

    /**
     * Add to home, or Added again to take it off. Ignored with no card on screen, which also covers a
     * card that is being worked out again after a killed process, and while an earlier toggle is
     * still running, so a double tap does one thing.
     */
    fun toggle() {
        val chosen = station ?: return
        if (toggleJob?.isActive == true) return
        toggleJob = scope.launch {
            if (read().addedParkRide) remove(chosen) else add(chosen)
        }
    }

    /** Works the card out again from the saved station id, when the process was killed with a card. */
    fun restore() {
        val stationId = (read().parkRideCard as? ParkRideCardPhase.Locating)?.stationId ?: return
        resolve { gateway.stationById(stationId) }
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

    // The card shows at once with names only, then settles when the one availability call is back.
    private suspend fun show(found: ParkRideStationPickerItem) {
        station = found
        val card = ParkRideCardStation(
            stationId = found.stationId,
            stationName = found.stationName,
            carParks = found.carParksWithoutReadings(),
        )
        write { copy(parkRideCard = ParkRideCardPhase.Card(card)) }
        val carParks = attempt { gateway.carParks(found) }
        write { withReadings(found.stationId, carParks) }
    }

    // Added at once, without waiting for the network. The availability call may still be running, and
    // when it read the saved rows before this add it filed its reading under the stop id, so the
    // name is corrected once that call has landed.
    private suspend fun add(chosen: ParkRideStationPickerItem) {
        attempt { gateway.saveStation(chosen) } ?: return
        val prefill = attempt { gateway.startPrefill(chosen) }
        write { withAddedParkRide(prefill?.stopId, prefill?.name) }
        job?.join()
        attempt { gateway.correctStopName(chosen) }
    }

    private suspend fun remove(chosen: ParkRideStationPickerItem) {
        attempt { gateway.removeStation(chosen) } ?: return
        write { withoutParkRide() }
    }
}
