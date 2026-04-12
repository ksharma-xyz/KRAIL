package xyz.ksharma.krail.departures.network.api.di

import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.departures.network.api.service.DeparturesService
import xyz.ksharma.krail.departures.network.api.service.RealDeparturesService
import xyz.ksharma.krail.departures.network.api.service.departuresHttpClient

val departuresNetworkModule = module {
    single {
        RealDeparturesService(
            httpClient = departuresHttpClient(get()),
            ioDispatcher = get(named(IODispatcher)),
        )
    } bind DeparturesService::class
}
