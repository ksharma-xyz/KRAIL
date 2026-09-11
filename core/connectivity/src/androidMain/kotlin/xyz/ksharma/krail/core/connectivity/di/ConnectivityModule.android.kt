package xyz.ksharma.krail.core.connectivity.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.ksharma.krail.core.connectivity.AndroidConnectivityObserver
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver

actual val connectivityModule = module {
    // createdAtStart, because a lazily-created observer is a broken observer: it
    // would be constructed at the moment of the first failed request and report
    // Unknown for exactly the classification that needed it. The OS registration
    // has to be live before anything can fail.
    single<ConnectivityObserver>(createdAtStart = true) {
        AndroidConnectivityObserver(
            context = androidContext(),
            // App-scoped and never cancelled, matching the other always-on
            // singletons (analytics, remote config, app start). The observer's
            // StateFlow is shared Eagerly, so this scope holds the OS
            // registration for the life of the process.
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
}
