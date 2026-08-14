package xyz.ksharma.krail.trip.planner.ui.search.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.core.speechtotext.SpeechToTextAvailability
import xyz.ksharma.krail.core.speechtotext.SpeechToTextResult
import xyz.ksharma.krail.core.speechtotext.SpeechToTextService
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopTextResolver
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private const val NEARBY_STOP_RADIUS_KM = 1.0

/**
 * Owns the AI box that `SearchStopRow` shows in place of its From/To fields: typed text ->
 * [AiTextService.extractTripIntent] -> resolve origin/destination text against the *same* stop
 * search [SearchStopViewModel][xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel]
 * already uses (no second search path) -> the row's own From/To fields. There is no separate
 * confirm card: the filled row *is* the confirmation, and the rider still taps Search itself.
 * Speaking feeds the same pipeline via [SpeechToTextService] (a final transcript becomes
 * [AiSearchInputUiState.typedText] and triggers [submit] directly), so typing and speaking are
 * the same one path in.
 *
 * Every unresolved outcome is explicit, not silent — unlike the alert-summary card (which
 * can legitimately render nothing), a rider who typed something and hit submit needs to see
 * *something* happen: [AiSearchInputPhase.DOWNLOADING] when the on-device model just isn't
 * downloaded yet (a real, temporary, retriable state — not a failure), and
 * [AiSearchInputPhase.UNRESOLVED] for flag-off/unsupported-device/a genuinely failed call.
 */
