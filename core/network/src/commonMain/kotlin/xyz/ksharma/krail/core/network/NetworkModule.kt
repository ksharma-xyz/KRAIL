package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import org.koin.dsl.module

val coreNetworkModule = module {
    single<HttpClient> {
        baseHttpClient(
            appInfoProvider = get(),
        )
    }
    single {
        BffEndpointResolver(
            appInfoProvider = get(),
            flag = get(),
            debugStore = get(),
        )
    }
}
