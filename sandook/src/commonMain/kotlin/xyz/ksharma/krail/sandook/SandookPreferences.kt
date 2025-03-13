package xyz.ksharma.krail.sandook

interface SandookPreferences {

    // Integer preferences
    fun getLong(key: String): Long?
    fun setLong(key: String, value: Long)

    // String preferences
    fun getString(key: String): String?
    fun setString(key: String, value: String)

    // Boolean preferences
    fun getBoolean(key: String): Boolean?
    fun setBoolean(key: String, value: Boolean)

    // Double preferences
    fun getDouble(key: String): Double?
    fun setDouble(key: String, value: Double)

    // Delete preference
    fun deletePreference(key: String)

    companion object {
        // Increment this when bundling new stops data
        const val NSW_STOPS_VERSION = 2L

        const val KEY_NSW_STOPS_VERSION = "KEY_NSW_STOPS_VERSION"
    }
}
