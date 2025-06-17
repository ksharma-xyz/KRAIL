package xyz.ksharma.krail.sandook

internal class RealSandookPreferences(
    private val queries: AppPreferencesQueries,
) : SandookPreferences {

    override fun getLong(key: String): Long? {
        return queries.getLongPreference(key).executeAsOneOrNull()?.int_value
    }

    override fun setLong(key: String, value: Long) {
        queries.setLongPreference(key, value)
    }

    override fun getString(key: String): String? {
        return queries.getStringPreference(key).executeAsOneOrNull()?.string_value
    }

    override fun setString(key: String, value: String) {
        queries.setStringPreference(key, value)
    }

    override fun getBoolean(key: String): Boolean? {
        return queries
            .getBooleanPreference(key)
            .executeAsOneOrNull()?.let { it.bool_value == 1L }
    }

    override fun setBoolean(key: String, value: Boolean) {
        queries.setBooleanPreference(key, if (value) 1L else 0L)
    }

    override fun getDouble(key: String): Double? {
        return queries
            .getDoublePreference(key)
            .executeAsOneOrNull()?.float_value
    }

    override fun setDouble(key: String, value: Double) {
        queries.setDoublePreference(key, value)
    }

    override fun deletePreference(key: String) {
        queries.deletePreference(key)
    }
}
