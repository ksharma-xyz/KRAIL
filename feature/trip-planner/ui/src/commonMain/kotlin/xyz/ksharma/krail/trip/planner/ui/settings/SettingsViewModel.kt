package xyz.ksharma.krail.trip.planner.ui.settings

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
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SocialConnectionLinkClickEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.social.ui.toAnalyticsEventPlatform
import xyz.ksharma.krail.trip.planner.ui.settings.ReferFriendManager.getReferText
import xyz.ksharma.krail.trip.planner.ui.state.settings.SettingsEvent
import xyz.ksharma.krail.trip.planner.ui.state.settings.SettingsState

class SettingsViewModel(
    private val appInfoProvider: AppInfoProvider,
    private val analytics: Analytics,
    private val platformOps: PlatformOps,
) : ViewModel() {

    private val _uiState: MutableStateFlow<SettingsState> = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState
        .onStart {
            fetchAppVersion()
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.Settings)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SocialLinkClick -> {
                platformOps.openUrl(url = event.krailSocialType.url)
                analytics.track(
                    event = SocialConnectionLinkClickEvent(
                        socialPlatformType = event.krailSocialType.toAnalyticsEventPlatform(),
                        source = SocialConnectionLinkClickEvent.SocialConnectionSource.SETTINGS,
                    ),
                )
            }
        }
    }

    private fun fetchAppVersion() {
        updateUiState {
            copy(
                appVersion = appInfoProvider.getAppInfo().appVersionDisplay,
            )
        }
    }

    fun onReferFriendClick() {
        platformOps.sharePlainText(text = getReferText(), title = "Tell your mates about KRAIL App")
        analytics.track(
            AnalyticsEvent.ReferFriend(
                entryPoint = AnalyticsEvent.ReferFriend.EntryPoint.SETTINGS,
            ),
        )
    }

    fun onIntroClick() {
        analytics.track(AnalyticsEvent.SettingsHowToUseClickEvent)
    }

    fun onOurStoryClick() {
        analytics.track(AnalyticsEvent.OurStoryClick)
    }

    private fun updateUiState(block: SettingsState.() -> SettingsState) {
        _uiState.update(block)
    }
}
