package xyz.ksharma.krail.trip.planner.ui.search.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.core.speechtotext.SpeechToTextAvailability
import xyz.ksharma.krail.core.speechtotext.SpeechToTextResult
import xyz.ksharma.krail.core.speechtotext.SpeechToTextService
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopTextResolver
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private const val NEARBY_STOP_RADIUS_KM = 1.0

// Ceiling on one listening session. The rider's stop button is the real control; ending it
// otherwise is normally the recogniser's call, since it stops when they stop talking, with the
// silence windows widened on the Android side so a thinking pause mid sentence does not count as
// finished. Those windows are hints an OEM recogniser may ignore, and one that never reports an
// end leaves a live microphone and a running waveform with no way out but backing off the sheet.
//
// Ten seconds is longer than anyone takes to say where they are going. A rider still mid sentence
// at ten is not cut off: STILL_SPEAKING_WINDOW is the stretch before the ceiling that decides
// whether words are still arriving, and if they are, EXTENSION runs the session to fifteen, which
// is a hard stop. Measured on words arriving in that window rather than at all, because every
// session has words in it somewhere.
//
// STILL_SPEAKING_WINDOW must not be shorter than the recogniser's own silence tolerance
// (SILENCE_THAT_ENDS_THE_SESSION_MILLIS in AndroidSpeechToTextService, currently the same four
// seconds). If it were, this ceiling could end a session the recogniser still considered live:
// a rider who paused at eight seconds and was about to carry on would look finished to us and
// unfinished to it, and we would be the ones who cut them off. Kept in step by hand, since the
// two live in different modules on purpose - this one must not depend on a platform source set.
private const val LISTENING_CEILING_MILLIS = 10_000L
private const val LISTENING_EXTENSION_MILLIS = 5_000L
private const val STILL_SPEAKING_WINDOW_MILLIS = 4_000L

private const val AI_OUTCOME_TAG = "[AI_OUTCOME]"

// Reasons the UI branches on. Strings rather than a type because SpeechToTextService reports
// its own platform reasons through the same field, and this side has to hold both.
internal const val MIC_DENIED = "no_permission"
internal const val MIC_NEEDS_SETTINGS = "needs_settings"
internal const val MIC_UNSUPPORTED = "unsupported"

