package xyz.ksharma.krail.taj

enum class PlatformType {
    IOS,
    ANDROID,
    UNKNOWN,
}

expect fun getAppPlatformType(): PlatformType
