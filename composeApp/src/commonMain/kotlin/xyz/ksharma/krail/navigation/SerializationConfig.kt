package xyz.ksharma.krail.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import xyz.ksharma.krail.core.navigation.AppUpgradeRoute
import xyz.ksharma.krail.core.navigation.SplashRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.DateTimeSelectorRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.DiscoverRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.OurStoryRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.ServiceAlertRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.ThemeSelectionRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute

/**
 * Serialization config for multiplatform support (iOS, Web).
 * Required because reflection is not available on non-JVM platforms.
 * Read more- https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html#polymorphic-serialization-for-destination-keys
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
