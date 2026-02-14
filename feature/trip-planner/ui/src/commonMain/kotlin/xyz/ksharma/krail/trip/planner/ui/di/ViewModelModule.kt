package xyz.ksharma.krail.trip.planner.ui.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.io.gtfs.GtfsQualifiers
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertsViewModel
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorViewModel
import xyz.ksharma.krail.trip.planner.ui.discover.DiscoverViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.InviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.savedtrips.RealInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealStopResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.NearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.settings.SettingsViewModel
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryViewModel
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

val viewModelsModule = module {
    viewModelOf(::SearchStopViewModel)
    viewModelOf(::ServiceAlertsViewModel)
    viewModelOf(::DateTimeSelectorViewModel)
    viewModelOf(::OurStoryViewModel)

    viewModel {
        SearchStopViewModel(
            analytics = get(),
            stopResultsManager = get(),
            nearbyStopsManager = get(),
            flag = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }

    viewModel {
        IntroViewModel(
            analytics = get(),
            platformOps = get(),
            preferences = get(),
            nswStopsManager = get(named(GtfsQualifiers.NSW_STOPS_MANAGER)),
            nswBusRoutesManager = get(named(GtfsQualifiers.NSW_BUS_ROUTES_MANAGER)),
        )
    }

    viewModel {
        SettingsViewModel(
            appInfoProvider = get(),
            analytics = get(),
            platformOps = get(),
        )
    }

    viewModel {
        SavedTripsViewModel(
            sandook = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
            nswParkRideFacilityManager = get(),
            parkRideService = get(),
            parkRideSandook = get(),
            stopResultsManager = get(),
            flag = get(),
            preferences = get(),
            platformOps = get(),
            infoTileManager = get(),
            inviteFriendsTileManager = get(),
        )
    }

    viewModel {
        TimeTableViewModel(
            tripPlanningService = get(),
            rateLimiter = get(),
            sandook = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
            festivalManager = get(),
        )
    }

    viewModel {
        ThemeSelectionViewModel(
            sandook = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
            preferences = get(),
        )
    }

    viewModel {
        DiscoverViewModel(
            discoverSydneyManager = get(),
            ioDispatcher = get(named(IODispatcher)),
            analytics = get(),
            platformOps = get(),
            appInfoProvider = get(),
            appCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }

    single<StopResultsManager> { RealStopResultsManager(get(), get(), get()) }

    single<NearbyStopsManager> {
        xyz.ksharma.krail.trip.planner.ui.searchstop.map.RealNearbyStopsManager(
            repository = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }

    single<InviteFriendsTileManager> { RealInviteFriendsTileManager(get()) }
}
