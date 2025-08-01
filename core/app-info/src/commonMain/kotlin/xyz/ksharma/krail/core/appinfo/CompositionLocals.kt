package xyz.ksharma.krail.core.appinfo

import androidx.compose.runtime.staticCompositionLocalOf
import xyz.ksharma.krail.core.log.logError

expect fun getAppPlatformType(): DevicePlatformType

val LocalAppInfo = staticCompositionLocalOf<AppInfo?> {
    logError("LocalAppInfo not provided, using null")
    null
}
