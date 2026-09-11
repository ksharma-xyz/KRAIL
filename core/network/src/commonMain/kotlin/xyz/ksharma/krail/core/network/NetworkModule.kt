package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.network.error.NetworkCaller

val coreNetworkModule = module {
    single<HttpClient> {
        // NetworkCaller is resolved inside the lambda, not captured here. The retry hook
        // fires from inside the Ktor pipeline long after construction, and resolving it
        // eagerly would make the client and the caller depend on each other at build time.
        val networkCaller: () -> NetworkCaller = { get() }
        baseHttpClient(
            appInfoProvider = get(),
            connectivity = get(),
            onRetry = { endpoint, upstream ->
                networkCaller().recordRetry(endpoint = endpoint, upstream = upstream)
            },
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
