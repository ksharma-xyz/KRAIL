package xyz.ksharma.krail.core.network

import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * Debug implementation of ApiEnvironmentProvider.
 *
 * Reads environment selection from Sandook preferences (set via debug menu).
 * Supports city selection - returns production URL for selected city or local BFF.
 *
 * Preferences keys:
 * - KEY_API_ENVIRONMENT: "PRODUCTION" or "LOCAL_BFF"
 * - KEY_SELECTED_CITY: "SYDNEY", "MELBOURNE", etc. (future)
 */
class DebugApiEnvironmentProvider(
    private val sandookPreferences: SandookPreferences,
) : ApiEnvironmentProvider {

    override fun getBaseUrl(): String {
        val config = getCurrentConfig()
        return config.getBaseUrl()
    }

    override fun getCurrentEnvironment(): EnvironmentType {
        val savedEnv = sandookPreferences.getString(KEY_API_ENVIRONMENT)
        return when (savedEnv) {
            EnvironmentType.LOCAL_BFF.name -> EnvironmentType.LOCAL_BFF
            else -> EnvironmentType.PRODUCTION
        }
    }

    private fun getCurrentConfig(): ApiConfig {
        val environment = getCurrentEnvironment()
        val city = getCurrentCity()
        return ApiConfig(city = city, environment = environment)
    }

    private fun getCurrentCity(): City {
        // TODO: In future, read from preferences when multi-city is implemented
        // val savedCity = sandookPreferences.getString(KEY_SELECTED_CITY)
        // return City.valueOf(savedCity ?: City.SYDNEY.name)
        return City.SYDNEY
    }

    companion object {
        const val KEY_API_ENVIRONMENT = "KEY_API_ENVIRONMENT"
        // const val KEY_SELECTED_CITY = "KEY_SELECTED_CITY" // For future use
    }
}

