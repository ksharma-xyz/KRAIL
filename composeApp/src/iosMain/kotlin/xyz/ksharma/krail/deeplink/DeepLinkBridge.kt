package xyz.ksharma.krail.deeplink

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.ksharma.krail.core.deeplink.DeepLinkManager
import xyz.ksharma.krail.core.log.log

/**
 * Bridge function for iOS to handle deep links through the KMP DeepLinkManager
 */
fun handleDeepLinkFromIos(url: String) {
    log("iOS Bridge: Received deep link: $url")

    // Get the DeepLinkManager from Koin
    val deepLinkManager = object : KoinComponent {
        val manager: DeepLinkManager by inject()
    }.manager

    deepLinkManager.handleDeepLink(url)
}
