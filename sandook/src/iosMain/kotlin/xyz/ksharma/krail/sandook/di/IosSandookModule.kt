package xyz.ksharma.krail.sandook.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.krail.sandook.IosSandookDriverFactory
import xyz.ksharma.krail.sandook.SandookDriverFactory

actual val sqlDriverModule = module {
    // RealSandook (and the other repos) are bound in the shared sandookModule and consume the
    // single shared KrailSandook, so they all share one driver / connection pool. Do not create
    // additional drivers here: separate pools over the same db file race for write locks and
    // throw SQLITE_BUSY (this caused the iOS theme-selection crash during first-launch GTFS imports).
    singleOf(::IosSandookDriverFactory) { bind<SandookDriverFactory>() }
}
