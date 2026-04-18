package xyz.ksharma.krail.trip.planner.ui.state.departureboard

/**
 * User-initiated events for the Departure Board accordion on the Saved Trips screen.
 *
 * All UI interactions are funnelled through a single [DepartureBoardViewModel.onEvent] call,
 * mirroring the pattern used by [SavedTripUiEvent] and [DeparturesUiEvent].
 */
sealed interface DepartureBoardUiEvent {

    /**
     * Expands the departure board card for [stopId], starting live polling.
     * If the same stop is already expanded this is a no-op.
     */
    data class ExpandStop(val stopId: String) : DepartureBoardUiEvent

    /**
     * Collapses the currently open departure board card, stopping polling.
     */
    data object CollapseStop : DepartureBoardUiEvent

    /**
     * Triggers a one-shot fetch of past departures for [stopId].
     * Results appear in [DeparturesState.previousDepartures].
     */
    data class LoadPreviousDepartures(val stopId: String) : DepartureBoardUiEvent

    /**
     * Requests a silent refresh of the departures for [stopId] without clearing the list.
     */
    data class RefreshStop(val stopId: String) : DepartureBoardUiEvent

    /**
     * Notifies the ViewModel that the user changed the line filter on the saved-trips accordion.
     *
     * @param stopId        The stop whose departures are filtered.
     * @param selected      `true` = filter applied; `false` = filter cleared.
     * @param lineNumber    The affected line, or `null` if unavailable. Always the affected line
     *                      even when [selected] is `false`.
     * @param transportMode Human-readable mode name for the selected line, or `null` when cleared.
     */
    data class LineFilterChanged(
        val stopId: String,
        val selected: Boolean,
        val lineNumber: String?,
        val transportMode: String?,
    ) : DepartureBoardUiEvent

    /**
     * Notifies the ViewModel that the user tapped the "Show / Hide previous departures" button.
     * Fired on every toggle so analytics is tracked per click.
     *
     * @param stopId The stop whose previous departures are toggled.
     * @param show   `true` = user opened the panel; `false` = user closed it.
     */
    data class TogglePreviousDepartures(
        val stopId: String,
        val show: Boolean,
    ) : DepartureBoardUiEvent
}
