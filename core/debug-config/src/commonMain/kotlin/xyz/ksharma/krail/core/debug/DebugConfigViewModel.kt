package xyz.ksharma.krail.core.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.network.EnvironmentType

/**
 * ViewModel for Debug Configuration Screen.
 */
class DebugConfigViewModel(
    private val debugConfigManager: DebugConfigManager,
) : ViewModel() {

    val currentEnvironment = debugConfigManager.currentEnvironment

    fun setEnvironment(environment: EnvironmentType) {
        viewModelScope.launch {
            debugConfigManager.setEnvironment(environment)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            debugConfigManager.resetToDefaults()
        }
    }

    fun clearAllAppData() {
        viewModelScope.launch {
            debugConfigManager.clearAllAppData()
        }
    }
}
