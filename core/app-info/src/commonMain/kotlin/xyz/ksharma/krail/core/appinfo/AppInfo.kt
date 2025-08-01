package xyz.ksharma.krail.core.appinfo

/**
 * Inject [AppInfoProvider] to get instance of [AppInfo].
 */
interface AppInfo {
    /**
     * Platform type. E.g. Android, iOS.
     */
    val devicePlatformType: DevicePlatformType

    /**
     * Debug status. E.g. true if debug build.
     */
    val isDebug: Boolean

    /**
     * App version.
     */
    val appVersion: String

    /**
     * OS version.
     * E.g.
     *  - "30" for Android 11.
     *  - "14.5" for iOS 14.5
     */
    val osVersion: String

    /**
     * Font size.
     * E.g. "L" for iOS
     * E.g. "1.0" for Android
     */
    val fontSize: String

    /**
     * Dark theme status.
     * E.g. true if dark theme is enabled.
     */
    val isDarkTheme: Boolean

    /**
     * Device model.
     * E.g. "Pixel 4" for Android
     * E.g. "iPhone" for iOS - Apple does not share this info.
     */
    val deviceModel: String

    /**
     * Device manufacturer.
     * E.g. "Google", "Samsung" for Android
     * E.g. "Apple" for iOS
     */
    val deviceManufacturer: String

    /**
     * Locale.
     * Android - Provides list of all language tags
     * iOS - Provides locale identifier, such as "en_US"
     */
    val locale: String

    /**
     * Battery level.
     * E.g. 50 for 50% battery level.
     */
    val batteryLevel: Int

    /**
     * Timezone.
     * E.g. "Australia/Sydney"
     */
    val timeZone: String

    val appStoreUrl: String
}

enum class DevicePlatformType {
    ANDROID,
    IOS,
    UNKNOWN,
}

interface AppInfoProvider {
    suspend fun getAppInfo(): AppInfo
}
