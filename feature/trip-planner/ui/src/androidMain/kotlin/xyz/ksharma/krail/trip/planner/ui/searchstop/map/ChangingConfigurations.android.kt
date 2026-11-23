package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `Activity.isChangingConfigurations`, which the framework documents for exactly this use in `onStop`: it is true
 * while the Activity is being destroyed to be recreated with a new configuration. It is read when asked, at stop
 * time, never remembered.
 */
@Composable
internal actual fun rememberIsChangingConfigurations(): () -> Boolean {
    val context = LocalContext.current
    return remember(context) { { context.findActivity()?.isChangingConfigurations == true } }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