/**
 * Owns the AI search sheet the home screen opens over itself: typed text ->
 * [AiTextService.extractTripIntent] -> resolve origin/destination text against the *same* stop
 * search [SearchStopViewModel][xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel]
 * already uses (no second search path) -> the home row's own From/To fields. The sheet is an
 * input method, not a destination: it writes into the same two fields a rider would fill by
 * hand, and everything downstream of it is the ordinary trip flow.
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

    init {
        // Read here rather than through `stateIn(WhileSubscribed)`: this state is read
        // directly as `uiState.value` in places with no active collector, and a shared flow
        // would report its initial value to all of them. The ViewModel is built on screen
        // entry, so this is re-read often enough, and [AiSearchInputEvent.OpenInput] reads it
        // again at the moment it matters.
        _uiState.update { it.copy(isFeatureEnabled = isAiSearchInputEnabled()) }
    }

    private var listeningJob: Job? = null
    private var listeningTimeoutJob: Job? = null

    // Only ever compared against an earlier reading of itself, never read as a total.
    private var wordsHeardSoFar: Int = 0

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
                _uiState.update { it.copy(isListening = false, speechUnavailableReason = MIC_DENIED) }

            AiSearchInputEvent.MicPermissionBlocked ->
                _uiState.update { it.copy(isListening = false, speechUnavailableReason = MIC_NEEDS_SETTINGS) }

            AiSearchInputEvent.SpeechUnsupported ->
                _uiState.update { it.copy(isListening = false, speechUnavailableReason = MIC_UNSUPPORTED) }
            // Guarded as well as hidden. The button is gone when the feature is off, so this
            // can only be reached by a caller that has not been told; opening a sheet whose
            // only action is inert is the failure this is here to prevent.
            AiSearchInputEvent.OpenInput -> _uiState.update {
                it.copy(isInputOpen = isAiSearchInputEnabled(), isFeatureEnabled = isAiSearchInputEnabled())
            }
            AiSearchInputEvent.CloseInput -> closeInput()
            AiSearchInputEvent.Submit -> submit()
            // Everything the rider produced is thrown away; what the app knows about itself
            // is not, or starting over would hide the way back in.
            AiSearchInputEvent.StartOver ->
                _uiState.update { AiSearchInputUiState(isFeatureEnabled = it.isFeatureEnabled) }
            AiSearchInputEvent.StartListening -> startListening()
            AiSearchInputEvent.StopListening -> stopListening()
        }
    }

    /**
     * Dismissing the sheet (swipe down, scrim tap, system back) throws the draft away rather
     * than keeping it for the next time the wheel is tapped — the sheet reads as a fresh
     * prompt every time it opens, and a half-typed sentence from minutes ago would silently
     * submit itself.
     */
    private fun closeInput() {
        // Cancelled, not just stopped: unlike [stopListening] — which keeps collecting so a
        // late final transcript still lands — backing out means the rider is done. Leaving the
        // collection alive would let a transcript arriving a second later run [submit] and
        // write stops into a row the rider had already dismissed the sheet on.
        listeningJob?.cancel()
        listeningJob = null
        listeningTimeoutJob?.cancel()
        listeningTimeoutJob = null
        if (_uiState.value.isListening) speechToTextService.stopListening()
        _uiState.update { AiSearchInputUiState(isFeatureEnabled = it.isFeatureEnabled) }
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
        listeningTimeoutJob?.cancel()

        listeningJob = viewModelScope.launch {
            val availability = speechToTextService.checkAvailability()
            if (availability is SpeechToTextAvailability.Unavailable) {
                _uiState.update { it.copy(speechUnavailableReason = availability.reason) }
                logOutcome(reason = "speech_unavailable")
                return@launch
            }

            // Clears any previous failure message as the new attempt begins, so a retry does
            // not start under the last attempt's error.
            _uiState.update {
                it.copy(isListening = true, speechTranscript = "", speechUnavailableReason = null)
            }

            startListeningTimeout()

            speechToTextService.startListening().collect { result ->
                when (result) {
                    is SpeechToTextResult.Partial -> {
                        // Counted, not timestamped: the ceiling only needs to know whether new
                        // words arrived during a window, and a counter is readable from a test
                        // running on virtual time where a wall clock is not.
                        wordsHeardSoFar++
                        _uiState.update { it.copy(speechTranscript = result.text) }
                    }

                    is SpeechToTextResult.Final -> {
                        _uiState.update {
                            it.copy(isListening = false, speechTranscript = result.text, typedText = result.text)
                        }
                        submit()
                    }

                    is SpeechToTextResult.Error -> {
                        _uiState.update { it.copy(isListening = false, speechUnavailableReason = result.reason) }
                        logOutcome(reason = "speech_error")
                    }
                }
            }
        }
    }

    /**
     * The backstop for a recogniser that never says it is done. Stopping is the same graceful
     * stop the rider's own stop button does, so whatever was said up to that point still comes
     * back through the flow as a final transcript rather than being thrown away.
     *
     * The rider's stop button is the real control; this only decides when to stop waiting for
     * someone who has finished but whose recogniser has not noticed.
     *
     * Ten seconds is the ordinary ceiling, which is longer than any trip anyone says out loud.
     * A rider still mid-sentence at ten seconds is not cut off: the extension runs to fifteen,
     * which is a hard stop. "Still speaking" is measured by whether new words arrived in the
     * two seconds before the ceiling, not by whether any arrived at all, since every session
     * has words in it somewhere.
     */
    private fun startListeningTimeout() {
        listeningTimeoutJob?.cancel()
        listeningTimeoutJob = viewModelScope.launch {
            delay(LISTENING_CEILING_MILLIS - STILL_SPEAKING_WINDOW_MILLIS)
            val wordsBeforeTheWindow = wordsHeardSoFar
            delay(STILL_SPEAKING_WINDOW_MILLIS)

            if (wordsHeardSoFar > wordsBeforeTheWindow) {
                delay(LISTENING_EXTENSION_MILLIS)
                if (_uiState.value.isListening) {
                    logOutcome(reason = "listening_hit_extended_ceiling")
                    stopListening()
                }
                return@launch
            }

            if (_uiState.value.isListening) {
                logOutcome(reason = "listening_hit_ceiling")
                stopListening()
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
        listeningTimeoutJob?.cancel()
        listeningTimeoutJob = null
        speechToTextService.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    private fun submit() {
        val flagEnabled = isAiSearchInputEnabled()
        val text = _uiState.value.typedText.trim()
        if (!flagEnabled || text.isEmpty()) {
            logOutcome(reason = if (!flagEnabled) "flag_off" else "empty_text")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phase = AiSearchInputPhase.EXTRACTING,
                    unresolvedReason = null,
                    unmatchedPlace = null,
                )
            }

            val availability = aiTextService.checkExtractionAvailability()
            if (availability is AiAvailability.Unavailable) {
                val isTemporary = availability.reason == "downloadable" || availability.reason == "downloading"
                _uiState.update {
                    it.copy(phase = if (isTemporary) AiSearchInputPhase.DOWNLOADING else AiSearchInputPhase.UNRESOLVED)
                }
                logOutcome(reason = "model_unavailable_${availability.reason}")
                return@launch
            }

            val rawExtraction = aiTextService.extractTripIntent(text)
            if (rawExtraction == null) {
                _uiState.update {
                    it.copy(
                        phase = AiSearchInputPhase.UNRESOLVED,
                        unresolvedReason = UnresolvedReason.COULD_NOT_READ,
                    )
                }
                logOutcome()
                return@launch
            }
            val extraction = rawExtraction.withSinglePlaceInTheRightField(text)

            val toStopItem = extraction.destinationText?.let { resolveStop(it) }
            val originText = extraction.originText
            val (fromText, fromStopItem) = when {
                originText != null -> originText to resolveStop(originText)

                // Only fall back to the nearest stop when a destination was actually
                // understood. Without that condition, a sentence about nothing at all ("hey
                // how are you") resolved to no places, took the fallback anyway, and filled
                // the From field with wherever the rider happened to be standing. That reads
                // as the app having understood something, which is the one thing it must not
                // fake.
                toStopItem != null -> resolveCurrentLocationStop()

                else -> null to null
            }
            val dateTimeSelectionItem =
                resolveTimeIntent(extraction.timeIntent, riderText = text) ?: leaveNowDateTimeSelectionItem()

            val intent = ResolvedTripIntent(
                fromText = fromText,
                fromStopItem = fromStopItem,
                toText = extraction.destinationText,
                toStopItem = toStopItem,
                dateTimeSelectionItem = dateTimeSelectionItem,
                modeHints = extraction.modeHints,
            )
            _uiState.update { it.withResolution(intent, namedAnyPlace = extraction.namesAPlace()) }
            logOutcome()
        }
    }

    /**
     * One wide line per attempt, at the end of it, rather than a handful of narrow ones spread
     * through the coroutine that a person then has to stitch back together in logcat. Grep
     * [AI_OUTCOME_TAG] and each attempt is one row.
     *
     * Deliberately carries no rider text: not what they typed, not what was heard, not the
     * place name quoted back at them. Phases, reasons and booleans say what happened without
     * saying where anyone is going.
     */
    private fun logOutcome(reason: String? = null) {
        val state = _uiState.value
        val resolved = state.resolved
        log(
            "$AI_OUTCOME_TAG phase=${state.phase}" +
                " reason=${reason ?: state.unresolvedReason?.name ?: "none"}" +
                " speechProblem=${state.speechUnavailableReason ?: "none"}" +
                " from=${resolved?.fromStopItem != null} to=${resolved?.toStopItem != null}" +
                " spokeIt=${state.speechTranscript.isNotEmpty()}",
        )
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
        return stop.stopName to stop
    }
}

