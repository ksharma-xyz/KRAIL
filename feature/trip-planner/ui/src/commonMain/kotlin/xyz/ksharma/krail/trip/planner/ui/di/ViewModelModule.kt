package xyz.ksharma.krail.trip.planner.ui.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertsViewModel
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealStopResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.settings.SettingsViewModel
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryViewModel
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

val viewModelsModule = module {
    viewModelOf(::SearchStopViewModel)
    viewModelOf(::ServiceAlertsViewModel)
    viewModelOf(::DateTimeSelectorViewModel)
    viewModelOf(::IntroViewModel)
    viewModelOf(::OurStoryViewModel)

    viewModel {
        SettingsViewModel(
            appInfoProvider = get(),
            analytics = get(),
            share = get(),
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
        )
    }

    viewModel {
        TimeTableViewModel(
            tripPlanningService = get(),
            rateLimiter = get(),
            sandook = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }

    viewModel {
        ThemeSelectionViewModel(
            sandook = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }

    single<StopResultsManager> { RealStopResultsManager(get(), get(), get()) }
}
