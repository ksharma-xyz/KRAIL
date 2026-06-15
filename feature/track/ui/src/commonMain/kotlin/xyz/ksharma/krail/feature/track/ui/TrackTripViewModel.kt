package xyz.ksharma.krail.feature.track.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.core.festival.model.NoFestival
import xyz.ksharma.krail.core.festival.model.greetingAndEmoji
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.share.ShareManager
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.LiveTrackingOverlay
import xyz.ksharma.krail.feature.track.TrackTripState
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.feature.track.TripDeepLink
import xyz.ksharma.krail.feature.track.TripDeepLinkDecoder
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("LongParameterList")
@OptIn(ExperimentalTime::class)
class TrackTripViewModel(
    private val encodedData: String?,
    tripPlanningService: TripPlanningService,
    private val trackingManager: TrackingManager,
    ioDispatcher: CoroutineDispatcher,
    private val festivalManager: FestivalManager,
    gtfsRealtimeRepository: GtfsRealtimeRepository,
    sandook: Sandook,
    private val shareManager: ShareManager,
    val isTripTrackingEnabled: Boolean,
) : ViewModel() {

    private val loadingEmoji: String by lazy {
        (festivalManager.festivalOnDate() ?: NoFestival()).greetingAndEmoji.second
    }

    private val _uiState = MutableStateFlow<TrackTripState>(TrackTripState.Initial)

    private val tripPoller = TripPoller(
        scope = viewModelScope,
        ioDispatcher = ioDispatcher,
        tripPlanningService = tripPlanningService,
        trackingManager = trackingManager,
        gtfsRealtimeRepository = gtfsRealtimeRepository,
        sandook = sandook,
        state = _uiState,
    )

    /** Current time, ticking every second while a trip is being tracked. */
    val clock: StateFlow<Instant> = tripPoller.clock

    /**
     * Countdown split into (label, value) so the UI can animate only the changing
     * value. e.g. ("Arriving in", "4m 32s"), ("Arrived", "just now").
     */
    val countdownDisplay: StateFlow<Pair<String, String>> = tripPoller.countdownDisplay

    /**
     * Polling and the countdown clock are tied to this flow's subscriber lifecycle:
     * - [onStart]: resumes polling when the UI (re)subscribes after a navigation return.
     * - [onCompletion]: stops polling and the clock when the last subscriber disconnects
     *   (screen leaves, app backgrounds). The poller's `lastPollInstant` is kept so the
     *   next resume can smart-delay: no API call if within
     *   [xyz.ksharma.krail.feature.track.TrackingConfig.POLL_INTERVAL_MS].
     * - [SharingStarted.WhileSubscribed] with a 5 s stop timeout tolerates quick config
     *   changes (rotation) without an unnecessary stop/restart cycle.
     */
    val uiState: StateFlow<TrackTripState> = _uiState
        .onStart {
            currentCoroutineContext().ensureActive()
            val current = _uiState.value
            val deepLink = trackingManager.tracked.value?.deepLink
            log(
                "TrackTrip: uiState.onStart — state=${current::class.simpleName}, " +
                    "trackedTrip=${deepLink?.fromStopName ?: "none"} → ${deepLink?.toStopName ?: "none"}, " +
                    "pollingActive=${tripPoller.isPollingActive}, " +
                    "vmEncodedData=${encodedData?.take(VM_LOG_PREFIX_LEN)?.plus("…") ?: "null"}",
            )
            if (current is TrackTripState.Tracking && deepLink != null && !tripPoller.isPollingActive) {
                log("TrackTrip: uiState.onStart — resuming poll (WhileSubscribed)")
                tripPoller.startPolling(deepLink)
            }
        }
        .onCompletion {
            // UI subscribers gone (screen left or app backgrounded) — pause polling and
            // clock until the next subscriber arrives. The poller's `lastPollInstant`
            // is preserved so we can calculate the remaining interval on resume.
            log("TrackTrip: uiState.onCompletion — pausing poll (no UI subscribers)")
            tripPoller.stopPolling()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
            initialValue = TrackTripState.Loading(),
        )

    val liveOverlay: StateFlow<LiveTrackingOverlay?> = tripPoller.liveOverlay

    val stopCoordinates: StateFlow<Map<String, LatLng>> = tripPoller.stopCoordinates

    init {
        log(
            "[TRACK_RESOLVE] ViewModel created — " +
                "vmHash=${hashCode()}, " +
                "encodedData=${encodedData?.take(VM_LOG_PREFIX_LEN)?.plus("…") ?: "null"}",
        )
        resolveInitialState()
    }

    private fun resolveInitialState() {
        val existingTracked = trackingManager.tracked.value
        val newDeepLink = encodedData?.let {
            TripDeepLinkDecoder.decode(it).also { decoded ->
                if (decoded != null) {
                    log(
                        "[TRACK_RESOLVE] decoded deep link — ${decoded.fromStopName} → ${decoded.toStopName}, " +
                            "depUtc=${decoded.departureUtcDateTime}, legs=${decoded.legs.size}",
                    )
                } else {
                    logError("[TRACK_RESOLVE] failed to decode encodedData=${it.take(DECODE_LOG_PREFIX_LEN)}…")
                }
            }
        }

        val isSameTrip = existingTracked != null && newDeepLink != null &&
            existingTracked.deepLink == newDeepLink
        log(
            "[TRACK_RESOLVE] resolveInitialState — " +
                "existingTracked=${existingTracked?.deepLink?.fromStopName ?: "none"} → " +
                "${existingTracked?.deepLink?.toStopName ?: "none"}, " +
                "existingIsArrived=${existingTracked?.isArrived}, " +
                "newDeepLink=${newDeepLink?.fromStopName ?: "none"} → ${newDeepLink?.toStopName ?: "none"}, " +
                "isSameTrip=$isSameTrip, " +
                "hasExistingDisplay=${existingTracked?.display != null}",
        )

        if (existingTracked != null && tripPoller.isTripExpired(existingTracked.deepLink)) {
            log("[TRACK_RESOLVE] existing trip expired → ArrivedAndFinished")
            tripPoller.transitionToArrivedAndFinished()
            return
        }

        resolveFromState(existingTracked, newDeepLink)
    }

    private fun resolveFromState(existingTracked: TrackedJourney?, newDeepLink: TripDeepLink?) {
        when {
            newDeepLink != null && tripPoller.isTripExpired(newDeepLink) -> {
                log("[TRACK_RESOLVE] new deep link expired → ArrivedAndFinished")
                tripPoller.transitionToArrivedAndFinished()
            }
            newDeepLink != null && existingTracked != null &&
                existingTracked.deepLink != newDeepLink && existingTracked.isArrived -> {
                log("[TRACK_RESOLVE] existing trip already arrived, clearing → Prompt for new trip")
                trackingManager.stop()
                _uiState.value = TrackTripState.Prompt(newDeepLink)
            }
            newDeepLink != null && existingTracked != null && existingTracked.deepLink != newDeepLink -> {
                log(
                    "[TRACK_RESOLVE] → Prompt (different trip requested, clearing previous: " +
                        "${existingTracked.deepLink.fromStopName} → ${existingTracked.deepLink.toStopName}), " +
                        "new trip ${newDeepLink.fromStopName} → ${newDeepLink.toStopName}",
                )
                trackingManager.stop()
                _uiState.value = TrackTripState.Prompt(newDeepLink)
            }
            existingTracked != null -> handleResumeExistingTrip(existingTracked)
            newDeepLink != null -> {
                log("[TRACK_RESOLVE] → Prompt (${newDeepLink.fromStopName} → ${newDeepLink.toStopName})")
                _uiState.value = TrackTripState.Prompt(newDeepLink)
            }
            else -> {
                logError("[TRACK_RESOLVE] → Error (no encodedData, no existing tracking)")
                _uiState.value = TrackTripState.Error
            }
        }
    }

    private fun handleResumeExistingTrip(existingTracked: TrackedJourney) {
        existingTracked.display?.let { display ->
            if (existingTracked.isArrived) {
                log("[TRACK_RESOLVE] → Arrived (restored from cache)")
                _uiState.value = TrackTripState.Arrived(display)
            } else {
                log(
                    "[TRACK_RESOLVE] → Tracking (resumed) — " +
                        "${existingTracked.deepLink.fromStopName} → ${existingTracked.deepLink.toStopName}",
                )
                _uiState.value = TrackTripState.Tracking(display)
                tripPoller.startPolling(existingTracked.deepLink)
            }
        } ?: run {
            log(
                "[TRACK_RESOLVE] → Loading (no display yet, starting poll for " +
                    "${existingTracked.deepLink.fromStopName})",
            )
            _uiState.value = TrackTripState.Loading(existingTracked.deepLink, loadingEmoji)
            tripPoller.startPolling(existingTracked.deepLink)
        }
    }

    fun retry() {
        log("TrackTrip: retry() — re-resolving state")
        tripPoller.stopPolling()
        tripPoller.reset()
        resolveInitialState()
    }

    fun onStartTracking(deepLink: TripDeepLink) {
        log("TrackTrip: onStartTracking — ${deepLink.fromStopName} → ${deepLink.toStopName}")
        tripPoller.reset()
        trackingManager.start(deepLink)
        _uiState.value = TrackTripState.Loading(deepLink, loadingEmoji)
        tripPoller.startPolling(deepLink)
        // Re-kick live-position polling so the first GTFS-RT call happens immediately
        // after tracking starts, rather than waiting out the current retry-delay cycle.
        if (!tripPoller.isLivePositionPollingActive) {
            log("[LIVETRACK] onStartTracking — livePositionJob was dead, restarting")
            tripPoller.startLivePositionPolling()
        } else {
            log("[LIVETRACK] onStartTracking — livePositionJob already active, no restart needed")
        }
    }

    fun onStopTracking() {
        log("TrackTrip: onStopTracking")
        tripPoller.stopPolling()
        trackingManager.stop()
    }

    fun shareTrip(bitmap: ImageBitmap, text: String) {
        viewModelScope.launch {
            shareManager.shareImage(bitmap = bitmap, text = text)
                .onFailure { error -> logError("error sharing track trip", error) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tripPoller.stopPolling()
    }

    companion object {
        private const val WHILE_SUBSCRIBED_TIMEOUT_MS = 5_000L
        private const val VM_LOG_PREFIX_LEN = 20
        private const val DECODE_LOG_PREFIX_LEN = 30
    }
}
