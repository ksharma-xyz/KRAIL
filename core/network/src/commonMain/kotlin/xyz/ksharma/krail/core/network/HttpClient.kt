package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver

expect fun baseHttpClient(
    appInfoProvider: AppInfoProvider,
    connectivity: ConnectivityObserver,
): HttpClient
