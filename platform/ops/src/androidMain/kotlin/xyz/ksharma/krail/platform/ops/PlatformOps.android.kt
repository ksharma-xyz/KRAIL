package xyz.ksharma.krail.platform.ops

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError

internal class AndroidPlatformOps(private val context: Context) : PlatformOps {

    // "Tell your mates about KRAIL"
    override fun sharePlainText(text: String, title: String) {
        handleShareClick(context = context, text = text, mimeType = "text/plain", title = title)
    }

    override fun openUrl(url: String) {
        log("openUrl: $url")
        if (url.isBlank()) return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun openMapDirections(latitude: Double, longitude: Double, label: String) {
        log("openMapDirections: $latitude,$longitude")
        // `google.navigation:` would force Google Maps; the `geo:` scheme lets the system
        // offer whatever maps apps are installed, honouring the rider's default.
        val encodedLabel = label.encodeGeoLabel()
        val uri = "geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // No maps app at all is possible (bare emulators, stripped devices), and an unhandled
        // intent would otherwise crash the app.
        runCatching { context.startActivity(intent) }
            .onFailure { logError("No app available to handle map directions", it) }
    }

    /** Parentheses terminate the `geo:` label, so they cannot survive inside it. */
    private fun String.encodeGeoLabel(): String = replace("(", "").replace(")", "").trim()
}

// https://developer.android.com/training/sharing/send#why-to-use-system-sharesheet
private fun handleShareClick(context: Context, text: String, mimeType: String, title: String) {
    log("handleShareClick: $text")

    // Create and start the share intent
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = mimeType
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val shareIntent = Intent.createChooser(intent, title)
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
}
