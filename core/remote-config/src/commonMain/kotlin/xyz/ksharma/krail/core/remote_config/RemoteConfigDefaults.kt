package xyz.ksharma.krail.core.remote_config

import xyz.ksharma.krail.core.remote_config.flag.FlagKeys

/**
 * Holds the default values for remote configuration.
 * Add new default values here. These will be used as fallbacks when the remote config is not available
 * due to network or other issues.
 */
object RemoteConfigDefaults {

    /**
     * Returns a list of default configuration key-value pairs.
     * These defaults are used as fallbacks when remote config values are not available.
     */
    fun getDefaults(): Array<Pair<String, Any?>> {
        return arrayOf(
            Pair(FlagKeys.LOCAL_STOPS_ENABLED.key, true),
            Pair(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key,
                """["200060", "200070", "200080", "206010", "2150106", "200017", "200039", "201016", "201039", "201080", "200066", "200030", "200046", "200050"]""".trimMargin()
            )
        )
    }
}
