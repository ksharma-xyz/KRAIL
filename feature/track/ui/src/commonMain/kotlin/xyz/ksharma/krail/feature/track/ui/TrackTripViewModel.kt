@file:Suppress(
    "LongMethod",
    "TooManyFunctions",
    "CyclomaticComplexMethod",
    "LoopWithTooManyJumpStatements",
    "ForbiddenComment",
)

package xyz.ksharma.krail.feature.track.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiDateString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.core.festival.model.NoFestival
import xyz.ksharma.krail.core.festival.model.greetingAndEmoji
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.share.ShareManager
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.LegTrackingInfo
import xyz.ksharma.krail.feature.track.LiveTrackingOverlay
import xyz.ksharma.krail.feature.track.TrackTripState
import xyz.ksharma.krail.feature.track.TrackedJourneyDisplay
import xyz.ksharma.krail.feature.track.TrackedLeg
import xyz.ksharma.krail.feature.track.TrackingConfig
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.feature.track.TripDeepLink
import xyz.ksharma.krail.feature.track.TripDeepLinkDecoder
import xyz.ksharma.krail.feature.track.ui.TripResponseMapper.findMatchingJourney
import xyz.ksharma.krail.feature.track.ui.TripResponseMapper.toTrackedJourneyDisplay
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("LongParameterList")
@OptIn(ExperimentalTime::class)
class TrackTripViewModel(
    private val encodedData: String?,
    private val tripPlanningService: TripPlanningService,
    private val trackingManager: TrackingManager,
    private val ioDispatcher: CoroutineDispatcher,
    private val festivalManager: FestivalManager,
    private val gtfsRealtimeRepository: GtfsRealtimeRepository,
    private val sandook: Sandook,
    private val shareManager: ShareManager,
) : ViewModel() {

    private val loadingEmoji: String by lazy {
        (festivalManager.festivalOnDate() ?: NoFestival()).greetingAndEmoji.second
    }

    private val _uiState = MutableStateFlow<TrackTripState>(TrackTripState.Initial)
    private val _clock = MutableStateFlow(Clock.System.now())
    private var clockJob: Job? = null

    /** Current time, ticking every second while a trip is being tracked. */
    val clock: StateFlow<Instant> = _clock

    /**
     * Countdown split into (label, value) so the UI can animate only the changing value.
     * e.g. ("Arriving in", "4m 32s"), ("Departed", "just now").
     */
    val countdownDisplay: StateFlow<Pair<String, String>> =
        combine(_uiState, _clock) { state, now ->
            when (state) {
                is TrackTripState.Tracking -> computeCountdown(state.journey, now)
                is TrackTripState.Arrived -> computeCountdown(state.journey, now)
                else -> "" to ""
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "" to "",
        )

    /**
     * Polling and the countdown clock are tied to this flow's subscriber lifecycle:
     * - [onStart]: resumes polling when the UI (re)subscribes after a navigation return.
     * - [onCompletion]: stops polling and the clock when the last subscriber disconnects
     *   (screen leaves, app backgrounds). [lastPollInstant] is kept so the next resume
     *   can smart-delay: no API call if within the [TrackingConfig.POLL_INTERVAL_MS] window.
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
                    "pollingActive=${pollingJob?.isActive}, " +
                    "vmEncodedData=${encodedData?.take(20)?.plus("…") ?: "null"}",
            )
            if (current is TrackTripState.Tracking && deepLink != null && pollingJob?.isActive != true) {
                log("TrackTrip: uiState.onStart — resuming poll (WhileSubscribed)")
                startPolling(deepLink)
            }
        }
        .onCompletion {
            // UI subscribers gone (screen left or app backgrounded) — pause polling and clock
            // until the next subscriber arrives. lastPollInstant is preserved so we can
            // calculate the remaining interval on resume.
            log("TrackTrip: uiState.onCompletion — pausing poll (no UI subscribers)")
            stopPolling()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrackTripState.Loading(),
        )

    private var pollingJob: Job? = null
    private var livePositionJob: Job? = null

    private var cachedStopCoordinates: Map<String, LatLng> = emptyMap()

    private val _liveOverlay = MutableStateFlow<LiveTrackingOverlay?>(null)

    /**
     * Live vehicle positions and stop delays. Collected only while the map panel is visible.
     * [SharingStarted.WhileSubscribed] pauses GTFS-RT polling when no UI is collecting —
     * same pattern as [uiState].
     */
    val liveOverlay: StateFlow<LiveTrackingOverlay?> = _liveOverlay
        .onStart {
            log("[LIVETRACK] liveOverlay.onStart — subscriber attached, starting live position polling")
            startLivePositionPolling()
        }
        .onCompletion {
            log("[LIVETRACK] liveOverlay.onCompletion — last subscriber gone, stopping live position polling")
            stopLivePositionPolling()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _stopCoordinates = MutableStateFlow<Map<String, LatLng>>(emptyMap())

    /**
     * stopId → LatLng, fetched once when journey display is first available.
     * feature/trip-planner/ui uses this to build JourneyMapUiState via TrackedJourneyMapMapper
     * (keeping the mapper in the UI layer where JourneyMap lives, avoiding a circular dep).
     */
    val stopCoordinates: StateFlow<Map<String, LatLng>> = _stopCoordinates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    private var lastPollInstant: Instant? = null

    // TODO: Evaluate whether this should instead be driven by a "minimum shown departure deviation"
    //  threshold — e.g. if real-time data says the train is > 2 min late AND we haven't started
    //  yet, allow the countdown to reappear. For now the latch is the safest UX default.
    private var hasPassedArrivalMoment = false

    init {
        @Suppress("MagicNumber")
        log(
            "[TRACK_RESOLVE] ViewModel created — " +
                "vmHash=${hashCode()}, " +
                "encodedData=${encodedData?.take(20)?.plus("…") ?: "null"}",
        )
        resolveInitialState()
    }

    @Suppress("MagicNumber")
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
                    logError("[TRACK_RESOLVE] failed to decode encodedData=${it.take(30)}…")
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

        // Expire any stale existing trip before evaluating the new deep link.
        if (existingTracked != null && isTripExpired(existingTracked.deepLink)) {
            log(
                "[TRACK_RESOLVE] existing trip expired " +
                    "(departure > ${TrackingConfig.DEPARTURE_EXPIRED_HOURS}h ago) → " +
                    "ArrivedAndFinished",
            )
            transitionToArrivedAndFinished()
            return
        }

        when {
            // New expired deep link → nothing to show, clean up and go back immediately
            newDeepLink != null && isTripExpired(newDeepLink) -> {
                log("[TRACK_RESOLVE] new deep link expired → ArrivedAndFinished")
                transitionToArrivedAndFinished()
            }

            // Existing trip already finished AND new trip requested → auto-clear and show Prompt.
            // No need to ask the user; the old trip is done.
            newDeepLink != null && existingTracked != null &&
                existingTracked.deepLink != newDeepLink && existingTracked.isArrived -> {
                log("[TRACK_RESOLVE] existing trip already arrived, clearing → Prompt for new trip")
                trackingManager.stop()
                _uiState.value = TrackTripState.Prompt(newDeepLink)
            }

            // New deep link + actively tracking a DIFFERENT trip → clear old and prompt for new.
            // A deep link tap is explicit user intent to switch trips, so we don't need confirmation.
            newDeepLink != null && existingTracked != null && existingTracked.deepLink != newDeepLink -> {
                log(
                    "[TRACK_RESOLVE] → Prompt (different trip requested, clearing previous: " +
                        "${existingTracked.deepLink.fromStopName} → ${existingTracked.deepLink.toStopName}), " +
                        "new trip ${newDeepLink.fromStopName} → ${newDeepLink.toStopName}",
                )
                trackingManager.stop()
                _uiState.value = TrackTripState.Prompt(newDeepLink)
            }

            // Resume existing tracking (same trip or opened from SavedTrips card with no encoded data)
            existingTracked != null -> {
                existingTracked.display?.let { display ->
                    if (existingTracked.isArrived) {
                        log("[TRACK_RESOLVE] → Arrived (restored from cache)")
                        _uiState.value = TrackTripState.Arrived(display)
                    } else {
                        log(
                            "[TRACK_RESOLVE] → Tracking (resumed) — " +
                                "${existingTracked.deepLink.fromStopName} → " +
                                "${existingTracked.deepLink.toStopName}",
                        )
                        _uiState.value = TrackTripState.Tracking(display)
                        startPolling(existingTracked.deepLink)
                    }
                } ?: run {
                    log(
                        "[TRACK_RESOLVE] → Loading (no display yet, starting poll for " +
                            "${existingTracked.deepLink.fromStopName})",
                    )
                    _uiState.value = TrackTripState.Loading(existingTracked.deepLink, loadingEmoji)
                    startPolling(existingTracked.deepLink)
                }
            }

            // New deep link, nothing tracked yet → show prompt
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

    /** True when [TrackingConfig.DEPARTURE_EXPIRED_HOURS] have elapsed since the scheduled departure. */
    private fun isTripExpired(deepLink: TripDeepLink): Boolean {
        val depInstant = runCatching { Instant.parse(deepLink.departureUtcDateTime) }.getOrNull()
            ?: return false
        return Clock.System.now() > depInstant + TrackingConfig.DEPARTURE_EXPIRED_HOURS.hours
    }

    /**
     * Transitions to [TrackTripState.ArrivedAndFinished]: stops polling, resets state,
     * removes the trip from the tracking manager. The screen observes this state and
     * navigates back automatically.
     */
    private fun transitionToArrivedAndFinished() {
        log("TrackTrip: → ArrivedAndFinished")
        stopPolling()
        hasPassedArrivalMoment = false
        lastPollInstant = null
        trackingManager.stop()
        _uiState.value = TrackTripState.ArrivedAndFinished
    }

    fun retry() {
        log("TrackTrip: retry() — re-resolving state")
        stopPolling()
        lastPollInstant = null
        resolveInitialState()
    }

    fun onStartTracking(deepLink: TripDeepLink) {
        log("TrackTrip: onStartTracking — ${deepLink.fromStopName} → ${deepLink.toStopName}")
        log("[LIVETRACK] onStartTracking — livePositionJob active=${livePositionJob?.isActive}")
        hasPassedArrivalMoment = false
        lastPollInstant = null
        gtfsRealtimeRepository.clearCache()
        trackingManager.start(deepLink)
        _uiState.value = TrackTripState.Loading(deepLink, loadingEmoji)
        startPolling(deepLink)
        // Re-kick live-position polling so the first GTFS-RT call happens immediately after
        // tracking starts, rather than waiting out the current retry-delay cycle.
        if (livePositionJob?.isActive != true) {
            log("[LIVETRACK] onStartTracking — livePositionJob was dead, restarting")
            startLivePositionPolling()
        } else {
            log("[LIVETRACK] onStartTracking — livePositionJob already active, no restart needed")
        }
    }

    fun onStopTracking() {
        log("TrackTrip: onStopTracking")
        hasPassedArrivalMoment = false
        lastPollInstant = null
        stopPolling()
        trackingManager.stop()
    }

    fun shareTrip(bitmap: ImageBitmap, text: String) {
        viewModelScope.launch {
            shareManager.shareImage(bitmap = bitmap, text = text)
                .onFailure { error -> logError("error sharing track trip", error) }
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                _clock.value = Clock.System.now()
                delay(1.seconds)
            }
        }
    }

    private fun startPolling(deepLink: TripDeepLink) {
        stopPolling()
        if (isTripExpired(deepLink)) {
            log("TrackTrip: startPolling — trip already expired, skipping")
            transitionToArrivedAndFinished()
            return
        }
        startClock()
        log(
            "TrackTrip: startPolling — ${deepLink.fromStopName} → ${deepLink.toStopName}, " +
                "interval=${TrackingConfig.POLL_INTERVAL_MS}ms, vmHash=${hashCode()}",
        )
        pollingJob = viewModelScope.launch {
            while (isActive) {
                // Guard before each API call — trip may expire between polls
                if (isTripExpired(deepLink)) {
                    log(
                        "TrackTrip: poll loop — departure expired " +
                            "(>${TrackingConfig.DEPARTURE_EXPIRED_HOURS}h ago)",
                    )
                    transitionToArrivedAndFinished()
                    break
                }

                // Smart delay: if we polled recently (e.g. user returned to this screen),
                // wait out the remaining interval instead of hitting the API immediately.
                // lastPollInstant == null means first poll — go straight through.
                val elapsedMs =
                    lastPollInstant?.let { (Clock.System.now() - it).inWholeMilliseconds }
                        ?: Long.MAX_VALUE
                val remainingWaitMs =
                    (TrackingConfig.POLL_INTERVAL_MS - elapsedMs).coerceAtLeast(0L)
                if (remainingWaitMs > 0L) {
                    log("TrackTrip: poll — polled ${elapsedMs}ms ago, waiting ${remainingWaitMs}ms before next call")
                    delay(remainingWaitMs)
                    continue
                }

                setRefreshing(true)
                fetchAndUpdate(deepLink)
                lastPollInstant = Clock.System.now()
                setRefreshing(false)
                val current = _uiState.value
                when {
                    current is TrackTripState.Arrived -> {
                        log("TrackTrip: poll loop exit — Arrived")
                        break
                    }

                    current is TrackTripState.ArrivedAndFinished -> {
                        log("TrackTrip: poll loop exit — ArrivedAndFinished")
                        break
                    }

                    current !is TrackTripState.Tracking -> {
                        log("TrackTrip: poll loop exit — state=${current::class.simpleName}")
                        break
                    }

                    else -> delay(TrackingConfig.POLL_INTERVAL_MS)
                }
            }
        }
    }

    private fun setRefreshing(refreshing: Boolean) {
        val current = _uiState.value
        if (current is TrackTripState.Tracking) {
            _uiState.value = current.copy(isRefreshing = refreshing)
        }
    }

    private suspend fun fetchAndUpdate(deepLink: TripDeepLink) {
        log("TrackTrip: fetchAndUpdate — ${deepLink.fromStopName} → ${deepLink.toStopName}")
        runCatching {
            val depInstant = Instant.parse(deepLink.departureUtcDateTime)
            val date = depInstant.toApiDateString()
            val time = depInstant.toApiTimeString()
            log("TrackTrip: fetchAndUpdate — calling API date=$date time=$time")

            val response = withContext(ioDispatcher) {
                tripPlanningService.trip(
                    originStopId = deepLink.fromStopId,
                    destinationStopId = deepLink.toStopId,
                    date = date,
                    time = time,
                    excludeProductClassSet = deepLink.excludedProductClasses.toSet(),
                )
            }
            val journeyCount = response.journeys?.size ?: 0
            val deepLinkLegIds = deepLink.legs.map { it.transportationId }
            log(
                "TrackTrip: fetchAndUpdate — response received — $journeyCount journeys, " +
                    "seeking legs=$deepLinkLegIds",
            )

            val matchedJourney = response.findMatchingJourney(deepLink)
            if (matchedJourney == null) {
                logError(
                    "TrackTrip: fetchAndUpdate — NotFound: no journey matched deep link legs " +
                        "(deepLink legs=${deepLink.legs.map { it.transportationId }}). " +
                        "Possible causes: trip cancelled, schedule changed, or departure >2h ago",
                )
                _uiState.value = TrackTripState.NotFound
                return
            }
            log("TrackTrip: fetchAndUpdate — journey matched, building display")

            val display = matchedJourney.toTrackedJourneyDisplay(deepLink) ?: run {
                logError("TrackTrip: fetchAndUpdate — Error: failed to build TrackedJourneyDisplay")
                _uiState.value = TrackTripState.Error
                return
            }

            trackingManager.update(display)
            fetchStopCoordinatesIfNeeded(display)

            val now = Clock.System.now()
            val arrivalInstant = Instant.parse(display.destinationUtcDateTime)
            val arrivalFinishedAt = arrivalInstant + TrackingConfig.ARRIVAL_FINISHED_MINUTES.minutes
            when {
                now > arrivalFinishedAt -> {
                    log(
                        "TrackTrip: fetchAndUpdate — ArrivedAndFinished " +
                            "(>${TrackingConfig.ARRIVAL_FINISHED_MINUTES}min past arrival)",
                    )
                    transitionToArrivedAndFinished()
                }

                now > arrivalInstant -> {
                    log(
                        "TrackTrip: fetchAndUpdate — Arrived " +
                            "(departs ${display.originTime}, arrives ${display.destinationTime})",
                    )
                    trackingManager.markArrived()
                    _uiState.value = TrackTripState.Arrived(display)
                    scheduleAutoRemoval(arrivalInstant)
                }

                else -> {
                    log(
                        "TrackTrip: fetchAndUpdate — Tracking " +
                            "(departs ${display.originTime}, arrives ${display.destinationTime})",
                    )
                    _uiState.value = TrackTripState.Tracking(journey = display)
                }
            }
        }.onFailure { error ->
            logError("TrackTrip: fetchAndUpdate — exception during fetch/parse", error)
            val cachedDisplay = trackingManager.tracked.value?.display
            if (cachedDisplay != null) {
                log("TrackTrip: fetchAndUpdate — falling back to cached display")
                _uiState.value = TrackTripState.Tracking(journey = cachedDisplay)
            } else {
                logError("TrackTrip: fetchAndUpdate — no cached display, showing Error state")
                _uiState.value = TrackTripState.Error
            }
        }
    }

    private fun scheduleAutoRemoval(arrivalInstant: Instant) {
        viewModelScope.launch {
            val removeAt = arrivalInstant + TrackingConfig.ARRIVAL_FINISHED_MINUTES.minutes
            val delayMs = (removeAt - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(0)
            log("TrackTrip: scheduleAutoRemoval — ArrivedAndFinished in ${delayMs}ms")
            delay(delayMs)
            transitionToArrivedAndFinished()
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        clockJob?.cancel()
        clockJob = null
    }

    private fun startLivePositionPolling() {
        log("[LIVETRACK] startLivePositionPolling — starting job (prev active=${livePositionJob?.isActive})")
        livePositionJob?.cancel()
        livePositionJob = viewModelScope.launch {
            log("[LIVETRACK] livePositionJob launched")
            while (isActive) {
                val tracked = trackingManager.tracked.value
                if (tracked == null) {
                    // Not tracking yet (e.g. Prompt state) — wait and retry instead of breaking,
                    // so the loop is still alive when tracking starts.
                    log("[LIVETRACK] tracked=null, waiting ${TrackingConfig.GTFS_RT_POLL_INTERVAL_MS}ms before retry")
                    delay(TrackingConfig.GTFS_RT_POLL_INTERVAL_MS)
                    continue
                }
                if (tracked.isArrived) {
                    log("[LIVETRACK] tracked.isArrived=true — stopping live position polling")
                    break
                }
                val display = tracked.display
                if (display == null) {
                    // Trip API hasn't returned yet — retry quickly so the first GTFS-RT poll
                    // fires immediately after trip data lands rather than waiting a full 30s cycle.
                    log("[LIVETRACK] tracked.display=null (trip API not yet returned), retrying in 2s")
                    delay(GTFS_RT_RETRY_DELAY_MS)
                    continue
                }
                val transportLegCount = display.legs.filterIsInstance<TrackedLeg.Transport>().size
                log("[LIVETRACK] pollLivePositions — legs=${display.legs.size}, transport legs=$transportLegCount")
                pollLivePositions(display)
                log("[LIVETRACK] pollLivePositions complete — sleeping ${TrackingConfig.GTFS_RT_POLL_INTERVAL_MS}ms")
                delay(TrackingConfig.GTFS_RT_POLL_INTERVAL_MS)
            }
            log("[LIVETRACK] livePositionJob loop exited")
        }
    }

    private fun stopLivePositionPolling() {
        log("[LIVETRACK] stopLivePositionPolling — cancelling job (active=${livePositionJob?.isActive})")
        livePositionJob?.cancel()
        livePositionJob = null
    }

    private suspend fun pollLivePositions(display: TrackedJourneyDisplay) {
        val legInfos = display.legs.mapIndexedNotNull { index, leg ->
            if (leg !is TrackedLeg.Transport) return@mapIndexedNotNull null
            LegTrackingInfo(
                legIndex = index,
                transportMode = leg.transportMode,
                lineName = leg.lineName,
                realtimeTripId = leg.realtimeTripId,
            )
        }
        if (legInfos.isEmpty()) {
            log("[LIVETRACK] pollLivePositions — no transport legs, skipping")
            return
        }
        log("[LIVETRACK] pollLivePositions — ${legInfos.size} legs, originUtc=${display.originUtcDateTime}")
        val overlay = gtfsRealtimeRepository.pollLiveTracking(
            legs = legInfos,
            originUtcDateTime = display.originUtcDateTime,
        )
        log(
            "[LIVETRACK] pollLivePositions done — " +
                "positions=${overlay.vehiclePositions.size} delays=${overlay.stopDelays.size}",
        )
        _liveOverlay.value = overlay
    }

    private fun fetchStopCoordinatesIfNeeded(display: TrackedJourneyDisplay) {
        if (cachedStopCoordinates.isNotEmpty()) return

        // Primary path: use coordinates embedded in the API response (StopSequence.coord).
        // This avoids a DB lookup and works even when the GTFS-static DB has no record for
        // a platform-level stop id.
        val apiCoords = display.legs
            .filterIsInstance<TrackedLeg.Transport>()
            .flatMap { it.stops }
            .filter { it.stopId.isNotBlank() && it.lat != null && it.lon != null }
            .associate { it.stopId to LatLng(latitude = it.lat!!, longitude = it.lon!!) }

        if (apiCoords.isNotEmpty()) {
            cachedStopCoordinates = apiCoords
            _stopCoordinates.value = apiCoords
            log("[LIVETRACK] stop coordinates from API response — ${apiCoords.size} stops")
            return
        }

        // Fallback: DB lookup (for cached/restored journeys that pre-date the lat/lon fields).
        log("[LIVETRACK] stop coordinates not in API response, falling back to DB lookup")
        viewModelScope.launch(ioDispatcher) {
            val allStopIds = display.legs
                .asSequence()
                .filterIsInstance<TrackedLeg.Transport>()
                .flatMap { it.stops }
                .map { it.stopId }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()

            if (allStopIds.isEmpty()) {
                log("[LIVETRACK] stop coordinates DB fallback — no stop IDs, map will stay loading")
                return@launch
            }

            cachedStopCoordinates = sandook.selectStopCoordinatesBatch(allStopIds)
                .mapValues { (_, pair) -> LatLng(latitude = pair.first, longitude = pair.second) }

            log("TrackTrip: stop coordinates loaded — ${cachedStopCoordinates.size}/${allStopIds.size} found")
            _stopCoordinates.value = cachedStopCoordinates
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    @Suppress("MagicNumber")
    private fun computeCountdown(
        journey: TrackedJourneyDisplay,
        now: Instant,
    ): Pair<String, String> {
        val originInstant = runCatching { Instant.parse(journey.originUtcDateTime) }.getOrNull()
            ?: return "" to ""
        // Use the last transport stop's utcTime as destination — identical source to what
        // TrackedLegView uses, so CountdownCard and the timeline always agree.
        val lastStopUtc =
            (journey.legs.lastOrNull { it is TrackedLeg.Transport } as? TrackedLeg.Transport)
                ?.stops
                ?.lastOrNull()
                ?.utcTime
        val destinationInstant = lastStopUtc
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: runCatching { Instant.parse(journey.destinationUtcDateTime) }.getOrNull()
            ?: return "" to ""
        return if (now < originInstant) {
            val secs = (originInstant - now).inWholeSeconds
            val value = when {
                secs < SECONDS_PER_MINUTE -> "${secs}s"
                secs < SECONDS_PER_HOUR -> {
                    val mins = secs / SECONDS_PER_MINUTE
                    val rem = secs % SECONDS_PER_MINUTE
                    if (rem > 0L) "${mins}m ${rem}s" else "${mins}m"
                }

                else -> (originInstant - now).toGenericFormattedTimeString()
            }
            LABEL_DEPARTING_IN to value
        } else {
            val duration = destinationInstant - now
            val totalSeconds = duration.inWholeSeconds

            // One-way latch: once we've crossed into arrived territory, never go back.
            if (totalSeconds <= 0L) hasPassedArrivalMoment = true

            when {
                hasPassedArrivalMoment && totalSeconds > 0L -> LABEL_ARRIVED to LABEL_JUST_NOW
                totalSeconds <= -SECONDS_PER_MINUTE -> LABEL_ARRIVED to duration.toGenericFormattedTimeString()
                totalSeconds < 0L -> LABEL_ARRIVED to LABEL_JUST_NOW
                totalSeconds < SECONDS_PER_MINUTE -> LABEL_ARRIVING_IN to "${totalSeconds}s"
                totalSeconds < SECONDS_PER_HOUR -> {
                    val mins = totalSeconds / SECONDS_PER_MINUTE
                    val secs = totalSeconds % SECONDS_PER_MINUTE
                    LABEL_ARRIVING_IN to if (secs > 0L) "${mins}m ${secs}s" else "${mins}m"
                }

                else -> LABEL_ARRIVING_IN to duration.toGenericFormattedTimeString()
            }
        }
    }

    companion object {
        private const val SECONDS_PER_MINUTE = 60L
        private const val SECONDS_PER_HOUR = 3600L
        private const val LABEL_ARRIVED = "Arrived"
        private const val LABEL_JUST_NOW = "just now"
        private const val LABEL_ARRIVING_IN = "Arriving in"
        private const val LABEL_DEPARTING_IN = "Departing in"
        private const val GTFS_RT_RETRY_DELAY_MS = 2_000L
    }
}
