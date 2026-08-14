package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

enum class AiSearchInputPhase { IDLE, EXTRACTING, DOWNLOADING, RESOLVED, UNRESOLVED }

/**
 * @param isSheetOpen Whether the AI search sheet is showing over the home screen. Owned here
 * rather than as `rememberSaveable` in the screen so that closing-on-resolve and the reset
 * that follows it happen in one state emission — a screen-local flag would race with
 * [AiSearchInputEvent.StartOver] and could leave an empty sheet open.
 */
data class AiSearchInputUiState(
    val typedText: String = "",
    val phase: AiSearchInputPhase = AiSearchInputPhase.IDLE,
    val resolved: ResolvedTripIntent? = null,
    val isSheetOpen: Boolean = false,
    val isListening: Boolean = false,
    val speechTranscript: String = "",
    val speechUnavailableReason: String? = null,
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
}

sealed interface AiSearchInputEvent {
    data class TypedTextChanged(val text: String) : AiSearchInputEvent
    data object OpenSheet : AiSearchInputEvent
    data object CloseSheet : AiSearchInputEvent
    data object Submit : AiSearchInputEvent
    data object StartOver : AiSearchInputEvent
    data object StartListening : AiSearchInputEvent
    data object StopListening : AiSearchInputEvent
    data object MicPermissionDenied : AiSearchInputEvent
}
