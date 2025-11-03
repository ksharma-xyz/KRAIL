package xyz.ksharma.krail.core.remoteconfig.flag

interface Flag {
    fun getFlagValue(key: String): FlagValue
}
