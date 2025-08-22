package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.core.appinfo.AppInfo
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appinfo.DevicePlatformType

class FakeAppInfoProvider : AppInfoProvider {

    var mockAppInfo: AppInfo = FakeAppInfo(
        appVersion = "1.0.0",
        devicePlatformType = DevicePlatformType.ANDROID
    )

    override fun getAppInfo(): AppInfo = mockAppInfo
}