/**
 * One stop resolving is enough to be useful: the row is an ordinary editable From/To pair, so
 * whichever field the AI did resolve gets written and the rider fills the other one the usual
 * way. Nothing resolving at all is a failure, not a result — the sheet stays open with the
 * rider's text still in it so they can reword rather than retype.
 */
private fun AiSearchInputUiState.withResolution(
    intent: ResolvedTripIntent,
    namedAnyPlace: Boolean,
): AiSearchInputUiState = copy(
    phase = if (intent.hasAnyStop) AiSearchInputPhase.RESOLVED else AiSearchInputPhase.UNRESOLVED,
    resolved = if (intent.hasAnyStop) intent else null,
    isInputOpen = !intent.hasAnyStop,
    // A rider who named a place we could not find needs different words from one who named no
    // place at all. The first can be told which name failed; the second only needs an example.
    unresolvedReason = when {
        intent.hasAnyStop -> null
        namedAnyPlace -> UnresolvedReason.STOP_NOT_FOUND
        else -> UnresolvedReason.NO_PLACE_MENTIONED
    },
    // Quoted back only when those exact words are in what the rider wrote. The model's output
    // is used to look stops up, never shown: a place name it invented or reworded, printed
    // back inside quote marks, would read as something the rider said when it is not.
    unmatchedPlace = if (intent.hasAnyStop) {
        null
    } else {
        intent.firstNamedPlace()?.takeIf { place ->
            typedText.contains(place, ignoreCase = true)
        }
    },
)

private fun TripIntentExtraction.namesAPlace(): Boolean =
    !originText.isNullOrBlank() || !destinationText.isNullOrBlank()

private fun ResolvedTripIntent.firstNamedPlace(): String? =
    listOfNotNull(toText, fromText).firstOrNull { it.isNotBlank() }
