package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.crowding.CarriageForecastRepository
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo

/**
 * Refreshes the network-wide carriage forecasts alongside the results screen's own refresh
 * (every 30 seconds while it is visible), and only when a result has a train that could use
 * them. TfNSW updates the forecasts about once a minute, so a single download on opening goes
 * stale for a train twenty minutes out. The index covers every Sydney Trains trip, so swapping
 * origin and destination on the same screen does not need a second download.
 *
 * A refresh is skipped while one is in flight. A failed refresh keeps the last index, so one
 * dropped request never folds the cars away; after [MAX_FAILURES] in a row the index is cleared
 * and the carriage row folds. The rider is never told it is missing.
 */
internal class CarriageForecastLoader(
    private val repository: CarriageForecastRepository?,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    var index: CarriageIndex = CarriageIndex.EMPTY
        private set

    private var inFlight = false
    private var failures = 0

    /** Calls [onChanged] only when the index actually changed, so an unchanged feed redraws nothing. */
    fun refreshFor(journeys: List<JourneyCardInfo>, onChanged: () -> Unit) {
        val repository = repository ?: return
        if (inFlight || !journeys.wantsCarriages()) return
        inFlight = true
        scope.launch(dispatcher) {
            try {
                repository.load()
                    .onSuccess { loaded ->
                        failures = 0
                        log("[CROWDING] carriage forecasts loaded for ${loaded.tripCount} trips")
                        if (loaded != index) {
                            index = loaded
                            onChanged()
                        }
                    }
                    .onFailure { error ->
                        failures++
                        log("[CROWDING] carriage forecasts unavailable ($failures in a row): ${error.message}")
                        if (failures >= MAX_FAILURES && index != CarriageIndex.EMPTY) {
                            index = CarriageIndex.EMPTY
                            onChanged()
                        }
                    }
            } finally {
                inFlight = false
            }
        }
    }

    private companion object {
        const val MAX_FAILURES = 3
    }
}
