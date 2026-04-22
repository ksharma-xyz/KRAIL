package xyz.ksharma.krail.feature.track.ui.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.feature.track.ui.TrackTripViewModel

val trackUiModule = module {
    viewModel { params ->
        TrackTripViewModel(
            encodedData = params.getOrNull<String>(),
            tripPlanningService = get(),
            trackingManager = get(),
            ioDispatcher = get(named(IODispatcher)),
            festivalManager = get(),
            gtfsRealtimeRepository = get(),
            sandook = get(),
        )
    }
}
