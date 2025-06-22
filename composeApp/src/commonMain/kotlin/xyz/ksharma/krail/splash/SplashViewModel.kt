package xyz.ksharma.krail.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appstart.AppStart
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.coroutines.ext.safeResult
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_HAS_SEEN_INTRO
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle

class SplashViewModel(
    private val sandook: Sandook,
    private val analytics: Analytics,
    private val appInfoProvider: AppInfoProvider,
    private val ioDispatcher: CoroutineDispatcher,
    private val appStart: AppStart,
    private val preferences: SandookPreferences,
) : ViewModel() {

    private val _uiState: MutableStateFlow<SplashState> = MutableStateFlow(SplashState())
    val uiState: MutableStateFlow<SplashState> = _uiState

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
        .onStart {
            coroutineScope {
                loadKrailThemeStyle()
                displayIntroScreen()
                appStart.start()
                trackAppStartEvent()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private suspend fun displayIntroScreen() = safeResult(ioDispatcher) {
        val hasSeenIntro = preferences.getBoolean(KEY_HAS_SEEN_INTRO)
        log("Has seen intro: $hasSeenIntro")
        if (hasSeenIntro == true) {
            updateUiState { copy(hasSeenIntro = true) }
        } else {
            updateUiState { copy(hasSeenIntro = false) }
        }
    }

    private suspend fun trackAppStartEvent() = with(appInfoProvider.getAppInfo()) {
        log("AppInfo: $this, krailTheme: ${_uiState.value.themeStyle.id}")
        analytics.track(
            AnalyticsEvent.AppStart(
                platformType = devicePlatformType.name,
                osVersion = osVersion,
                appVersion = appVersion,
                fontSize = fontSize,
                isDarkTheme = isDarkTheme,
                deviceModel = deviceModel,
                krailTheme = _uiState.value.themeStyle.id,
                locale = locale,
                batteryLevel = batteryLevel,
                timeZone = timeZone
            )
        )
    }

    private suspend fun loadKrailThemeStyle() = safeResult(ioDispatcher) {
        // First app launch there will be no product class, so use default theme style.
        val themeId = sandook.getProductClass()?.toInt()
        val themeStyle = KrailThemeStyle.entries.find { it.id == themeId }

        updateUiState {
            copy(
                themeStyle = themeStyle ?: DEFAULT_THEME_STYLE
            )
        }
    }.onFailure {
        logError("Error loading KRAIL theme style: $it", it)
    }.onSuccess {
        log("Krail theme style loaded: $it")
    }

    private fun updateUiState(block: SplashState.() -> SplashState) {
        _uiState.update(block)
    }
}
