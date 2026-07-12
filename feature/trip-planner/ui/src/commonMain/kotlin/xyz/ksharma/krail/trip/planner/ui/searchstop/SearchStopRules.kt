package xyz.ksharma.krail.trip.planner.ui.searchstop

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
 * Whether the pill row (label shortcuts) should render.
 *
 * Hide it on a fresh-install / empty-search canvas where there are no stops below to
 * assign — an empty pill row with nothing to act on feels broken to the user.
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
