package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import org.koin.dsl.module
import xyz.ksharma.krail.core.appinfo.AppInfoProvider

val coreNetworkModule = module {
    single<HttpClient> {
        baseHttpClient(
            appInfoProvider = get(),
        )
    }

    single<ApiEnvironmentProvider> {
        val appInfo = get<AppInfoProvider>()
        if (appInfo.getAppInfo().isDebug) {
            DebugApiEnvironmentProvider(sandookPreferences = get())
        } else {
            ProductionApiEnvironmentProvider()
        }
    }
}
