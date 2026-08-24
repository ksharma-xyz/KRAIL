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
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.speechtotext.SpeechToTextAvailability
import xyz.ksharma.krail.core.speechtotext.SpeechToTextResult
import xyz.ksharma.krail.core.speechtotext.SpeechToTextService
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.RiderOriginLocator
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopTextResolver
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

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

// How long a resolved answer stays on the dialog before it closes itself. Long enough for the
// spinning border to finish its beat and start settling (WorkingBorder keeps turning 1.1s past
// the answer), short enough that the close still reads as a consequence of the send. The rider
// is not reading the answer here — the row behind the dialog is where the answer lands, and the
// close is what points them at it.
private const val HANDOFF_SETTLE_MILLIS = 1_500L

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
    // growing branches in here. Its chain decides which capability answers: the rider's own
    // labels first, then the same stop search the search-stop screen uses. The answer is
    // treated as a guess - no auto-pick concept exists anywhere else in this codebase, and it
    // stays deliberately scoped to this AI context - so a miss resolves to null and leaves the
    // field for the rider to fill by hand.
    private val stopTextResolver: StopTextResolver,
    // Where a journey starts when the rider did not say. One collaborator rather than a
    // location lambda, a nearby repository and a label locator sitting side by side: they only
    // ever answer one question between them.
    //
    // The location half is Composable-supplied. UserLocationManager exists only as
    // `rememberUserLocationManager()` — its permission and location controllers need Activity
    // context tied to composition — so there is no Koin single to inject here. The Composable
    // layer builds the lambda and passes it through koinViewModel's parametersOf, the same
    // pattern TrackTripViewModel already uses.
    private val riderOriginLocator: RiderOriginLocator = RiderOriginLocator(),
    // The rider's own label words, for the case where the model reads a sentence correctly and
    // still finds no place in it because the place is a word like "work".
    private val riderLabels: suspend () -> List<String> = { emptyList() },
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
        checkDeviceCapability()
    }

    private var listeningJob: Job? = null
    private var listeningTimeoutJob: Job? = null

    // Only ever compared against an earlier reading of itself, never read as a total.
    private var wordsHeardSoFar: Int = 0

    // What the field held when the rider tapped the mic. Spoken words are added to it rather
    // than replacing it, so a half-typed sentence survives being spoken into. Held outside the
    // state because it is not something the screen renders, and cleared wherever the draft is.
    private var textBeforeSpeaking: String = ""

    // Held outside the state as well as in it, because every reset below rebuilds the state
    // from scratch and this is the one fact about the device that a reset must not forget.
    private var isDeviceCapable: Boolean = true

    fun onEvent(event: AiSearchInputEvent) {
        when (event) {
            is AiSearchInputEvent.TypedTextChanged -> _uiState.update {
                // Typing is the way out of every problem this surface can show, so the message
                // clears as they type rather than needing a control of its own to dismiss. A
                // banner with an X on it asks the rider to tidy up after a failure that was
                // not theirs; editing the sentence is already them saying they have moved on.
                //
                // Only on real text: the text field reports its initial empty value as soon as
                // it composes, which would otherwise clear the message before it is ever read.
                val startedEditing = event.text.isNotEmpty()
                it.copy(
                    typedText = event.text,
                    speechUnavailableReason = if (startedEditing) null else it.speechUnavailableReason,
                    // An unresolved result is a problem about the last sentence. The moment the
                    // rider changes that sentence it is stale, and leaving it up makes the new
                    // attempt look like it has already failed. A RESOLVED phase steps down for
                    // the same reason and one more: the dialog closes itself a beat after a
                    // resolve, and a rider who starts rewording inside that beat has opted out
                    // of the handoff — leaving the phase RESOLVED would let the delayed close
                    // pull the dialog out from under their edit.
                    phase = if (startedEditing &&
                        (
                            it.phase == AiSearchInputPhase.UNRESOLVED ||
                                it.phase == AiSearchInputPhase.RESOLVED
                            )
                    ) {
                        AiSearchInputPhase.IDLE
                    } else {
                        it.phase
                    },
                    unresolvedReason = if (startedEditing) null else it.unresolvedReason,
                    unmatchedPlace = if (startedEditing) null else it.unmatchedPlace,
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
            //
            // A fresh state, not just the flag flipped: the dialog reads as a fresh prompt
            // every time it opens. After a handoff the state still carries the last resolve
            // (phase RESOLVED, the resolved intent, the sentence) — reopening onto that would
            // show a stale answer for a row the rider may have edited since.
            AiSearchInputEvent.OpenInput -> {
                textBeforeSpeaking = ""
                val enabled = isAiSearchInputEnabled()
                _uiState.update {
                    AiSearchInputUiState(
                        isInputOpen = enabled,
                        isFeatureEnabled = enabled,
                        isDeviceCapable = isDeviceCapable,
                    )
                }
            }
            AiSearchInputEvent.CloseInput -> closeInput()
            AiSearchInputEvent.Submit -> submit()
            // Everything the rider produced is thrown away; what the app knows about itself
            // is not, or starting over would hide the way back in.
            AiSearchInputEvent.StartOver -> {
                textBeforeSpeaking = ""
                _uiState.update {
                    AiSearchInputUiState(
                        isFeatureEnabled = it.isFeatureEnabled,
                        isDeviceCapable = isDeviceCapable,
                    )
                }
            }

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
        textBeforeSpeaking = ""
        if (_uiState.value.isListening) speechToTextService.stopListening()
        _uiState.update {
            AiSearchInputUiState(
                isFeatureEnabled = it.isFeatureEnabled,
                isDeviceCapable = isDeviceCapable,
            )
        }
    }

    /**
     * Speech writes into [AiSearchInputUiState.typedText] and nothing else — the same field
     * typing fills, so [AiTextService.extractTripIntent] only ever needs one caller. Same "just
     * another way to produce the string" shape
     * [xyz.ksharma.krail.core.textrecognition.TextRecognitionService] documents for OCR.
     *
     * Both partial and final transcripts land there: partials so the rider can see the words
     * arriving, the final so the last correction sticks. Neither submits. The rider presses
     * send when what is in the box is what they meant.
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

            // Whatever is already in the field is kept, and the spoken words are added to it.
            //
            // Speaking used to replace it. A rider who typed half a sentence, tapped the mic
            // and watched their own words disappear had no way back to them: the field is the
            // only copy. Every mic that lives inside a text field works this way, from the iOS
            // keyboard's own dictation to Gboard's, because a mic beside a keyboard is another
            // way to type rather than a different way to ask.
            //
            // Replacing does have one case going for it, a rider re-tapping the mic to redo a
            // mis-heard sentence, and adding gives them the sentence twice. That is a worse
            // reading of the same tap but a far better failure: it is on screen, it is
            // obviously wrong, and it is one gesture to clear. Losing typed words is silent.
            textBeforeSpeaking = _uiState.value.typedText
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
                        // Into typedText, not just the transcript. The field renders typedText,
                        // so writing only the transcript meant a rider watched an empty box
                        // while they talked and everything appeared at once when they stopped.
                        // Partials are what make speaking feel like it is being heard.
                        //
                        // Blank is not a transcript. A recogniser that reports one is saying it
                        // has nothing to add, not that the rider unsaid what they said, and
                        // writing it through erases words already in the field.
                        if (result.text.isNotBlank()) {
                            _uiState.update {
                                it.copy(
                                    speechTranscript = result.text,
                                    typedText = joinSpokenText(textBeforeSpeaking, result.text),
                                )
                            }
                        }
                    }

                    is SpeechToTextResult.Final -> {
                        // Fills the field and stops. It used to submit here, which took the
                        // decision away from the rider: the recogniser deciding it had heard a
                        // full sentence is not the same as a rider deciding they have finished
                        // saying one, and a mis-heard word was already on its way to a search
                        // before they could look at it. Speaking is a way of typing; send is
                        // still theirs to press.
                        //
                        // A blank final stops the session and leaves the field exactly as the
                        // partials left it. iOS ends a session by asking the recogniser to
                        // finish, and the result that comes back can carry an empty
                        // transcription — a session boundary, not a correction. Writing it in
                        // wiped the sentence the rider had just watched arrive, with nothing on
                        // screen to say why. Android never reaches this because its recogniser
                        // reports a missing result as an error instead.
                        _uiState.update {
                            if (result.text.isBlank()) {
                                it.copy(isListening = false)
                            } else {
                                it.copy(
                                    isListening = false,
                                    speechTranscript = result.text,
                                    typedText = joinSpokenText(textBeforeSpeaking, result.text),
                                )
                            }
                        }
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
                isDeviceCapable = availability.reason.canBecomeAvailable()
                _uiState.update { it.withUnavailableModel(availability.reason) }
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
            // "work by 9am tomm" came back with no place at all: to the model, "work" is an
            // ordinary noun, not this rider's stop. Their own word, from their own text, matched
            // against their own labels — nothing here comes from the model.
            val extraction = rawExtraction
                .withSinglePlaceInTheRightField(text)
                .withLabelWordAsDestination(riderText = text, labels = riderLabels())

            val toStopItem = extraction.destinationText?.let { stopTextResolver.resolve(it) }
            val originText = extraction.originText
            val (fromText, fromStopItem) = resolveTripOrigin(
                originText = originText,
                namedOrigin = originText?.let { stopTextResolver.resolve(it) },
                toStopItem = toStopItem,
                nearbyOrigin = ::resolveCurrentLocationStop,
            )

            // No fallback to "leave now". A rider who mentioned no time gets no time, because
            // the home screen now shows this as a chip: falling back produced "Leave Today
            // 12:29 AM" on a sentence that said nothing about when, which is the app inventing
            // a decision and then displaying it back as though the rider had made it. Null
            // already means now everywhere downstream.
            val dateTimeSelectionItem = resolveTimeIntent(extraction.timeIntent, riderText = text)

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
            closeAfterHandoff()
        }
    }

    /**
     * A resolve is a handoff: the stops and the time are already written into the home row by
     * the time this state lands (SavedTripsEntry writes them on the RESOLVED emission), so the
     * dialog's job is done. It stays up for one settle beat — long enough for the working
     * border to finish — then closes itself onto the row it just filled.
     *
     * Guarded on the phase still being RESOLVED: a rider who dismissed during the beat has
     * already reset the state, and one who started rewording has been stepped down to IDLE by
     * [AiSearchInputEvent.TypedTextChanged]. In both cases the close belongs to them now, not
     * to this timer.
     */
    private suspend fun closeAfterHandoff() {
        if (_uiState.value.phase != AiSearchInputPhase.RESOLVED) return
        delay(HANDOFF_SETTLE_MILLIS)
        _uiState.update {
            if (it.phase == AiSearchInputPhase.RESOLVED && it.isInputOpen) {
                // The phase steps down WITH the close, in the same emission. The row's writes
                // key off phase == RESOLVED (SavedTripsEntry), and that effect re-launches
                // every time the home entry recomposes — coming back from the stop-search
                // screen included. A state left at RESOLVED after the handoff replayed the
                // AI's stops over whatever the rider had picked by hand since. IDLE makes the
                // resolve a record, not an instruction: `resolved` itself is kept for the
                // handoff spin and for anything that wants to read what happened.
                it.copy(isInputOpen = false, phase = AiSearchInputPhase.IDLE)
            } else {
                it
            }
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
     * The rider didn't say where they're leaving from ("need to be at Central by 6:30pm" —
     * no "from X") — falls back to the nearest transit stop to their current GPS location
     * via [resolveNearbyStop], same as tapping a "near me" pill would, rather than leaving
     * the field blank and forcing a manual search for something this app can already answer
     * on its own. A permission denial or GPS failure (handled entirely inside
     * `UserLocationManager.getCurrentLocation`'s check→request→fetch flow, upstream of this
     * lambda) just means the field stays unresolved (pencil-editable), never a crash or a
     * blocking prompt the rider didn't ask for by typing into this flow in the first place.
     */
    private suspend fun resolveCurrentLocationStop(excludeStopId: String?): Pair<String?, StopItem?> {
        val stop = riderOriginLocator.originStop(excludeStopId = excludeStopId)
        return stop?.stopName to stop
    }

    /**
     * Asked once on entry so the wheel is not offered on a phone that cannot run the model.
     * The gate used to be the remote-config flag alone, so a rider whose device has no
     * on-device AI saw the button, typed a sentence, and got a failure every single time.
     *
     * Optimistic until it answers: [isDeviceCapable] starts true, so the button is there for
     * the fraction of a second this takes rather than appearing late on every launch. Both
     * platform implementations cache the underlying check, so this is one real call per
     * process, not one per screen entry.
     */
    private fun checkDeviceCapability() {
        viewModelScope.launch {
            val available = aiTextService.checkExtractionAvailability()
            val capable = available !is AiAvailability.Unavailable ||
                available.reason.canBecomeAvailable()
            isDeviceCapable = capable
            _uiState.update { it.copy(isDeviceCapable = capable) }
            log("$AI_OUTCOME_TAG deviceCapable=$capable")
        }
    }
}

/**
 * A resolve keeps the dialog up only for the settle beat (see `closeAfterHandoff`); a failure
 * keeps it up until the rider acts.
 *
 * The answer is not read on this surface any more — it lands on the home row's own From/To
 * fields and time chip, which the dialog closes onto. What this surface still owes the rider
 * is the failure story: nothing resolving keeps their text in the field with a message naming
 * the one thing that would help, so they can reword rather than retype. `isInputOpen` stays
 * true here even on a resolve because the close is timed, not immediate: the delayed close in
 * `closeAfterHandoff` is what takes it down after the border has settled.
 */
private fun AiSearchInputUiState.withResolution(
    intent: ResolvedTripIntent,
    namedAnyPlace: Boolean,
): AiSearchInputUiState = copy(
    phase = if (intent.hasAnyStop) AiSearchInputPhase.RESOLVED else AiSearchInputPhase.UNRESOLVED,
    resolved = if (intent.hasAnyStop) intent else null,
    isInputOpen = true,
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
