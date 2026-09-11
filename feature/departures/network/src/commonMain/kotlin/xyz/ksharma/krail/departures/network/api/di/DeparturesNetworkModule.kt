package xyz.ksharma.krail.departures.network.api.di

import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.network.ApiCredential
import xyz.ksharma.krail.core.network.forApi
import xyz.ksharma.krail.departures.network.api.service.DeparturesService
import xyz.ksharma.krail.departures.network.api.service.RealDeparturesService

val departuresNetworkModule = module {
    single {
        RealDeparturesService(
            httpClient = get<HttpClient>().forApi(ApiCredential.NswApiKey),
            ioDispatcher = get(named(IODispatcher)),
            resolver = get(),
        )
    } bind DeparturesService::class
}
