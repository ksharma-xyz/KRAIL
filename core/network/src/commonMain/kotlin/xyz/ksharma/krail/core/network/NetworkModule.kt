package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.network.error.NetworkCaller

val coreNetworkModule = module {
    single<HttpClient> {
        baseHttpClient(
            appInfoProvider = get(),
            connectivity = get(),
        )
    }
    single {
        NetworkCaller(
            connectivity = get(),
            ioDispatcher = get(named(IODispatcher)),
            analytics = get(),
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
