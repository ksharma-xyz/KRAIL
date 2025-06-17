package xyz.ksharma.krail.core.remote_config.flag

sealed class FlagValue {
    data class BooleanValue(val value: Boolean) : FlagValue()
    data class StringValue(val value: String) : FlagValue()
    data class JsonValue(val value: String) : FlagValue()
    data class NumberValue(val value: Long) : FlagValue()
}

fun FlagValue.asBoolean(): Boolean {
    return when (this) {
        is FlagValue.BooleanValue -> this.value
        is FlagValue.StringValue -> this.value.toBoolean()
        else -> false
    }
}

fun FlagValue.asString(): String {
    return when (this) {
        is FlagValue.StringValue -> this.value
        else -> ""
    }
}

fun FlagValue.asNumber(fallback: Long): Long {
    return when (this) {
        is FlagValue.NumberValue -> this.value
        else -> fallback
    }
}
