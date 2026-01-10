package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

data class SearchStopState(
    val searchResults: ImmutableList<SearchResult> = persistentListOf(),
    val recentStops: ImmutableList<StopResult> = persistentListOf(),
    val isError: Boolean = false,
    val displayMapSelection: Boolean = false,
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
         * Trip search result (from exact route short name match)
         *
         * **Important**: Each Trip object represents ONE specific journey/direction!
         *
         * Terminology (GTFS standard):
         * - Route: A transit route (e.g., "702 bus route")
         * - Trip: A specific journey on that route in one direction
         * - Stop: A physical location where vehicles stop
         *
         * Example: Searching "702" returns 4 separate Trip objects (one per direction):
         * - Trip("702", "Blacktown to Parramatta", stops=[A, B, C, D, E])
         * - Trip("702", "Parramatta to Blacktown", stops=[E, D, C, B, A])
         * - Trip("702", "Mayfield to Warabrook", stops=[X, Y, Z])
         * - Trip("702", "Warabrook to Mayfield", stops=[Z, Y, X])
         *
         * Data flow:
         * 1. Proto file (NswBusTripOption): Each trip has headsign and ordered list of stopIds
         * 2. Database (NswBusTripOptions): Stops stored with stopSequence (0, 1, 2, ...) preserving proto order
         * 3. ViewModel: Groups trips by headsign, takes ONE representative trip per direction
         * 4. This object: Clean UI state with only what's needed for display
         *
         * The [stops] field contains:
         * - Stops from ONE specific trip (the representative trip for this direction)
         * - Already sorted by stopSequence from database (ORDER BY ts.stopSequence ASC)
         * - Ready for direct UI rendering (no flatMap/distinctBy/sortedBy needed in Composable)
         */
        data class Trip(
            val tripId: String, // Unique identifier for this trip (used as key in UI)
            val routeShortName: String, // The route this trip belongs to (e.g., "702")
            val headsign: String, // The direction/destination (e.g., "Blacktown to Parramatta")
            val stops: ImmutableList<TripStop> = persistentListOf(), // Ordered stops for this trip
            val transportMode: TransportMode,
        ) : SearchResult()
    }

    /**
     * Represents a stop within a trip
     *
     * Fields:
     * - stopId: Unique identifier from NswStops table
     * - stopName: Display name of the stop
     * - stopSequence: Order in which this stop is visited (0, 1, 2, ...)
     *   This comes from the index in the proto file's stopIds array and is stored
     *   in the database to preserve the correct order.
     * - transportModeType: List of transport modes available at this stop
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
