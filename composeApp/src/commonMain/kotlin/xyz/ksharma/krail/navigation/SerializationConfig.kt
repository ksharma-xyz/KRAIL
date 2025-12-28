package xyz.ksharma.krail.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import xyz.ksharma.krail.trip.planner.ui.entries.DateTimeSelectorRoute
import xyz.ksharma.krail.trip.planner.ui.entries.DiscoverRoute
import xyz.ksharma.krail.trip.planner.ui.entries.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.entries.OurStoryRoute
import xyz.ksharma.krail.trip.planner.ui.entries.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.entries.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.entries.ServiceAlertRoute
import xyz.ksharma.krail.trip.planner.ui.entries.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.entries.ThemeSelectionRoute
import xyz.ksharma.krail.trip.planner.ui.entries.TimeTableRoute

/**
 * Serialization config for multiplatform support (iOS, Web).
 * Required because reflection is not available on non-JVM platforms.
 */
val krailNavSerializationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            // App routes
            subclass(SplashRoute::class, SplashRoute.serializer())
            subclass(AppUpgradeRoute::class, AppUpgradeRoute.serializer())

            // Trip Planner routes
            subclass(SavedTripsRoute::class, SavedTripsRoute.serializer())
            subclass(SearchStopRoute::class, SearchStopRoute.serializer())
            subclass(TimeTableRoute::class, TimeTableRoute.serializer())
            subclass(ThemeSelectionRoute::class, ThemeSelectionRoute.serializer())
            subclass(ServiceAlertRoute::class, ServiceAlertRoute.serializer())
            subclass(SettingsRoute::class, SettingsRoute.serializer())
            subclass(DateTimeSelectorRoute::class, DateTimeSelectorRoute.serializer())
            subclass(OurStoryRoute::class, OurStoryRoute.serializer())
            subclass(IntroRoute::class, IntroRoute.serializer())
            subclass(DiscoverRoute::class, DiscoverRoute.serializer())
        }
    }
}
