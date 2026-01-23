package xyz.ksharma.krail.core.log

import co.touchlab.kermit.Logger

private const val MAX_TAG_LENGTH: Int = 23

actual fun log(message: String) {
    // TODO -  fix it
//    if (BuildConfig.DEBUG) {
    Logger.d(messageString = message, tag = getTag())
    //   }
}

actual fun logError(message: String, throwable: Throwable?) {
    // if (BuildConfig.DEBUG) {
    Logger.e(messageString = message, throwable = throwable, tag = getTag())
    // }
}

private fun getTag(): String {
    val stackTrace = Throwable().stackTrace

    val callerElement = stackTrace.firstOrNull { element ->
        element.className != "xyz.ksharma.krail.core.log.Log_androidKt" &&
            !element.className.startsWith("kotlin.") &&
            !element.className.startsWith("kotlinx.coroutines.") &&
            !element.methodName.contains("\$default")
    }
    val tag = runCatching { callerElement?.toStringTag() ?: "NULL_CALLER" }.getOrElse {
        println("Error while getting tag: $it")
        "ERROR_Unknown"
    }
    return tag
}

internal fun StackTraceElement.toStringTag(): String {
    val maxTagLength = MAX_TAG_LENGTH
    val callerClassName = className.substringAfterLast('.')
        .substringBefore('$')
    return callerClassName.take(maxTagLength)
}
