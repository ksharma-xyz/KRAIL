package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.journeymap.JourneyMapScreen
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapMapper.toJourneyMapState
import xyz.ksharma.krail.trip.planner.ui.navigation.JourneyMapRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

/**
 * Journey Map Entry - Full-screen map visualization
 *
 * Shows a journey route on a map with better gesture support than bottom sheet.
 *
 * Important: Uses the parent TimeTableViewModel to access journey data.
 * The journey is already loaded in memory from TimeTable screen, so no API call is needed.
 */
@Composable
internal fun EntryProviderScope<NavKey>.JourneyMapEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<JourneyMapRoute> { key ->
        LaunchedEffect(key) {
            log("🗺️ JourneyMapEntry - Entering with journeyId: ${key.journeyId}")
        }

        // Get the TimeTableViewModel - it already has the journey data loaded
        val viewModel: TimeTableViewModel = koinViewModel()

        log("🗺️ JourneyMapEntry - ViewModel instance: ${viewModel.hashCode()}")

        // Journey data is already in memory - transform it directly
        // No need for Loading/Error states since this is instant and can't fail
        val journeyMapState = remember(key.journeyId) {
            log("🗺️ JourneyMapEntry - Transforming journey for: ${key.journeyId}")
            val rawJourney = viewModel.getRawJourneyById(key.journeyId)
            log("🗺️ JourneyMapEntry - Raw journey found: ${rawJourney != null}")

            // Transform to map state - this is a fast, synchronous operation
            rawJourney?.toJourneyMapState()
                ?: run {
                    // This should never happen - journey was just displayed on previous screen
                    log("🗺️ JourneyMapEntry - ERROR: Journey not found in ViewModel!")
                    // Return empty map state as fallback
                    JourneyMapUiState.Ready(
                        mapDisplay = JourneyMapDisplay(),
                        cameraFocus = null,
                    )
                }
        }

        JourneyMapScreen(
            journeyMapState = journeyMapState,
            onBackClick = { tripPlannerNavigator.goBack() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
