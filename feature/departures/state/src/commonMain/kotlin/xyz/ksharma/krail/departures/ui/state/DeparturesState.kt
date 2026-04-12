package xyz.ksharma.krail.departures.ui.state

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture

/**
 * Represents the complete UI state for the departures feature.
 *
 * Consumed by the departures ViewModel and rendered by the departures UI components.
 * Designed to be shown in context-sensitive surfaces such as the stop selection
 * bottom sheet or a dedicated departures screen.
 *
 * State transitions:
 *  - Initial: [isLoading] = true, [departures] empty.
 *  - Success: [isLoading] = false, [departures] populated.
 *  - Error:   [isLoading] = false, [isError] = true, [departures] empty.
 *  - Refresh: [silentLoading] = true while existing [departures] remain visible.
 */
@Stable
data class DeparturesState(

    /**
     * True during the initial load before any departures have been fetched.
     * Show a full-screen loading indicator while this is true.
     */
    val isLoading: Boolean = true,

    /**
     * True when a background refresh is in progress but existing [departures]
     * are still visible. Use to show a subtle loading indicator rather than
     * replacing the list with a spinner.
     */
    val silentLoading: Boolean = false,

    /**
     * True when the departures request has failed and no data is available.
     */
    val isError: Boolean = false,

    /**
     * The human-readable name of the stop whose departures are displayed.
     * Null until the first successful response is received.
     */
    val stopName: String? = null,

    /**
     * The ordered list of upcoming departures. Empty during loading / error states.
     */
    val departures: ImmutableList<StopDeparture> = persistentListOf(),

    /**
     * Departures that occurred in the past ~15 minutes.
     * Empty until the user requests them via the "Show previous" toggle.
     * Preserved across regular refreshes so the user doesn't lose the data on auto-refresh.
     */
    val previousDepartures: ImmutableList<StopDeparture> = persistentListOf(),

    /**
     * True while a past-departures fetch is in flight after the user taps "Show previous".
     */
    val isPreviousLoading: Boolean = false,

    /**
     * How many minutes into the past the "Show previous" window covers.
     * Set by the repository from [DepartureBoardConfig.previousDeparturesWindowMinutes] so
     * the UI can display the correct number without hardcoding it. Will reflect remote-config
     * overrides automatically once that wiring is in place.
     */
    val previousWindowMinutes: Long = 30L,
)
