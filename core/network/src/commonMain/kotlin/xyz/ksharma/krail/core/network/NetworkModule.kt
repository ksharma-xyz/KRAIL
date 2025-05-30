package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val coreNetworkModule = module {
    single<HttpClient> {
        baseHttpClient(
            appInfoProvider = get(),
            coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
}
