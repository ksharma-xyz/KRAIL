package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import xyz.ksharma.krail.trip.planner.ui.state.discover.DiscoverEvent
import xyz.ksharma.krail.trip.planner.ui.state.discover.DiscoverState

class DiscoverViewModel(
) : ViewModel() {

    private val _uiState: MutableStateFlow<DiscoverState> = MutableStateFlow(DiscoverState())
    val uiState: StateFlow<DiscoverState> = _uiState
        .onStart {
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiscoverState())

    fun onEvent(event: DiscoverEvent) {
        when (event) {
            DiscoverEvent.ButtonClicked -> {}
        }
    }
}
