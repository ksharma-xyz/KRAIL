package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.toPersistentList
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

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
        selectedModes: Set<TransportMode> = TransportMode.values(),
    ): List<SearchStopState.StopResult> {
        log("selectedModes: " + selectedModes)

        return locations.orEmpty().mapNotNull { location ->
            val stopName = location.disassembledName ?: return@mapNotNull null // Skip if stop name is null
            val stopId = location.id ?: return@mapNotNull null // Skip if stop ID is null
            val modes = location.productClasses.orEmpty()
                .mapNotNull { productClass -> TransportMode.toTransportModeType(productClass) }

            log("productClasses [${location.name}]: ${location.productClasses}")

            // Filter based on selected mode types
            if (selectedModes.isNotEmpty() && !modes.any { it in selectedModes }) {
                return@mapNotNull null
            }

            SearchStopState.StopResult(
                stopName = stopName,
                stopId = stopId,
                transportModeType = modes.toPersistentList(),
            )
        }.sortedBy { stopResult ->
            stopResult.transportModeType.minOfOrNull { mode ->
                selectedModes.find { it == mode }?.priority ?: Int.MAX_VALUE
            } ?: Int.MAX_VALUE
        }
    }
}
