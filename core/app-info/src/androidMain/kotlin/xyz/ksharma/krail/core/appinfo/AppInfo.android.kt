package xyz.ksharma.krail.core.appinfo

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AndroidAppInfo(private val context: Context) : AppInfo {

    override val devicePlatformType: DevicePlatformType = DevicePlatformType.ANDROID

    override val isDebug: Boolean
        get() =
            context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    override val appVersion: String
        get() {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionName ?: "Unknown"
        }
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

    override fun toString() =
        "AndroidAppInfo(type=$devicePlatformType, isDebug=$isDebug, appVersion=$appVersion, osVersion=$osVersion, " +
            "fontSize=$fontSize, isDarkTheme=$isDarkTheme, deviceModel=$deviceModel, " +
            "deviceManufacturer=$deviceManufacturer, locale=$locale, batteryLevel=$batteryLevel, " +
            "timeZone=$timeZone)"
}

class AndroidAppInfoProvider(
    private val context: Context,
    private val defaultDispatcher: CoroutineDispatcher,
) : AppInfoProvider {
    override suspend fun getAppInfo(): AppInfo = withContext(defaultDispatcher) {
        AndroidAppInfo(context)
    }
}

actual fun getAppPlatformType() = DevicePlatformType.ANDROID
