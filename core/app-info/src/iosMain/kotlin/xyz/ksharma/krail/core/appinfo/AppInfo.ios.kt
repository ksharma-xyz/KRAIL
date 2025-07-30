package xyz.ksharma.krail.core.appinfo

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.localeIdentifier
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.absoluteValue

class IOSAppInfo : AppInfo {

    override val devicePlatformType: DevicePlatformType = DevicePlatformType.IOS

    @OptIn(ExperimentalNativeApi::class)
    override val isDebug: Boolean
        get() = Platform.isDebugBinary

    override val appVersion: String
        get() {
            val shortVersion =
                NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
                    ?: "Unknown"
            val buildVersion =
                NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String ?: "Unknown"
            return "$shortVersion ${if (isDebug) "($buildVersion)" else ""}"
        }

    override val osVersion: String
        get() = UIDevice.currentDevice.systemVersion

    override val fontSize: String
        get() {
            val contentSizeCategory = UIApplication.sharedApplication.preferredContentSizeCategory
            return contentSizeCategory?.substring(startIndex = "UICTContentSizeCategory".length)
                ?: contentSizeCategory.toString()
        }

    override val isDarkTheme: Boolean
        get() {
            val userInterfaceStyle = UIScreen.mainScreen.traitCollection.userInterfaceStyle
            return userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }

    // Note: this is not real device model, Apple does not share this info.
    override val deviceModel: String
        get() = UIDevice.currentDevice.model

    override val deviceManufacturer: String
        get() = "Apple"

    override val locale: String
        get() = NSLocale.currentLocale.localeIdentifier

    override val batteryLevel: Int
        get() {
            val level = UIDevice.currentDevice.batteryLevel
            return (level * 100).toInt().absoluteValue
        }

    override val timeZone: String
        get() = NSTimeZone.localTimeZone.name

    override val appStoreUrl: String
        get() = "https://apps.apple.com/us/app/krail-app/id6738934832"

    override fun toString() =
        "iOSAppInfo(type=$devicePlatformType, isDebug=$isDebug, appVersion=$appVersion, osVersion=$osVersion, " +
            "fontSize=$fontSize, isDarkTheme=$isDarkTheme, deviceModel=$deviceModel, " +
            "deviceManufacturer=$deviceManufacturer, locale=$locale, batteryLevel=$batteryLevel, " +
            "timeZone=$timeZone)"
}

class IosAppInfoProvider : AppInfoProvider {
    override suspend fun getAppInfo(): AppInfo {
        return IOSAppInfo()
    }
}

actual fun getAppPlatformType() = DevicePlatformType.IOS
