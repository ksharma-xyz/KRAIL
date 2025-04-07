package xyz.ksharma.krail.platform.ops

import android.content.Context
import android.content.Intent
import xyz.ksharma.krail.core.log.log

class AndroidContentSharing(private val context: Context) : ContentSharing {

    override fun sharePlainText(text: String) {
        handleShareClick(context = context, text = text, mimeType = "text/plain")
    }
}

// https://developer.android.com/training/sharing/send#why-to-use-system-sharesheet
fun handleShareClick(context: Context, text: String, mimeType: String) {
    log("handleShareClick: $text")

    // Create and start the share intent
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = mimeType
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val shareIntent = Intent.createChooser(intent, "Tell your mates about KRAIL")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
}
