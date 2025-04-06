package xyz.ksharma.krail.trip.planner.ui.settings

// androidMain
import android.content.Context
import android.content.Intent
import xyz.ksharma.krail.core.log.log

class AndroidSharer(private val context: Context) : Sharer {

    override fun shareText() {
        handleShareClick(context, Sharer.options.random())
    }
}

// In your androidMain implementation
fun handleShareClick(context: Context, text: String) {
    log("handleShareClick: $text")

//    startActivity(shareIntent)

    // Create and start the share intent
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val shareIntent = Intent.createChooser(intent, "Share via")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)

//    context.startActivity(Intent.createChooser(intent, "Share via"))
}
