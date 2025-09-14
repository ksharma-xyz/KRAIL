package xyz.ksharma.krail

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import xyz.ksharma.krail.core.deeplink.DeepLinkManager
import xyz.ksharma.krail.core.log.log

class MainActivity : ComponentActivity() {

    private val deepLinkManager: DeepLinkManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Handle initial deep link
        handleIntent(intent)

        setContent {
            KrailApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle new deep link when app is already running
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.toString()?.let { deepLink ->
            log("MainActivity: Processing deep link: $deepLink")
            deepLinkManager.handleDeepLink(deepLink)
        }
    }
}
