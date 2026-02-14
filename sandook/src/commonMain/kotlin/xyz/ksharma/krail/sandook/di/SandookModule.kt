package xyz.ksharma.krail.sandook.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent
import xyz.ksharma.krail.sandook.DiscoverCardSeenPreferences
import xyz.ksharma.krail.sandook.KrailSandook
import xyz.ksharma.krail.sandook.NswBusRoutesSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswStopsSandook
import xyz.ksharma.krail.sandook.RealDiscoverCardSeenPreferences
import xyz.ksharma.krail.sandook.RealNswBusRoutesSandook
import xyz.ksharma.krail.sandook.RealNswParkRideSandook
import xyz.ksharma.krail.sandook.RealNswStopsSandook
import xyz.ksharma.krail.sandook.RealSandook
import xyz.ksharma.krail.sandook.RealSandookPreferences
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookDriverFactory
import xyz.ksharma.krail.sandook.SandookPreferences

val sandookModule = module {
    includes(sqlDriverModule)
    // Provide the database driver factory
    single<SandookDriverFactory> { get() }

    // Create a single shared database instance
    single {
        val driver = get<SandookDriverFactory>().createDriver()
        KrailSandook(driver)
    }

    // Expose query objects from the database
    single { get<KrailSandook>().nswParkRideQueries }

    // Expose query objects from the database
    single { get<KrailSandook>().appPreferencesQueries }

    // Add the missing SandookPreferences dependency
    single<SandookPreferences> { RealSandookPreferences(get()) }

    /**
     * Important: Expose the queries otherwise they won't be available for injection
     * E.g. If you wanna inject it in RealDiscoverCardSeenPreferences, then you need to expose
     *      it here. Otherwise it will not be available for injection and Koin will throw an error.
     */
    single { get<KrailSandook>().discoverCardQueries }

    // get for [DiscoverCardQueries] will only work, if we have exposed it to Koin separately.
    single<DiscoverCardSeenPreferences> {
        RealDiscoverCardSeenPreferences(get())
    }

    // Provide repositories
    single<NswParkRideSandook> {
        RealNswParkRideSandook(
            parkRideQueries = get(),
            ioDispatcher = get(named(DispatchersComponent.IODispatcher)),
        )
    }

    single<NswBusRoutesSandook> {
        RealNswBusRoutesSandook(
            factory = get(),
        )
    }

    single<NswStopsSandook> {
        RealNswStopsSandook(
            factory = get(),
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
