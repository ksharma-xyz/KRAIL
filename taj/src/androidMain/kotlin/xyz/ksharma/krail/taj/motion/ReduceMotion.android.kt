package xyz.ksharma.krail.taj.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Android has no single reduce-motion switch. "Remove animations" in accessibility settings, and
// the developer option of the same name, both set the animator duration scale to zero.
@Composable
actual fun isReduceMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
