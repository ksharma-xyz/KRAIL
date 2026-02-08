package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator

/**
 * Entry provider for Trip Planner feature.
 * Aggregates all trip planner navigation entries.
 *
 * Each entry is now in its own file for better maintainability and scalability.
 * Uses only TripPlannerNavigator interface - no direct Navigator dependency!
 */
@Composable
fun EntryProviderScope<NavKey>.TripPlannerEntries(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    SavedTripsEntry(tripPlannerNavigator)
    SearchStopEntry(tripPlannerNavigator)
    TimeTableEntry(tripPlannerNavigator)
    JourneyMapEntry(tripPlannerNavigator)
    ThemeSelectionEntry(tripPlannerNavigator)
    SettingsEntry(tripPlannerNavigator)
    OurStoryEntry(tripPlannerNavigator)
    IntroEntry(tripPlannerNavigator)
    DiscoverEntry(tripPlannerNavigator)
}
