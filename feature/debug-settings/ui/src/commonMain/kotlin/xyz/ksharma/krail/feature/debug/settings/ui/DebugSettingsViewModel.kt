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
import xyz.ksharma.krail.feature.debug.settings.state.FlagOverride
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore

/**
 * Shared ViewModel for the three Debug Config screens.
 *
 * Reads from [DebugNetworkConfigStore.state] and [Flag] (so the Feature
 * Flags screen can show "Currently: <RC value>"). Writes go through
 * [DebugNetworkConfigStore.set] which persists and re-emits.
 */
class DebugSettingsViewModel(
    private val store: DebugNetworkConfigStore,
    private val flag: Flag,
) : ViewModel() {

    private val _liveBffEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** Live RC value of `enable_proto_bff`, refreshed when the screen subscribes. */
    val liveBffEnabled: StateFlow<Boolean> = _liveBffEnabled

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

    fun selectNetworkTarget(target: NetworkTarget) {
        onEvent(DebugSettingsEvent.SetNetworkTarget(target))
    }

    fun selectFlagOverride(override: FlagOverride) {
        onEvent(DebugSettingsEvent.SetFlagOverride(override))
    }

    fun reset() {
        onEvent(DebugSettingsEvent.Reset)
    }

    private fun refreshLiveFlag() {
        _liveBffEnabled.value = flag.getFlagValue(FlagKeys.ENABLE_PROTO_BFF.key).asBoolean(false)
    }

    companion object {
        private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
