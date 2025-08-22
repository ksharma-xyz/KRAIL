package xyz.ksharma.krail.core.appinfo

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build

class AndroidAppInfo(private val context: Context) : AppInfo {

    override val devicePlatformType: DevicePlatformType = DevicePlatformType.ANDROID

    override val isDebug: Boolean
        get() =
            context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    private val packageInfo by lazy {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }

    override val appVersion: String
        get() = packageInfo.versionName ?: "Unknown"

    override val appBuildNumber: String
        get() = packageInfo.longVersionCode.toString()

    override val osVersion: String
        get() = Build.VERSION.SDK_INT.toString()

    override val fontSize: String
        get() {
            val configuration = context.resources.configuration
            return configuration.fontScale.toString()
        }

    override val isDarkTheme: Boolean
        get() {
            val uiMode = context.resources.configuration.uiMode
            return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

    /**
     * Device model. E.g. "Pixel 4"
     */
    override val deviceModel: String
        get() = Build.MODEL

    /**
     * Device manufacturer. E.g. "Google"
     */
    override val deviceManufacturer: String
        get() = Build.BRAND

    /**
     * Locale. E.g. "en_US"
     */
    override val locale: String
        get() {
            val localeList = context.resources.configuration.locales
            return localeList.toLanguageTags()
        }

    override val batteryLevel: Int
        get() {
            val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }

    override val timeZone: String
        get() = java.util.TimeZone.getDefault().id

    override val appStoreUrl: String
        get() = "https://play.google.com/store/apps/details?id=xyz.ksharma.krail"

    override fun toString() =
        "AndroidAppInfo(type=$devicePlatformType, isDebug=$isDebug, appVersion=$appVersion, osVersion=$osVersion, " +
                "fontSize=$fontSize, isDarkTheme=$isDarkTheme, deviceModel=$deviceModel, " +
                "deviceManufacturer=$deviceManufacturer, locale=$locale, batteryLevel=$batteryLevel, " +
                "timeZone=$timeZone)"
}

class AndroidAppInfoProvider(
    private val context: Context,
) : AppInfoProvider {
    override fun getAppInfo(): AppInfo = AndroidAppInfo(context)
}

actual fun getAppPlatformType() = DevicePlatformType.ANDROID
