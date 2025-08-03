package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.sandook.SandookPreferences

class FakeSandookPreferences : SandookPreferences {

    private val preferences = mutableMapOf<String, Any>()

    override fun getLong(key: String): Long? {
        return preferences[key] as? Long
    }

    override fun setLong(key: String, value: Long) {
        preferences[key] = value
    }

    override fun getString(key: String): String? {
        return preferences[key] as? String
    }

    override fun setString(key: String, value: String) {
        preferences[key] = value
    }

    override fun getBoolean(key: String): Boolean? {
        return preferences[key] as? Boolean
    }

    override fun setBoolean(key: String, value: Boolean) {
        preferences[key] = value
    }

    override fun getDouble(key: String): Double? {
        return preferences[key] as? Double
    }

    override fun setDouble(key: String, value: Double) {
        preferences[key] = value
    }

    override fun deletePreference(key: String) {
        preferences.remove(key)
    }
}
