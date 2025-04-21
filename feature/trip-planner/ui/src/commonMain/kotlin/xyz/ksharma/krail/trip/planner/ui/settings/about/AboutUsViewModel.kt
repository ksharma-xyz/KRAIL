package xyz.ksharma.krail.trip.planner.ui.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.trip.planner.ui.state.settings.about.AboutUsState

class AboutUsViewModel(
    private val analytics: Analytics,
) : ViewModel() {

    private val _uiState: MutableStateFlow<AboutUsState> = MutableStateFlow(AboutUsState())
    val uiState: StateFlow<AboutUsState> = _uiState
        .onStart {

            analytics.trackScreenViewEvent(screen = AnalyticsScreen.OurStory)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AboutUsState())

}
