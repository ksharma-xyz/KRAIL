package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.crowding.CarriageForecastRepository
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo

/**
 * Fetches the network-wide carriage forecasts at most once per results screen, and only once a
 * result actually has a train that could use them. The index covers every Sydney Trains trip,
 * so swapping origin and destination on the same screen does not need a second download.
 *
 * A failure leaves [index] empty and is not retried on this screen: the carriage row is an
 * extra, and the rider is never told it is missing.
 */
internal class CarriageForecastLoader(
    private val repository: CarriageForecastRepository?,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    var index: CarriageIndex = CarriageIndex.EMPTY
        private set

    private var started = false

    fun loadOnceFor(journeys: List<JourneyCardInfo>, onLoaded: () -> Unit) {
        val repository = repository ?: return
        if (started || !journeys.wantsCarriages()) return
        started = true
        scope.launch(dispatcher) {
            repository.load()
                .onSuccess {
                    index = it
                    log("[CROWDING] carriage forecasts loaded for ${it.tripCount} trips")
                    onLoaded()
                }
                .onFailure { log("[CROWDING] carriage forecasts unavailable: ${it.message}") }
        }
    }
}
