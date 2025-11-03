package xyz.ksharma.krail.core.remoteconfig.flag

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remoteconfig.RemoteConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigValue

internal class RemoteConfigFlag(private val remoteConfig: RemoteConfig) : Flag {

    override fun getFlagValue(key: String): FlagValue {
        val configValue = remoteConfig.getConfigValue(key)
        log("RemoteConfigFlag: getFlagValue: key=$key, configValue=$configValue")

        return when {
            // Boolean
            configValue.isBoolean() -> {
                FlagValue.BooleanValue(configValue.asBoolean())
            }

            // Json
            configValue.isJson() -> {
                FlagValue.JsonValue(configValue.asString())
            }

            // Integer
            configValue.isNumber() -> {
                FlagValue.NumberValue(configValue.asNumber())
            }

            // String otherwise
            else -> FlagValue.StringValue(configValue.asString())
        }
    }

    private fun RemoteConfigValue.isBoolean(): Boolean =
        asString().toBooleanStrictOrNull() != null

    private fun RemoteConfigValue.isJson(): Boolean =
        asString().startsWith("[") || asString().startsWith("{")

    private fun RemoteConfigValue.isNumber(): Boolean = asString().toLongOrNull() != null

    private fun RemoteConfigValue.asNumber(): Long = asLong()
}
