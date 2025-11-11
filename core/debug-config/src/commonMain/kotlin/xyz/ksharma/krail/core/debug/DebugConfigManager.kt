package xyz.ksharma.krail.core.debug

import kotlinx.coroutines.flow.StateFlow
import xyz.ksharma.krail.core.network.EnvironmentType

/**
 * Debug configuration manager interface.
 * Only handles debug UI and settings - actual API environment switching
 * is handled by ApiEnvironmentProvider in core/network.
 */
interface DebugConfigManager {

    /**
     * Current selected environment.
     */
    val currentEnvironment: StateFlow<EnvironmentType>

    /**
     * Set the API environment (saves to Sandook preferences).
     */
    suspend fun setEnvironment(environment: EnvironmentType)

    /**
     * Reset to default configuration.
     */
    suspend fun resetToDefaults()

    /**
     * Clear all app data (preferences, database, etc.)
     */
    suspend fun clearAllAppData()
}

