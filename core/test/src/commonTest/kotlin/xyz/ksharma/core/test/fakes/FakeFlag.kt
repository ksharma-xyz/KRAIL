package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue

class FakeFlag : Flag {
    private val flagValues = mutableMapOf<String, FlagValue>()

    fun setFlagValue(key: String, value: FlagValue) {
        flagValues[key] = value
    }

    override fun getFlagValue(key: String): FlagValue {
        return flagValues[key] ?: when (key) {
            FlagKeys.OUR_STORY_TEXT.key -> FlagValue.StringValue("Story Text")
            FlagKeys.DISCLAIMER_TEXT.key -> FlagValue.StringValue("Disclaimer Text")
            else -> FlagValue.BooleanValue(false)
        }
    }

    fun setDiscoverAvailable(value: Boolean) {
        setFlagValue(FlagKeys.DISCOVER_AVAILABLE.key, FlagValue.BooleanValue(value))
    }
}