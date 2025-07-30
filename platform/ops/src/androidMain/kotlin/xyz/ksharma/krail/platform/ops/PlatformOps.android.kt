package xyz.ksharma.krail.platform.ops

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import xyz.ksharma.krail.core.log.log

class AndroidPlatformOps(private val context: Context) : PlatformOps {

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
