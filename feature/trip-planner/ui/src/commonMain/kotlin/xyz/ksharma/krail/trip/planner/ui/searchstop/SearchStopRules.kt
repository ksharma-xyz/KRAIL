package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

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
 * Whether the pill row (label shortcuts + trailing Manage button) should render.
 *
 * Two independent reasons to hide it:
 * - Fresh-install / empty-search canvas with no stops below to assign — an empty pill
 *   row with nothing to act on feels broken to the user.
 * - No label is actually set yet. `LabelShortcutsRow` only ever renders `isSet`
 *   labels as pills, and Home/Work are always seeded on install (unset), so
 *   `stopLabels` itself is never empty — without this check the row would render as
 *   a lone floating "Manage" button with no pills next to it, which reads as broken
 *   (nothing on screen yet to manage).
 *
 * - In Recent mode: show iff the user has at least one recent stop.
 * - In Results mode: show iff the search returned at least one result. Loading,
 *   NoMatch and Error states all hide the row.
 */
internal fun shouldShowPillRow(
    listState: ListState,
    recentStops: List<SearchStopState.StopResult>,
    stopLabels: List<StopLabel>,
): Boolean {
    if (stopLabels.none { it.isSet }) return false
    return when (listState) {
        ListState.Recent -> recentStops.isNotEmpty()
        is ListState.Results -> listState.results.isNotEmpty()
        ListState.NoMatch, ListState.Error -> false
    }
}

/**
 * On a fresh install (Recent mode, zero recents) we surface a small curated set of
 * [EMPTY_STATE_STOPS] so a brand-new user can pick a major interchange without typing,
 * instead of facing a blank canvas. As soon as the user has at least one recent stop,
 * recents take over and this returns false (empty-state stops are a first-open
 * fallback, never shown alongside recents).
 */
internal fun shouldShowEmptyStateStops(
    listState: ListState,
    recentStops: List<SearchStopState.StopResult>,
): Boolean = listState == ListState.Recent && recentStops.isEmpty()
