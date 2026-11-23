package xyz.ksharma.krail.core.analytics

sealed class AnalyticsScreen(val name: String) {
    data object Settings : AnalyticsScreen(name = "Settings")
    data object SavedTrips : AnalyticsScreen(name = "SavedTrips")
    data object TimeTable : AnalyticsScreen(name = "TimeTable")

    // Shown when the timetable load fails with no data to display (full error screen).
    data object TimeTableError : AnalyticsScreen(name = "TimeTableError")
    data object PlanTrip : AnalyticsScreen(name = "PlanTrip")
    data object ThemeSelection : AnalyticsScreen(name = "ThemeSelection")
    data object SearchStop : AnalyticsScreen(name = "SearchStop")
    data object ManageStopLabels : AnalyticsScreen(name = "ManageStopLabels")
    data object AddParkRide : AnalyticsScreen(name = "AddParkRide")
    data object ServiceAlerts : AnalyticsScreen(name = "ServiceAlerts")

    // The first Intro and the two step Intro's names, which older builds send and the ledger records. The rebuilt
    // Intro sends one name for each of its five screens, below, and sends neither of these.
    data object Intro : AnalyticsScreen(name = "Intro")
    data object IntroFirstTrip : AnalyticsScreen(name = "IntroFirstTrip")

    // The five screens of the rebuilt Intro, in the order a rider sees them. Each is sent once, the first time it is
    // on screen in a run.
    data object IntroParkRide : AnalyticsScreen(name = "IntroParkRide")
    data object IntroDestination : AnalyticsScreen(name = "IntroDestination")
    data object IntroOrigin : AnalyticsScreen(name = "IntroOrigin")
    data object IntroConfirm : AnalyticsScreen(name = "IntroConfirm")
    data object IntroTheme : AnalyticsScreen(name = "IntroTheme")
    data object OurStory : AnalyticsScreen(name = "OurStory")

    // Map screens — used for ScreenViewEvent + BigQuery engagement_time_msec queries
    data object SearchStopMap : AnalyticsScreen(name = "SearchStopMap")
    data object JourneyMap : AnalyticsScreen(name = "JourneyMap")
}
