package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

data class SearchStopState(
    val searchResults: ImmutableList<SearchResult> = persistentListOf(),
    val recentStops: ImmutableList<StopResult> = persistentListOf(),
    val isError: Boolean = false,
    val isLoading: Boolean = false,
) {
    /**
     * Base sealed class for search results - can be either a single stop or a route group
     */
    sealed class SearchResult {
        /**
         * Individual stop result (from stop name/ID search)
         */
        data class Stop(
            val stopName: String,
            val stopId: String,
            val transportModeType: ImmutableList<TransportMode> = persistentListOf(),
        ) : SearchResult()

        /**
         * Route group result (from route short name search)
         * Contains all variants and trips for a route
         */
        data class Route(
            val routeShortName: String,
            val variants: ImmutableList<RouteVariant> = persistentListOf(),
        ) : SearchResult()
    }

    /**
     * Represents a variant of a route (e.g., different directions or service patterns)
     */
    data class RouteVariant(
        val routeId: String,
        val routeName: String,
        val trips: ImmutableList<TripOption> = persistentListOf(),
    )

    /**
     * Represents a specific trip/direction for a route variant
     */
    data class TripOption(
        val tripId: String,
        val headsign: String,
        val stops: ImmutableList<TripStop> = persistentListOf(),
    )

    /**
     * Represents a stop within a trip (with sequence information)
     */
    data class TripStop(
        val stopId: String,
        val stopName: String,
        val stopSequence: Int,
        val transportModeType: ImmutableList<TransportMode> = persistentListOf(),
    )

    /**
     * Legacy StopResult for backward compatibility (e.g., recent stops)
     */
    data class StopResult(
        val stopName: String,
        val stopId: String,
        val transportModeType: ImmutableList<TransportMode> = persistentListOf(),
    )
}
