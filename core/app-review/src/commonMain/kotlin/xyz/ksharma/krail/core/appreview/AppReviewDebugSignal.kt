package xyz.ksharma.krail.core.appreview

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Debug-only proof channel for the review trigger.
 *
 * The real review sheet is unobservable: Play throttles it and a **sideloaded** debug build
 * cannot show it at all, so on a device there is no way to confirm the trigger fired at the
 * right moment. The requester emits the firing [DelightMoment] source here on every real
 * request. In debug builds a composable observes this and shows a stand-in sheet as proof; in
 * release builds nothing subscribes, so it is inert.
 *
 * This is a diagnostic, not the review itself. It never replaces the real platform call.
 */
interface AppReviewDebugSignal {

    /** Emits the analytics `source` of each fired review request. */
    val requests: SharedFlow<String>

    /** Records that a review request just fired for [source]. Never throws, never suspends. */
    fun signalRequested(source: String)
}

internal class RealAppReviewDebugSignal : AppReviewDebugSignal {

    // Buffered and drop-oldest so an emit with no active subscriber (every release build, and
    // debug before the observer mounts) never suspends or fails.
    private val _requests = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val requests: SharedFlow<String> = _requests.asSharedFlow()

    override fun signalRequested(source: String) {
        _requests.tryEmit(source)
    }
}
