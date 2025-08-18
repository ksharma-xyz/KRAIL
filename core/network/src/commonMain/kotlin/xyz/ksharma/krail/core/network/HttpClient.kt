package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import xyz.ksharma.krail.core.appinfo.AppInfoProvider

expect fun baseHttpClient(
    appInfoProvider: AppInfoProvider,
): HttpClient
