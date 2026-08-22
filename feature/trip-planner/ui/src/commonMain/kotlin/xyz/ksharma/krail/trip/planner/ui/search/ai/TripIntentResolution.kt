package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.AiUnavailableReasons

/**
 * The parts of a submit that are decisions rather than orchestration, kept out of
 * [AiSearchInputViewModel] so that class stays a coordinator.
 *
 * A separate file rather than private functions on the ViewModel, deliberately: detekt counts
 * branches inside nested and local functions toward the enclosing one, so moving this out of
 * the ViewModel only reduces its complexity if it moves out of the class as well. The functions
 * here are pure, which also means they can be reasoned about without a ViewModel around them.
 */

/**
 * The model could not be asked, and the rider is told which of those it was.
 *
 * Every one of these used to land as [AiSearchInputPhase.UNRESOLVED] with no reason set, which
 * the banner renders as "Something is missing there. Name where you are going, and where from."
 * That is advice about a sentence, given to a rider whose sentence was never read. On a phone
 * that cannot run the model at all it is unanswerable, and riders reworded the same request
 * until they gave up. Each reason now names the thing that would actually help, and only one of
 * them is about the sentence.
 */
internal fun AiSearchInputUiState.withUnavailableModel(reason: String): AiSearchInputUiState {
    val isStillArriving = reason == AiUnavailableReasons.MODEL_DOWNLOADING
    return copy(
        phase = if (isStillArriving) AiSearchInputPhase.DOWNLOADING else AiSearchInputPhase.UNRESOLVED,
        unresolvedReason = when (reason) {
            // DOWNLOADING carries its own message; a reason here would be a second one.
            AiUnavailableReasons.MODEL_DOWNLOADING -> null
            AiUnavailableReasons.NEEDS_DEVICE_SETTING -> UnresolvedReason.MODEL_NEEDS_SETTING
            AiUnavailableReasons.DEVICE_UNSUPPORTED -> UnresolvedReason.MODEL_UNAVAILABLE
            // Includes CHECK_FAILED, which Android suffixes with the throwable message, so
            // that arrives here by fallthrough rather than by equality. A check that failed
            // says nothing about the device: it is worth retrying, which is exactly what
            // COULD_NOT_READ tells the rider to do.
            else -> UnresolvedReason.COULD_NOT_READ
        },
        // Only a device that can never run it loses the way in. A model still downloading, or
        // one switched off in Settings, both become available without a new build, and hiding
        // the button would hide the thing that says so.
        isDeviceCapable = reason.canBecomeAvailable(),
    )
}

/**
 * Whether this reason could stop being true without the rider changing phone. Downloads finish
 * and settings get switched on; a device with no on-device AI stays that way.
 */
internal fun String.canBecomeAvailable(): Boolean = this != AiUnavailableReasons.DEVICE_UNSUPPORTED
