package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * Curated quick-select stops shown on SearchStopScreen's empty state — when the user
 * has no recent search history (true first open). Lets a brand-new user jump to a major
 * interchange without typing, instead of facing a blank canvas. Replaced by real recents
 * the moment the user has any — see [shouldShowEmptyStateStops] and `SEARCH_STOP_UX.md`.
 *
 * Hardcoded by product decision; the order is intentional (Town Hall, Central,
 * Parramatta, Wynyard). Stop IDs and transport modes match the bundled NSW GTFS data.
 */
internal val EMPTY_STATE_STOPS: ImmutableList<SearchStopState.StopResult> = persistentListOf(
    SearchStopState.StopResult(
        stopName = "Town Hall Station",
        stopId = "200070",
        transportModeType = persistentListOf(TransportMode.Train, TransportMode.LightRail),
    ),
    SearchStopState.StopResult(
        stopName = "Central Station",
        stopId = "200060",
        transportModeType = persistentListOf(
            TransportMode.Train,
            TransportMode.Metro,
            TransportMode.LightRail,
        ),
    ),
    SearchStopState.StopResult(
        stopName = "Parramatta Station",
        stopId = "215020",
        transportModeType = persistentListOf(TransportMode.Train, TransportMode.Bus),
    ),
    SearchStopState.StopResult(
        stopName = "Wynyard Station",
        stopId = "200080",
        transportModeType = persistentListOf(TransportMode.Train, TransportMode.LightRail),
    ),
)
