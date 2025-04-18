package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.platform.ops.ContentSharing
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.ui.settings.ReferFriendManager.getReferText
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent

class IntroViewModel(
    private val analytics: Analytics,
    private val share: ContentSharing,
    private val preferences: SandookPreferences,
) : ViewModel() {

    private val _uiState: MutableStateFlow<IntroState> = MutableStateFlow(IntroState.default())
    val uiState: StateFlow<IntroState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.Intro)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IntroState.default())

    fun onEvent(event: IntroUiEvent) {
        when (event) {
            IntroUiEvent.ReferFriend -> {
                share.sharePlainText(getReferText())
                analytics.track(AnalyticsEvent.ReferFriend)
            }

            IntroUiEvent.Complete -> {
                preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO, true)
            }
        }
    }

    private fun updateUiState(block: IntroState.() -> IntroState) {
        _uiState.update(block)
    }
}
