package xyz.ksharma.krail.sandook.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.RealNswParkRideSandook
import xyz.ksharma.krail.sandook.RealSandook
import xyz.ksharma.krail.sandook.RealSandookPreferences
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences

val sandookModule = module {
    includes(sqlDriverModule)
    singleOf(::RealSandookPreferences) { bind<SandookPreferences>() }

    single<NswParkRideSandook> {
        RealNswParkRideSandook(
            factory = get(),
            ioDispatcher = get(named(DispatchersComponent.IODispatcher)),
        )
    }

    single<Sandook> {
        RealSandook(
            factory = get(),
            ioDispatcher = get(named(DispatchersComponent.IODispatcher)),
        )
    }
}

expect val sqlDriverModule: Module
