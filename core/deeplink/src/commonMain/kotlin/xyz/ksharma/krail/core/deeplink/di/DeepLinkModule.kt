package xyz.ksharma.krail.core.deeplink.di

import org.koin.dsl.module
import xyz.ksharma.krail.core.deeplink.DeepLinkManager
import xyz.ksharma.krail.core.deeplink.RealDeepLinkManager

val deepLinkModule = module {
    single<DeepLinkManager> { RealDeepLinkManager() }
}