class AiSearchInputViewModel(
    private val aiTextService: AiTextService,
    private val speechToTextService: SpeechToTextService,
    // A chain of capabilities rather than one search call, so what counts as "smart" about
    // resolving a place is declared in one ordered list (see StopTextResolver) instead of
    // growing branches in here.
    private val stopTextResolver: StopTextResolver,
    private val nearbyStopsRepository: NearbyStopsRepository,
    // A lambda, not a UserLocationManager, because UserLocationManager only exists as
    // `rememberUserLocationManager()` — a @Composable factory (its underlying permission/
    // location controllers need Activity context tied to composition) — there is no Koin
    // single<UserLocationManager> to inject here. The Composable layer (AiSearchInputRoute)
    // builds this from rememberUserLocationManager() and passes it in via koinViewModel's
    // parametersOf, same pattern TrackTripViewModel already uses for its own
    // Composable-supplied param. NearbyStopsRepository itself has no such constraint, so it's
    // a plain constructor dependency rather than folded into this lambda too.
    private val resolveCurrentLocation: suspend () -> Location? = { null },
    private val isAiSearchInputEnabled: () -> Boolean = { false },
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiSearchInputUiState())
    val uiState: StateFlow<AiSearchInputUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null

    fun onEvent(event: AiSearchInputEvent) {
        when (event) {
            is AiSearchInputEvent.TypedTextChanged -> _uiState.update {
                // Typing is the way out of a mic problem, so the warning clears as they type.
                // Only on real text: the text field reports its initial empty value as soon as
                // it composes, which would otherwise clear the warning before it is ever read.
                it.copy(
                    typedText = event.text,
                    speechUnavailableReason = if (event.text.isEmpty()) it.speechUnavailableReason else null,
                )
            }

            AiSearchInputEvent.MicPermissionDenied ->
                _uiState.update { it.copy(isListening = false, speechUnavailableReason = "no_permission") }
            AiSearchInputEvent.OpenBox -> _uiState.update { it.copy(isBoxOpen = true) }
            AiSearchInputEvent.CloseBox -> closeBox()
            AiSearchInputEvent.Submit -> submit()
            AiSearchInputEvent.StartOver -> _uiState.update { AiSearchInputUiState() }
            AiSearchInputEvent.StartListening -> startListening()
            AiSearchInputEvent.StopListening -> stopListening()
        }
    }

    /**
     * Backing out of the box (system back) throws the draft away rather than keeping it for
     * the next time the wheel is tapped — the box reads as a fresh prompt every time it
     * opens, and a half-typed sentence from minutes ago would silently submit itself.
     */
    private fun closeBox() {
        // Cancelled, not just stopped: unlike [stopListening] — which keeps collecting so a
        // late final transcript still lands — backing out means the rider is done. Leaving the
        // collection alive would let a transcript arriving a second later run [submit] and
        // write stops into a row the rider had already walked away from.
        listeningJob?.cancel()
        listeningJob = null
        if (_uiState.value.isListening) speechToTextService.stopListening()
        _uiState.update { AiSearchInputUiState() }
    }

    /**
     * A final transcript sets [AiSearchInputUiState.typedText] and calls [submit] directly
     * instead of a separate speech-specific extraction path — same "just another way to
     * produce the string" shape [xyz.ksharma.krail.core.textrecognition.TextRecognitionService]
     * documents for OCR, so [AiTextService.extractTripIntent] only ever needs one caller.
     */
    private fun startListening() {
        // A previous session that is still winding down will fail a new start with a busy
        // error from the platform recogniser, which used to surface as a permission problem.
        // Stopping and starting again in quick succession is an easy thing for a rider to do,
        // so an existing session is torn down before a new one begins. Only when there is one:
        // a first start has nothing to stop, and asking anyway would be a no-op call the
        // platform still has to service.
        if (listeningJob != null) {
            listeningJob?.cancel()
            speechToTextService.stopListening()
        }
        listeningJob = null

        listeningJob = viewModelScope.launch {
            val availability = speechToTextService.checkAvailability()
            log("AiSearchInputViewModel: speech availability -> $availability")
            if (availability is SpeechToTextAvailability.Unavailable) {
                _uiState.update { it.copy(speechUnavailableReason = availability.reason) }
                return@launch
            }

            // Clears any previous failure message as the new attempt begins, so a retry does
            // not start under the last attempt's error.
            _uiState.update {
                it.copy(isListening = true, speechTranscript = "", speechUnavailableReason = null)
            }

            speechToTextService.startListening().collect { result ->
                when (result) {
                    is SpeechToTextResult.Partial ->
                        _uiState.update { it.copy(speechTranscript = result.text) }

                    is SpeechToTextResult.Final -> {
                        _uiState.update {
                            it.copy(isListening = false, speechTranscript = result.text, typedText = result.text)
                        }
                        submit()
                    }

                    is SpeechToTextResult.Error -> {
                        log("AiSearchInputViewModel: speech error -> ${result.reason}")
                        _uiState.update { it.copy(isListening = false, speechUnavailableReason = result.reason) }
                    }
                }
            }
        }
    }

    /**
     * Deliberately does NOT cancel [listeningJob] — [SpeechToTextService.stopListening] only
     * *requests* a graceful finish; both platform implementations still deliver an eventual
     * [SpeechToTextResult.Final] (or [SpeechToTextResult.Error]) through the same flow
     * [startListening] is collecting, then complete it themselves. Cancelling the job here
     * would tear that collection down first and silently drop whatever the rider just said.
     * `isListening` still flips immediately so the UI stops showing a live mic right away.
     */
    private fun stopListening() {
        speechToTextService.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    private fun submit() {
        val flagEnabled = isAiSearchInputEnabled()
        val text = _uiState.value.typedText.trim()
        log("AiSearchInputViewModel: submit, flagEnabled=$flagEnabled, textLength=${text.length}")
        if (!flagEnabled || text.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(phase = AiSearchInputPhase.EXTRACTING) }

            val availability = aiTextService.checkExtractionAvailability()
            log("AiSearchInputViewModel: extraction availability -> $availability")
            if (availability is AiAvailability.Unavailable) {
                val isTemporary = availability.reason == "downloadable" || availability.reason == "downloading"
                _uiState.update {
                    it.copy(phase = if (isTemporary) AiSearchInputPhase.DOWNLOADING else AiSearchInputPhase.UNRESOLVED)
                }
                return@launch
            }

            val extraction = aiTextService.extractTripIntent(text)
            log("AiSearchInputViewModel: extraction -> ${if (extraction == null) "null" else "parsed"}")
            if (extraction == null) {
                _uiState.update { it.copy(phase = AiSearchInputPhase.UNRESOLVED) }
                return@launch
            }

            val toStopItem = extraction.destinationText?.let { resolveStop(it) }
            val originText = extraction.originText
            val (fromText, fromStopItem) = if (originText != null) {
                originText to resolveStop(originText)
            } else {
                resolveCurrentLocationStop()
            }
            val dateTimeSelectionItem = resolveTimeIntent(extraction.timeIntent) ?: leaveNowDateTimeSelectionItem()

            val intent = ResolvedTripIntent(
                fromText = fromText,
                fromStopItem = fromStopItem,
                toText = extraction.destinationText,
                toStopItem = toStopItem,
                dateTimeSelectionItem = dateTimeSelectionItem,
                modeHints = extraction.modeHints,
            )
            _uiState.update { it.withResolution(intent) }
        }
    }

    /**
     * Hands the extracted text to [stopTextResolver], whose chain decides which capability
     * answers: the rider's own labels first, then the same stop search the search-stop screen
     * uses. The answer is treated as a guess — no auto-pick concept exists anywhere else in
     * this codebase, and it stays deliberately scoped to this AI context — so a miss resolves
     * to `null` and leaves the field for the rider to fill by hand.
     */
    private suspend fun resolveStop(query: String): StopItem? = stopTextResolver.resolve(query)

    /**
     * The rider didn't say where they're leaving from ("need to be at Central by 6:30pm" —
     * no "from X") — falls back to the nearest transit stop to their current GPS location
     * via [resolveNearbyStop], same as tapping a "near me" pill would, rather than leaving
     * the field blank and forcing a manual search for something this app can already answer
     * on its own. A permission denial or GPS failure (handled entirely inside
     * `UserLocationManager.getCurrentLocation`'s check→request→fetch flow, upstream of this
     * lambda) just means the field stays unresolved (pencil-editable), never a crash or a
     * blocking prompt the rider didn't ask for by typing into this flow in the first place.
     */
    private suspend fun resolveCurrentLocationStop(): Pair<String?, StopItem?> {
        val location = resolveCurrentLocation() ?: return null to null
        val stop = nearbyStopsRepository.getStopsNearby(
            centerLat = location.latitude,
            centerLon = location.longitude,
            radiusKm = NEARBY_STOP_RADIUS_KM,
            maxResults = 1,
        ).firstOrNull()?.let { StopItem(stopName = it.stopName, stopId = it.stopId) } ?: return null to null
        log("AiSearchInputViewModel: origin not stated, using nearby stop ${stop.stopName}")
        return stop.stopName to stop
    }
}

/**
 * One stop resolving is enough to be useful: the row is an ordinary editable From/To pair, so
 * whichever field the AI did resolve gets written and the rider fills the other one the usual
 * way. Nothing resolving at all is a failure, not a result — the box stays open with the
 * rider's text still in it so they can reword rather than retype.
 */
private fun AiSearchInputUiState.withResolution(intent: ResolvedTripIntent): AiSearchInputUiState =
    copy(
        phase = if (intent.hasAnyStop) AiSearchInputPhase.RESOLVED else AiSearchInputPhase.UNRESOLVED,
        resolved = if (intent.hasAnyStop) intent else null,
        isBoxOpen = !intent.hasAnyStop,
    )
