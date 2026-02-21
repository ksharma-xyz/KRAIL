package xyz.ksharma.krail.core.analytics

sealed class AnalyticsScreen(val name: String) {
    data object Settings : AnalyticsScreen(name = "Settings")
    data object SavedTrips : AnalyticsScreen(name = "SavedTrips")
    data object TimeTable : AnalyticsScreen(name = "TimeTable")
    data object PlanTrip : AnalyticsScreen(name = "PlanTrip")
    data object ThemeSelection : AnalyticsScreen(name = "ThemeSelection")
    data object SearchStop : AnalyticsScreen(name = "SearchStop")
    data object ServiceAlerts : AnalyticsScreen(name = "ServiceAlerts")
    data object Intro : AnalyticsScreen(name = "Intro")
    data object OurStory : AnalyticsScreen(name = "OurStory")

    // Map screens — used for ScreenViewEvent + BigQuery engagement_time_msec queries
    data object SearchStopMap : AnalyticsScreen(name = "SearchStopMap")
    data object JourneyMap : AnalyticsScreen(name = "JourneyMap")
}
