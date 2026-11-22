package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState

/**
 * What the destination and origin screens read from the stops table, worked out in the background so it is ready
 * when the rider gets there: the stations near them, from the fix the Park & Ride question already took, and the
 * modes of the stops it will list. It runs in [scope], the view model's scope, starts no timer and no repeat, and
 * every job it starts ends with the scope.
 *
 * The rider's position arrives in [onFix], is used for one query inside one coroutine and goes out of scope with
 * it. It is never written to the state, never saved and never logged.
 */
internal class IntroStopsStep(
    private val scope: CoroutineScope,
    private val readiness: AppDataReadiness,
    private val gateway: IntroStopsGateway,
    private val write: (IntroState.() -> IntroState) -> Unit,
) {
    private var nearbyJob: Job? = null

    /**
     * The nearest stations to [location], for the origin list. It waits for the stops, as the nearest Park & Ride
     * station does, and nothing the rider sees waits for it: if the stops are not ready in time, or the query fails,
     * the origin list is the curated one. No second fix and no second permission prompt: this is the fix that was
     * already taken.
     */
    fun onFix(location: LatLng) {
        nearbyJob?.cancel()
        nearbyJob = scope.launch {
            if (readiness.awaitReady(PARK_RIDE_STOPS_WAIT_MS) !is StopsReadiness.Ready) return@launch
            val found = attempt { gateway.nearbyStations(location) } ?: return@launch
            write { withNearby(found) }
        }
    }

    /**
     * The modes of the curated stops, for the row badges, once the stops are ready. It waits without a limit and
     * without polling: the wait is a collector on the readiness, and it ends with the scope. A table that never
     * becomes ready leaves the rows without badges, and nothing else changes.
     */
    fun loadModes(suggested: SuggestedStops) {
        scope.launch {
            readiness.state.first { it is StopsReadiness.Ready }
            val ids = (suggested.start + suggested.end).map { it.stopId }.distinct()
            attempt { gateway.modesFor(ids) }?.let { modes -> write { withModes(modes) } }
        }
    }
}
