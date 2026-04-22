package xyz.ksharma.krail.core.deeplink

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class RealPendingDeepLinkManager : PendingDeepLinkManager {

    private var pending: String? = null

    private val _hotEvents = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val hotEvents: SharedFlow<String> = _hotEvents.asSharedFlow()

    override fun setPending(encodedData: String) {
        pending = encodedData
    }

    override fun dispatchHot(encodedData: String) {
        pending = encodedData
        _hotEvents.tryEmit(encodedData)
    }

    override fun consumePending(): String? = pending.also { pending = null }

    override fun hasPending(): Boolean = pending != null
}
