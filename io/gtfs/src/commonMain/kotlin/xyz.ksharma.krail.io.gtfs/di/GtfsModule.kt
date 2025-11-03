package xyz.ksharma.krail.io.gtfs.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.io.gtfs.nswstops.NswStopsManager
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager

val gtfsModule = module {
    single<StopsManager> {
        NswStopsManager(
            ioDispatcher = get(named(IODispatcher)),
            sandook = get(),
            preferences = get(),
            savedTripValidator = get(),
        )
    }
}
