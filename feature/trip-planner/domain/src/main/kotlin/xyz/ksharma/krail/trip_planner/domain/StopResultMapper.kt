package xyz.ksharma.krail.trip_planner.domain

import xyz.ksharma.krail.trip_planner.domain.model.TransportModeType
import xyz.ksharma.krail.trip_planner.domain.model.toTransportModeType
import xyz.ksharma.krail.trip_planner.network.api.model.StopFinderResponse

object StopResultMapper {

    /**
     * Maps a [StopFinderResponse] to a list of [StopResult] objects, filtering the results based
     * on the selected transport mode types.
     *
     * @param selectedModes The list of selected transport mode types. By default, all mode types are
     * considered selected.
     *
     * @return A list of [StopResult] objects representing the filtered stops.
     */
    fun StopFinderResponse.toStopResults(
        selectedModes: List<TransportModeType> = TransportModeType.entries,
    ): List<StopResult> {

        return locations.orEmpty().mapNotNull { location ->
            val stopName = location.name ?: return@mapNotNull null // Skip if stop name is null
            val stopId = location.id ?: return@mapNotNull null // Skip if stop ID is null
            val modes = location.productClasses.orEmpty()
                .mapNotNull { it.toTransportModeType() }

            // Filter based on selected mode types
            if (selectedModes.isNotEmpty() && !modes.any { it in selectedModes }) {
                return@mapNotNull null
            }

            StopResult(stopName = stopName, stopId = stopId, transportModeType = modes)
        }.sortedBy { stopResult ->
            stopResult.transportModeType.minOfOrNull { mode ->
                selectedModes.find { it == mode }?.priority ?: Int.MAX_VALUE
            } ?: Int.MAX_VALUE
        }
    }

    data class StopResult(
        val stopName: String,
        val stopId: String,
        val transportModeType: List<TransportModeType> = emptyList(),
    )
}
