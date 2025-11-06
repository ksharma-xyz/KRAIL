package xyz.ksharma.krail.core.network

/**
 * Provides API base URLs based on:
 * 1. Current city selected by user (e.g., Sydney, Melbourne, Brisbane)
 * 2. Environment (Production vs Local BFF for debugging)
 *
 * **Production**: Returns city-specific production URL
 * **Debug**: Can override to use local BFF server
 *
 * Future-proof for multi-city expansion.
 */
interface ApiEnvironmentProvider {
    /**
     * Get base URL for the current city and environment.
     *
     * Examples:
     * - Sydney Production: https://api.transport.nsw.gov.au
     * - Melbourne Production: https://api.ptv.vic.gov.au (future)
     * - Local BFF (debug): http://10.0.2.2:8080
     */
    fun getBaseUrl(): String

    /**
     * Get the current environment type (Production or Local BFF).
     */
    fun getCurrentEnvironment(): EnvironmentType
}

/**
 * Environment types - independent of city.
 */
enum class EnvironmentType {
    /** Production API for the selected city */
    PRODUCTION,

    /** Local BFF server (debug only) */
    LOCAL_BFF;

    val displayName: String
        get() = when (this) {
            PRODUCTION -> "Production"
            LOCAL_BFF -> "Local BFF (Emulator)"
        }
}

/**
 * Supported cities with their production API base URLs.
 * Add new cities here as you expand.
 */
enum class City(val displayName: String, val productionBaseUrl: String) {
    SYDNEY("Sydney", "https://api.transport.nsw.gov.au"),
    // Future cities:
    // MELBOURNE("Melbourne", "https://api.ptv.vic.gov.au"),
    // BRISBANE("Brisbane", "https://api.translink.com.au"),
    // PERTH("Perth", "https://api.transperth.wa.gov.au"),
}

/**
 * Configuration combining city and environment.
 */
data class ApiConfig(
    val city: City,
    val environment: EnvironmentType,
) {
    /**
     * Resolve to actual base URL.
     */
    fun getBaseUrl(): String = when (environment) {
        EnvironmentType.PRODUCTION -> city.productionBaseUrl
        EnvironmentType.LOCAL_BFF -> "http://10.0.2.2:8080" // Local BFF for any city
    }

    val displayDescription: String
        get() = "${city.displayName} - ${environment.displayName}"
}
