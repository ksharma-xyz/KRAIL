package xyz.ksharma.krail.core.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.krail.core.network.EnvironmentType

/**
 * No-op implementation for production builds.
 * Always returns production values.
 */
class NoOpDebugConfigManager : DebugConfigManager {

    private val _currentEnvironment = MutableStateFlow(EnvironmentType.PRODUCTION)
    override val currentEnvironment: StateFlow<EnvironmentType> = _currentEnvironment.asStateFlow()

    override suspend fun setEnvironment(environment: EnvironmentType) {
        // No-op in production
    }

    override suspend fun resetToDefaults() {
        // No-op in production
    }

    override suspend fun clearAllAppData() {
        // No-op in production - this should never be called
    }
}

