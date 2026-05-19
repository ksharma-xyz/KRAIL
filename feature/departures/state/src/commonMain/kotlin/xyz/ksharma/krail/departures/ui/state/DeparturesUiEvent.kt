package xyz.ksharma.krail.departures.ui.state

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource

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
     * @param stopId   NSW Transport stop ID, e.g. "10111010".
     * @param stopName Human-readable stop name shown in the UI header (used for analytics).
     * @param source   Which surface opened the board (used for analytics).
     */
    data class LoadDepartures(
        val stopId: String,
        val stopName: String = "",
        val source: DepartureBoardSource,
    ) : DeparturesUiEvent

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

    /**
     * Stops the active polling loop for the current stop.
     *
     * Typically fired when the departure board card collapses in uncontrolled mode
     * so no background fetches run while departures are not visible.
     */
    data object StopPolling : DeparturesUiEvent

    /**
     * Notifies the ViewModel that the user changed the line filter.
     *
     * Fired by the UI whenever the selected filter chip changes. The ViewModel uses
     * this to log analytics — no departure data is re-fetched.
     *
     * @param stopId        The stop whose departures are currently displayed.
     * @param selected      `true` = filter applied; `false` = filter cleared.
     * @param lineNumber    The affected line (e.g. "T1", "333"). Always present — even when
     *                      [selected] is `false`, the previously selected line is passed.
     * @param transportMode Human-readable mode name for the selected line (e.g. "Train"),
     *                      or `null` if unavailable.
     */
    data class LineFilterChanged(
        val stopId: String,
        val selected: Boolean,
        val lineNumber: String?,
        val transportMode: String?,
    ) : DeparturesUiEvent

    /**
     * Notifies the ViewModel that the user tapped the "Show / Hide previous departures" button.
     * Fired on every toggle — both show and hide — so analytics is tracked per click.
     *
     * @param stopId The stop whose previous departures are toggled.
     * @param show   `true` = user opened the panel; `false` = user closed it.
     */
    data class TogglePreviousDepartures(
        val stopId: String,
        val show: Boolean,
    ) : DeparturesUiEvent

    /**
     * Notifies the ViewModel that the user tapped the Departure Board header to expand
     * or collapse the live board on any stop-sheet surface.
     *
     * @param stopId   The stop's ID.
     * @param stopName The stop's human-readable name.
     * @param expand   `true` = board is being expanded; `false` = board is being collapsed.
     * @param source   Which surface the toggle happened on.
     */
    data class DepartureBoardToggle(
        val stopId: String,
        val stopName: String,
        val expand: Boolean,
        val source: DepartureBoardSource,
    ) : DeparturesUiEvent
}
