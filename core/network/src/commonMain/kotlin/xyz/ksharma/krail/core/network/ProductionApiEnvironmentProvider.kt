package xyz.ksharma.krail.core.network

/**
 * Production implementation of ApiEnvironmentProvider.
 *
 * Returns production URL for the currently selected city.
 * In future, city selection can be read from preferences/database.
 * For now, defaults to Sydney.
 */
internal class ProductionApiEnvironmentProvider : ApiEnvironmentProvider {

    private val currentCity = City.SYDNEY

    override fun getBaseUrl(): String {
        return ApiConfig(
            city = currentCity,
            environment = EnvironmentType.PRODUCTION
        ).getBaseUrl()
    }

    override fun getCurrentEnvironment(): EnvironmentType = EnvironmentType.PRODUCTION
}
