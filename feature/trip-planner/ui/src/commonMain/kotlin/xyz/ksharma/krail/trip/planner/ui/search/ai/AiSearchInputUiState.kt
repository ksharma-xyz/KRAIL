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
 * @property MODEL_UNAVAILABLE This device cannot run the on-device model at all. Nothing about
 * the sentence is the problem and no rewording will help.
 * @property MODEL_NEEDS_SETTING The device could run it and the rider has it switched off. The
 * only one of these with a fix the rider can carry out themselves.
 */
enum class UnresolvedReason {
    NO_PLACE_MENTIONED,
    STOP_NOT_FOUND,
    COULD_NOT_READ,
    MODEL_UNAVAILABLE,
    MODEL_NEEDS_SETTING,
}

/**
 * @param isInputOpen Whether the AI search sheet is showing over the home screen. Owned here
 * rather than as `rememberSaveable` in the screen so that closing-on-resolve and the reset
 * that follows it happen in one state emission — a screen-local flag would race with
 * [AiSearchInputEvent.StartOver] and could leave an empty sheet open.
 * @param isFeatureEnabled Whether the feature flag is on. The way in has to know this: the
 * flag reached the ViewModel, which refused to submit, but never reached the UI, so with the
 * feature off the wheel still rendered and did nothing when tapped. A button that does
 * nothing is worse than no button.
 * @param isDeviceCapable Whether this device can run the on-device model at all. Same argument
 * as [isFeatureEnabled], one layer down: the flag can be on for a rider whose phone has no
 * on-device AI, and for them the wheel was a button that always failed. True until the
 * asynchronous check says otherwise, so the button does not appear late on every launch.
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
    val isDeviceCapable: Boolean = true,
)

/**
 * Whether to offer the way in at all. The flag says the feature is switched on for this rider;
 * [AiSearchInputUiState.isDeviceCapable] says their phone can actually run it.
 *
 * The gate used to be the flag alone, so on a device with no on-device AI the wheel rendered,
 * accepted a sentence, and failed every time. A button that always fails is worse than no
 * button, which is the same reason the flag reached the UI in the first place.
 */
val AiSearchInputUiState.isWayInAvailable: Boolean
    get() = isFeatureEnabled && isDeviceCapable

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

/**
 * The one gate for writing this state's resolve into anything outside the AI flow (the home
 * row's From/To fields and time chip). True only while the resolve is LIVE — the dialog is
 * showing its settle beat and the answer has not been handed over and consumed.
 *
 * The consumer (SavedTripsEntry's LaunchedEffect) re-launches every time its screen
 * recomposes into existence, back-navigation included. Acting on `resolved != null` alone
 * replayed old answers over stops the rider had since picked by hand; the phase stepping
 * down at dialog close (closeAfterHandoff) is what turns this false and keeps a re-launched
 * collector idle. Pinned by `the row-write gate closes with the dialog`.
 */
val AiSearchInputUiState.isHandoffActionable: Boolean
    get() = phase == AiSearchInputPhase.RESOLVED && resolved != null

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
