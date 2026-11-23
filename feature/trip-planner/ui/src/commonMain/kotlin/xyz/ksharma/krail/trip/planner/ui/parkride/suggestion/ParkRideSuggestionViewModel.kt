package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideUserFacilityEvent
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_PARK_RIDE_SUGGESTION_SHOWN
import xyz.ksharma.krail.trip.planner.ui.homemap.mapHomeEnabled
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideStationAdder
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/**
 * What the suggestion card shows, or nothing. [tile] is the closed Park & Ride tile of the station on offer:
 * its name, and the stored free spots and time when there are any.
 */
@Stable
internal data class ParkRideSuggestionState(val tile: ParkRideTile? = null)

internal sealed interface ParkRideSuggestionEvent {
    /** The rider's current position, or null while it is not known. The map home already tracks it. */
    data class LocationChanged(val fix: LatLng?) : ParkRideSuggestionEvent

    /** The card was drawn for [stationId]. The first one marks the suggestion as shown, for good. */
    data class Shown(val stationId: String) : ParkRideSuggestionEvent

    /** Add to home, through the picker's own add. */
    data object AddToHome : ParkRideSuggestionEvent

    /** Not now. It only dismisses. */
    data object NotNow : ParkRideSuggestionEvent
}

/** What the card is worked out from, so one value goes through the pipeline and not five. */
private data class SuggestionInputs(
    val hasSavedStation: Boolean,
    val home: LatLng?,
    val fix: LatLng?,
    val committed: ParkRideStationPickerItem?,
    val dismissed: Boolean,
)

/**
 * The one-time Park & Ride suggestion on the map home, `docs/MAP_HOME_UX.md` rule 47 and assumption A10 in
 * `docs/features/map-home/ERD.md`.
 *
 * **It is decided here, never in the screen.** The station on offer, and the fact that the card has been
 * drawn, live in this ViewModel, so a rotation shows the same card and a screen that is rebuilt cannot offer
 * a second one. The first time the card is drawn the screen says so and the one boolean
 * [KEY_PARK_RIDE_SUGGESTION_SHOWN] is written, the same key the Intro writes when it asks the question, so a
 * rider the Intro asked and a rider who has seen the card are both never asked again, whether or not they
 * added. From that moment the station is held, so a position that jitters never swaps the card under the
 * rider's thumb. The card goes when the rider taps Add to home or Not now, or when a station is saved by any
 * other way. A process that is killed with the card still up does not bring it back.
 *
 * **Cost.** No network call, ever. A rider who has been asked, or a build with the maps flag off, does nothing
 * but one preference read. Otherwise the catalogue is read once, each time something it depends on changes it
 * is one small local lookup (the saved stations, Home's stop, the stored readings of one station), and the
 * position is the one the map home already tracks, so there is no extra location fix. No timer, no polling.
 * Add to home is one local write through [ParkRideStationAdder], the picker's own.
 */
internal class ParkRideSuggestionViewModel(
    private val preferences: SandookPreferences,
    private val sources: ParkRideSuggestionSources,
    private val adder: ParkRideStationAdder,
    flag: Flag,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val mapHomeOn = mapHomeEnabled(flag)
    private val radiusKm = resolveParkRideSuggestionRadiusKm(flag)

    // Read once, on first use, so a rider asked in an earlier session never costs more than this one read.
    private val askedBefore: Boolean by lazy {
        preferences.getBoolean(KEY_PARK_RIDE_SUGGESTION_SHOWN) == true
    }
    private val stations: List<ParkRideStationPickerItem> by lazy { sources.stations() }

    private val fix = MutableStateFlow<LatLng?>(null)
    private val committed = MutableStateFlow<ParkRideStationPickerItem?>(null)
    private val dismissed = MutableStateFlow(false)

    /**
     * Held for a short grace after the screen leaves, so a rotation re-subscribes inside it. The station the
     * card committed to is in this ViewModel and not in this flow, so it survives the grace as well.
     */
    val uiState: StateFlow<ParkRideSuggestionState> = cardFlow()
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ParkRideSuggestionState(),
        )

    fun onEvent(event: ParkRideSuggestionEvent) {
        when (event) {
            is ParkRideSuggestionEvent.LocationChanged -> onLocationChanged(event.fix)
            is ParkRideSuggestionEvent.Shown -> onShown(event.stationId)
            ParkRideSuggestionEvent.AddToHome -> onAddToHome()
            ParkRideSuggestionEvent.NotNow -> dismissed.value = true
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun cardFlow(): Flow<ParkRideSuggestionState> = flow {
        // Nothing to work out without the map home, or for a rider who was asked before, here or in the Intro.
        if (!mapHomeOn || askedBefore) {
            emit(ParkRideSuggestionState())
            return@flow
        }
        emitAll(
            combine(
                sources.hasSavedStation(),
                sources.homePosition(),
                fix,
                committed,
                dismissed,
            ) { hasSavedStation, home, fix, committed, dismissed ->
                SuggestionInputs(hasSavedStation, home, fix, committed, dismissed)
            }.distinctUntilChanged().mapLatest { inputs -> stateFor(inputs) },
        )
    }

    private suspend fun stateFor(inputs: SuggestionInputs): ParkRideSuggestionState {
        if (inputs.dismissed || inputs.hasSavedStation) return ParkRideSuggestionState()
        val station = inputs.committed ?: stationToOffer(inputs) ?: return ParkRideSuggestionState()
        return ParkRideSuggestionState(tile = suggestionTile(station, sources.cachedReadings()))
    }

    private fun stationToOffer(inputs: SuggestionInputs): ParkRideStationPickerItem? {
        if (!canSuggest(mapHomeOn, askedBefore, inputs.hasSavedStation)) return null
        return stationToSuggest(suggestionAnchor(inputs.home, inputs.fix), stations, radiusKm)
    }

    /** The position only decides which station to offer, so once one is held or the card is gone it is ignored. */
    private fun onLocationChanged(position: LatLng?) {
        if (mapHomeOn && committed.value == null && !dismissed.value) fix.value = position
    }

    /**
     * The card was drawn. The first time, the station on screen is the one held and the boolean is written, off
     * the main thread. A card drawn again, as a screen that comes back draws it, changes nothing.
     */
    private fun onShown(stationId: String) {
        if (committed.value != null || dismissed.value) return
        val station = stations.firstOrNull { it.stationId == stationId } ?: return
        if (!committed.compareAndSet(expect = null, update = station)) return
        viewModelScope.launchWithExceptionHandler<ParkRideSuggestionViewModel>(ioDispatcher) {
            preferences.setBoolean(KEY_PARK_RIDE_SUGGESTION_SHOWN, true)
        }
    }

    /**
     * Add to home. The card goes at once, so a second tap cannot add twice, and the station goes through the
     * same add the picker uses, so it appears as a tile in the Park & Ride row from the table the home observes.
     */
    private fun onAddToHome() {
        val station = committed.value ?: return
        if (!dismissed.compareAndSet(expect = false, update = true)) return
        viewModelScope.launchWithExceptionHandler<ParkRideSuggestionViewModel>(ioDispatcher) {
            adder.add(station, ParkRideUserFacilityEvent.Source.HOME_SUGGESTION)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
