package xyz.ksharma.krail.core.deeplink

import kotlinx.coroutines.flow.SharedFlow

/**
 * Interface for managing deep link navigation events.
 * This allows feature modules to interact with deep link functionality
 * without depending on the concrete implementation.
 */
interface DeepLinkManager {

    /**
     * Flow of deep link navigation events
     */
    val deepLinkEvents: SharedFlow<DeepLinkEvent>

    /**
     * Process a deep link URL and emit navigation event if valid
     */
    fun handleDeepLink(url: String)

    /**
     * Called by navigation system when it's ready to receive events
     */
    fun onNavigationReady()

    /**
     * Called when user navigates away from a deep-linked screen
     */
    fun onNavigatedAwayFromDeepLinkedScreen(routeKey: String)

    /**
     * Called when user arrives at a deep-linked screen
     */
    fun onArrivedAtDeepLinkedScreen(routeKey: String)

    /**
     * Clear all processed state
     */
    fun clearAllProcessedState()
}

/**
 * Sealed class representing different deep link navigation events
 */
sealed class DeepLinkEvent {
    data class NavigateToTimeTable(
        val fromStopId: String,
        val fromStopName: String,
        val toStopId: String,
        val toStopName: String
    ) : DeepLinkEvent()

    // Add more deep link types here as needed
    // data class NavigateToStopInfo(val stopId: String) : DeepLinkEvent()
    // data class NavigateToRoute(val routeId: String) : DeepLinkEvent()
}
