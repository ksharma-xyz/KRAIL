package xyz.ksharma.krail.core.log

import co.touchlab.kermit.Logger
import platform.Foundation.NSLog
import kotlin.experimental.ExperimentalNativeApi
@OptIn(ExperimentalNativeApi::class)
actual fun log(message: String) {
    if (Platform.isDebugBinary) {
        Logger.d(messageString = message)
    }
}

@OptIn(ExperimentalNativeApi::class)
actual fun logError(message: String, throwable: Throwable?) {
    if (!Platform.isDebugBinary) return
    if (throwable != null) {
        Logger.e(throwable = throwable, messageString = message + " CAUSE ${throwable.cause}")
        NSLog("ERROR: $message. Throwable: $throwable CAUSE ${throwable.cause}")
    } else {
        Logger.e(messageString = message)
    }
}
