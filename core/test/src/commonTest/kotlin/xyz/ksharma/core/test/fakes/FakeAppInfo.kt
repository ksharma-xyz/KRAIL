package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.core.appinfo.AppInfo
import xyz.ksharma.krail.core.appinfo.DevicePlatformType

class FakeAppInfo(
    override val devicePlatformType: DevicePlatformType = DevicePlatformType.ANDROID,
    override val isDebug: Boolean = false,
    override val appVersion: String = "1.0.0",
    override val appBuildNumber: String = "123",
    override val osVersion: String = "30",
    override val fontSize: String = "1.0",
    override val isDarkTheme: Boolean = false,
    override val deviceModel: String = "Pixel 4",
    override val deviceManufacturer: String = "Google",
    override val locale: String = "en_AU",
    override val timeZone: String = "Australia/Sydney",
    override val appStoreUrl: String = "https://play.google.com/store/apps/details?id=xyz.ksharma.krail"
) : AppInfo