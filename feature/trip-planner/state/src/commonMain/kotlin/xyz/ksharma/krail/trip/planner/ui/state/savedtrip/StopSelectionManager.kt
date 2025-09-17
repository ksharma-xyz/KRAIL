package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Singleton manager for stop selection state that survives navigation lifecycle.
 * This ensures selected stops persist when navigating between screens.
 */
class StopSelectionManager {

    private val _fromStop = MutableStateFlow<StopItem?>(null)
    val fromStop: StateFlow<StopItem?> = _fromStop.asStateFlow()

    private val _toStop = MutableStateFlow<StopItem?>(null)
    val toStop: StateFlow<StopItem?> = _toStop.asStateFlow()

    fun setFromStop(stop: StopItem?) {
        _fromStop.value = stop
    }

    fun setToStop(stop: StopItem?) {
        _toStop.value = stop
    }

    fun reverseStops() {
        val tempFrom = _fromStop.value
        _fromStop.value = _toStop.value
        _toStop.value = tempFrom
    }

    fun clearStops() {
        _fromStop.value = null
        _toStop.value = null
    }
}
