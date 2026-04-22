package xyz.ksharma.krail

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import org.koin.android.ext.android.inject
import xyz.ksharma.krail.deeplink.AppDeepLinkHandler

class MainActivity : ComponentActivity() {

    private val deepLinkHandler: AppDeepLinkHandler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Store deep link for Splash to consume after the navigation graph is ready.
        deepLinkHandler.handleColdStart(intent?.data)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            KrailApp()
        }
    }

    // Called when the app is already running and a new intent arrives (deep link tap,
    // app-icon tap from Recent Apps, etc.). The navigation graph is already built at
    // this point, so we dispatch a hot event that KrailNavHost reacts to immediately.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkHandler.handleHotIntent(intent.data)
    }
}
