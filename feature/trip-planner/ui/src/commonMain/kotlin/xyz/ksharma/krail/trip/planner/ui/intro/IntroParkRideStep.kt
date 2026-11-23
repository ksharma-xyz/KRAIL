package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/**
 * The longest the Park & Ride step waits for the stops before it gives up on a station. Past it the screen is passed over
 * with no message and no error, because the nearest station needs the stops table for its coordinates and nothing is known about
 * what is near, so the rider is not told there is nothing.
 */
internal const val PARK_RIDE_STOPS_WAIT_MS = 10_000L

/**
 * The Park & Ride screen of the Intro: the answer, the one location result, the nearest station and
 * its one availability reading, and what the rider does with the card, which is to add the station or to
 * decline it. Both move the rider on to the next screen, as does the OK on the message that no station is near,
 * within [INTRO_PARK_RIDE_RADIUS_KM] or, failing that, [INTRO_PARK_RIDE_WIDER_RADIUS_KM]. Only a location that cannot be used,
 * or stops that were not ready to look in, pass the screen over by themselves, with no message. It runs in [scope], the view
 * model's scope, so all of it is cancelled when the view model is cleared, and it starts no timer and no repeat.
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
     * The permission result and, for a granted one, the fix. A result nobody asked for is ignored, and the call
     * says whether the result was taken, so the one fix can also be given to what else reads it. No usable
     * location moves to the trip step with no message and no error. A granted result with no fix is recorded as
     * unavailable, because there is nothing to find a station from.
     */
    fun onLocationResult(result: LocationResult, location: LatLng?): Boolean {
        if (read().parkRideCard !is ParkRideCardPhase.Locating) return false
        if (result == LocationResult.GRANTED && location != null) {
            write { copy(locationResult = LocationResult.GRANTED) }
            resolve(noneNearIsAnAnswer = true) { gateway.nearestStation(location) }
        } else {
            val recorded = if (result == LocationResult.GRANTED) LocationResult.UNAVAILABLE else result
            write { copy(locationResult = recorded, parkRideCard = ParkRideCardPhase.None).leavingParkRide() }
            onLeftQuestion()
        }
        return true
    }

    /**
     * Add to home: the station is added and the rider moves on, in one step. A station already added is
     * left as it is, so a rider who comes back to the card and presses Add again moves on and adds
     * nothing twice. Ignored with no card on screen, which also covers a card that is being worked out
     * again after a killed process, and while an earlier add or decline is still running, so a double
     * tap does one thing.
     */
    fun addAndContinue() {
        val chosen = station ?: return
        if (toggleJob?.isActive == true) return
        if (read().addedParkRide) {
            write { leavingParkRide() }
            onLeftQuestion()
        } else {
            toggleJob = scope.launch { add(chosen) }
        }
    }

    /**
     * Not now: the rider moves on and nothing is added. A station they had added before coming back is
     * taken off again, because this answer is the latest one. A look that is still waiting is given up
     * and the card goes with it, so the rider is never held on a wait they have said no to.
     */
    fun decline() {
        if (toggleJob?.isActive == true) return
        val phase = read().parkRideCard
        val chosen = station
        when {
            phase is ParkRideCardPhase.Locating -> {
                cancel()
                write { copy(parkRideCard = ParkRideCardPhase.None).leavingParkRide() }
                onLeftQuestion()
            }
            phase is ParkRideCardPhase.Card && chosen != null && read().addedParkRide ->
                toggleJob = scope.launch { remove(chosen) }
            phase is ParkRideCardPhase.Card -> {
                write { leavingParkRide() }
                onLeftQuestion()
            }
        }
    }

    /**
     * OK, on the message that no station is near: the rider moves on to the next screen with nothing added. Only on that
     * message, and only while the rider is on this screen, so a second press does nothing.
     */
    fun acknowledgeNoStation() {
        if (read().parkRideCard != ParkRideCardPhase.NoStationNear) return
        write { leavingParkRide() }
        onLeftQuestion()
    }

    /**
     * Works the card out again from the saved station id, when the process was killed with a card. A station that is no
     * longer in the catalogue passes the screen over with no message, because the rider was never told there was none.
     */
    fun restore() {
        val stationId = (read().parkRideCard as? ParkRideCardPhase.Locating)?.stationId ?: return
        resolve(noneNearIsAnAnswer = false) { gateway.stationById(stationId) }
    }

    // Finds the station once the stops are ready. A station shows the card. None, when the look itself worked and
    // [noneNearIsAnAnswer], shows the message and waits for the rider's OK. Stops that were not ready in time, a look that
    // failed and a saved station that has gone say nothing was learned about what is near, so the screen is passed over, quietly.
    private fun resolve(noneNearIsAnAnswer: Boolean, find: suspend () -> ParkRideStationPickerItem?) {
        job?.cancel()
        job = scope.launch {
            val looked = if (readiness.awaitReady(PARK_RIDE_STOPS_WAIT_MS) is StopsReadiness.Ready) {
                attempt { listOfNotNull(find()) }
            } else {
                null
            }
            when {
                looked == null || (looked.isEmpty() && !noneNearIsAnAnswer) -> {
                    write { copy(parkRideCard = ParkRideCardPhase.None).leavingParkRide() }
                    onLeftQuestion()
                }
                looked.isEmpty() -> write { copy(parkRideCard = ParkRideCardPhase.NoStationNear) }
                else -> {
                    // The card shows at once with names only, then settles when the one availability call is back.
                    val found = looked.single()
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
            }
        }
    }

    // Added at once, without waiting for the network. The availability call may still be running, and
    // when it read the saved rows before this add it filed its reading under the stop id, so the
    // name is corrected once that call has landed.
    private suspend fun add(chosen: ParkRideStationPickerItem) {
        attempt { gateway.saveStation(chosen) } ?: return
        val prefill = attempt { gateway.startPrefill(chosen) }
        write { withAddedParkRide(prefill?.stopId, prefill?.name).leavingParkRide() }
        onLeftQuestion()
        job?.join()
        attempt { gateway.correctStopName(chosen) }
    }

    // The rider moves on whether or not the rows could be removed, so a failing write never traps them.
    private suspend fun remove(chosen: ParkRideStationPickerItem) {
        val removed = attempt { gateway.removeStation(chosen) }
        write { (if (removed != null) withoutParkRide() else this).leavingParkRide() }
        onLeftQuestion()
    }
}
