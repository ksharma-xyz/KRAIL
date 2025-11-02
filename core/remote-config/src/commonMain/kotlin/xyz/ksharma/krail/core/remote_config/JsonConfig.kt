package xyz.ksharma.krail.core.remote_config

import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration for parsing remote config values.
 *
 * This lenient configuration ensures backward and forward compatibility:
 * - Unknown enum values are coerced to their default value (prevents crashes on old apps)
 * - Unknown JSON keys are ignored (allows adding new fields without breaking old apps)
 * - Lenient parsing handles minor JSON formatting issues
 *
 * Use this for all remote config JSON parsing to maintain consistency across the app.
 */
object JsonConfig {
    val lenient = Json {
        coerceInputValues = true // Coerce unknown enum values to default
        ignoreUnknownKeys = true // Ignore unknown JSON fields
        isLenient = true // Be lenient with JSON format
    }
}
