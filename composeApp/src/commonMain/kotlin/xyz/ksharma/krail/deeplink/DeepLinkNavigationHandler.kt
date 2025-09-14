package xyz.ksharma.krail.deeplink

import androidx.navigation.NavHostController
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.ksharma.krail.core.deeplink.DeepLinkEvent
import xyz.ksharma.krail.core.deeplink.DeepLinkManager
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute

/**
 * Handles navigation for deep link events.
 * This keeps navigation logic separate from the main app composables.
 */
class DeepLinkNavigationHandler(
    private val navController: NavHostController
) : KoinComponent {

    private val deepLinkManager: DeepLinkManager by inject()

    /**
     * Handle a deep link navigation event
     */
    fun handleNavigationEvent(event: DeepLinkEvent) {
        log("DeepLinkNavigationHandler: Handling navigation event: $event")

        when (event) {
            is DeepLinkEvent.NavigateToTimeTable -> {
                navigateToTimeTable(event)
            }
            // Add more navigation handlers here as needed
        }
    }

    private fun navigateToTimeTable(event: DeepLinkEvent.NavigateToTimeTable) {
        val route = TimeTableRoute(
            fromStopId = event.fromStopId,
            fromStopName = event.fromStopName,
            toStopId = event.toStopId,
            toStopName = event.toStopName
        )

        log("DeepLinkNavigationHandler: Navigating to TimeTable with route: $route")

        try {
            navController.navigate(route)
            log("DeepLinkNavigationHandler: Navigation successful")

            // Notify that we've arrived at the deep-linked screen
            val routeKey = "TimeTable_${event.fromStopId}_${event.toStopId}"
            deepLinkManager.onArrivedAtDeepLinkedScreen(routeKey)
        } catch (e: Exception) {
            log("DeepLinkNavigationHandler: Navigation failed: ${e.message}")
        }
    }
}