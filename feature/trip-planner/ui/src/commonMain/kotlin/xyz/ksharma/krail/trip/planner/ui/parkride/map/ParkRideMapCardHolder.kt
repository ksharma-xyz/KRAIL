package xyz.ksharma.krail.trip.planner.ui.parkride.map

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import xyz.ksharma.krail.coroutines.ext.coroutineExceptionHandler
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ParkRideMapCardUi

/**
 * The one open parking card of a stop map's ViewModel: it follows the station that was opened and is [show]n through the
 * ViewModel's own state, until the card is closed or another is opened.
 *
 * The card is loaded on [dispatcher] and shown on [scope]'s own, the thread [open] and [close] are called on, so nothing
 * of a card can be shown after it has been closed.
 */
internal class ParkRideMapCardHolder(
    private val source: ParkRideMapSource,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val show: (ParkRideMapCardUi?) -> Unit,
) {
    private var job: Job? = null

    fun open(stopId: String) {
        close()
        job = scope.launch(coroutineExceptionHandler(message = "ParkRideMapCardHolder")) {
            source.card(stopId).flowOn(dispatcher).collect { show(it) }
        }
    }

    fun close() {
        job?.cancel()
        job = null
        show(null)
    }
}
