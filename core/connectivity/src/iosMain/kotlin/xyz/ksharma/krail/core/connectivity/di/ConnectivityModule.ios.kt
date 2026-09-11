package xyz.ksharma.krail.core.connectivity.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver
import xyz.ksharma.krail.core.connectivity.IosConnectivityObserver

actual val connectivityModule = module {
    // createdAtStart — see the Android module for why a lazy observer is useless.
    single<ConnectivityObserver>(createdAtStart = true) {
        IosConnectivityObserver(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
}
