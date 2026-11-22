package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState

/**
 * How long typing must pause before the stop list is searched again. It is Search Stop's own pause, so the two
 * searches react at the same speed.
 */
internal const val INTRO_SEARCH_DEBOUNCE_MS = SearchStopViewModel.SEARCH_DEBOUNCE_MS

/**
 * What the destination and origin screens read from the stops table: the stations near the rider, from the fix the
 * Park & Ride question already took, and the modes of the stops it will list, both worked out in the background so
 * they are ready when the rider gets there, and the typed search. It runs in [scope], the view model's scope,
 * starts no timer and no repeat, and every job it starts ends with the scope.
 *
 * The rider's position arrives in [onFix], is used for one query inside one coroutine and goes out of scope with
 * it. It is never written to the state, never saved and never logged.
 */
internal class IntroStopsStep(
    private val scope: CoroutineScope,
    private val readiness: AppDataReadiness,
    private val gateway: IntroStopsGateway,
    private val read: () -> IntroState,
    private val write: (IntroState.() -> IntroState) -> Unit,
) {
    private var nearbyJob: Job? = null
    private var searchJob: Job? = null

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

    /**
     * The text in the search field is [text]. It is kept at once, and blank text shows the suggestions again at
     * once. Otherwise, after [INTRO_SEARCH_DEBOUNCE_MS] without more typing, the local stop search runs and its answer
     * replaces the suggestions. Typing on cancels the search that was waiting or running, so only the text the
     * rider ended on is answered.
     *
     * A stops table that is not ready cannot answer, and the field stays usable and finds nothing: the search waits
     * for the stops to be Ready, which is a collector and not a poll, and fills in as soon as they are. Stops that
     * failed, and a search that throws, leave the suggestions showing, with no error and no Try again.
     *
     * The text is never reported, logged or saved: it is in the state for the screen to read and nowhere else.
     */
    fun onQueryChanged(text: String) {
        if (read().query == text) return
        write { withQuery(text) }
        searchJob?.cancel()
        if (text.isBlank()) return
        searchJob = scope.launch {
            delay(INTRO_SEARCH_DEBOUNCE_MS)
            val answered = readiness.state.first { it is StopsReadiness.Ready || it is StopsReadiness.Failed }
            val found = if (answered is StopsReadiness.Ready) attempt { gateway.search(text) } else null
            write { withSearchResult(text, found) }
        }
    }

    /** The rider has left the screen they were typing on, so a search still waiting or running is not wanted. */
    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
    }
}
