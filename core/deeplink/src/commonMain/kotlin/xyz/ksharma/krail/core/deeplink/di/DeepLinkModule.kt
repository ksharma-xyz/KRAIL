package xyz.ksharma.krail.core.deeplink.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.deeplink.PendingDeepLinkManager
import xyz.ksharma.krail.core.deeplink.RealPendingDeepLinkManager

val deepLinkModule = module {
    single<PendingDeepLinkManager> { RealPendingDeepLinkManager() }
}
