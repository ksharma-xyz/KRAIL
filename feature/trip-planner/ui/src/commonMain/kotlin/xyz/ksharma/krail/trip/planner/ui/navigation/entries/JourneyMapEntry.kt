package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * Map state transformation is performed on background thread to avoid blocking UI.
 */
@Composable
internal fun EntryProviderScope<NavKey>.JourneyMapEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<JourneyMapRoute> { key ->
        // Get the TimeTableViewModel - it already has the journey data loaded
        val viewModel: TimeTableViewModel = koinViewModel()

        // Convert to map state in background thread to avoid blocking UI
        val journeyMapState by produceState<JourneyMapUiState>(
            initialValue = JourneyMapUiState.Loading,
            key1 = key.journeyId,
        ) {
            log("🗺️ JourneyMapEntry - Computing map state for: ${key.journeyId}")
            value = withContext(Dispatchers.Default) {
                val rawJourney = viewModel.getRawJourneyById(key.journeyId)
                log("🗺️ JourneyMapEntry - Raw journey found: ${rawJourney != null}")

                // Transform to map state - run on background thread
                rawJourney?.toJourneyMapState() ?: run {
                    // Fallback: This should never happen as journey was just displayed
                    log("🗺️ JourneyMapEntry - WARNING: Journey not found, using empty state")
                    JourneyMapUiState.Ready(
                        mapDisplay = JourneyMapDisplay(),
                        cameraFocus = null,
                    )
                }
            }
        }

        JourneyMapScreen(
            journeyMapState = journeyMapState,
            onBackClick = { tripPlannerNavigator.goBack() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
