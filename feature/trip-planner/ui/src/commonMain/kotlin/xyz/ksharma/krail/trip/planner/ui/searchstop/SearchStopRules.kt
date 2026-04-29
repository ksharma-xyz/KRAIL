package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Pure decision functions for SearchStopScreen orchestration logic.
 *
 * These rules drive what the screen renders in response to its state. Keeping them
 * here (rather than inline in composables) means we can unit-test each rule in
 * isolation — see `SearchStopRulesTest`. Anything that can be reduced to a pure
 * function of state should live here.
 *
 * See `feature/trip-planner/ui/SEARCH_STOP_UX.md` for the human-readable description
 * of every rule.
 */

/**
 * Picks the contextual hint shown directly under the pill row.
 *
 * Rule order:
 * 1. Edit mode wins — the user is reordering, hint reflects that.
 * 2. Otherwise, if assigning mode is active AND the target label is still unset,
 *    point the user at the ⭐ buttons.
 * 3. If the target label has already been satisfied (a stop got attached), the
 *    banner collapses — the work is done, no need to keep nagging.
 * 4. Idle state — return null, no banner.
 */
internal fun pillRowBannerText(
    editing: Boolean,
    assigningLabel: StopLabel?,
    stopLabels: List<StopLabel>,
): String? = when {
    editing -> "Long press and then drag the pill to reorder and select Done to save."
    assigningLabel != null -> {
        val current = stopLabels.firstOrNull { it.label == assigningLabel.label }
        if (current?.isSet == true) {
            null
        } else {
            "Tap the ⭐ next to a stop to save it as ${assigningLabel.label}"
        }
    }
    else -> null
}

/**
 * Set of stopIds that are currently saved against any label. Used by the star toggle
 * on stop result rows so we know whether to render the filled or outlined star.
 */
internal fun savedStopIds(labels: List<StopLabel>): Set<String> =
    labels.mapNotNull { it.stopId }.toSet()

/**
 * Detects either flavour of 1:1 invariant violation when assigning [stop] to [target]:
 *
 * - **StopAlreadyOnAnotherLabel** — the stop is currently saved as a different label.
 *   Confirming the conflict moves the stop, clearing the previous label.
 * - **LabelHasDifferentStop** — [target] already has a different stop attached.
 *   Confirming overwrites the existing stop on that label.
 * - **null** — no conflict; the assignment can proceed silently.
 */
@Suppress("ReturnCount")
internal fun conflictForAssign(
    target: StopLabel,
    stop: StopItem,
    allLabels: List<StopLabel>,
): AssignConflict? {
    val stopAlreadyOnAnother = allLabels.firstOrNull {
        it.stopId == stop.stopId && it.label != target.label
    }
    if (stopAlreadyOnAnother != null) {
        return AssignConflict.StopAlreadyOnAnotherLabel(
            target = target,
            stop = stop,
            existingLabel = stopAlreadyOnAnother,
        )
    }
    if (target.isSet && target.stopId != stop.stopId) {
        return AssignConflict.LabelHasDifferentStop(
            target = target,
            stop = stop,
            existingStopName = target.stopName.orEmpty(),
        )
    }
    return null
}

/**
 * Whether the pill row (label shortcuts + assigning banner) should render.
 *
 * Hide it on a fresh-install / empty-search canvas where there are no stops below
 * with stars to tap — otherwise tapping an unset Home pill enters assigning mode
 * with nothing to act on, which feels broken to the user.
 *
 * - In Recent mode: show iff the user has at least one recent stop.
 * - In Results mode: show iff the search returned at least one result. Loading,
 *   NoMatch and Error states all hide the row.
 */
internal fun shouldShowPillRow(
    listState: ListState,
    recentStops: List<SearchStopState.StopResult>,
): Boolean = when (listState) {
    ListState.Recent -> recentStops.isNotEmpty()
    is ListState.Results -> listState.results.isNotEmpty()
    ListState.NoMatch, ListState.Error -> false
}

internal sealed interface AssignConflict {
    data class StopAlreadyOnAnotherLabel(
        val target: StopLabel,
        val stop: StopItem,
        val existingLabel: StopLabel,
    ) : AssignConflict

    data class LabelHasDifferentStop(
        val target: StopLabel,
        val stop: StopItem,
        val existingStopName: String,
    ) : AssignConflict
}
