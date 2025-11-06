package xyz.ksharma.krail.core.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.network.EnvironmentType
import xyz.ksharma.krail.core.network.DebugApiEnvironmentProvider
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * Real implementation of DebugConfigManager with Sandook persistence.
 * Saves environment selection to same key used by DebugApiEnvironmentProvider.
 */
class RealDebugConfigManager(
    private val sandookPreferences: SandookPreferences,
    private val sandook: Sandook,
) : DebugConfigManager {

    private val _currentEnvironment = MutableStateFlow(loadSavedEnvironment())
    override val currentEnvironment: StateFlow<EnvironmentType> = _currentEnvironment.asStateFlow()

    override suspend fun setEnvironment(environment: EnvironmentType) {
        log("DebugConfig: Switching environment to ${environment.name}")
        _currentEnvironment.value = environment
        // Save using the same key that DebugApiEnvironmentProvider reads from
        sandookPreferences.setString(DebugApiEnvironmentProvider.KEY_API_ENVIRONMENT, environment.name)
    }

    override suspend fun resetToDefaults() {
        log("DebugConfig: Resetting to defaults")
        _currentEnvironment.value = EnvironmentType.PRODUCTION
        sandookPreferences.deletePreference(DebugApiEnvironmentProvider.KEY_API_ENVIRONMENT)
    }

    override suspend fun clearAllAppData() {
        log("DebugConfig: Clearing all app data")

        // Clear Sandook database
        sandook.clearTheme()
        sandook.clearSavedTrips()
        sandook.clearAlerts()

        // Clear all shared preferences except NSW stops version
        val stopsVersion = sandookPreferences.getLong(SandookPreferences.KEY_NSW_STOPS_VERSION)

        // todo - create clear all methods in SandookPreferences
        sandookPreferences.deletePreference(SandookPreferences.KEY_HAS_SEEN_INTRO)
        sandookPreferences.deletePreference(SandookPreferences.KEY_DISCOVER_CLICKED_BEFORE)
        sandookPreferences.deletePreference(SandookPreferences.KEY_DISMISSED_INFO_TILES)
        sandookPreferences.deletePreference(SandookPreferences.KEY_THEME_MODE)
        sandookPreferences.deletePreference(SandookPreferences.KEY_HAS_SEEN_INVITE_FRIENDS_TILE)
        sandookPreferences.deletePreference(DebugApiEnvironmentProvider.KEY_API_ENVIRONMENT)

        // Restore stops version
        stopsVersion?.let { sandookPreferences.setLong(SandookPreferences.KEY_NSW_STOPS_VERSION, it) }

        // Reset to production environment
        _currentEnvironment.value = EnvironmentType.PRODUCTION

        log("DebugConfig: App data cleared successfully")
    }

    private fun loadSavedEnvironment(): EnvironmentType {
        val savedEnv = sandookPreferences.getString(DebugApiEnvironmentProvider.KEY_API_ENVIRONMENT)
        return savedEnv?.let { envName ->
            try {
                EnvironmentType.valueOf(envName)
            } catch (e: Exception) {
                log("DebugConfig: Invalid saved environment: $envName, using PRODUCTION")
                EnvironmentType.PRODUCTION
            }
        } ?: EnvironmentType.PRODUCTION
    }
}
