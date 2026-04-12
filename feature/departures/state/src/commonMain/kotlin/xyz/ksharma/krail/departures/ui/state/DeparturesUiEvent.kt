package xyz.ksharma.krail.departures.ui.state

/**
 * User-initiated events that drive the departures feature.
 *
 * Sent from the UI to the ViewModel (when the ViewModel is introduced in the ui module).
 */
sealed interface DeparturesUiEvent {

    /**
     * Requests departure data for [stopId].
     *
     * Triggers a full load — replaces any existing departures and shows the
     * loading indicator. Typically fired when the departures surface first appears.
     *
     * @param stopId NSW Transport stop ID, e.g. "10111010".
     */
    data class LoadDepartures(val stopId: String) : DeparturesUiEvent

    /**
     * Requests a silent refresh of the currently displayed stop's departures.
     *
     * Does not clear the existing list — [DeparturesState.silentLoading] is set
     * to true while the refresh is in flight so the UI can show a subtle indicator.
     */
    data object Refresh : DeparturesUiEvent

    /**
     * Requests past departures (~15 min window) for [stopId].
     *
     * Results land in [DeparturesState.previousDepartures] and are shown when the
     * user taps "Show previous" in the departure board UI.
     */
    data class LoadPreviousDepartures(val stopId: String) : DeparturesUiEvent
}
