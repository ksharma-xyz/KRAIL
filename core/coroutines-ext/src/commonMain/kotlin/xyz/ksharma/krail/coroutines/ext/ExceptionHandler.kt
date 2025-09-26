package xyz.ksharma.krail.coroutines.ext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.log.logError

inline fun <reified T> CoroutineScope.launchWithExceptionHandler(
    dispatcher: CoroutineDispatcher,
    noinline errorBlock: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Unit,
) = launch(
    context = dispatcher + coroutineExceptionHandler(
        message = T::class.simpleName,
        errorBlock = errorBlock,
    ),
) {
    block()
}

fun coroutineExceptionHandler(
    message: String? = null,
    errorBlock: () -> Unit = {},
) = CoroutineExceptionHandler { _, throwable ->
    logError(message = message ?: throwable.message ?: "", throwable = throwable)
    errorBlock()
}
