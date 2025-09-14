package xyz.ksharma.krail.core.deeplink

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xyz.ksharma.krail.core.log.log

/**
 * Concrete implementation of DeepLinkManager.
 * This handles deep link navigation throughout the app lifecycle.
 */
internal class RealDeepLinkManager : DeepLinkManager {

    private val _deepLinkEvents = MutableSharedFlow<DeepLinkEvent>(
        replay = 0, // Back to 0 - we'll handle cold start differently
        extraBufferCapacity = 1
    )
    override val deepLinkEvents: SharedFlow<DeepLinkEvent> = _deepLinkEvents.asSharedFlow()

    private var lastProcessedUrl: String? = null
    private var currentDeepLinkedRoute: String? = null

    // Store pending deep link for cold start scenario
    private var pendingDeepLinkEvent: DeepLinkEvent? = null
    private var hasNavigationStarted = false

    /**
     * Process a deep link URL and emit navigation event if valid.
     * Handles both cold start and hot start scenarios properly.
     */
    override fun handleDeepLink(url: String) {
        log("RealDeepLinkManager: Processing deep link: $url")
        log("RealDeepLinkManager: Last processed URL: $lastProcessedUrl")
        log("RealDeepLinkManager: Current deep-linked route: $currentDeepLinkedRoute")
        log("RealDeepLinkManager: Has navigation started: $hasNavigationStarted")

        // 1. SECURITY: Validate and sanitize the deep link first
        val validationResult = DeepLinkValidator.validateDeepLink(url)
        if (!validationResult.isValid) {
            log("RealDeepLinkManager: Deep link validation failed: ${(validationResult as ValidationResult.Invalid).reason}")
            // For security, we don't process invalid deep links at all
            return
        }

        // 2. Sanitize the URL after validation
        val sanitizedUrl = DeepLinkValidator.sanitizeUrl(url)
        log("RealDeepLinkManager: Deep link validated and sanitized: $sanitizedUrl")

        // 3. Parse the validated URL
        val parsedData = DeepLinkHandler.parseDeepLink(sanitizedUrl)
        if (parsedData == null) {
            log("RealDeepLinkManager: Failed to parse validated deep link: $sanitizedUrl")
            return
        }

        log("RealDeepLinkManager: Successfully parsed deep link: $parsedData")

        when (parsedData["type"]) {
            "TimeTableRoute" -> {
                val routeKey = "TimeTable_${parsedData["fromStopId"]}_${parsedData["toStopId"]}"

                val event = DeepLinkEvent.NavigateToTimeTable(
                    fromStopId = parsedData["fromStopId"] ?: "",
                    fromStopName = parsedData["fromStopName"] ?: "",
                    toStopId = parsedData["toStopId"] ?: "",
                    toStopName = parsedData["toStopName"] ?: ""
                )

                if (hasNavigationStarted) {
                    // Hot start - navigation is ready, emit immediately
                    log("RealDeepLinkManager: Hot start - emitting event immediately: $event")
                    _deepLinkEvents.tryEmit(event)
                } else {
                    // Cold start - store for later when navigation is ready
                    log("RealDeepLinkManager: Cold start - storing pending event: $event")
                    pendingDeepLinkEvent = event
                }

                // Update state after processing (use original URL for tracking)
                lastProcessedUrl = url
                currentDeepLinkedRoute = routeKey
            }
            else -> {
                log("RealDeepLinkManager: Unknown deep link type: ${parsedData["type"]}")
            }
        }
    }

    /**
     * Called by navigation system when it's ready to receive events.
     * Emits any pending deep link event from cold start.
     */
    override fun onNavigationReady() {
        log("RealDeepLinkManager: Navigation system is ready")
        hasNavigationStarted = true

        pendingDeepLinkEvent?.let { event ->
            log("RealDeepLinkManager: Emitting pending deep link event: $event")
            _deepLinkEvents.tryEmit(event)
            pendingDeepLinkEvent = null // Clear after emitting
        }
    }

    /**
     * Called when user navigates away from a deep-linked screen.
     * This allows the same deep link to be processed again.
     */
    override fun onNavigatedAwayFromDeepLinkedScreen(routeKey: String) {
        log("DeepLinkManagerImpl: User navigated away from deep-linked screen: $routeKey")

        if (currentDeepLinkedRoute == routeKey) {
            log("DeepLinkManagerImpl: Clearing current deep-linked route and allowing re-processing")
            currentDeepLinkedRoute = null
            // Keep lastProcessedUrl but clear the route tracking
        }
    }

    /**
     * Called when user arrives at a deep-linked screen to track current state
     */
    override fun onArrivedAtDeepLinkedScreen(routeKey: String) {
        log("DeepLinkManagerImpl: User arrived at deep-linked screen: $routeKey")
        currentDeepLinkedRoute = routeKey
    }

    /**
     * Clear all processed state - useful for testing or edge cases
     */
    override fun clearAllProcessedState() {
        log("DeepLinkManagerImpl: Clearing all processed state")
        lastProcessedUrl = null
        currentDeepLinkedRoute = null
    }
}