package xyz.ksharma.krail.core.remoteconfig

interface RemoteConfig {

    /**
     * Retrieves the remote configuration value for the given key as a Boolean.
     *
     * @param key The key for which the configuration value is to be retrieved.
     *
     * @return The remote configuration value associated with the key as a Boolean.
     */
    fun getConfigValue(key: String): RemoteConfigValue

    /**
     * Sets up the remote configuration by initializing necessary settings and defaults.
     * This method should be called during the initialization phase of the application.
     */
    fun setup()
}
