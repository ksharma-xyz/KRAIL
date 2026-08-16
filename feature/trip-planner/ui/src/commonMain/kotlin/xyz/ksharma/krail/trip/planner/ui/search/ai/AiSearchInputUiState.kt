package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

enum class AiSearchInputPhase { IDLE, EXTRACTING, DOWNLOADING, RESOLVED, UNRESOLVED }

/**
 * Why nothing was resolved, so the rider is told the one thing that would help rather than a
 * single sentence covering three different problems.
 *
 * @property NO_PLACE_MENTIONED The sentence was read fine and named no place at all.
 * @property STOP_NOT_FOUND A place was named and no stop matches it.
 * @property COULD_NOT_READ The model itself gave nothing usable back.
 */
enum class UnresolvedReason { NO_PLACE_MENTIONED, STOP_NOT_FOUND, COULD_NOT_READ }

/**
 * @param isInputOpen Whether the AI search sheet is showing over the home screen. Owned here
 * rather than as `rememberSaveable` in the screen so that closing-on-resolve and the reset
 * that follows it happen in one state emission — a screen-local flag would race with
 * [AiSearchInputEvent.StartOver] and could leave an empty sheet open.
 * @param isFeatureEnabled Whether the feature flag is on. The way in has to know this: the
 * flag reached the ViewModel, which refused to submit, but never reached the UI, so with the
 * feature off the wheel still rendered and did nothing when tapped. A button that does
 * nothing is worse than no button.
 */
data class AiSearchInputUiState(
    val typedText: String = "",
    val phase: AiSearchInputPhase = AiSearchInputPhase.IDLE,
    val resolved: ResolvedTripIntent? = null,
    val isInputOpen: Boolean = false,
    val isListening: Boolean = false,
    val speechTranscript: String = "",
    val speechUnavailableReason: String? = null,
    val unresolvedReason: UnresolvedReason? = null,
    /** The place the rider named that no stop matched, quoted back to them. */
    val unmatchedPlace: String? = null,
    val isFeatureEnabled: Boolean = false,
)

/**
 * @param fromText / [toText] The raw extracted text, kept alongside the resolved stops so a
 * partial result is still explainable — the rider sees what the AI heard even for the field
 * that found no matching stop.
 * @param fromStopItem / [toStopItem] `null` means that field found no stop. The other field
 * is still written into the row, and the rider fills this one in the ordinary way.
 */
data class ResolvedTripIntent(
    val fromText: String?,
    val fromStopItem: StopItem?,
    val toText: String?,
    val toStopItem: StopItem?,
    val dateTimeSelectionItem: DateTimeSelectionItem?,
    val modeHints: List<String>,
) {
    val hasAnyStop: Boolean get() = fromStopItem != null || toStopItem != null

    /**
     * Both ends known, so there is a timetable that can actually be loaded from here. This is
     * the line between showing the rider an answer and handing a half-filled row back to them:
     * with one stop missing there is nothing to show times for, and the surface keeps its
     * original behaviour of filling what it found and getting out of the way.
     */
    val hasWholeTrip: Boolean get() = fromStopItem != null && toStopItem != null
}

sealed interface AiSearchInputEvent {
    data class TypedTextChanged(val text: String) : AiSearchInputEvent
    data object OpenInput : AiSearchInputEvent
    data object CloseInput : AiSearchInputEvent
    data object Submit : AiSearchInputEvent
    data object StartOver : AiSearchInputEvent
    data object StartListening : AiSearchInputEvent
    data object StopListening : AiSearchInputEvent

    /** Refused just now, and the system will ask again next time the mic is tapped. */
    data object MicPermissionDenied : AiSearchInputEvent

    /** Refused for good. The mic's job changes to opening Settings. */
    data object MicPermissionBlocked : AiSearchInputEvent

    /** No recogniser, or the microphone is restricted by the device. Speaking is off. */
    data object SpeechUnsupported : AiSearchInputEvent
}
