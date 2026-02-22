package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
actual fun StatusBarAppearanceEffect(lightStatusBar: Boolean) {
    val isDark = isSystemInDarkTheme()
    val activity: ComponentActivity? = LocalActivity.current as? ComponentActivity
    DisposableEffect(lightStatusBar, isDark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (lightStatusBar) {
                // Map is always light — force dark icons so they're readable
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            } else {
                // Back to list/themed background — follow system dark/light mode
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
            },
        )
        onDispose {
            // Restore system-default appearance when leaving the screen
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
            )
        }
    }
}
