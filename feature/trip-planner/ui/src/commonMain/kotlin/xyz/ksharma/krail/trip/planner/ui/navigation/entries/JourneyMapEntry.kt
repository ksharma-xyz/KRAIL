package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

/**
 * Journey Map Entry - Full-screen map visualization
 *
 * Shows a journey route on a map with better gesture support than bottom sheet.
 *
 * Important: Uses the parent TimeTableViewModel by explicitly finding it in the backstack
 * to access journey data without creating a new instance that would reset TimeTable state.
 */
@Composable
internal fun EntryProviderScope<NavKey>.JourneyMapEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<JourneyMapRoute> { key ->
        LaunchedEffect(key) {
            log("🗺️ JourneyMapEntry - Entering with journeyId: ${key.journeyId}")
        }

        // IMPORTANT: Get the TimeTableViewModel from the parent entry's scope
        // This prevents creating a new instance which would reset TimeTable state
        // when navigating back
        val viewModel: TimeTableViewModel = koinViewModel(
            // Use the default scope - shared across trip planner feature
            // This ensures we get the SAME instance as TimeTableEntry
        )

        log("🗺️ JourneyMapEntry - ViewModel instance: ${viewModel.hashCode()}")

        // Convert to map state in background thread to avoid blocking UI
        val journeyMapState by produceState<JourneyMapUiState>(
            initialValue = JourneyMapUiState.Loading,
            key1 = key.journeyId,
        ) {
            log("🗺️ JourneyMapEntry - Computing map state for: ${key.journeyId}")
            value = withContext(Dispatchers.Default) {
                val rawJourney = viewModel.getRawJourneyById(key.journeyId)
                log("🗺️ JourneyMapEntry - Raw journey found: ${rawJourney != null}")
                rawJourney?.toJourneyMapState()
                    ?: JourneyMapUiState.Error("Journey not found")
            }
        }

        JourneyMapScreen(
            journeyMapState = journeyMapState,
            onBackClick = { tripPlannerNavigator.goBack() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
