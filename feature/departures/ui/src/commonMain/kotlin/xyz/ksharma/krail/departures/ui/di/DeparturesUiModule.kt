package xyz.ksharma.krail.departures.ui.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.departures.ui.DepartureBoardConfig
import xyz.ksharma.krail.departures.ui.DepartureBoardRepository
import xyz.ksharma.krail.departures.ui.DeparturesViewModel

val departuresUiModule = module {
    // Defaults are used here. Override this single<DepartureBoardConfig> { ... } binding
    // in the app module when remote config values are available.
    single { DepartureBoardConfig() }
    single {
        DepartureBoardRepository(
            departuresService = get(),
            ioDispatcher = get(named(IODispatcher)),
            config = get(),
        )
    }
    viewModel {
        DeparturesViewModel(repository = get())
    }
}
