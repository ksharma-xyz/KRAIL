package xyz.ksharma.krail.analytics

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.navigation.AddParkRideRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.JourneyMapRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.ManageStopLabelsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.OurStoryRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.ThemeSelectionRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute

/** Reported when a route has no [AnalyticsScreen] counterpart, rather than inventing one. */
const val UNMAPPED_SCREEN_NAME = "Unmapped"

/**
 * Maps a navigation route to the [AnalyticsScreen] vocabulary already used by
 * `view_screen`, so window transitions can be joined to screen views without a second set
 * of screen names to reconcile.
 *
 * Routes with no [AnalyticsScreen] (Splash, Discover, TrackTrip) report
 * [UNMAPPED_SCREEN_NAME]. Minting names here instead would put strings into the analytics
 * vocabulary that no other event uses, which is worse than one honest bucket.
 */
fun NavKey.toAnalyticsScreenName(): String = when (this) {
    is SavedTripsRoute -> AnalyticsScreen.SavedTrips.name
    is TimeTableRoute -> AnalyticsScreen.TimeTable.name
    is SearchStopRoute -> AnalyticsScreen.SearchStop.name
    is JourneyMapRoute -> AnalyticsScreen.JourneyMap.name
    is ThemeSelectionRoute -> AnalyticsScreen.ThemeSelection.name
    is SettingsRoute -> AnalyticsScreen.Settings.name
    is OurStoryRoute -> AnalyticsScreen.OurStory.name
    is IntroRoute -> AnalyticsScreen.Intro.name
    is ManageStopLabelsRoute -> AnalyticsScreen.ManageStopLabels.name
    is AddParkRideRoute -> AnalyticsScreen.AddParkRide.name
    else -> {
        // Logged, not sent: a route reaching this branch is either genuinely screenless
        // (Splash) or a gap in the mapping, and the two are indistinguishable in the data.
        log("[DEVICE_WINDOW] no AnalyticsScreen for route ${this::class.simpleName}")
        UNMAPPED_SCREEN_NAME
    }
}
