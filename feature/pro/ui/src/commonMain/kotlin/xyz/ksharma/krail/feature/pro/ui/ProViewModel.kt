package xyz.ksharma.krail.feature.pro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remoteconfig.JsonConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.feature.pro.state.ProDebugStore
import xyz.ksharma.krail.feature.pro.state.ProEvent
import xyz.ksharma.krail.feature.pro.state.ProFeature
import xyz.ksharma.krail.feature.pro.state.ProPlan
import xyz.ksharma.krail.feature.pro.state.ProState

class ProViewModel(
    private val flag: Flag,
    private val proDebugStore: ProDebugStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProState())
    val uiState: StateFlow<ProState> = _uiState
        .onStart { loadFeatures() }
        .combine(proDebugStore.isProEnabled) { state, isProActive ->
            state.copy(isProActive = isProActive)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProState())

    fun onEvent(event: ProEvent) {
        when (event) {
            is ProEvent.SelectPlan -> _uiState.update { it.copy(selectedPlan = event.plan) }
            is ProEvent.SubscribeTapped -> Unit // IAP integration future
            is ProEvent.RestorePurchaseTapped -> Unit // IAP integration future
        }
    }

    private fun loadFeatures() {
        val features = flag.getFlagValue(FlagKeys.PRO_FEATURES.key).toProFeatureList()
        _uiState.update { it.copy(features = features) }
    }

    private fun FlagValue.toProFeatureList(): List<ProFeature> {
        val jsonStr = when (this) {
            is FlagValue.JsonValue -> this.value
            else -> RemoteConfigDefaults.getDefaults()
                .firstOrNull { it.first == FlagKeys.PRO_FEATURES.key }?.second as? String ?: "[]"
        }
        return try {
            JsonConfig.lenient.decodeFromString<List<ProFeature>>(jsonStr)
                .filter { it.enabled }
                .ifEmpty { ProFeature.defaults() }
        } catch (e: Exception) {
            logError("Error decoding pro_features_json: $jsonStr", e)
            ProFeature.defaults()
        }
    }
}
