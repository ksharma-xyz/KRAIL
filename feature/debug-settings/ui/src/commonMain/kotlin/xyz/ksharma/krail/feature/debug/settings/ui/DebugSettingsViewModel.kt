package xyz.ksharma.krail.feature.debug.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import xyz.ksharma.krail.sandook.LifecycleCounter
import xyz.ksharma.krail.sandook.UserLifecycleStore

/**
 * Shared ViewModel for the Debug Config screens.
 *
 * Reads from [DebugNetworkConfigStore.state] and [Flag] (so the Network
 * screen's FOLLOW_RC row can show the live `enable_proto_bff` value plus
 * what it maps to). Writes go through [DebugNetworkConfigStore.set] which
 * persists and re-emits.
 */
class DebugSettingsViewModel(
    private val store: DebugNetworkConfigStore,
    private val flag: Flag,
    private val userLifecycleStore: UserLifecycleStore,
    private val aiTextService: AiTextService,
) : ViewModel() {

    private val _bffEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** Live RC value of `enable_proto_bff`, refreshed when the screen subscribes. */
    val bffEnabled: StateFlow<Boolean> = _bffEnabled

    // Starts `true` (assume available) rather than `false` so the toggles don't flash
    // disabled for the one frame before the real check completes.
    private val _onDeviceAiAvailable = MutableStateFlow(true)

    /**
     * Whether [AiTextService] reports [AiAvailability.Available] on this device right now —
     * used to disable the Alert Summary / AI Search Input toggles and explain why, rather
     * than letting a developer flip a flag that can never actually light up on this
     * particular device/OS combo (e.g. no Apple Intelligence hardware, or a device that
     * hasn't downloaded the on-device model yet).
     */
    val onDeviceAiAvailable: StateFlow<Boolean> = _onDeviceAiAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            _onDeviceAiAvailable.value = aiTextService.checkAvailability() is AiAvailability.Available
        }
    }

    /** Persisted debug-network state. */
    val state: StateFlow<DebugSettingsState> = store.state
        .onStart { refreshLiveFlag() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = DebugSettingsState.default(),
        )

    fun onEvent(event: DebugSettingsEvent) {
        viewModelScope.launch { store.set(event) }
    }

    fun selectSource(source: NetworkSource) {
        onEvent(DebugSettingsEvent.SetSource(source))
    }

    fun setTripTrackingEnabled(enabled: Boolean) {
        onEvent(DebugSettingsEvent.SetTripTrackingEnabled(enabled))
    }

    fun setAddressSearchEnabled(enabled: Boolean) {
        onEvent(DebugSettingsEvent.SetAddressSearchEnabled(enabled))
    }

    fun setAlertSummaryEnabled(enabled: Boolean) {
        onEvent(DebugSettingsEvent.SetAlertSummaryEnabled(enabled))
    }

    fun setAiSearchInputEnabled(enabled: Boolean) {
        onEvent(DebugSettingsEvent.SetAiSearchInputEnabled(enabled))
    }

    fun reset() {
        onEvent(DebugSettingsEvent.Reset)
    }

    /**
     * Clears the "already asked for review" count so the in-app review can fire again while
     * testing. Deliberately keeps the install date, so age-based gates stay realistic.
     */
    fun resetInAppReviewAsks() {
        viewModelScope.launch {
            userLifecycleStore.reset(LifecycleCounter.REVIEW_PROMPT_REQUESTED)
        }
    }

    private fun refreshLiveFlag() {
        _bffEnabled.value = flag.getFlagValue(FlagKeys.ENABLE_PROTO_BFF.key).asBoolean(false)
    }

    companion object {
        private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
