package xyz.ksharma.krail.feature.debug.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore

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
) : ViewModel() {

    private val _bffEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** Live RC value of `enable_proto_bff`, refreshed when the screen subscribes. */
    val bffEnabled: StateFlow<Boolean> = _bffEnabled

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

    fun reset() {
        onEvent(DebugSettingsEvent.Reset)
    }

    private fun refreshLiveFlag() {
        _bffEnabled.value = flag.getFlagValue(FlagKeys.ENABLE_PROTO_BFF.key).asBoolean(false)
    }

    companion object {
        private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
