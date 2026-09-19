package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver

/**
 * The one HTTP client in the app.
 *
 * Nothing here is platform-specific any more. The configuration lives in
 * [installKrailDefaults] in `commonMain`, and the only thing a platform supplies is
 * the engine, so the two platforms cannot drift apart in what plugins they run or in
 * what order they run them.
 *
 * Never add a second client. `docs/NETWORK_RELIABILITY.md` has the reasoning; a new
 * upstream that needs a credential adds an [ApiCredential] case, not a new client and
 * not a new `expect`/`actual` pair.
 */
fun baseHttpClient(
    appInfoProvider: AppInfoProvider,
    connectivity: ConnectivityObserver,
    onRetry: (endpoint: String, upstream: String) -> Unit,
): HttpClient = HttpClient(krailHttpClientEngine()) {
    installKrailDefaults(
        appInfoProvider = appInfoProvider,
        connectivity = connectivity,
        onRetry = onRetry,
    )
}
