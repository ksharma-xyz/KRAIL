package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.InteractionPage
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.ui.settings.ReferFriendManager.getReferText
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent

class IntroViewModel(
    private val analytics: Analytics,
    private val platformOps: PlatformOps,
    private val preferences: SandookPreferences,
    private val stopsManager: StopsManager,
) : ViewModel() {

    private val _uiState: MutableStateFlow<IntroState> = MutableStateFlow(IntroState.default())
    val uiState: StateFlow<IntroState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.Intro)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IntroState.default())

    fun onEvent(event: IntroUiEvent) {
        when (event) {
            is IntroUiEvent.ReferFriend -> {
                platformOps.sharePlainText(getReferText())
                analytics.track(
                    AnalyticsEvent.ReferFriend(
                        entryPoint = event.analyticsEntryPoint,
                    )
                )
            }

            is IntroUiEvent.Complete -> {
                viewModelScope.launch {
                    stopsManager.insertStops()
                    preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO, true)
                    analytics.track(
                        AnalyticsEvent.IntroLetsKrailClickEvent(
                            pageType = event.pageType.toInteractionPage(),
                            pageNumber = event.pageNumber,
                        )
                    )
                }
            }
        }
    }

    private fun IntroState.IntroPageType.toInteractionPage(): InteractionPage =
        when (this) {
            IntroState.IntroPageType.SAVE_TRIPS -> InteractionPage.SAVE_TRIPS
            IntroState.IntroPageType.REAL_TIME_ROUTES -> InteractionPage.REAL_TIME_ROUTES
            IntroState.IntroPageType.ALERTS -> InteractionPage.ALERTS
            IntroState.IntroPageType.PLAN_TRIP -> InteractionPage.PLAN_TRIP
            IntroState.IntroPageType.SELECT_MODE -> InteractionPage.SELECT_MODE
            IntroState.IntroPageType.INVITE_FRIENDS -> InteractionPage.INVITE_FRIENDS
            IntroState.IntroPageType.PARK_RIDE -> InteractionPage.PARK_RIDE
        }

    private fun updateUiState(block: IntroState.() -> IntroState) {
        _uiState.update(block)
    }
}
