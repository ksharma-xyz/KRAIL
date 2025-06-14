package xyz.ksharma.krail.sandook.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent
import xyz.ksharma.krail.sandook.IosSandookDriverFactory
import xyz.ksharma.krail.sandook.RealSandook
import xyz.ksharma.krail.sandook.SandookDriverFactory

actual val sqlDriverModule = module {
    singleOf(::IosSandookDriverFactory) { bind<SandookDriverFactory>() }

    single<RealSandook> {
        RealSandook(
            factory = IosSandookDriverFactory(),
            ioDispatcher = get(named(DispatchersComponent.IODispatcher)),
        )
    }
}
