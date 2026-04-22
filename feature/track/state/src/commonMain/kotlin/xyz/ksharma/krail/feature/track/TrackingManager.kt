package xyz.ksharma.krail.feature.track

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory singleton. Lives as long as the app process — kill app = tracking stops. */
class TrackingManager {

    private val _tracked = MutableStateFlow<TrackedJourney?>(null)
    val tracked: StateFlow<TrackedJourney?> = _tracked.asStateFlow()

    fun start(deepLink: TripDeepLink) {
        _tracked.value = TrackedJourney(deepLink = deepLink)
    }

    fun update(display: TrackedJourneyDisplay) {
        _tracked.update { current ->
            current?.copy(display = display)
        }
    }

    fun markArrived() {
        _tracked.update { current ->
            current?.copy(isArrived = true)
        }
    }

    fun stop() {
        _tracked.value = null
    }

    fun isTracking(deepLink: TripDeepLink): Boolean =
        _tracked.value?.deepLink == deepLink
}
