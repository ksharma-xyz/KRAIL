package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.AiUnavailableReasons
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The parts of a submit that are decisions rather than orchestration, kept out of
 * [AiSearchInputViewModel] so that class stays a coordinator.
 *
 * A separate file rather than private functions on the ViewModel, deliberately: detekt counts
 * branches inside nested and local functions toward the enclosing one, so moving this out of
 * `submit` only reduces its complexity if it moves out of the class as well. The functions here
 * are pure, which also means they can be reasoned about without a ViewModel around them.
 */

/**
 * Which stop, if any, the journey starts from.
 *
 * @param originText what the rider called the origin, or null if they did not name one.
 * @param namedOrigin what [originText] resolved to, or null if it resolved to nothing.
 * @param toStopItem the resolved destination, used to refuse an origin equal to it.
 * @return the origin text to show and the stop it resolved to, either of which may be null.
 * @param nearbyOrigin the fallback for an unstated origin, invoked only when it is needed:
 * it reads the rider's location, which is not something to do speculatively.
 */
internal suspend fun resolveTripOrigin(
    originText: String?,
    namedOrigin: StopItem?,
    toStopItem: StopItem?,
    nearbyOrigin: suspend (excludeStopId: String?) -> Pair<String?, StopItem?>,
): Pair<String?, StopItem?> {
    // A trip from a stop to itself is never something a rider asked for. "lets go to work"
    // came back from the iOS model with "work" in BOTH fields, both resolved to the rider's
    // Work stop, and the row was filled Work to Work. Both prompts now say not to do it, but a
    // prompt is advice and this is the contract.
    val originIsTheDestination = namedOrigin.sameStopAs(toStopItem)

    return when {
        // A named origin keeps this branch even when it resolved to nothing: the rider said a
        // place, and the row shows the words they used with the field left for them to fill.
        // Only a named origin that collapsed ONTO the destination is diverted, to the same
        // fallback an unstated origin takes, which already knows to exclude it.
        originText != null && !originIsTheDestination -> originText to namedOrigin

        // Only fall back to the nearest stop when a destination was actually understood.
        // Without that condition, a sentence about nothing at all ("hey how are you") resolved
        // to no places, took the fallback anyway, and filled the From field with wherever the
        // rider happened to be standing. That reads as the app having understood something,
        // which is the one thing it must not fake.
        toStopItem != null -> {
            val (text, stop) = nearbyOrigin(toStopItem.stopId)
            // The fallback excludes by stop id, and the same station can carry more than one
            // (a label pinned to the parent, a nearby result on a platform). Checked again by
            // name here so the row cannot end up showing one station twice under two ids.
            if (stop.sameStopAs(toStopItem)) null to null else text to stop
        }

        else -> null to null
    }
}

/**
 * Whether two resolutions are the same place, for the one purpose of refusing to put a station
 * in both ends of a trip.
 *
 * Not just an id comparison. The same station carries more than one stop id in this data (a
 * label pinned to the parent, a nearby-stop result on a platform), so an id check alone let
 * "Central Station to Central Station" through under two different ids. Names are compared as
 * well, which is safe here because the only decision it drives is dropping an origin the rider
 * never asked for.
 */
internal fun StopItem?.sameStopAs(other: StopItem?): Boolean {
    if (this == null || other == null) return false
    return stopId == other.stopId || stopName.equals(other.stopName, ignoreCase = true)
}

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
