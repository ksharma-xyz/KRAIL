package xyz.ksharma.krail.trip.planner.ui.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.navigation.EntryBuilderDescriptor
import xyz.ksharma.krail.core.navigation.EntryBuilderQualifiers
import xyz.ksharma.krail.core.navigation.NavigatorBase
import xyz.ksharma.krail.trip.planner.ui.entries.TripPlannerEntries
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigatorImpl

/**
 * Koin module for Trip Planner feature navigation entries.
 *
 * Provides entry builders using the multibinding approach, allowing this feature
 * module to contribute its navigation entries to the app's navigation system.
 *
 * This module has NO dependency on the app module's Navigator class.
 * It only depends on NavigatorBase from core:navigation, avoiding circular dependencies.
 */
val tripPlannerNavigationModule = module {
    // Provide Trip Planner entry builder
    factory<EntryBuilderDescriptor>(qualifier = EntryBuilderQualifiers.TRIP_PLANNER) {
        EntryBuilderDescriptor(
            name = EntryBuilderQualifiers.Names.TRIP_PLANNER,
            builder = { navigator ->
                // Cast to NavigatorBase (from core:navigation)
                val baseNavigator = navigator as NavigatorBase
                // Wrap in feature-specific implementation
                val tripPlannerNavigator = TripPlannerNavigatorImpl(baseNavigator)
                // Provide entries
                TripPlannerEntries(tripPlannerNavigator)
            },
        )
    }
}
